package bot01;

import battlecode.common.*;

import java.util.ArrayList;

import static java.lang.Math.min;

// Navigation class
public class Nav {

    private boolean isBugging;
    private int closestDist;
    private MapLocation currentDest;
    private ArrayList<MapLocation> threats= new ArrayList<>();
    private MapLocation lastLoc;

    public Nav() {
        isBugging = false;
        closestDist = 1000000;
        currentDest = null;
        lastLoc = null;
    }

    // use bug navigation algorithm to navigate to destination
    public void bugNav(RobotController rc, MapLocation dest) throws GameActionException {
        if (currentDest != dest) {
            navReset(dest);
        }
        closestDist = min(closestDist, rc.getLocation().distanceSquaredTo(dest));
        MapLocation loc = rc.getLocation();
        Direction optDir = loc.directionTo(dest);
        if (!rc.isReady()) return;
        if (!isBugging) {
            // if the state is free
            if (canGo(rc, optDir)) {
                rc.move(optDir);
            }
            else isBugging = true;
        }
        if (isBugging) {
            // if the state is bug
            boolean canMove = false;
            for (int i = 0; i < 8; i++) {
                if (canGo(rc, optDir)) {
                    canMove = true;
                    break;
                }
                else optDir = optDir.rotateRight();
                if (i == 7) return;
            }
            if (canMove) {
                lastLoc = rc.getLocation();
                rc.move(optDir);
            }
            else {
                if (!threats.isEmpty()) {
                    Direction safe = rc.getLocation().directionTo(threats.get(threats.size() - 1)).opposite();
                    if (rc.canMove(safe)) rc.move(safe);
                    else {
                        safe = safe.rotateLeft();
                        if (rc.canMove(safe)) rc.move(safe);
                        else {
                            safe = safe.rotateRight();
                            safe = safe.rotateRight();
                            if (rc.canMove(safe)) rc.move(safe);
                        }
                    }
                }
                // if you still can't move you're kind of screwed
            }
            if (rc.getLocation().distanceSquaredTo(dest)<closestDist) isBugging = false;
        }
    }

    public boolean canGo(RobotController rc, Direction dir) throws GameActionException {
        if (rc.getType() == RobotType.MINER) return canGoMiner(rc, dir);
        else if (rc.getType() == RobotType.DELIVERY_DRONE) return canGoDrone(rc, dir);
        return true;
    }

    public boolean canGoMiner(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        if (rc.getLocation().add(dir).equals(lastLoc)) return false;
        return true;
    }

    public boolean canGoDrone(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        RobotInfo[] robots = rc.senseNearbyRobots();
        MapLocation goodLoc = rc.getLocation().add(dir);
        for (MapLocation loc: threats) {
            if (goodLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) return false;
        }
        return true;
    }

    private void navReset(MapLocation dest) {
        isBugging = false;
        closestDist = 1000000;
        currentDest = dest;
    }

    public void addThreat(MapLocation loc) {
        threats.add(loc);
    }

    public void removeThreat(MapLocation loc) {
        threats.remove(loc);
    }

    public boolean isThreat(MapLocation loc) {
        return threats.contains(loc);
    }
}
