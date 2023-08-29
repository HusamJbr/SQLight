package com.husam.cachemanager.replacers;

import com.husam.utils.Pair;

public class ClockReplacer<T> implements Replacer<T> {
    @Override
    public Pair<Boolean, T> victim() {
        return null;
    }

    @Override
    public void pin(T element) {

    }

    @Override
    public void unpin(T element) {

    }

    @Override
    public int size() {
        return 0;
    }
}
