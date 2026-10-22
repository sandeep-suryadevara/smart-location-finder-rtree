import java.util.ArrayList;
import java.util.List;

/**
 * MEMBER 2 - R-tree construction.
 * A tree node holds at most RTree.MAX_ENTRIES entries.
 */
public final class Node {
    public final boolean leaf;
    public final List<Entry> entries = new ArrayList<>();
    public Node parent;

    public Node(boolean leaf) {
        this.leaf = leaf;
    }

    public void add(Entry e) {
        entries.add(e);
        if (e.child != null) e.child.parent = this;
    }

    /** Tight bounding rectangle of all entries. O(M). */
    public Rect computeMBR() {
        Rect r = entries.get(0).mbr;
        for (int i = 1; i < entries.size(); i++) r = r.union(entries.get(i).mbr);
        return r;
    }

    /** The entry in this (internal) node that points to the given child. */
    public Entry entryFor(Node child) {
        for (Entry e : entries) if (e.child == child) return e;
        throw new IllegalStateException("child not found in parent");
    }
}
