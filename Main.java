import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Scanner;

/**
 * MEMBER 1 - Entry point and console demo.
 *   java Main            -> GUI if a display exists, otherwise console demo
 *   java Main gui        -> map window (drag = range query, click = nearest 5)
 *   java Main demo       -> interactive console demo
 *   java Main bench      -> full benchmark, writes results.csv
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase()
                : (GraphicsEnvironment.isHeadless() ? "demo" : "gui");
        switch (mode) {
            case "gui":   MapGUI.launch(); break;
            case "bench": Benchmark.runAll("results.csv"); break;
            case "demo":  consoleDemo(); break;
            default:
                System.out.println("Usage: java Main [gui | demo | bench]");
        }
    }

    private static void consoleDemo() {
        int n = 10_000;
        List<Place> places = DataGenerator.generate(n, 42L);
        RTree tree = new RTree();
        for (Place p : places) tree.insert(p);
        System.out.printf("Built R-tree: %,d places, height %d, %,d nodes, invariants: %s%n",
                tree.size(), tree.height(), tree.nodeCount(),
                tree.checkInvariants() == null ? "OK" : "BROKEN");
        System.out.printf("Map bounds: lat %.2f-%.2f, lng %.2f-%.2f%n",
                DataGenerator.LAT_MIN, DataGenerator.LAT_MAX, DataGenerator.LNG_MIN, DataGenerator.LNG_MAX);
        System.out.println("Categories: " + String.join(", ", DataGenerator.CATEGORIES));
        System.out.println("Type:  range minLat minLng maxLat maxLng [category]");
        System.out.println("       near  lat lng k [category]");
        System.out.println("       quit");

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equals("quit")) break;
            try {
                String[] t = line.split("\\s+", 7);
                if (t[0].equals("range")) {
                    double la1 = Double.parseDouble(t[1]), ln1 = Double.parseDouble(t[2]);
                    double la2 = Double.parseDouble(t[3]), ln2 = Double.parseDouble(t[4]);
                    String cat = t.length > 5 ? line.substring(line.indexOf(t[5])) : null;
                    Rect q = new Rect(Math.min(ln1, ln2), Math.min(la1, la2),
                                      Math.max(ln1, ln2), Math.max(la1, la2));
                    SearchResult a = SpatialQuery.rangeSearch(tree, q, cat);
                    SearchResult b = SpatialQuery.linearSearch(places, q, cat);
                    int shown = Math.min(10, a.matches.size());
                    for (int i = 0; i < shown; i++) System.out.println("  " + a.matches.get(i));
                    if (a.matches.size() > shown) System.out.println("  ... " + (a.matches.size() - shown) + " more");
                    System.out.printf("Matches: %d | R-tree: %d comparisons, %d nodes, %.1f us | "
                                    + "Linear: %d comparisons, %.1f us | same result: %s%n",
                            a.matches.size(), a.comparisons, a.nodesVisited, a.nanos / 1000.0,
                            b.comparisons, b.nanos / 1000.0,
                            a.matches.size() == b.matches.size() ? "yes" : "NO");
                } else if (t[0].equals("near")) {
                    double lat = Double.parseDouble(t[1]), lng = Double.parseDouble(t[2]);
                    int k = Integer.parseInt(t[3]);
                    String cat = t.length > 4 ? line.substring(line.indexOf(t[4])) : null;
                    for (Place p : SpatialQuery.nearest(tree, lat, lng, k, cat)) System.out.println("  " + p);
                } else {
                    System.out.println("Unknown command.");
                }
            } catch (Exception ex) {
                System.out.println("Bad input: " + ex.getMessage());
            }
        }
    }
}
