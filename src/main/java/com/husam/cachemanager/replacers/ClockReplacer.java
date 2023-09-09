package com.husam.cachemanager.replacers;

import com.husam.common.DatabaseConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * clock replacement algorithm
 * that uses coarse grain locking, stability and predictability in DBMS is the most important
 * a clock hand traverse (sweeps) the circular list and gives second chance to each entry
 * when the entry get used it will mark it as used, so the hand will re-set it again
 * at first all frames will be in the replacer, and to use a new frame you have to evict
 * which will take o(1) at first till the replacer is full
 */
public class ClockReplacer implements Replacer {

    private static final Logger LOGGER = LogManager.getLogger(ClockReplacer.class);
    private static class Entry {
        boolean isPinned;
        boolean useBit;

        Entry(boolean isPinned) {
            this.useBit = isPinned;
            this.isPinned = isPinned;
        }
    }
    // we use array of objects to avoid allocating and deallocating objects in each pin and unpin
    private final Entry[] clock;
    private int size;
    private final int numOfFrames;
    private int hand;
    private final Lock latch;

    public ClockReplacer(int numOfFrames) {
        this.clock = new Entry[numOfFrames];
        this.size = numOfFrames;
        this.numOfFrames = numOfFrames;
        this.hand = 0;
        this.latch = new ReentrantLock();
        for(int i = 0; i < numOfFrames; i++) {
            clock[i] = new Entry(false);
        }
    }

    @Override
    public int victim() {
        latch.lock();
        int chances = 0;
        try {
            while (true) {
                chances++;
                if(chances == size * 2) {
                    return DatabaseConfig.getInstance().getInvalidFrameId();
                }
                hand = (hand + 1 % size);
                Entry entry = clock[hand];
                if (entry.isPinned){
                    continue;
                }
                if(entry.useBit) {
                    entry.useBit = false;
                    continue;
                }
                return hand;
            }
        } finally {
            latch.unlock();
        }
    }

    private boolean isValidFrameId(int frameId) {
        if(frameId < 0 || frameId >= size) {
            LOGGER.fatal("asked to pin frameId that is invalid, the frameId = " + frameId);
            return false;
        }
        return true;
    }
    @Override
    public void pin(int frameId) {
        assert isValidFrameId(frameId);
        latch.lock();
        try {
            Entry entry = clock[frameId];
            if(!entry.isPinned) {
                entry.isPinned = true;
                size--;
            }
            entry.useBit = true;
        } finally {
            latch.unlock();
        }
    }

    @Override
    public void unpin(int frameId) {
        assert isValidFrameId(frameId);
        latch.lock();
        try {
            Entry entry = clock[frameId];
            if(entry.isPinned) {
                entry.isPinned = false;
                size++;
            }
        } finally {
            latch.unlock();
        }
    }

    @Override
    public void remove(int frameId) {
        assert isValidFrameId(frameId);
        latch.lock();
        try {
            Entry entry = clock[frameId];
            if(entry.isPinned) {
                throw new RuntimeException("tried to remove a frame that is pinned, according to specification this error must be thrown");
            }
            // we pin the frame since it can't be evicted,
            // the PBM will deal with it, and make it evictable when it uses the frame,
            // for now the frame will be in the freeList in the BPM
            entry.isPinned = true;
            entry.useBit = false;
            size--;
        } finally {
            latch.unlock();
        }
    }


    @Override
    public int size() {
        latch.lock();
        try {
            return size;
        } finally {
            latch.unlock();
        }
    }
}
