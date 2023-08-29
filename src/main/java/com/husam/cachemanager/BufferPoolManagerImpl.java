package com.husam.cachemanager;

import com.husam.cachemanager.replacers.ClockReplacer;
import com.husam.storageengine.diskmanager.DiskManager;
import com.husam.storageengine.page.Page;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * frame ids are similar to buffer descriptors
 * */
public class BufferPoolManagerImpl implements BufferPoolManager {
    private DiskManager diskManager;

    // the next page id to be allocated
    private AtomicInteger nextPageId;

    private int poolSize;

    // page table for keeping track of buffer pool pages, it maps page ids to frame ids
    private ConcurrentHashMap<Integer, Integer> pageTable;

    // frame ids is basically equivalent to indexes in the pages array
    private Page[] pages;

    // replacer to find unpinned frames for replacement
    private ClockReplacer<Integer> replacer;

    // free list of the frame ids
    private ConcurrentLinkedQueue<Integer> freeFramesQueue;

    public BufferPoolManagerImpl(int poolSize, DiskManager diskManager, int nextPageId) {
        this.diskManager = diskManager;
        this.poolSize = poolSize;
        this.nextPageId.set(nextPageId);
    }

    @Override
    public int getPoolSize() {
        return this.poolSize;
    }

    @Override
    public Page newPage() {
        return null;
    }

    @Override
    public boolean deletePage(int pageId) {
        return false;
    }

    @Override
    public Page fetchPage(int pageId) {
        return null;
    }

    @Override
    public boolean flushPage(int pageId) {
        return false;
    }

    @Override
    public void flushAllPages() {

    }
}
