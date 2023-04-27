package com.husam.utils.list;

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

public class Node<V> {
    private V value;
    private AtomicReference<Node<V>> prev;
    private AtomicMarkableReference<Node<V>> next;

    public Node() {
        prev = null;
        next = null;
    }

    public void setNext(AtomicMarkableReference<Node<V>> next) {
        this.next = next;
    }

    public void setPrev(AtomicReference<Node<V>> prev) {
        this.prev = prev;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public AtomicMarkableReference<Node<V>> getNext() {
        return next;
    }

    public AtomicReference<Node<V>> getPrev() {
        return prev;
    }

    public V getValue() {
        return value;
    }
}
