package com.husam.cachemanager;

import com.husam.cachemanager.replacers.ClockReplacer;
import com.husam.common.DatabaseConfig;
import com.husam.storageengine.diskmanager.DiskManager;
import com.husam.storageengine.page.BasicPageGuard;
import com.husam.storageengine.page.Page;
import com.husam.storageengine.page.ReadPageGuard;
import com.husam.storageengine.page.WritePageGuard;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/***
 * buffer pool that uses coarse grain locking, there isn't any other DBMS uses lock free or any other less synchronization mechanism,
 * stability and predictability in DBMS is the most important
 * to improve performance we can use things like multiple buffer pools, pre-fetching, scan sharing and buffer pool bypass
 * you can think of frame ids as buffer descriptors
 * */

// TODO: next step would be to implement the PageGuard functionalities and implement the object it self
public class BufferPoolManagerImpl implements BufferPoolManager {

    private final DiskManager diskManager;
    // the next page id to be allocated
    private final AtomicInteger nextPageId;

    private final int poolSize;

    // page table for keeping track of buffer pool pages, it maps page ids to frame ids
    private final Map<Integer, Integer> pageTable;

    // frame ids is basically equivalent to indexes in the pages array
    private final Page[] pages;

    // replacer to find unpinned frames for replacement
    private final ClockReplacer replacer;

    // free list of the frame ids
    private final Queue<Integer> freeFramesQueue;
    private final Lock latch;

    public BufferPoolManagerImpl(int poolSize, DiskManager diskManager, int nextPageId) {
        assert diskManager != null;
        this.diskManager = diskManager;
        this.poolSize = poolSize;
        this.nextPageId = new AtomicInteger(nextPageId);
        this.pageTable = new TreeMap<>();
        this.pages = new Page[poolSize];
        this.replacer = new ClockReplacer(poolSize);
        this.freeFramesQueue = new LinkedList<>();
        latch = new ReentrantLock();
        for(int i = 0; i < poolSize; i++) {
            this.pages[i] = new Page();
            this.freeFramesQueue.offer(i);
        }
    }

    @Override
    public int getPoolSize() {
        return this.poolSize;
    }

    @Override
    public Page newPage() {
        latch.lock();
        try {
            int freeFrame = getFrame();
            if(freeFrame == DatabaseConfig.getInstance().getInvalidFrameId()) {
                return null;
            }
            int newPageId = allocatePage();
            pageTable.put(newPageId, freeFrame);
            pages[freeFrame].setPageId(newPageId);
            pages[freeFrame].incrementPinCount();
            assert pages[freeFrame].getPinCount() == 1;
            replacer.pin(freeFrame);
            return pages[freeFrame];
        } finally {
            latch.unlock();
        }
    }

    @Override
    public BasicPageGuard newPageGuarded() {
        return null;
    }


    @Override
    public boolean deletePage(int pageId) {
        latch.lock();
        try {
            Integer frameId = pageTable.get(pageId);
            if(frameId == null) {
                return true;
            }
            if(pages[frameId].getPinCount() > 0) {
                return false;
            }
            replacer.remove(frameId);
            freeFramesQueue.offer(frameId);
            pageTable.remove(pageId);
            pages[frameId].resetMemory();
            pages[frameId].setPageId(DatabaseConfig.getInstance().getInvalidPageId());
            pages[frameId].setDirty(false);
            pages[frameId].setPinCount(0);
            deallocatePage(pageId);
            return true;
        } finally {
            latch.unlock();
        }
    }

    @Override
    public Page fetchPage(int pageId) {
        latch.lock();
        try {
            Integer frameId = pageTable.get(pageId);
            if(frameId != null) {
                pages[frameId].incrementPinCount();
                replacer.pin(frameId);
                return pages[frameId];
            }
            frameId = getFrame();
            if(frameId != DatabaseConfig.getInstance().getInvalidFrameId()) {
                diskManager.readPage(pageId, pages[frameId].getData());
                pages[frameId].setPageId(pageId);
                pages[frameId].incrementPinCount();
                replacer.pin(frameId);
                pageTable.put(pageId, frameId);
                return pages[frameId];
            }
            return null;
        } finally {
            latch.unlock();
        }
    }

    @Override
    public BasicPageGuard fetchPageBasic(int pageId) {
        return null;
    }

    @Override
    public ReadPageGuard fetchPageRead(int pageId) {
        return null;
    }

    @Override
    public WritePageGuard fetchPageWrite(int pageId) {
        return null;
    }

    // flush REGARDLESS of the dirty flag
    @Override
    public boolean flushPage(int pageId) {
        latch.lock();
        try {
            Integer frameId = pageTable.get(pageId);
            if(frameId == null) {
                return false;
            }
            Page page = pages[frameId];
            // we shouldn't latch the page,
            // since we are just flushing, if another thread is operating
            // on the same page, then the page will be pinned, and the BPM will synchronize
            // and the other thread will set the dirty flag to true again
            doFlushPage(page);
            return true;
        } finally {
            latch.unlock();
        }
    }

    @Override
    public void flushAllPages() {
        // for performance issues we don't latch here, since it's fine if before complete flushing everything
        // another thread comes and uses the BPM
        for(int i = 0; i < poolSize; i++) {
            flushPage(pages[i].getPageId());
        }
    }

    @Override
    public boolean unpinPage(int pageId, boolean pageGotDirty) {
        Integer frameId = pageTable.get(pageId);
        if(frameId == null) {
            return false;
        }
        if(pages[frameId].getPinCount() <= 0) {
            return false;
        }
        pages[frameId].decrementPinCount();
        if(pages[frameId].getPinCount() == 0) {
            replacer.unpin(frameId);
        }
        if(pageGotDirty) {
            pages[frameId].setDirty(true);
        }
        return true;
    }

    // Caller should acquire the latch before calling this function.
    private int getFrame() {
        if(!this.freeFramesQueue.isEmpty()) {
            return freeFramesQueue.poll();
        }
        int frameId = replacer.victim();
        if(frameId == DatabaseConfig.getInstance().getInvalidFrameId()) {
            return DatabaseConfig.getInstance().getInvalidFrameId();
        }
        Page oldPage = pages[frameId];
        // it's completely fine to not acquire a latch on the page
        // since the page will not be victimized if it's pinned and still in use, in other words,
        // no other threads will be operating on the same page
        if(oldPage.isDirty()) {
            doFlushPage(oldPage);
        }
        pageTable.remove(oldPage.getPageId());
        oldPage.resetMemory();
        assert oldPage.getPinCount() == 0; // the pin count must be zero its victim
        return frameId;
    }

    // Caller should acquire the latch before calling this function.
    // Make sure that frameId has a valid page, this is a private method the caller
    // of this method should know exactly what is going on
    private void doFlushPage(Page page) {
        diskManager.writePage(page.getPageId(), page.getData());
        page.setDirty(false);
    }

    /**
     * @return the id of the allocated page
     * @brief Allocate a page on disk. Caller should acquire the latch before calling this function.
     */
    private int allocatePage() {
        // for now on each program run we will start from page 0
        // later on we can add a data structure to track deallocated pages and use them to reduce fragmentation
        return nextPageId.getAndIncrement();
    }

    /**
     * @param pageId id of the page to deallocate
     * @brief Deallocate a page on disk. Caller should acquire the latch before calling this function.
     */
    void deallocatePage(int pageId) {
        // This is a no-nop right now without a more complex data structure to track deallocated pages
    }
}
