package bot01;

import battlecode.common.MapLocation;

public class Vector {
    private int x;
    private int y;

    public Vector(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public MapLocation addWith(MapLocation loc) {
        return new MapLocation(loc.x+x, loc.y+y);
    }

    public static Vector vectorSubtract(MapLocation loc, MapLocation HQLoc) {
        return new Vector(loc.x-HQLoc.x, loc.y-HQLoc.y);
    }

    public boolean equals(Vector other) {
        return other.x == x && other.y == y;
    }
}
