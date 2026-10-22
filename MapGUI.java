import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MEMBER 4 - Live demo window.
 *   drag on the map  -> rectangular range query (R-tree vs linear scan, side by side)
 *   click on the map -> 5 nearest places of the chosen category
 */
public class MapGUI extends JFrame {
    private static final int REPS = 50;   // repeat each query to get a stable average time
    private static final Color[] COLORS = {
            new Color(0xD32F2F), new Color(0xF57C00), new Color(0x388E3C),
            new Color(0x7B1FA2), new Color(0x00796B), new Color(0x1976D2)};

    private List<Place> places;
    private RTree tree;
    private final MapPanel map = new MapPanel();
    private final JTextArea info = new JTextArea();
    private final JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{1000, 10000, 50000, 100000});
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JCheckBox showIndex = new JCheckBox("Show R-tree leaf boxes");

    public static void launch() {
        SwingUtilities.invokeLater(() -> new MapGUI().setVisible(true));
    }

    private MapGUI() {
        super("Smart Location Finder - R-tree demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        catBox.addItem("All");
        for (String c : DataGenerator.CATEGORIES) catBox.addItem(c);
        sizeBox.setSelectedItem(10000);

        JButton regen = new JButton("Build index");
        regen.addActionListener(e -> rebuild());
        showIndex.addActionListener(e -> { map.showIndex = showIndex.isSelected(); map.repaint(); });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Places:"));
        top.add(sizeBox);
        top.add(regen);
        top.add(new JLabel("  Category:"));
        top.add(catBox);
        top.add(showIndex);
        add(top, BorderLayout.NORTH);

        add(map, BorderLayout.CENTER);

        info.setEditable(false);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(info);
        sp.setPreferredSize(new Dimension(380, 0));
        add(sp, BorderLayout.EAST);

        setSize(1200, 720);
        setLocationRelativeTo(null);
        rebuild();
    }

    private void rebuild() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int n = (Integer) sizeBox.getSelectedItem();
        places = DataGenerator.generate(n, 42L);
        long t0 = System.nanoTime();
        tree = new RTree();
        for (Place p : places) tree.insert(p);
        double ms = (System.nanoTime() - t0) / 1e6;
        map.places = places;
        map.leafBoxes = tree.leafMBRs();
        map.matches = new HashSet<>();
        map.nearest = new ArrayList<>();
        map.selection = null;
        info.setText(String.format("Built R-tree in %.0f ms%nPlaces      : %,d%nHeight      : %d%n"
                        + "Nodes       : %,d%nLeaf boxes  : %,d%n%nDrag on the map to run a range query.%n"
                        + "Click on the map to find the 5 nearest places.%n",
                ms, n, tree.height(), tree.nodeCount(), map.leafBoxes.size()));
        map.repaint();
        setCursor(Cursor.getDefaultCursor());
    }

    private String selectedCategory() {
        String c = (String) catBox.getSelectedItem();
        return "All".equals(c) ? null : c;
    }

    private void runRange(Rect q) {
        String cat = selectedCategory();
        SearchResult a = null, b = null;
        long rN = 0, lN = 0;
        for (int i = 0; i < REPS; i++) {
            a = SpatialQuery.rangeSearch(tree, q, cat);
            b = SpatialQuery.linearSearch(places, q, cat);
            rN += a.nanos;
            lN += b.nanos;
        }
        map.matches = new HashSet<>(a.matches);
        map.nearest = new ArrayList<>();
        map.repaint();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("RANGE QUERY  category: %s%n", cat == null ? "All" : cat));
        sb.append(String.format("Region: %s%n%n", q));
        sb.append(String.format("Matches            : %d%n", a.matches.size()));
        sb.append(String.format("Same as linear scan: %s%n%n", a.matches.size() == b.matches.size() ? "YES" : "NO"));
        sb.append(String.format("            R-tree     Linear%n"));
        sb.append(String.format("comparisons %-10d %-10d%n", a.comparisons, b.comparisons));
        sb.append(String.format("nodes opened %-9d %-10s%n", a.nodesVisited, "-"));
        sb.append(String.format("avg time us %-10.1f %-10.1f%n", rN / 1000.0 / REPS, lN / 1000.0 / REPS));
        sb.append(String.format("%nFirst results:%n"));
        for (int i = 0; i < Math.min(40, a.matches.size()); i++) sb.append("  ").append(a.matches.get(i)).append('\n');
        if (a.matches.size() > 40) sb.append("  ... ").append(a.matches.size() - 40).append(" more\n");
        info.setText(sb.toString());
        info.setCaretPosition(0);
    }

    private void runNearest(double lat, double lng) {
        String cat = selectedCategory();
        List<Place> res = SpatialQuery.nearest(tree, lat, lng, 5, cat);
        map.nearest = res;
        map.matches = new HashSet<>();
        map.selection = null;
        map.repaint();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("5 NEAREST  category: %s%nFrom: (%.5f, %.5f)%n%n", cat == null ? "All" : cat, lat, lng));
        for (Place p : res) sb.append("  ").append(p).append('\n');
        info.setText(sb.toString());
        info.setCaretPosition(0);
    }

    // ================================================================== map panel
    private class MapPanel extends JPanel {
        List<Place> places = new ArrayList<>();
        List<Rect> leafBoxes = new ArrayList<>();
        Set<Place> matches = new HashSet<>();
        List<Place> nearest = new ArrayList<>();
        Rect selection;
        boolean showIndex = false;
        private Point start, end;

        MapPanel() {
            setBackground(Color.WHITE);
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { start = e.getPoint(); end = start; }
                @Override public void mouseDragged(MouseEvent e) {
                    end = e.getPoint();
                    selection = toRect(start, end);
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) {
                    end = e.getPoint();
                    if (start == null || tree == null) return;
                    if (start.distance(end) < 5) {
                        runNearest(toLat(end.y), toLng(end.x));
                    } else {
                        selection = toRect(start, end);
                        runRange(selection);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private double toLng(int px) { return DataGenerator.LNG_MIN + px / (double) getWidth() * (DataGenerator.LNG_MAX - DataGenerator.LNG_MIN); }
        private double toLat(int py) { return DataGenerator.LAT_MAX - py / (double) getHeight() * (DataGenerator.LAT_MAX - DataGenerator.LAT_MIN); }
        private int toX(double lng) { return (int) ((lng - DataGenerator.LNG_MIN) / (DataGenerator.LNG_MAX - DataGenerator.LNG_MIN) * getWidth()); }
        private int toY(double lat) { return (int) ((DataGenerator.LAT_MAX - lat) / (DataGenerator.LAT_MAX - DataGenerator.LAT_MIN) * getHeight()); }

        private Rect toRect(Point a, Point b) {
            double x1 = toLng(Math.min(a.x, b.x)), x2 = toLng(Math.max(a.x, b.x));
            double y1 = toLat(Math.max(a.y, b.y)), y2 = toLat(Math.min(a.y, b.y));
            return new Rect(x1, y1, x2, y2);
        }

        private Color colorOf(String cat) {
            for (int i = 0; i < DataGenerator.CATEGORIES.length; i++)
                if (DataGenerator.CATEGORIES[i].equals(cat)) return COLORS[i];
            return Color.GRAY;
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            String cat = selectedCategory();

            if (showIndex) {
                g.setColor(new Color(25, 118, 210, 90));
                for (Rect r : leafBoxes) {
                    int x = toX(r.minX), y = toY(r.maxY);
                    g.drawRect(x, y, Math.max(1, toX(r.maxX) - x), Math.max(1, toY(r.minY) - y));
                }
            }
            for (Place p : places) {
                boolean hit = matches.contains(p);
                if (hit) g.setColor(colorOf(p.category));
                else g.setColor(cat == null || cat.equals(p.category) ? new Color(150, 150, 150) : new Color(225, 225, 225));
                int s = hit ? 4 : 2;
                g.fillRect(toX(p.lng) - s / 2, toY(p.lat) - s / 2, s, s);
            }
            for (Place p : nearest) {
                int x = toX(p.lng), y = toY(p.lat);
                g.setColor(new Color(0xD32F2F));
                g.fillOval(x - 6, y - 6, 12, 12);
                g.setColor(Color.WHITE);
                g.drawOval(x - 6, y - 6, 12, 12);
            }
            if (selection != null) {
                int x = toX(selection.minX), y = toY(selection.maxY);
                int w = toX(selection.maxX) - x, h = toY(selection.minY) - y;
                g.setColor(new Color(25, 118, 210, 50));
                g.fillRect(x, y, w, h);
                g.setColor(new Color(25, 118, 210));
                g.drawRect(x, y, w, h);
            }
        }
    }
}
