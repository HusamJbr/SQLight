package com.husam.utils.list;

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a Lock-free doubly linked list described by Paul A. Martin
 * the same algorithm is used by java ConcurrentLinkedDeque
 * I modified it a bit to be more modern
 */

public class DoublyLinkedList<V> {
    private Node<V> headDummy, tailDummy;

    public DoublyLinkedList() {
        Node<V> hd = new Node<>();
        Node<V> td = new Node<>();
        hd.setPrev(null);
        td.setNext(null);
        hd.setNext(new AtomicMarkableReference<>(td, false));
        td.setPrev(new AtomicReference<>(hd));
        headDummy = hd;
        tailDummy = td;
    }

    public boolean deleteNode(Node<V> thisNode) { // keep trying
        return deleteNode(thisNode, true);
    }

    private Node<V> getLiveForward(Node<V> refNode) {// aka listNext
        Node<V> forwardNode = fixForward(refNode);
        if (forwardNode.getNext() == null) return null;
        return forwardNode;
    }

    public V popHead() {
        while (true) {
            Node<V> hn = getHead();
            if (hn == null) {
                return null;
            }
            if (deleteNode(hn, true)) {
                return hn.getValue();
            }
        }
    }

    public void pushHead(V val) {
        boolean done = false;
        while (!done) done = insertAfter(headDummy, val);
    }

    public V popTail() {
        while (true) {
            Node<V> tn = getTail();
            if (tn == null) return null;
            if (deleteNode(tn, false)) return tn.getValue();
        }
    }

    public void pushTail(V val) {
        boolean done = false;
        while (!done) done = insertBefore(tailDummy, val);
    }

    private Node<V> getLiveBack(Node<V> refNode) { // aka listPrevious
        Node<V> backNode = getBack(refNode);
        if ((backNode == null) || (backNode == headDummy)) return null;
        return backNode;
    }

    private Node<V> getBack(Node<V> refNode) {
        AtomicReference<Node<V>> prevRef = refNode.getPrev();
        Node<V> currentNode = refNode;
        while (true) {
            prevRef = currentNode.getPrev();
            Node<V> backNode = prevRef.get();
            AtomicMarkableReference<Node<V>> backAftRef = backNode.getNext();
            Node<V> backAftNode = backAftRef.getReference();
            if (backAftRef.isMarked()) currentNode = backNode;
            else if (backAftNode == refNode) return backNode;
            else {
                Node<V> maybeBack = fixForwardUntil(backNode, refNode);
                if ((maybeBack == null) && backNode.getNext().isMarked())
                    currentNode = backNode;
                else return maybeBack;
            }
        }
    }

    public Node<V> getTail() {
        return getLiveBack(tailDummy);
    }

    public Node<V> getHead() {
        return getLiveForward(headDummy);
    }

    public boolean insertAfter(Node<V> previous, V val) {
        Node<V> myNode = new Node<>();
        myNode.setValue(val);
        while (true) {
            AtomicMarkableReference<Node<V>> prevAftRef = previous.getNext();
            if (prevAftRef.isMarked()) return false;
            Node<V> prevAfter = fixForward(previous);
            if (insertBetween(myNode, previous, prevAfter)) return
                    true;
        }
    }

    public boolean insertBefore(Node<V> following, V val) {
        Node<V> myNode = new Node<>();
        myNode.setValue(val);
        while (true) {
            AtomicMarkableReference<Node<V>> folAftRef = following.getNext();
            if (folAftRef != null && folAftRef.isMarked()) return false;
            Node<V> beforeFollowing = getBack(following);
            if (beforeFollowing == null) return false;
            if (insertBetween(myNode, beforeFollowing, following))
                return true;
        }
    }

    private boolean deleteNode(Node<V> thisNode, boolean retry) {
        AtomicMarkableReference<Node<V>> nextRef;
        while (true) {// til somebody deletes this node
            nextRef = thisNode.getNext();
            if (nextRef.isMarked()) return false; // already deleted
            Node<V> next = nextRef.getReference();
            if (thisNode.getNext().compareAndSet(next, next, false,
                    true)) break;
            if (!retry) return false;
        }
        getBack(thisNode); // just for cleanup
        return true;
    }

    private void reflectForward(Node<V> previous) {
        AtomicMarkableReference<Node<V>> prevAftRef = previous.getNext();
        if (prevAftRef.isMarked()) return;
        Node<V> afterNode = prevAftRef.getReference();
        AtomicReference<Node<V>> afterBeforeRef = afterNode.getPrev();
        Node<V> afterBeforeNode = afterBeforeRef.get();
        if (afterBeforeNode == previous) return;
        AtomicMarkableReference<Node<V>> afterAftRef = afterNode.getNext();
        if ((afterAftRef == null) || !afterAftRef.isMarked())
            afterNode.getPrev().set(previous);
    }

    private Node<V> fixForward(Node<V> thisNode) { // return live or dummy successor
        AtomicMarkableReference<Node<V>> thisAftRef = thisNode.getNext();
        Node<V> laterNode = thisAftRef.getReference();
        Node<V> laterLater;
        while (true) {
            AtomicMarkableReference<Node<V>> nextRef = laterNode.getNext();
            if ((nextRef == null) || !nextRef.isMarked()) {
                reflectForward(thisNode);
                return laterNode;
            } else {
                laterLater = nextRef.getReference();
                thisNode.getNext().compareAndSet(laterNode, laterLater,
                        false, false);
                laterNode = laterLater;
            }
        }
    }

    private Node<V> fixForwardUntil(Node<V> thisNode, Node<V> laterNode) {
        AtomicMarkableReference<Node<V>> thisNodeAftRef, workNodeAftRef,
                laterNodeAftRef;
        Node<V> nextNode;
        Node<V> workNode = thisNode;
        while (true) {
            thisNodeAftRef = thisNode.getNext();
            if (thisNodeAftRef.isMarked()) return null;
            laterNodeAftRef = laterNode.getNext();
            if ((laterNodeAftRef != null) &&
                    laterNodeAftRef.isMarked()) return null; // just quit
            workNodeAftRef = workNode.getNext();
            if (workNodeAftRef == null) return null; // hit tailDummy
            if (!(workNodeAftRef.isMarked())) { //don't alter deleted nodes
                fixForward(workNode);
                workNodeAftRef = workNode.getNext();
            } // get the updated value
            nextNode = workNodeAftRef.getReference();
            if (nextNode == laterNode) return workNode;
            else if (nextNode.getNext() == null) return null;
            else workNode = nextNode;
        }
    }

    private boolean insertBetween(Node<V> thisNode, Node<V> prev, Node<V> aft) {
        if (thisNode.getPrev() != null) thisNode.getPrev().set(prev);
        else thisNode.setPrev(new AtomicReference<>(prev));
        if (thisNode.getNext() != null) thisNode.getNext().set(aft, false);
        else thisNode.setNext(new AtomicMarkableReference<>(aft, false));
        if (prev.getNext().compareAndSet(aft, thisNode, false, false)) {
            reflectForward(thisNode); // cleanup aft node backpointer
            return true;
        }
        return false;
    }
}
