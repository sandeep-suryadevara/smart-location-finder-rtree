import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * MEMBER 3 - Query engine.
 *  - rangeSearch  : R-tree rectangle query (prunes branches whose MBR misses the query)
 *  - linearSearch : baseline, checks every place
 *  - nearest      : k-nearest places using best-first search on MINDIST
 *  category == null means "all categories".
 */
public final class SpatialQuery {
    private SpatialQuery() {}

    // ------------------------------------------------------------ range search
    public static SearchResult rangeSearch(RTree tree, Rect q, String category) {
        SearchResult r = new SearchResult();
        long t0 = System.nanoTime();
        search(tree.getRoot(), q, category, r);
        r.nanos = System.nanoTime() - t0;
        return r;
    }

    private static void search(Node n, Rect q, String category, SearchResult r) {
        r.nodesVisited++;
        for (Entry e : n.entries) {
            r.comparisons++;
            if (!e.mbr.intersects(q)) continue;          // prune this whole branch
            if (n.leaf) {
                if (category == null || e.place.category.equals(category)) r.matches.add(e.place);
            } else {
                search(e.child, q, category, r);
            }
        }
    }

    public static SearchResult linearSearch(List<Place> places, Rect q, String category) {
        SearchResult r = new SearchResult();
        long t0 = System.nanoTime();
        for (Place p : places) {
            r.comparisons++;
            if (q.containsPoint(p.lng, p.lat)
                    && (category == null || p.category.equals(category))) {
                r.matches.add(p);
            }
        }
        r.nanos = System.nanoTime() - t0;
        return r;
    }

    // -------------------------------------------------------- k nearest places
    private static final class Item {
        final double distSq;
        final Node node;     // exactly one of node / place is set
        final Place place;
        Item(double d, Node n, Place p) { distSq = d; node = n; place = p; }
    }

    /** Distances are Euclidean in degree space (fine for a city-sized area). */
    public static List<Place> nearest(RTree tree, double lat, double lng, int k, String category) {
        List<Place> out = new ArrayList<>();
        PriorityQueue<Item> pq = new PriorityQueue<>(Comparator.comparingDouble(i -> i.distSq));
        pq.add(new Item(0, tree.getRoot(), null));
        while (!pq.isEmpty() && out.size() < k) {
            Item it = pq.poll();
            if (it.place != null) { out.add(it.place); continue; }
            for (Entry e : it.node.entries) {
                double d = e.mbr.minDistSq(lng, lat);
                if (it.node.leaf) {
                    if (category == null || e.place.category.equals(category))
                        pq.add(new Item(d, null, e.place));
                } else {
                    pq.add(new Item(d, e.child, null));
                }
            }
        }
        return out;
    }

    /** Baseline for correctness testing of nearest(). */
    public static List<Place> nearestLinear(List<Place> places, double lat, double lng,
                                            int k, String category) {
        List<Place> cand = new ArrayList<>();
        for (Place p : places) if (category == null || p.category.equals(category)) cand.add(p);
        cand.sort(Comparator.comparingDouble(p -> sq(p.lat - lat) + sq(p.lng - lng)));
        return new ArrayList<>(cand.subList(0, Math.min(k, cand.size())));
    }

    private static double sq(double v) { return v * v; }
}
