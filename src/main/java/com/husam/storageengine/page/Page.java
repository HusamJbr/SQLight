package com.husam.storageengine.page;

import com.husam.cachemanager.BufferPoolManager;
import com.husam.common.DatabaseConfig;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Page is the basic unit of storage within the database system. Page provides a wrapper for actual data pages being
 * held in main memory. Page also contains book-keeping information that is used by the buffer pool manager, e.g.
 * pin count, dirty flag, page id, etc.
 */

// TODO: check out the page structure, and how to serialize the page data to other page objects, checkout security reasons
public class Page {
    protected static final int SIZE_PAGE_HEADER = 8;
    protected static final int OFFSET_PAGE_START = 0;
    protected static final int OFFSET_LSN = 4;
    private final byte[] data;
    private int pageId;
    private int pinCount;
    private boolean isDirty;
    // this latch is to operate on the internal data byte array,
    // but for the metadata we don't need any latch since it must be modified by the BPM
    // and the BPM will synchronize it
    private final ReadWriteLock rwLatch;

    /** Constructor. Zeros out the page data. */
    public Page() {
        this.data = new byte[DatabaseConfig.getInstance().getPageSize()];
        resetMemory();
        this.pageId = DatabaseConfig.getInstance().getInvalidPageId();
        this.pinCount = 0;
        this.isDirty = false;
        this.rwLatch = new ReentrantReadWriteLock();
    }

    /** @return the actual data contained within this page */
    public byte[] getData() {
        return data;
    }

    /** @return the page id of this page */
    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    /** @return the pin count of this page */
    public int getPinCount() {
        return pinCount;
    }

    public void setPinCount(int pinCount) {
        this.pinCount = pinCount;
    }

    public void incrementPinCount() {
        this.pinCount++;
    }

    public void decrementPinCount() {
        this.pinCount++;
    }

    /** @return true if the page in memory has been modified from the page on disk, false otherwise */
    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    /** Acquire the page write latch. */
    public void writeLatch() {
        rwLatch.writeLock().lock();
    }

    /** Release the page write latch. */
    public void writeUnlatch() {
        rwLatch.writeLock().unlock();
    }

    /** Acquire the page read latch. */
    public void readLatch() {
        rwLatch.readLock().lock();
    }

    /** Release the page read latch. */
    public void readUnlatch() {
        rwLatch.readLock().unlock();
    }

    /** @return the page LSN. */
    public int getLSN() {
        return getIntFromByteArray(data, OFFSET_LSN);
    }

    /** Sets the page LSN. */
    public void setLSN(int lsn) {
        putIntToByteArray(data, OFFSET_LSN, lsn);
    }

    // Helper method to get an integer from a byte array
    private int getIntFromByteArray(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) |
                ((bytes[offset + 1] & 0xFF) << 8) |
                ((bytes[offset + 2] & 0xFF) << 16) |
                ((bytes[offset + 3] & 0xFF) << 24);
    }

    // Helper method to put an integer into a byte array
    private void putIntToByteArray(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /** Zeroes out the data that is held within the page. */
    public void resetMemory() {
        Arrays.fill(data, OFFSET_PAGE_START, DatabaseConfig.getInstance().getPageSize(), (byte) 0);
    }
}