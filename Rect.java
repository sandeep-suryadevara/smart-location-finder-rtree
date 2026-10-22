/**
 * MEMBER 1 - Data layer.
 * Minimum Bounding Rectangle (MBR). x = longitude, y = latitude.
 * Used for R-tree node boxes AND for the user's query region.
 */
public final class Rect {
    public final double minX, minY, maxX, maxY;

    public Rect(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /** A point is stored as a degenerate rectangle (zero width and height). */
    public static Rect ofPoint(double x, double y) {
        return new Rect(x, y, x, y);
    }

    public double area() {
        return (maxX - minX) * (maxY - minY);
    }

    /** Smallest rectangle that covers both this and o. */
    public Rect union(Rect o) {
        return new Rect(Math.min(minX, o.minX), Math.min(minY, o.minY),
                        Math.max(maxX, o.maxX), Math.max(maxY, o.maxY));
    }

    /** How much this rectangle's area must grow to also cover o. */
    public double enlargement(Rect o) {
        return union(o).area() - area();
    }

    /** Boundary-inclusive overlap test. */
    public boolean intersects(Rect o) {
        return minX <= o.maxX && maxX >= o.minX && minY <= o.maxY && maxY >= o.minY;
    }

    public boolean contains(Rect o) {
        return minX <= o.minX && maxX >= o.maxX && minY <= o.minY && maxY >= o.maxY;
    }

    public boolean containsPoint(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    /** Squared distance from point (x,y) to the nearest point of this rectangle (0 if inside). */
    public double minDistSq(double x, double y) {
        double dx = Math.max(Math.max(minX - x, 0), x - maxX);
        double dy = Math.max(Math.max(minY - y, 0), y - maxY);
        return dx * dx + dy * dy;
    }

    @Override
    public String toString() {
        return String.format("[%.4f,%.4f - %.4f,%.4f]", minX, minY, maxX, maxY);
    }
}
