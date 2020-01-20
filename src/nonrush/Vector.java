package nonrush;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

import static nonrush.Util.directions;

public class Vector {
    private int x;
    private int y;
    private int magnitude;

    public Vector(int x, int y) {
        this.x = x;
        this.y = y;
        this.magnitude = Math.max(Math.abs(x), Math.abs(y));
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

    public Vector rotate(int rotateState) {
        if (rotateState == 0) return new Vector(this.x, this.y);
        else if (rotateState == 2) return new Vector(this.y, -this.x);
        else if (rotateState == 4) return new Vector(-this.x, -this.y);
        else if (rotateState == 6) return new Vector(-this.y, this.x);
        else {
            x /= magnitude;
            y /= magnitude;
            Direction d = getDir();
            for (int i = 0; i < rotateState; i++) {
                d = d.rotateRight();
            }
            return new Vector(magnitude*d.dx, magnitude*d.dy);
        }
    }

    public static Vector getVec(Direction dir) {
        return new Vector(dir.dx, dir.dy);
    }

    public Direction getDir() {
        for (Direction dir: directions) {
            if (dir.dx == x && dir.dy == y) return dir;
        }
        // should not get here
        return Direction.CENTER;
    }
}
