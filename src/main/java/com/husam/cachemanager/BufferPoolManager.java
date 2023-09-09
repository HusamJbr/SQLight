package com.husam.cachemanager;

import com.husam.storageengine.diskmanager.DiskManager;
import com.husam.storageengine.page.BasicPageGuard;
import com.husam.storageengine.page.Page;
import com.husam.storageengine.page.ReadPageGuard;
import com.husam.storageengine.page.WritePageGuard;

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
     * @return BasicPageGuard holding a new page
     * @brief PageGuard wrapper for NewPage
     * Functionality should be the same as NewPage, except that
     * instead of returning a pointer to a page, you return a
     * BasicPageGuard structure.
     */
    BasicPageGuard newPageGuarded();

    /**
     * @param pageId id of page to be deleted
     * @return false if the page exists but could not be deleted, true if the page didn't exist or deletion succeeded
     * @brief Delete a page from the buffer pool. If pageId is not in the buffer pool, do nothing and return true. If the
     * page is pinned and cannot be deleted, return false immediately.
     */
    boolean deletePage(int pageId);

    /**
     * Fetches the requested page from the buffer pool.
     *
     * @param pageId id of page to be fetched
     * @return null if pageId cannot be fetched, otherwise pointer to the requested page
     */
    Page fetchPage(int pageId);

    /**
     * @param pageId, the id of the page to fetch
     * @return PageGuard holding the fetched page
     * @brief PageGuard wrappers for FetchPage
     * Functionality should be the same as FetchPage, except
     * that, depending on the function called, a guard is returned.
     * If FetchPageRead or FetchPageWrite is called, it is expected that
     * the returned page already has a read or write latch held, respectively.
     * this is to avoid developer forgetting to unpin a page or forget to unlatch a page
     * using java try-with-resources and autoClosable interface that can be achieved
     */
    BasicPageGuard fetchPageBasic(int pageId);

    ReadPageGuard fetchPageRead(int pageId);

    WritePageGuard fetchPageWrite(int pageId);

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

    /**
     * @param pageId  id of page to be unpinned
     * @param isDirty true if the page should be marked as dirty, false otherwise
     * @return false if the page is not in the page table or its pin count is <= 0 before this call, true otherwise
     * @brief Unpin the target page from the buffer pool. If pageId is not in the buffer pool or its pin count is already
     * 0, return false.
     * <p>
     * Decrement the pin count of a page. If the pin count reaches 0, the frame should be evictable by the replacer.
     * Also, set the dirty flag on the page to indicate if the page was modified.
     */
    boolean unpinPage(int pageId, boolean isDirty);
}
