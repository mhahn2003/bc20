package bot01;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static java.lang.Math.min;

// Navigation class
public class Nav {

    private boolean isBugging;
    private int closestDist;

    public Nav() {
        isBugging = false;
        closestDist = 1000000;
    }

    // use bug navigation algorithm to navigate to destination
    public void bugNav(RobotController rc, MapLocation dest) throws GameActionException {
        closestDist = min(closestDist, rc.getLocation().distanceSquaredTo(dest));
        MapLocation loc = rc.getLocation();
        Direction optDir = loc.directionTo(dest);
        if (!isBugging) {
            // if the state is free
            if (canGo(rc, optDir)) rc.move(optDir);
            else isBugging = true;
        } else {
            // if the state is bug
            for (int i = 0; i < 8; i++) {
                if (canGo(rc, optDir)) break;
                else optDir = optDir.rotateRight();
                if (i == 7) return;
            }
            rc.move(optDir);
            if (rc.getLocation().distanceSquaredTo(dest)<closestDist) isBugging = false;
        }
    }

    public boolean canGo(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        return true;
    }

    public void navReset() {
        isBugging = false;
        closestDist = 1000000;
    }
}
