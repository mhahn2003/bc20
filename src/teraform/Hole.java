package teraform;

import battlecode.common.MapLocation;

import static teraform.Robot.HQLocation;

public class Hole {
    int x;
    int y;
    int value;
    static int dx;
    static int dy;


    public Hole(int x, int y) {
        this.x = x;
        this.y = y;
        value = 22*x+y;
        dx = HQLocation.x % 3;
        dy = HQLocation.y % 3;
    }

    public Hole(int value) {
        this.value = value;
        y = value % 22;
        x = (value-(value % 22))/22;
        dx = HQLocation.x % 3;
        dy = HQLocation.y % 3;
    }

    public MapLocation getMapLoc() {
        return new MapLocation(dx+x*3, dy+y*3);
    }

    public int getValue() { return value; }

    public int getX() { return x; }

    public int getY() { return y; }

    public static Hole getHole(MapLocation loc) {
        dx = HQLocation.x % 3;
        dy = HQLocation.y % 3;
        return new Hole((loc.x-dx)/3, (loc.y-dy)/3);
    }
}

