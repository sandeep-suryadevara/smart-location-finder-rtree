import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MEMBER 3 - Evaluation (benchmark and correctness checks).
 * Experiment 1: dataset size 1,000 / 10,000 / 100,000 with 1%-area queries.
 * Experiment 2: 100,000 places, query size from 0.1% to 50% of the map.
 * Every query is run on both methods and the results are compared (correctness).
 */
public final class Benchmark {
    private static final int WARMUP = 300;
    private static final int QUERIES = 1000;

    private Benchmark() {}

    public static void runAll(String csvPath) throws Exception {
        warmUp();
        List<String> csv = new ArrayList<>();
        csv.add("experiment,n,query_area_pct,build_ms,height,nodes,rtree_us,linear_us,speedup,"
                + "rtree_comparisons,linear_comparisons,avg_matches,correct");

        System.out.println("=== Experiment 1: effect of dataset size (query = 1% of map area) ===");
        System.out.printf("%-9s %-9s %-7s %-9s %-11s %-11s %-9s %-12s %-12s %-8s%n",
                "n", "build ms", "height", "nodes", "R-tree us", "Linear us", "speedup",
                "R-tree cmp", "Linear cmp", "correct");
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            csv.add(experiment("size", n, 0.01));
        }

        System.out.println();
        System.out.println("=== Experiment 2: effect of query size (n = 100,000) ===");
        System.out.printf("%-9s %-9s %-7s %-9s %-11s %-11s %-9s %-12s %-12s %-8s%n",
                "area %", "build ms", "height", "nodes", "R-tree us", "Linear us", "speedup",
                "R-tree cmp", "Linear cmp", "correct");
        for (double frac : new double[]{0.001, 0.01, 0.10, 0.50}) {
            csv.add(experiment("query", 100_000, frac));
        }

        try (PrintWriter w = new PrintWriter(csvPath)) {
            for (String line : csv) w.println(line);
        }
        System.out.println("\nSaved: " + csvPath);
    }

    /** Throw-away run so the JIT compiler has optimised the hot loops before any measurement. */
    private static void warmUp() {
        List<Place> places = DataGenerator.generate(20_000, 1L);
        RTree tree = new RTree();
        for (Place p : places) tree.insert(p);
        Random rnd = new Random(3L);
        double w = (DataGenerator.LNG_MAX - DataGenerator.LNG_MIN) * 0.1;
        double h = (DataGenerator.LAT_MAX - DataGenerator.LAT_MIN) * 0.1;
        for (int i = 0; i < 5000; i++) {
            Rect q = randomQuery(rnd, w, h);
            SpatialQuery.rangeSearch(tree, q, null);
            SpatialQuery.linearSearch(places, q, null);
        }
    }

    private static String experiment(String name, int n, double areaFrac) {
        List<Place> places = DataGenerator.generate(n, 42L);

        long b0 = System.nanoTime();
        RTree tree = new RTree();
        for (Place p : places) tree.insert(p);
        double buildMs = (System.nanoTime() - b0) / 1e6;

        String inv = tree.checkInvariants();
        if (inv != null) throw new IllegalStateException("R-tree invariant broken: " + inv);

        double w = (DataGenerator.LNG_MAX - DataGenerator.LNG_MIN) * Math.sqrt(areaFrac);
        double h = (DataGenerator.LAT_MAX - DataGenerator.LAT_MIN) * Math.sqrt(areaFrac);
        Random rnd = new Random(7L);

        // warm-up so the JIT compiler does not distort the first measurements
        for (int i = 0; i < WARMUP; i++) {
            Rect q = randomQuery(rnd, w, h);
            SpatialQuery.rangeSearch(tree, q, null);
            SpatialQuery.linearSearch(places, q, null);
        }

        rnd = new Random(99L);
        long rNanos = 0, lNanos = 0, rCmp = 0, lCmp = 0, matches = 0;
        boolean correct = true;
        for (int i = 0; i < QUERIES; i++) {
            Rect q = randomQuery(rnd, w, h);
            String cat = (i % 4 == 0) ? "Hospital" : null;   // mix of all-category and filtered
            SearchResult a = SpatialQuery.rangeSearch(tree, q, cat);
            SearchResult b = SpatialQuery.linearSearch(places, q, cat);
            rNanos += a.nanos; lNanos += b.nanos;
            rCmp += a.comparisons; lCmp += b.comparisons;
            matches += a.matches.size();
            if (!sameIds(a, b)) correct = false;
        }
        // nearest-k correctness (distance lists must be identical)
        for (int i = 0; i < 50; i++) {
            double lat = DataGenerator.LAT_MIN + rnd.nextDouble() * (DataGenerator.LAT_MAX - DataGenerator.LAT_MIN);
            double lng = DataGenerator.LNG_MIN + rnd.nextDouble() * (DataGenerator.LNG_MAX - DataGenerator.LNG_MIN);
            List<Place> x = SpatialQuery.nearest(tree, lat, lng, 5, "Hospital");
            List<Place> y = SpatialQuery.nearestLinear(places, lat, lng, 5, "Hospital");
            for (int k = 0; k < y.size(); k++) {
                double dx = Math.hypot(x.get(k).lat - lat, x.get(k).lng - lng);
                double dy = Math.hypot(y.get(k).lat - lat, y.get(k).lng - lng);
                if (Math.abs(dx - dy) > 1e-12) correct = false;
            }
        }

        double rUs = rNanos / 1000.0 / QUERIES;
        double lUs = lNanos / 1000.0 / QUERIES;
        double speedup = lUs / rUs;
        long rc = rCmp / QUERIES, lc = lCmp / QUERIES;
        String first = name.equals("size") ? String.format("%,d", n) : String.format("%.1f", areaFrac * 100);
        System.out.printf("%-9s %-9.0f %-7d %-9d %-11.2f %-11.2f %-9.1f %-12d %-12d %-8s%n",
                first, buildMs, tree.height(), tree.nodeCount(), rUs, lUs, speedup, rc, lc,
                correct ? "YES" : "NO");
        return String.format("%s,%d,%.1f,%.0f,%d,%d,%.2f,%.2f,%.1f,%d,%d,%.1f,%s",
                name, n, areaFrac * 100, buildMs, tree.height(), tree.nodeCount(),
                rUs, lUs, speedup, rc, lc, matches / (double) QUERIES, correct ? "YES" : "NO");
    }

    private static Rect randomQuery(Random rnd, double w, double h) {
        double x = DataGenerator.LNG_MIN + rnd.nextDouble() * ((DataGenerator.LNG_MAX - DataGenerator.LNG_MIN) - w);
        double y = DataGenerator.LAT_MIN + rnd.nextDouble() * ((DataGenerator.LAT_MAX - DataGenerator.LAT_MIN) - h);
        return new Rect(x, y, x + w, y + h);
    }

    private static boolean sameIds(SearchResult a, SearchResult b) {
        if (a.matches.size() != b.matches.size()) return false;
        List<Integer> x = new ArrayList<>(), y = new ArrayList<>();
        for (Place p : a.matches) x.add(p.id);
        for (Place p : b.matches) y.add(p.id);
        java.util.Collections.sort(x);
        java.util.Collections.sort(y);
        return x.equals(y);
    }
}
