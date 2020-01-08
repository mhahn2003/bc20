package bot01;

import battlecode.common.*;

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
        if (rc.getType() == RobotType.MINER) return canGoMiner(rc, dir);
        else if (rc.getType() == RobotType.DELIVERY_DRONE) return canGoDrone(rc, dir);
        // TODO: fix this function for different types of units (e.g. drones)
        return true;
    }

    public boolean canGoMiner(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        return true;
    }

    public boolean canGoDrone(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        RobotInfo[] robots = rc.senseNearbyRobots();
        MapLocation goodLoc = rc.getLocation().add(dir);
        for (RobotInfo r: robots) {
            if ((r.getType() == RobotType.NET_GUN || r.getType() == RobotType.HQ) && r.getTeam() != rc.getTeam()) {
                if (goodLoc.distanceSquaredTo(r.getLocation()) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) return false;
            }
        }
        return true;
    }

    public void navReset() {
        isBugging = false;
        closestDist = 1000000;
    }
}
