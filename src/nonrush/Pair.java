package nonrush;

import battlecode.common.MapLocation;

public class Pair {
    private MapLocation K;
    private MapLocation V;

    public Pair(MapLocation K, MapLocation V) {
        this.K = K;
        this.V = V;
    }

    public MapLocation getKey() {
        return K;
    }

    public MapLocation getValue() {
        return V;
    }

    public String toString() {
        return "[" + K + ", " + V + "]";
    }
}
