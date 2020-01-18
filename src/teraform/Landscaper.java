package teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static teraform.Cast.getMessage;
import static teraform.Cast.infoQ;
import static teraform.Util.directions;

public class Landscaper extends Unit {

    private ArrayList<MapLocation> visitedHole;
    private Vector[] untouchable;
    private MapLocation[] untouchableLoc;
    private int untouchSize = 12;
    private Direction fill;
    private Direction digLoc;


    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        visitedHole = new ArrayList<>();

    }

    public void initialize() throws GameActionException {
        super.initialize();
        untouchable = new Vector[]{new Vector(1, 0), new Vector(1, -1), new Vector(0, -1), new Vector(-1, -1), new Vector(-1, 0), new Vector(-1, 1), new Vector(0, 1), new Vector(1, 1), new Vector(0, 2), new Vector (2, 0), new Vector(0, -2), new Vector(-2, 0)};
        untouchableLoc = new MapLocation[untouchSize];
        for (int i = 0; i < untouchSize; i++) {
            untouchableLoc[i] = untouchable[i].rotate(rotateState).addWith(HQLocation);
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (teraformMode == 0) {
            System.out.println("Initially I have: " + Clock.getBytecodesLeft());
            if (teraformLoc[0] == null) {
                System.out.println("My teraformLoc is: null");
            } else {
                for (MapLocation loc : teraformLoc) {
                    System.out.println("My teraformLoc is: " + loc.toString());
                }
            }
            // if in position to build turtle, build it instead
//            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 8) teraformMode = 1;
                // build the teraform
                // assume landscaper factory is distance 10 away from HQ
            if (rc.getLocation().distanceSquaredTo(HQLocation) > 2 && rc.getLocation().distanceSquaredTo(HQLocation) < 300 && (enemyHQLocation == null || !(rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36 && rc.getLocation().distanceSquaredTo(HQLocation) > 36))) {
                System.out.println("Case 1");
                Direction dig = holeTo();
                System.out.println("After checking hole locations, I have: " + Clock.getBytecodesLeft());
                fill = null;
                digLoc = null;
                checkFillAndDig(dig);
                System.out.println("After checking both locations, I have: " + Clock.getBytecodesLeft());
                if (fill == null) {
                    System.out.println("No place to fill");
                    // no place to fill, check if we need to shave off height instead
                    if (digLoc == null) {
                        System.out.println("No place to dig");
                        // nothing to do here, move onto another location after crossing this one out
                        MapLocation closeHole = rc.getLocation().add(dig);
                        if (visitedHole.contains(closeHole)) {
                            MapLocation hole = closestHole();
                            System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                            if (rc.getID() % 3 != 0) {
                                if (hole != null) {
                                    System.out.println("closest hole is: " + hole);
                                    moveTo(hole);
                                } else {
                                    moveTo(enemyHQLocationSuspect);
                                }
                            } else {
                                moveTo(enemyHQLocationSuspect);
                            }
                        } else {
                            if (fillMore(closeHole)) {
                                System.out.println("There's more to do!");
                                moveTo(closeHole);
                            } else {
                                sendHole(closeHole);
                                MapLocation hole = closestHole();
                                System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                                if (rc.getID() % 3 != 0) {
                                    if (hole != null) {
                                        System.out.println("closest hole is: " + hole);
                                        moveTo(hole);
                                    } else {
                                        moveTo(enemyHQLocationSuspect);
                                    }
                                } else {
                                    moveTo(enemyHQLocationSuspect);
                                }
                            }
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
        if (teraformMode == 2) {
            if (rc.canDigDirt(rc.getLocation().directionTo(HQLocation))) rc.digDirt(rc.getLocation().directionTo(HQLocation));
            // build the turtle
            turtle.buildFort(rc);
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

    // returns the optimal height of a location. Adds 2 to the height if near water.
    public int optHeight(MapLocation loc) throws GameActionException {
        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
        int waterHeight = 0;
        for (Direction dir: directions) {
            MapLocation posWater = loc.add(dir);
            if (rc.canSenseLocation(posWater) && rc.senseFlooding(posWater)) waterHeight = 2;
        }
        return Math.min(12, (int) (Math.floor(Math.sqrt(distFromFactory)/1.3)) + factoryHeight)+waterHeight;
    }

    public Direction fillTo(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            boolean bad = false;
            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    bad = true;
                    break;
                }
            }
            if (bad) continue;
            RobotInfo rob = rc.senseRobotAtLocation(fill);
            if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)
                    && (rob == null || !(rob.getType().isBuilding() && rob.getTeam() == rc.getTeam()))) return dir;
        }
        // if can't find anything
        return null;
    }

    public void checkFillAndDig(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            boolean bad = false;
            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    bad = true;
                    break;
                }
            }
            if (bad) continue;
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)
                        && (rob == null || !(rob.getType().isBuilding() && rob.getTeam() == rc.getTeam()))) {
                    this.fill = dir;
                    return;
                }
                if ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40)
                        || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0)) {
                    this.digLoc = dir;
                    return;
                }
            }
        }
    }

    public Direction digTo(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            boolean bad = false;
            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    bad = true;
                    break;
                }
            }
            if (bad) continue;
            RobotInfo rob = rc.senseRobotAtLocation(fill);
            if ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0)) return dir;
        }
        // if can't find anything
        return null;
    }
    // scans teraformLoc and checks
    public MapLocation closestHole() throws GameActionException {
        if (teraformLoc[0] == null) return null;
        MapLocation closest = null;
        int heuristic = 0;
        for (MapLocation hole: teraformLoc) {
            if (visitedHole.contains(hole)) continue;
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
        Direction left = optDir.rotateLeft();
        Direction right = optDir.rotateRight();
        Direction leftLeft = left.rotateLeft();
        Direction rightRight = right.rotateRight();
        Direction leftLeftLeft = leftLeft.rotateLeft();
        Direction rightRightRight = rightRight.rotateRight();
        Direction op = optDir.opposite();
        if (canMove(optDir)) rc.move(optDir);
        else if (canMove(left)) rc.move(left);
        else if (canMove(right)) rc.move(right);
        else if (canMove(leftLeft)) rc.move(leftLeft);
        else if (canMove(rightRight)) rc.move(rightRight);
        else if (canMove(leftLeftLeft)) rc.move(leftLeftLeft);
        else if (canMove(rightRightRight)) rc.move(rightRightRight);
        else if (canMove(op)) rc.move(op);
        else {
            // TODO: call for drone help
        }
    }

    public boolean canMove(Direction dir) throws GameActionException {
        Direction hole = holeTo();
        MapLocation loc = rc.getLocation().add(dir);
        for (int i = 0; i < untouchSize; i++) {
            if (untouchableLoc[i].equals(loc)) return false;
        }
        return !hole.equals(dir) && rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }

    public boolean fillMore(MapLocation hole) throws GameActionException {
        System.out.println("Before completing fillMore I have: " + Clock.getBytecodesLeft());
        for (Direction dir: directions) {
            MapLocation fill = hole.add(dir);
            boolean bad = false;
            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    bad = true;
                    break;
                }
            }
            if (bad) continue;
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (((rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < 40 && rc.senseElevation(fill) != optHeight(fill)) ||
                        (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0))
                && !(rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying == 0)) {
//                    if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)) System.out.println("first");
//                    if (rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) System.out.println("second");
//                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) System.out.println("third");
//                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0) System.out.println("fourth");
//                    System.out.println("Direction " + dir + " looks ok");
//                    System.out.println("optimal height is " + optHeight(fill));
//                    System.out.println("After completing fillMore I have: " + Clock.getBytecodesLeft());
                    return true;
                }
            }
        }
        System.out.println("After completing fillMore I have: " + Clock.getBytecodesLeft());
        return false;
    }

    public void sendHole(MapLocation closeHole) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(8, rc.getTeam());
        for (MapLocation loc: visitedHole) {
            if (loc.equals(closeHole)) return;
        }
        replaceOrAdd(closeHole);
        boolean alreadySent = false;
        for (RobotInfo rob: robots) {
            if (rob.getType() == RobotType.LANDSCAPER && rob.getLocation().isAdjacentTo(closeHole) && rob.getID() < rc.getID()) {
                alreadySent = true;
                break;
            }
        }
        if (alreadySent) return;
        System.out.println("Sending hole");
        infoQ.add(getMessage(Cast.InformationCategory.HOLE, closeHole));
    }

    public void replaceOrAdd(MapLocation loc) {
        if (visitedHole.size() >= 5) {
            visitedHole.remove(0);
        }
        visitedHole.add(loc);
    }
}