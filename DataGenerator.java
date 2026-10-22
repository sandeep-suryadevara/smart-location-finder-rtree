import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MEMBER 1 - Data layer.
 * Generates reproducible sample places (fixed seed) inside a Chennai-like bounding box.
 * 70% of points sit in dense "neighbourhood" clusters, 30% are spread uniformly,
 * which is closer to real city data than purely uniform points.
 */
public final class DataGenerator {
    public static final double LAT_MIN = 12.80, LAT_MAX = 13.30;
    public static final double LNG_MIN = 80.00, LNG_MAX = 80.35;

    public static final String[] CATEGORIES =
            {"Hospital", "Restaurant", "ATM", "Pharmacy", "Fuel Station", "School"};

    private DataGenerator() {}

    public static List<Place> generate(int n, long seed) {
        Random rnd = new Random(seed);

        int clusters = 25;
        double[][] centre = new double[clusters][2];
        double[] spread = new double[clusters];
        for (int c = 0; c < clusters; c++) {
            centre[c][0] = LAT_MIN + rnd.nextDouble() * (LAT_MAX - LAT_MIN);
            centre[c][1] = LNG_MIN + rnd.nextDouble() * (LNG_MAX - LNG_MIN);
            spread[c] = 0.004 + rnd.nextDouble() * 0.02;
        }

        List<Place> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double lat, lng;
            if (rnd.nextDouble() < 0.70) {
                int c = rnd.nextInt(clusters);
                lat = centre[c][0] + rnd.nextGaussian() * spread[c];
                lng = centre[c][1] + rnd.nextGaussian() * spread[c];
            } else {
                lat = LAT_MIN + rnd.nextDouble() * (LAT_MAX - LAT_MIN);
                lng = LNG_MIN + rnd.nextDouble() * (LNG_MAX - LNG_MIN);
            }
            lat = Math.min(LAT_MAX, Math.max(LAT_MIN, lat));
            lng = Math.min(LNG_MAX, Math.max(LNG_MIN, lng));
            String cat = CATEGORIES[rnd.nextInt(CATEGORIES.length)];
            list.add(new Place(i, cat + " #" + i, cat, lat, lng));
        }
        return list;
    }
}
