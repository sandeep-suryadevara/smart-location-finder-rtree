/**
 * MEMBER 2 - R-tree construction.
 * One slot in a node: an MBR plus EITHER a child node (internal node)
 * OR a place (leaf node).
 */
public final class Entry {
    public Rect mbr;
    public final Node child;   // non-null in internal nodes
    public final Place place;  // non-null in leaf nodes

    public Entry(Rect mbr, Node child) {
        this.mbr = mbr;
        this.child = child;
        this.place = null;
    }

    public Entry(Rect mbr, Place place) {
        this.mbr = mbr;
        this.child = null;
        this.place = place;
    }
}
