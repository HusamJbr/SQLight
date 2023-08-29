package com.husam.storageengine.page;

import com.husam.cachemanager.BufferPoolManager;
import com.husam.common.DatabaseConfig;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class Page {
    // There is book-keeping information inside the page that should only be relevant to the buffer pool manager.
    protected BufferPoolManager bufferPoolManager;

    protected static final int SIZE_PAGE_HEADER = 8;
    protected static final int OFFSET_PAGE_START = 0;
    protected static final int OFFSET_LSN = 4;
    private byte[] data;
    private int pageId = DatabaseConfig.getInstance().getInvalidPageId();
    private int pinCount = 0;
    private boolean isDirty = false;
    private ReadWriteLock rwLatch = new ReentrantReadWriteLock();

    /** Constructor. Zeros out the page data. */
    public Page() {
        data = new byte[DatabaseConfig.getInstance().getPageSize()];
    }

    /** @return the actual data contained within this page */
    protected byte[] getData() {
        return data;
    }

    /** @return the page id of this page */
    protected int getPageId() {
        return pageId;
    }

    /** @return the pin count of this page */
    protected int getPinCount() {
        return pinCount;
    }

    /** @return true if the page in memory has been modified from the page on disk, false otherwise */
    protected boolean isDirty() {
        return isDirty;
    }

    /** Acquire the page write latch. */
    protected void writeLatch() {
        rwLatch.writeLock().lock();
    }

    /** Release the page write latch. */
    protected void writeUnlatch() {
        rwLatch.writeLock().unlock();
    }

    /** Acquire the page read latch. */
    protected void readLatch() {
        rwLatch.readLock().lock();
    }

    /** Release the page read latch. */
    protected void readUnlatch() {
        rwLatch.readLock().unlock();
    }

    /** @return the page LSN. */
    protected int getLSN() {
        return getIntFromByteArray(data, OFFSET_LSN);
    }

    /** Sets the page LSN. */
    protected void setLSN(int lsn) {
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
}