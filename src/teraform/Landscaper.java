package teraform;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Map;

import static teraform.Util.directions;

public class Landscaper extends Unit {

    public int factoryHeight;
    public Map<MapLocation, Boolean> holeLoc;

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void initialize() throws GameActionException {
        super.initialize();
        // find design school and record location
        if (factoryLocation == null) {
            for (Direction dir : directions) {
                MapLocation loc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo factory = rc.senseRobotAtLocation(loc);
                    if (factory != null && factory.getType() == RobotType.DESIGN_SCHOOL && factory.getTeam() == rc.getTeam()) {
                        factoryLocation = loc;
                        factoryHeight = rc.senseElevation(factoryLocation);
                        break;
                    }
                }
            }
        } else {
            if (rc.canSenseLocation(factoryLocation)) {
                factoryHeight = rc.senseElevation(factoryLocation);
            }
        }
        holeLoc = new HashMap<>();
        // generate possible holes
        for (int i = 0; i < rc.getMapWidth(); i++) {
            for (int j = 0; j < rc.getMapHeight(); j++) {
                if (HQLocation.x-i % 3 == 0 && HQLocation.y-j % 3 == 0) {
                    MapLocation hole = new MapLocation(i, j);
                    if (hole.equals(HQLocation)) continue;
                    holeLoc.put(hole, false);
                }
            }
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // TODO: implement landscapers
        if (teraformMode == 0) {
            // build the teraform
            // assume landscaper factory is distance 10 away from HQ
            if (rc.getLocation().distanceSquaredTo(HQLocation) > 18 && rc.getLocation().distanceSquaredTo(HQLocation) < 300 && (enemyHQLocation == null || !(rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36 && rc.getLocation().distanceSquaredTo(HQLocation) > 36))) {
                Direction fill = fillTo();
                Direction dig = digFrom();
                if (fill == null) {
                    // no place to fill, check if we need to shave off height instead
                    Direction digLoc = digTo();
                    if (digLoc == null) {
                        // nothing to do here, move onto another location after crossing this one out
                        MapLocation closeHole = rc.getLocation().add(dig);
                        holeLoc.replace(closeHole, true);
                        MapLocation hole = closestHole();
                        moveTo(hole);
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
            }
        }
        else if (teraformMode == 1) {
            // build the turtle
        }
    }

    public Direction digFrom() {
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            if (HQLocation.x-dig.x % 3 == 0 && HQLocation.y-dig.y % 3 == 0) {
                return dir;
            }
        }
        // this shouldn't happen
        return Direction.CENTER;
    }

    public int optHeight(MapLocation loc) {
        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
        return Math.min(15, (int) (Math.floor(Math.sqrt(distFromFactory)/3)) + factoryHeight);
    }

    public Direction fillTo() throws GameActionException {
        Direction dig = digFrom();
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
        Direction dig = digFrom();
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

    public MapLocation closestHole() throws GameActionException {
        MapLocation closest = null;
        int heuristic = 0;
        for (MapLocation hole: holeLoc.keySet()) {
            if (!holeLoc.get(hole)) {
                int holeH = rc.getLocation().distanceSquaredTo(hole)+HQLocation.distanceSquaredTo(hole);
                if (closest == null || holeH < heuristic) {
                    closest = hole;
                    heuristic = holeH;
                }
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
        MapLocation loc = rc.getLocation().add(dir);
        return rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }
}