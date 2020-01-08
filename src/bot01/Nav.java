package bot01;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.LinkedList;
import java.util.Queue;

// Navigation class
public class Nav {

    Queue<MapLocation> visited;
    private boolean isBugging;

    public Nav() {
        visited = new LinkedList<>();
        isBugging = false;
    }

    // use bug navigation algorithm to navigate to destination
    public void bugNav(RobotController rc, MapLocation dest) throws GameActionException {
        isBugging = true;
        MapLocation loc = rc.getLocation();
        Direction optDir = loc.directionTo(dest);
        Direction left = optDir.rotateLeft();
        Direction right = optDir.rotateRight();
        Direction leftLeft = left.rotateLeft();
        Direction rightRight= right.rotateRight();
        if (canGo(rc,optDir)) rc.move(optDir);
        else if (canGo(rc, left)) rc.move(left);
        else if (canGo(rc, right)) rc.move(right);
        else if (canGo(rc, leftLeft)) rc.move(leftLeft);
        else if (canGo(rc, rightRight)) rc.move(rightRight);
        stinkyTrail(rc.getLocation());
    }

    public boolean canGo(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        MapLocation goTo = rc.getLocation().add(dir);
        for (MapLocation stink: visited) {
            if (goTo.equals(stink)) return false;
        }
        return true;
    }

    // avoid going to blocks gone before
    public void stinkyTrail(MapLocation last) {
        while (visited.size() >= 5) visited.remove();
        visited.add(last);
    }

    public boolean isBugging() {
        return isBugging;
    }

    // end bug nav
    public void bugOff() {
        isBugging = false;
    }
}
