package com.husam.cachemanager.replacers;

import com.husam.utils.Pair;

import java.util.concurrent.atomic.AtomicStampedReference;

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

    private static class Node<T> {
        T item;
        final AtomicStampedReference<Node<T>> next;
        public Node(T x, Node<T> node) {
            item = x;
            next = new AtomicStampedReference<>(node, Integer.MIN_VALUE);
        }
    }
    private static class ConcurrentQueue<T> {

        // Pointer to sentinel node.  The first actual node is at head.getNext().
        private final AtomicStampedReference<Node<T>> head = new AtomicStampedReference<>(new Node<>(null, null), Integer.MIN_VALUE);

        // Pointer to last node on list
        private final AtomicStampedReference<Node<T>> tail = new AtomicStampedReference<>(head.getReference(), Integer.MIN_VALUE);

        public boolean enqueue(Node<T> node) {
            for (;;) {
                int[] curTailStamp = new int[1];
                Node<T> curTail = tail.get(curTailStamp);
                int[] curTailNextStamp = new int[1];
                Node<T> tailNext = curTail.next.get(curTailNextStamp);
                if (curTail == tail.getReference()) {
                    // the tail is pointing to null as we want, we don't need to advance the tail
                    // try inserting new node
                    if (tailNext == null) {
                        if (curTail.next.compareAndSet(null, node, curTailNextStamp[0], curTailNextStamp[0]+1)) {
                            tail.compareAndSet(curTail, node, curTailStamp[0], curTailStamp[0]+1);
                            return true;
                        }
                    } else { // it fails the tail is pointing one step before, we have to advance the tail
                        tail.compareAndSet(curTail, tailNext, curTailStamp[0], curTailStamp[0]+1);
                    }
                }
            }
        }

        public Node<T> dequeue() {
            for (;;) {
                int[] curHeadStamp = new int[1];
                Node<T> curHead = head.get(curHeadStamp);
                int[] curTailStamp = new int[1];
                Node<T> curTail = tail.get(curTailStamp);
                int[] headNextStamp = new int[1];
                Node<T> headNext = curHead.next.get(headNextStamp);
                if (curHead == head.getReference()) {
                    if (curHead == curTail) {
                        if (headNext == null) {
                            return null;
                        }
                        tail.compareAndSet(curTail, headNext, curTailStamp[0], curTailStamp[0]+1);
                    } else {
                        // the first node is sentinel, so we need to move the data from the next node to the first sentinel node
                        // we then make the next node the new sentinel, and we return the old sentinel that contains the needed data
                        T resultItem = headNext.item;
                        if(head.compareAndSet(curHead, headNext, curHeadStamp[0], curHeadStamp[0]+2)) {
                            curHead.item = resultItem;
                            return curHead;
                        }
                    }
                }
            }
        }

    }
}
