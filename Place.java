/**
 * MEMBER 1 - Data layer.
 * One place record: id, name, category, latitude, longitude.
 */
public final class Place {
    public final int id;
    public final String name;
    public final String category;
    public final double lat;   // y axis
    public final double lng;   // x axis

    public Place(int id, String name, String category, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public String toString() {
        return String.format("#%d %s [%s] (%.5f, %.5f)", id, name, category, lat, lng);
    }
}
