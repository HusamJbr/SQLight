package com.husam.cachemanager.replacers;

import com.husam.utils.Pair;

public interface Replacer<T> {

    /**
     * Remove the victim element as defined by the replacement policy.
     * @return A Pair object containing:
     * * A boolean value indicating whether a victim element was found (true) or not (false)
     * * The removed element, or null if no victim element was identified
     */
    Pair<Boolean, T> victim();

    /**
     * Pins an element, indicating that it should not be victimized until it is unpinned.
     *
     * @param element the cached element to pin
     */
    void pin(T element);

    /**
     * Unpins an element, indicating that it can now be victimized.
     *
     * @param element the cached element to unpin
     */
    void unpin(T element);

    /**
     * @return the number of elements in the replacer that can be victimized
     */
    int size();
}
