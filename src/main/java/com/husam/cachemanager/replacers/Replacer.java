package com.husam.cachemanager.replacers;

import com.husam.utils.Pair;

public interface Replacer {

    /**
     * victimize frameId as defined by the replacement policy.
     *
     * @return The removed frameId, or -1 if no victim was identified
     */
    int victim();

    /**
     * Pins an frameId, indicating that it should not be victimized until it is unpinned.
     *
     * @param frameId the frame id to pin
     */
    void pin(int frameId);

    /**
     * Unpins a frameId, indicating that it can be victimized.
     *
     * @param frameId the frame id to unpin
     */
    void unpin(int frameId);

    /**
     * @param frameId id of frame to be removed
     * @brief Remove an evictable frame from replacer.
     * This function should also decrement replacers' size if removal is successful.
     * Note that this is different from evicting a frame, which always remove the frame
     * with according to the replacement algorithm used. This function removes specified frame id,
     * no matter what replacement algorithm you're using.
     * If Remove is called on a non-evictable frame(pinned), throw an exception or abort the process.
     * If specified frame is not found, directly return from this function.
     */
    void remove(int frameId);

    /**
     * @return the number of elements in the replacer that can be victimized
     */
    int size();
}
