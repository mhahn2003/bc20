package pure_teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static pure_teraform.Cast.getMessage;
import static pure_teraform.Cast.infoQ;
import static pure_teraform.Util.directions;

public class Landscaper extends Unit {

    private ArrayList<MapLocation> visitedHole;
    private Direction fill;
    private Direction digLoc;


    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        visitedHole = new ArrayList<>();

    }

    public void initialize() throws GameActionException {
        super.initialize();
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (teraformMode == 0) {
//            if (rc.getLocation().isAdjacentTo(HQLocation)){
//                teraformMode=2;
//            }
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
            if (rc.getLocation().distanceSquaredTo(HQLocation) > 8 && (enemyHQLocation == null || !(rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36 && rc.getLocation().distanceSquaredTo(HQLocation) > 36))) {
                System.out.println("Case 1");
                Direction dig = holeTo();
                System.out.println("before checking hole locations, I have: " + Clock.getBytecodesLeft());
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
                                if (rc.getID() % 2 != 0) {
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
                // TODO: let drones carry them over
                System.out.println("HQLocation is: " + HQLocation);
                Direction op = rc.getLocation().directionTo(HQLocation).opposite();
                MapLocation loc = rc.getLocation().add(op);
                moveTo(loc);
            }
        }
//        if (teraformMode == 2) {
//            // dig hq if you can
//            if (rc.canDigDirt(rc.getLocation().directionTo(HQLocation))) rc.digDirt(rc.getLocation().directionTo(HQLocation));
//            // if spawn location has bad height then dig it
//            MapLocation spawn = HQLocation.add(rotateDir(Direction.NORTHEAST));
//            if (rc.getLocation().isAdjacentTo(spawn)) {
//                if (rc.canSenseLocation(spawn) && Math.abs(rc.senseElevation(spawn) - factoryHeight) > 3) {
//                    RobotInfo rob = rc.senseRobotAtLocation(spawn);
//                    if (rob == null) {
//                        if (rc.senseElevation(spawn) > factoryHeight) {
//                            // if the spawn location is higher
//                            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
//                                Direction dig = rc.getLocation().directionTo(spawn);
//                                if (rc.canDigDirt(dig)) rc.digDirt(dig);
//                            } else {
//                                if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
//                            }
//                        } else {
//                            // if the spawn location is lower
//                            if (rc.getDirtCarrying() == 0) {
//                                Direction dig = turtle.getDig();
//                                if (rc.canDigDirt(dig)) rc.digDirt(dig);
//                            } else {
//                                Direction fill = rc.getLocation().directionTo(spawn);
//                                if (rc.canDepositDirt(fill)) rc.depositDirt(fill);
//                            }
//                        }
//                    }
//                }
//            }
//            // build the turtle
//            turtle.buildFort(rc);
//        }
    }

    public Direction holeTo() throws GameActionException {
        int modX = HQLocation.x % 2;
        int modY = HQLocation.y % 2;
        int deepest=2000000000;
        Direction deepest_direction=Direction.CENTER;
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            if (dig.x % 2 == modX && dig.y % 2 == modY && rc.canDigDirt(dir)) {
                if (!rc.senseFlooding(dig) && surroundedLand(dig)) return dir;
                if (rc.senseElevation(dig)<deepest){
                    deepest=rc.senseElevation(dig);
                    deepest_direction=dir;
                }
            }
        }
        if (deepest<2000000000 && deepest_direction!=Direction.CENTER) return deepest_direction;
        System.out.println("ill be a grave bot");
        // this shouldn't happen
        return Direction.CENTER;
    }

    public boolean surroundedLand(MapLocation pos) throws GameActionException {
        // return true if surrounded by land
        for (int i = 0 ; i<8 ; i++){
            if ( rc.canSenseLocation(pos.add(directions[i])) && rc.senseFlooding(pos.add(directions[i])) ){
                return false;
            }
        }
        return true;
    } 

    public boolean isHole(Direction dir){
        MapLocation pos =rc.getLocation().add(dir);
        return pos.x%2 == HQLocation.x % 2 && pos.y%2 == HQLocation.y % 2 && pos.distanceSquaredTo(HQLocation) > 8;
    }

    public boolean isHole(MapLocation pos){
        return pos.x%2 == HQLocation.x % 2 && pos.y%2 == HQLocation.y % 2;
    }

    // returns the optimal height of a location. Adds 2 to the height if near water.
    public int optHeight(MapLocation loc) throws GameActionException {
        return 8;
    }

    public void checkFillAndDig(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            if (dig.equals(dir) || isHole(dir) && ! rc.senseFlooding(rc.getLocation().add(dir))) continue;
            MapLocation fill = rc.getLocation().add(dir);
            if (fill.distanceSquaredTo(HQLocation) <= 8) continue;
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
        MapLocation loc = rc.getLocation().add(dir);
        return !isHole(dir) && rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }

    public boolean fillMore(MapLocation hole) throws GameActionException {
        System.out.println("Before completing fillMore I have: " + Clock.getBytecodesLeft());
        for (Direction dir: directions) {
            MapLocation fill = hole.add(dir);
            if (fill.distanceSquaredTo(HQLocation) <= 8) continue;
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