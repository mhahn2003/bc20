package teraform;

import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static teraform.Util.directions;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void initialize() throws GameActionException {
        super.initialize();
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (teraformMode == 0) {
            System.out.println("Initially I have: " + Clock.getBytecodesLeft());
            // if in position to build turtle, build it instead
            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 8) teraformMode = 1;
            else {
                // build the teraform
                // assume landscaper factory is distance 10 away from HQ
                if (rc.getLocation().distanceSquaredTo(HQLocation) >= 9 && rc.getLocation().distanceSquaredTo(HQLocation) < 300 && (enemyHQLocation == null || !(rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36 && rc.getLocation().distanceSquaredTo(HQLocation) > 36))) {
                    Direction fill = fillTo();
                    Direction dig = holeTo();
                    if (fill == null) {
                        // no place to fill, check if we need to shave off height instead
                        Direction digLoc = digTo();
                        System.out.println("After checking digging locations, I have: " + Clock.getBytecodesLeft());
                        if (digLoc == null) {
                            // nothing to do here, move onto another location after crossing this one out
                            MapLocation closeHole = rc.getLocation().add(dig);
                            if (!holeLoc.contains(closeHole)) holeLoc.add(closeHole);
                            MapLocation hole = closestHole();
                            System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                            if (hole != null) {
                                System.out.println("closest hole is: " + hole);
                                moveTo(hole);
                            } else {
                                moveTo(enemyHQLocationSuspect);
                            }
                        } else {
                            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                                if (rc.canDigDirt(digLoc)) rc.digDirt(digLoc);
                            } else {
                                if (rc.canDepositDirt(dig)) rc.depositDirt(dig);
                            }
                        }

                    } else {
                        if (rc.getDirtCarrying() == 0) {
                            if (rc.canDigDirt(dig)) rc.digDirt(dig);
                        } else {
                            if (rc.canDepositDirt(fill)) rc.depositDirt(fill);
                        }
                    }
                } else {
                    MapLocation hole = closestHole();
                    System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                    if (hole != null) {
                        System.out.println("closest hole is: " + hole);
                        moveTo(hole);
                    } else {
                        moveTo(enemyHQLocationSuspect);
                    }
                }
            }
        }
        if (teraformMode == 1) {
            // build the turtle
            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 2) {
                // if adjacent, dig under
                if (rc.getDirtCarrying() == 0) {
                    if (rc.canDigDirt(Direction.CENTER)) rc.digDirt(Direction.CENTER);
                }
                else {
                    Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                    if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                }
            }
            else if (rc.getLocation().distanceSquaredTo(HQLocation) <= 8) {
                // dig from opposite
            }
        }
    }

    public Direction holeTo() {
        int modX = HQLocation.x % 3;
        int modY = HQLocation.y % 3;
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            if (dig.x % 3 == modX && dig.y % 3 == modY) {
                return dir;
            }
        }
        // this shouldn't happen
        return Direction.CENTER;
    }

    public int optHeight(MapLocation loc) {
        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
        return Math.min(12, (int) (Math.floor(Math.sqrt(distFromFactory)/2)) + factoryHeight);
    }

    public Direction fillTo() throws GameActionException {
        Direction dig = holeTo();
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            if (rc.canSenseLocation(fill)) {
                if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)) return dir;
            }
        }
        // if can't find anything
        return null;
    }

    public Direction digTo() throws GameActionException {
        Direction dig = holeTo();
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            if (rc.canSenseLocation(fill)) {
                if (rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) return dir;
            }
        }
        // if can't find anything
        return null;
    }
    // only scans the 9 nearest holes
    public MapLocation closestHole() throws GameActionException {
        MapLocation closest = null;
        int heuristic = 0;
        ArrayList<MapLocation> nearbyHoles = scanNearbyHoles();

        if (nearbyHoles.isEmpty()) return null;
        for (MapLocation hole: scanNearbyHoles()) {
            int holeH = rc.getLocation().distanceSquaredTo(hole)+HQLocation.distanceSquaredTo(hole);
            if (closest == null || holeH < heuristic) {
                closest = hole;
                heuristic = holeH;
            }
        }
        return closest;
    }

    // because we reduced everything (except deep tiles or super tall tiles) we should be able to move pretty freely
    public void moveTo(MapLocation loc) throws GameActionException {
        Direction optDir = rc.getLocation().directionTo(loc);
        if (canMove(optDir)) {
            rc.move(optDir);
        } else {
            optDir = optDir.rotateLeft();
            if (canMove(optDir)) {
                rc.move(optDir);
            } else {
                optDir = optDir.rotateRight().rotateRight();
                if (canMove(optDir)) {
                    rc.move(optDir);
                } else {
                    // TODO: call for drone help
                }
            }
        }

    }

    public boolean canMove(Direction dir) throws GameActionException {
        Direction hole = holeTo();
        MapLocation loc = rc.getLocation().add(dir);
        return !hole.equals(dir) && rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }

    public ArrayList<MapLocation> scanNearbyHoles() throws GameActionException {
        ArrayList<MapLocation> closeHoles = new ArrayList<>();
        MapLocation loc = rc.getLocation().add(holeTo());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                boolean flag = true;
                MapLocation hole = loc.translate(i*3, j*3);
                if (hole.equals(HQLocation)) continue;
                if (rc.onTheMap(hole)) {
                    for (MapLocation h: holeLoc) {
                        if (hole.equals(h)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        closeHoles.add(hole);
                    }
                }
            }
        }
        return closeHoles;
    }
}