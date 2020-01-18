package teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static java.lang.Math.min;
import static teraform.Util.*;
import static teraform.Robot.*;

// Navigation class
public class Nav {

    private boolean isBugging;
    private int closestDist;
    private MapLocation currentDest;
    private ArrayList<MapLocation> threats;
    private MapLocation lastLoc;
    private MapLocation lastLastLoc;
    private int travelDist;
    private int travelRound;
    private int wander;
    private int stuck;
    private MapLocation helpReq;
    private Vector[] untouchable;
    private MapLocation[] untouchableLoc;

    public Nav() {
        isBugging = false;
        closestDist = 1000000;
        currentDest = null;
        lastLoc = null;
        lastLastLoc = null;
        travelDist = 0;
        travelRound = 0;
        wander = 0;
        helpReq = null;
        stuck = 0;
        threats = new ArrayList<>();
        untouchable = new Vector[]{new Vector(0, 2), new Vector(0, -2), new Vector(2, 0), new Vector(-2, 0), new Vector(1, 1)};
        untouchableLoc = new MapLocation[5];
        for (int i = 0; i < 5; i++) {
            if (i == 4) untouchable[i] = untouchable[i].rotate(rotateState);
            untouchableLoc[i] = untouchable[i].addWith(HQLocation);
        }
    }

    // use bug navigation algorithm to navigate to destination
    public void bugNav(RobotController rc, MapLocation dest) throws GameActionException {
        if (rc.getType() == RobotType.DELIVERY_DRONE) {
            System.out.println("My threats are: " + threats.toString());
        }
        if (rc.getLocation().equals(dest)) {
            System.out.println("Location is equal");
            wander++;
            return;
        }
        if (!rc.isReady()) return;
        if (currentDest == null) {
            navReset(rc, dest);
        }
        if (!currentDest.equals(dest)) {
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
            if (canGo(rc, optDir, true)) {
                stuck = 0;
                rc.move(optDir);
            }
            else {
                tryToDig(rc, dest);
                isBugging = true;
            }
        }
        if (isBugging) {
            // if the state is bug
            boolean canMove = false;
            for (int i = 0; i < 8; i++) {
                if (i == 0) {
                    if (canGo(rc, optDir, true)) {
                        stuck = 0;
                        canMove = true;
                        break;
                    }
                }
                if (canGo(rc, optDir, false)) {
                    stuck = 0;
                    canMove = true;
                    break;
                }
                else optDir = optDir.rotateRight();
                tryToDig(rc, dest);
            }
            if (canMove) {
                lastLastLoc = lastLoc;
                lastLoc = rc.getLocation();
                rc.move(optDir);
            }
            else {
                boolean isStuck = true;
                if (!threats.isEmpty()) {
                    Direction safe = rc.getLocation().directionTo(threats.get(threats.size() - 1)).opposite();
                    if (rc.canMove(safe)) {
                        stuck = 0;
                        isStuck = false;
                        rc.move(safe);
                    }
                    else {
                        safe = safe.rotateLeft();
                        if (rc.canMove(safe)) {
                            stuck = 0;
                            isStuck = false;
                            rc.move(safe);
                        }
                        else {
                            safe = safe.rotateRight();
                            safe = safe.rotateRight();
                            if (rc.canMove(safe)) {
                                stuck = 0;
                                isStuck = false;
                                rc.move(safe);
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < 8; i++) {
                        if (rc.canMove(optDir)) {
                            stuck = 0;
                            isStuck = false;
                            rc.move(optDir);
                            break;
                        }
                        else optDir = optDir.rotateRight();
                        if (i == 7) {
                            return;
                        }
                    }
                }
                // if still can't move pretty screwed
                if (isStuck) stuck++;
            }
            if (rc.getLocation().distanceSquaredTo(dest)<closestDist) isBugging = false;
        }
        if (rc.getLocation().isAdjacentTo(currentDest)) wander++;
    }

    public boolean canGo(RobotController rc, Direction dir, boolean free) throws GameActionException {
        if (rc.getType() == RobotType.MINER) return canGoMiner(rc, dir, free);
        else if (rc.getType() == RobotType.LANDSCAPER) return canGoLandscaper(rc, dir);
        else if (rc.getType() == RobotType.DELIVERY_DRONE) return canGoDrone(rc, dir, free);
        return true;
    }

    public boolean canGoMiner(RobotController rc, Direction dir, boolean free) throws GameActionException {
        MapLocation moveTo = rc.getLocation().add(dir);
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        if (!free && (moveTo.equals(lastLoc) || moveTo.equals(lastLastLoc))) return false;
        if (!free) {
            for (MapLocation loc: untouchableLoc) {
                if (moveTo.equals(loc)) return false;
            }
        }
        // run away from enemy drones
        if (droneThreat(rc, moveTo)) return false;
        return true;
    }

    public boolean canGoLandscaper(RobotController rc, Direction dir) throws GameActionException {
        // TODO: change this to account for landscapers being able to dig
        MapLocation moveTo = rc.getLocation().add(dir);
        if (!rc.canMove(dir)) return false;
        if (rc.senseFlooding(rc.getLocation().add(dir))) return false;
        if (moveTo.equals(lastLoc) || moveTo.equals(lastLastLoc)) return false;
        // run away from enemy drones
        if (droneThreat(rc, moveTo)) return false;
        return true;
    }

    public boolean canGoDrone(RobotController rc, Direction dir, boolean free) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        MapLocation goodLoc = rc.getLocation().add(dir);
        if (!free && goodLoc.equals(lastLoc) || goodLoc.equals(lastLastLoc)) return false;
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
        helpReq = null;
        stuck = 0;
    }

    public void navSoftReset(RobotController rc, MapLocation dest) {
        if (currentDest != null) {
            travelDist += Math.abs(currentDest.x-dest.x) + Math.abs(currentDest.y-dest.y);
        }
        isBugging = false;
        closestDist = 1000000;
        currentDest = dest;
        wander = 0;
        stuck = 0;
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

    public ArrayList<MapLocation> getThreats() { return threats; }

    public boolean needHelp(RobotController rc, int turnCount, MapLocation loc) {
        if (!loc.equals(currentDest)) return false;
        if (rc.getLocation().isAdjacentTo(currentDest)) return false;
        // if under drone attack don't call for help
        if (droneThreat(rc, rc.getLocation())) return false;
        if (travelDist < 20) {
            if (rc.getRoundNum()-travelRound-15 > travelDist*5 && areDrones && turnCount > 25) {
                helpReq = rc.getLocation();
                System.out.println("I have traveled for " + (rc.getRoundNum()-travelRound));
                return true;
            }
            if (rc.getRoundNum()-travelRound-15 > travelDist*5 && areDrones && turnCount > 25) {
                helpReq = rc.getLocation();
                System.out.println("I have traveled for " + (rc.getRoundNum()-travelRound));
                return true;
            }
        }
        else {
            if (rc.getRoundNum()-travelRound - 10 > travelDist*7/4 && areDrones && turnCount > 25) {
                helpReq = rc.getLocation();
                System.out.println("I have traveled for " + (rc.getRoundNum()-travelRound));
                return true;
            }
        }
        return false;
    }

    public boolean outOfDrone(RobotController rc) {
        if (!rc.getLocation().equals(helpReq)) {
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

    public int getStuck() { return stuck; }

    private void tryToDig(RobotController rc, MapLocation dest) throws GameActionException {
        Direction optDir = rc.getLocation().directionTo(dest);
        if (rc.isReady() && rc.getType() == RobotType.LANDSCAPER && rc.getLocation().isAdjacentTo(dest)) {
            if (Math.abs(rc.senseElevation(rc.getLocation())-rc.senseElevation(dest)) > 3) {
                if (rc.senseElevation(rc.getLocation()) < rc.senseElevation(dest)) {
                    // if lower higher elevation
                    if (rc.canDigDirt(optDir)) rc.digDirt(optDir);
                } else {
                    // if higher elevation
                    if (rc.canDigDirt(Direction.CENTER)) rc.digDirt(Direction.CENTER);
                }
            }
        }
    }

    public void searchEnemyHQ(RobotController rc) throws GameActionException {
        if (wander >= wanderLimit) {
            resetEnemyHQSuspect();
        }
        bugNav(rc, enemyHQLocationSuspect);
    }

    public void resetEnemyHQSuspect() {
        idIncrease++;
        enemyHQLocationSuspect = suspects.get((rc.getID()+idIncrease) % 3);
    }

    // finds the next exploring location
    // if there is none left, bother enemy
    public void nextExplore() throws GameActionException {
        int closestDist = 1000000;
        for (int i = 0; i < 8; i++) {
            if (!suspectsVisited.get(suspects.get(i))) {
                if (rc.getLocation().distanceSquaredTo(suspects.get(i)) < closestDist) {
                    closestDist = rc.getLocation().distanceSquaredTo(suspects.get(i));
                    exploreTo = suspects.get(i);
                }
            }
        }
        if (closestDist == 1000000) exploreTo = enemyHQLocation;
    }
}
