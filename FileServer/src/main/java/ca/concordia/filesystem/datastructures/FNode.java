package ca.concordia.filesystem.datastructures;

public class FNode {

    private int blockIndex; // Negative if not in use
    private int next; // Index to next FNode, -1 if none

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    // Default constructor creates an unused node
    public FNode() {
        this.blockIndex = -1;
        this.next = -1;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public boolean isInUse() {
        return blockIndex >= 0;
    }

    public boolean hasNext() {
        return next != -1;
    }

    public void clear() {
        this.blockIndex = -1;
        this.next = -1;
    }

    @Override
    public String toString() {
        return String.format("FNode{blockIndex=%d, next=%d, inUse=%b, hasNext=%b}",
                blockIndex, next, isInUse(), hasNext());
    }
}
