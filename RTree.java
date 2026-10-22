import java.util.ArrayList;
import java.util.List;

/**
 * MEMBER 2 - R-tree construction (Guttman 1984, quadratic split).
 *
 * Insert:  chooseLeaf -> add -> split if overflow -> adjustTree (propagate up).
 * Invariants (checked by checkInvariants):
 *   - every leaf is at the same depth (tree is height-balanced)
 *   - every non-root node holds between MIN_ENTRIES and MAX_ENTRIES entries
 *   - every parent entry's MBR exactly covers its child's entries
 */
public class RTree {
    public static final int MAX_ENTRIES = 8;   // M
    public static final int MIN_ENTRIES = 3;   // m (about 40% of M)

    private Node root = new Node(true);
    private int size = 0;
    private int height = 1;

    public Node getRoot() { return root; }
    public int size()     { return size; }
    public int height()   { return height; }

    // ------------------------------------------------------------------ insert
    public void insert(Place p) {
        Entry e = new Entry(Rect.ofPoint(p.lng, p.lat), p);
        Node leaf = chooseLeaf(e.mbr);
        leaf.add(e);
        Node sibling = null;
        if (leaf.entries.size() > MAX_ENTRIES) sibling = splitNode(leaf);
        adjustTree(leaf, sibling);
        size++;
    }

    /** Descend from the root, always taking the child whose MBR needs the least enlargement. */
    private Node chooseLeaf(Rect r) {
        Node n = root;
        while (!n.leaf) {
            Entry best = null;
            double bestEnl = Double.MAX_VALUE, bestArea = Double.MAX_VALUE;
            for (Entry e : n.entries) {
                double enl = e.mbr.enlargement(r);
                double area = e.mbr.area();
                if (enl < bestEnl || (enl == bestEnl && area < bestArea)) {
                    best = e; bestEnl = enl; bestArea = area;
                }
            }
            n = best.child;
        }
        return n;
    }

    /** Walk up to the root, refreshing MBRs and adding split siblings to parents. */
    private void adjustTree(Node n, Node nn) {
        while (n != root) {
            Node parent = n.parent;
            parent.entryFor(n).mbr = n.computeMBR();
            Node parentSplit = null;
            if (nn != null) {
                parent.add(new Entry(nn.computeMBR(), nn));
                if (parent.entries.size() > MAX_ENTRIES) parentSplit = splitNode(parent);
            }
            n = parent;
            nn = parentSplit;
        }
        if (nn != null) {                       // root was split -> tree grows by one level
            Node newRoot = new Node(false);
            newRoot.add(new Entry(n.computeMBR(), n));
            newRoot.add(new Entry(nn.computeMBR(), nn));
            root = newRoot;
            height++;
        }
    }

    /**
     * Quadratic split. Keeps one group in 'node' and returns the new sibling.
     *  1. PickSeeds: the pair that would waste the most area if put together.
     *  2. PickNext: repeatedly give the entry with the strongest preference to its best group.
     */
    private Node splitNode(Node node) {
        List<Entry> all = new ArrayList<>(node.entries);
        node.entries.clear();
        Node sibling = new Node(node.leaf);

        // PickSeeds
        int s1 = 0, s2 = 1;
        double worst = -Double.MAX_VALUE;
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Rect a = all.get(i).mbr, b = all.get(j).mbr;
                double waste = a.union(b).area() - a.area() - b.area();
                if (waste > worst) { worst = waste; s1 = i; s2 = j; }
            }
        }
        Entry e1 = all.get(s1), e2 = all.get(s2);
        node.add(e1);
        sibling.add(e2);
        Rect m1 = e1.mbr, m2 = e2.mbr;

        List<Entry> rest = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) if (i != s1 && i != s2) rest.add(all.get(i));

        while (!rest.isEmpty()) {
            // Min-fill rule: if one group needs every remaining entry to reach m, give them all.
            if (node.entries.size() + rest.size() == MIN_ENTRIES) {
                for (Entry e : rest) { node.add(e); m1 = m1.union(e.mbr); }
                break;
            }
            if (sibling.entries.size() + rest.size() == MIN_ENTRIES) {
                for (Entry e : rest) { sibling.add(e); m2 = m2.union(e.mbr); }
                break;
            }
            // PickNext
            int pick = 0;
            double bestDiff = -1;
            for (int i = 0; i < rest.size(); i++) {
                double d1 = m1.enlargement(rest.get(i).mbr);
                double d2 = m2.enlargement(rest.get(i).mbr);
                double diff = Math.abs(d1 - d2);
                if (diff > bestDiff) { bestDiff = diff; pick = i; }
            }
            Entry e = rest.remove(pick);
            double d1 = m1.enlargement(e.mbr), d2 = m2.enlargement(e.mbr);
            boolean toFirst;
            if (d1 != d2) toFirst = d1 < d2;
            else if (m1.area() != m2.area()) toFirst = m1.area() < m2.area();
            else toFirst = node.entries.size() <= sibling.entries.size();
            if (toFirst) { node.add(e); m1 = m1.union(e.mbr); }
            else         { sibling.add(e); m2 = m2.union(e.mbr); }
        }
        return sibling;
    }

    // ------------------------------------------------------------ introspection
    /** MBRs of all leaf nodes (used by the GUI to draw the index). */
    public List<Rect> leafMBRs() {
        List<Rect> out = new ArrayList<>();
        collectLeaves(root, out);
        return out;
    }

    private void collectLeaves(Node n, List<Rect> out) {
        if (n.entries.isEmpty()) return;
        if (n.leaf) { out.add(n.computeMBR()); return; }
        for (Entry e : n.entries) collectLeaves(e.child, out);
    }

    public int nodeCount() { return countNodes(root); }

    private int countNodes(Node n) {
        int c = 1;
        if (!n.leaf) for (Entry e : n.entries) c += countNodes(e.child);
        return c;
    }

    /** Verifies all R-tree invariants. Returns null if the tree is valid, else a message. */
    public String checkInvariants() {
        int[] leafDepth = {-1};
        int[] count = {0};
        String err = check(root, 1, leafDepth, count);
        if (err != null) return err;
        if (count[0] != size) return "entry count " + count[0] + " != size " + size;
        if (leafDepth[0] != height) return "height mismatch";
        return null;
    }

    private String check(Node n, int depth, int[] leafDepth, int[] count) {
        if (n != root && (n.entries.size() < MIN_ENTRIES || n.entries.size() > MAX_ENTRIES))
            return "bad fill: " + n.entries.size();
        if (n.leaf) {
            if (leafDepth[0] == -1) leafDepth[0] = depth;
            else if (leafDepth[0] != depth) return "leaves at different depths";
            count[0] += n.entries.size();
            return null;
        }
        for (Entry e : n.entries) {
            Rect tight = e.child.computeMBR();
            if (!e.mbr.contains(tight) || !tight.contains(e.mbr)) return "MBR not tight";
            if (e.child.parent != n) return "bad parent pointer";
            String err = check(e.child, depth + 1, leafDepth, count);
            if (err != null) return err;
        }
        return null;
    }
}
