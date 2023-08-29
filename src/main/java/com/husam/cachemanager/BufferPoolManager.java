package com.husam.cachemanager;

import com.husam.storageengine.diskmanager.DiskManager;
import com.husam.storageengine.page.Page;

public interface BufferPoolManager {


    /**
     * Returns the size (number of frames) of the buffer pool.
     */
    int getPoolSize();

    /**
     * Creates a new page in the buffer pool.
     *
     * @return null if no new pages could be created, otherwise pointer to new page
     */
    Page newPage();

    /**
     * Deletes a page from the buffer pool.
     *
     * @param pageId id of page to be deleted
     * @return false if the page exists but could not be deleted, true if the page didn't exist or deletion succeeded
     */
    boolean deletePage(int pageId);
    /**
     * Fetches the requested page from the buffer pool.
     *
     * @param pageId     id of page to be fetched
     * @return null if pageId cannot be fetched, otherwise pointer to the requested page
     */
    Page fetchPage(int pageId);

    /**
     * Flushes the target page to disk.
     *
     * @param pageId id of page to be flushed, cannot be INVALID_PAGE_ID
     * @return false if the page could not be found in the page table, true otherwise
     */
    boolean flushPage(int pageId);

    /**
     * Flushes all the pages in the buffer pool to disk.
     */
    void flushAllPages();

}
