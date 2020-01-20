package nonrush;

import battlecode.common.MapLocation;

import static nonrush.Robot.HQLocation;

public class Hole {
    int x;
    int y;
    int value;
    static int dx;
    static int dy;


    public Hole(int x, int y) {
        this.x = x;
        this.y = y;
        value = 32*x+y;
        dx = HQLocation.x % 2;
        dy = HQLocation.y % 2;
    }
    public Hole(int value) {
        this.value = value;
        y = value % 32;
        x = (value-(value % 32))/32;
        dx = HQLocation.x % 2;
        dy = HQLocation.y % 2;
    }
    public MapLocation getMapLoc() {
        return new MapLocation(dx+x*2, dy+y*2);
    }

    public int getValue() { return value; }

    public int getX() { return x; }

    public int getY() { return y; }

    public static Hole getHole(MapLocation loc) {
        dx = HQLocation.x % 2;
        dy = HQLocation.y % 2;
        return new Hole((loc.x-dx)/2, (loc.y-dy)/2);
    }
}

