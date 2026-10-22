# smart-location-finder-rtree
A high-performance Java implementation of an R-Tree spatial index using Guttman's quadratic split algorithm. Features interactive map visualization (Swing), spatial range queries, k-NN search, and automated performance benchmarking against linear scans.


# Smart Location Finder — R-Tree Spatial Index

An end-to-end Data Structures and Algorithms (DSA) project implementing a height-balanced **R-Tree** spatial index in Java to query multi-dimensional geographical data efficiently. The system evaluates dynamic spatial indexing against traditional linear scans using realistic city dataset distributions, interactive Swing GUI visualizations, and automated benchmarking engines.

---

## Key Features

* **Custom R-Tree Spatial Index**: Built ground-up with Minimum Bounding Rectangles (MBRs) and Guttman's Quadratic Split algorithm ($M=8, m=3$).
* **Realistic Data Generation**: Synthesizes 70% clustered (neighborhood hotspots) and 30% uniform spatial data distributions across geographic bounding boxes.
* **Spatial Query Operations**:
  * **Range Queries**: Retrieves all places inside a rectangular coordinate window.
  * **k-Nearest Neighbor (k-NN) Search**: Identifies the $k$ closest places to any target coordinate.
  * **Category Filtering**: Filters results by POI type (e.g., Hospital, ATM, School, Pharmacy).
* **Interactive Swing GUI**: Visualizes MBR leaf nodes, live rectangle drag queries, k-NN radius selections, and side-by-side metric comparisons.
* **Automated Benchmarking Engine**: Evaluates runtime speedups, node visits, and bounding comparisons across datasets up to 100,000 places.

---

## Data Structure & Architecture

Unlike 1D linear structures or static QuadTrees, the R-Tree uses hierarchical bounding boxes that dynamically adjust as data points are inserted:

1. **`Rect`**: Represents Minimum Bounding Rectangles (MBRs) with spatial operations (union, intersection, area enlargement, min distance squared).
2. **`Place`**: Stores spatial point data (Latitude, Longitude, Name, Category).
3. **`Node` & `Entry`**: Height-balanced nodes holding up to $M$ entries. Internal entries point to sub-tree nodes, while leaf entries store spatial `Place` objects.
4. **`RTree`**: Manages node insertions, leaf choice via minimal area expansion, quadratic overflow splits, parent propagation, and depth invariant checks.

---

## File Overview & Module Breakdown

| Module / File | Responsibility |
| :--- | :--- |
| `Place.java` | Spatial point entity model holding coordinates and category details. |
| `Rect.java` | Spatial bounding box geometry and intersection mathematics. |
| `DataGenerator.java` | Generates reproducible clustered and uniform city spatial datasets. |
| `Entry.java` | Holds leaf places or child node pointers paired with bounding boxes. |
| `Node.java` | Tree node structure managing entries and parent/child pointers. |
| `RTree.java` | Core R-Tree construction, insertion, quadratic split, and invariant validation. |
| `SpatialQuery.java` | Search algorithms for rectangular range queries and k-NN traversal. |
| `SearchResult.java` | Search metric container tracking nanos, comparisons, and visited nodes. |
| `Benchmark.java` | Automated performance evaluation writing microsecond measurements to CSV. |
| `MapGUI.java` | Interactive Swing application for live visual query execution. |
| `Main.java` | Application launcher and interactive console demonstration mode. |

