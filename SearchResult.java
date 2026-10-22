import java.util.ArrayList;
import java.util.List;

/**
 * MEMBER 3 - Query engine.
 * Result plus the measurements used in the evaluation.
 */
public final class SearchResult {
    public final List<Place> matches = new ArrayList<>();
    /** R-tree nodes opened (linear scan: 0). */
    public long nodesVisited = 0;
    /** Rectangle/point tests performed. Linear scan always does n. */
    public long comparisons = 0;
    public long nanos = 0;
}
