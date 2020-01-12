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
    private int travelDist;
    private int travelRound;
    private int wander;
    private MapLocation helpReq;

    public Nav() {
        isBugging = false;
        closestDist = 1000000;
        currentDest = null;
        lastLoc = null;
        travelDist = 0;
        travelRound = 0;
        wander = 0;
        helpReq = null;
    }

    // use bug navigation algorithm to navigate to destination
    public void bugNav(RobotController rc, MapLocation dest) throws GameActionException {
        if (currentDest != dest) {
            navSoftReset(rc, dest);
        }
        // if currently getting help don't move
        if (helpReq != null) return;
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
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (rc.canMove(optDir)) {
                            rc.move(optDir);
                            break;
                        }
                        else optDir = optDir.rotateRight();
                        if (i == 7) return;
                    }
                }
                // if still can't move pretty screwed
            }
            if (rc.getLocation().distanceSquaredTo(dest)<closestDist) isBugging = false;
        }
        if (rc.getLocation().isAdjacentTo(currentDest)) wander++;
    }

    public boolean canGo(RobotController rc, Direction dir) throws GameActionException {
        if (rc.getType() == RobotType.MINER) return canGoMiner(rc, dir);
        else if (rc.getType() == RobotType.DELIVERY_DRONE) return canGoDrone(rc, dir);
        return true;
    }

    public boolean canGoMiner(RobotController rc, Direction dir) throws GameActionException {
        MapLocation moveTo = rc.getLocation().add(dir);
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        if (moveTo.equals(lastLoc)) return false;
        // run away from enemy drones
        if (droneThreat(rc, moveTo)) return false;
        return true;
    }

    public boolean canGoDrone(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        MapLocation goodLoc = rc.getLocation().add(dir);
        if (rc.getLocation().add(dir).equals(lastLoc)) return false;
        for (MapLocation loc: threats) {
            if (goodLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) return false;
        }
        return true;
    }

    public void navReset(RobotController rc, MapLocation dest) {
        isBugging = false;
        closestDist = 1000000;
        currentDest = dest;
        travelDist = Math.abs(rc.getLocation().x-dest.x)+Math.abs(rc.getLocation().y-dest.y);
        travelRound = rc.getRoundNum();
    }

    public void navSoftReset(RobotController rc, MapLocation dest) {
        if (currentDest != null) {
            travelDist += Math.abs(currentDest.x-dest.x) + Math.abs(currentDest.y-dest.y);
        }
        isBugging = false;
        closestDist = 1000000;
        currentDest = dest;
        wander = 0;
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

    public boolean needHelp(RobotController rc, int turnCount, MapLocation loc) {
        if (!loc.equals(currentDest)) return false;
        // if under drone attack don't call for help
        if (droneThreat(rc, rc.getLocation())) return false;
        if (travelDist < 15) {
            if (rc.getRoundNum()-travelRound-10 > travelDist && rc.getRoundNum() >= 100 && turnCount > 25) {
                helpReq = rc.getLocation();
//                System.out.println("I have traveled for " + (rc.getRoundNum()-travelRound));
                return true;
            }
        }
        else {
            if ((rc.getRoundNum()-travelRound)*3/2 - 10 > travelDist && rc.getRoundNum() >= 100 && turnCount > 25) {
                helpReq = rc.getLocation();
//                System.out.println("I have traveled for " + (rc.getRoundNum()-travelRound));
                return true;
            }
        }
        return false;
    }

    public boolean outOfDrone(RobotController rc) {
        if (!rc.getLocation().equals(helpReq)) {
            helpReq = null;
            navReset(rc, currentDest);
            return true;
        }
        return false;
    }

    public boolean droneThreat(RobotController rc, MapLocation loc) {
        RobotInfo[] robots = rc.senseNearbyRobots(8);
        for (RobotInfo r: robots) {
            if (r.getTeam() != rc.getTeam() && r.getType() == RobotType.DELIVERY_DRONE) return true;
        }
        return false;
    }

    public int getWander() {
        return wander;
    }
}
