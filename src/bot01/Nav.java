package bot01;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static java.lang.Math.min;

// Navigation class
public class Nav {

//    Queue<MapLocation> visited;
    private boolean isBugging;
    private int closestDist;

    public Nav() {
//        visited = new LinkedList<>();
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
//        if (tryMove(rc, optDir)) stinkyTrail(rc.getLocation(), rc);
//        else visited = new LinkedList<>();
    }

    public boolean canGo(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        MapLocation goTo = rc.getLocation().add(dir);
//        for (MapLocation stink: visited) {
//            if (goTo.equals(stink)) return false;
//        }
        return true;
    }

    // avoid going to blocks gone before
//    public void stinkyTrail(MapLocation last, RobotController rc) {
//        int trailSize = 6;
//        while (visited.size() >= trailSize) visited.remove();
//        visited.add(last);
//
//        // debug
//        if (!visited.isEmpty()) {
//            for (MapLocation loc : visited) {
//                rc.setIndicatorDot(loc, 255, 0, 0);
//            }
//        }
//    }

    public void navReset() {
        isBugging = false;
        closestDist = 1000000;
    }

//    public boolean tryMove(RobotController rc, Direction optDir) throws GameActionException {
//        Direction left = optDir.rotateLeft();
//        Direction right = optDir.rotateRight();
//        Direction leftLeft = left.rotateLeft();
//        Direction rightRight= right.rotateRight();
//        Direction leftLeftLeft = leftLeft.rotateLeft();
//        Direction rightRightRight = rightRight.rotateRight();
//        if (canGo(rc,optDir)) rc.move(optDir);
//        else if (canGo(rc, left)) rc.move(left);
//        else if (canGo(rc, right)) rc.move(right);
//        else if (canGo(rc, leftLeft)) rc.move(leftLeft);
//        else if (canGo(rc, rightRight)) rc.move(rightRight);
//        else if (canGo(rc, leftLeftLeft)) rc.move(leftLeftLeft);
//        else if (canGo(rc, leftLeftLeft)) rc.move(rightRightRight);
//        else if (canGo(rc, optDir.opposite())) rc.move(optDir.opposite());
//        else return false;
//        return true;
//    }
}
