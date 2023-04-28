package com.husam.cachemanager.replacers;

import com.husam.utils.Pair;
import com.husam.utils.list.AccessQueue;
import com.husam.utils.list.Node;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 *
 * LRU replacement policy uses non-blocking approach
 * */
public class LruReplacer<T> implements Replacer<T> {
    int capacity;
    AccessQueue<T> access;
    ConcurrentMap<T, Node<T>> table;

    public LruReplacer(int numPages) {
        this.capacity = numPages;
        access = new AccessQueue<>();
        table = new ConcurrentHashMap<>(16, 0.75f, 16);
    }

    @Override
    public Pair<Boolean, T> victim() {
        // last node at this time
        Node<T> last = access.getTail();
        if (last == null) {
            return new Pair<>(false, null);
        }
        // remove from table should be first to avoid overlapping between victim() and pin(V value)
        table.remove(last.getValue());
        access.deleteNode(last);
        return new Pair<>(true, last.getValue());
    }

    @Override
    public void pin(T element) {
        Node<T> node = table.remove(element);
        access.deleteNode(node);
    }

    @Override
    public void unpin(T element) {
        Node<T> node = access.pushHead(element);
        table.putIfAbsent(element, node);
        // to avoid memory leaks in the access queue
        if(table.get(element) != node) {
            access.deleteNode(node);
        }
    }

    @Override
    public int size() {
        return 0;
    }

}
