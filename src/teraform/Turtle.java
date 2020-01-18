package teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static teraform.Util.directions;
import static teraform.Robot.*;

public class Turtle {

    // 0: patrolling and rushing enemyHQ with disruptWithCow
    // 1: building outer wall
    // 2: building inner wall
    // 3: attack
    // TODO: implement state 3
    private int rotateState;
    private MapLocation HQLocation;
    private MapLocation netGunCenter;
    private ArrayList<MapLocation> digLocation;
    private Vector[] outerLoc;
    private Vector[] innerLoc;

    // initialize landscaper state
    public Turtle(RobotController rc, MapLocation HQLocation, int rotateState) throws GameActionException {
        this.HQLocation = HQLocation;
        this.rotateState = rotateState;
        netGunCenter = new Vector(6, 6).rotate(rotateState).addWith(HQLocation);
        // TODO: fix this
//        if (Vector.vectorSubtract(rc.getLocation(), HQLocation).equals(new Vector(-1, 1))) landscaperState = 3;
        outerLoc = new Vector[]{new Vector(2, 2), new Vector(2, 1), new Vector(2, 0), new Vector(2, -1), new Vector(2, -2), new Vector(1, -2), new Vector(0, -2), new Vector(-1, -2), new Vector(1, 2), new Vector(0, 2), new Vector(-1, 2), new Vector(-2, 2), new Vector(-2, 1), new Vector(-2, 0), new Vector(-2, -1)};
        innerLoc = new Vector[]{new Vector(-1, -1), new Vector(0, -1), new Vector(1, -1), new Vector(1, 0), new Vector(-1, 0), new Vector(-1, 1), new Vector(0, 1), new Vector(1, 1)} ;
        Vector[] digLoc = new Vector[]{new Vector(0, 2), new Vector(2, 0), new Vector(0, -2), new Vector(-2, 0)};
        digLocation = new ArrayList<>();
        for (int i = 0; i < outerLoc.length; i++) {
            outerLoc[i] = outerLoc[i].rotate(rotateState);
        }
        for (int i = 0; i < innerLoc.length; i++) {
            innerLoc[i] = innerLoc[i].rotate(rotateState);
        }
        for (int i = 0; i < digLoc.length; i++) {
            if (rc.onTheMap(digLoc[i].addWith(HQLocation))) digLocation.add(digLoc[i].addWith(HQLocation));
        }
    }

    public void buildFort(RobotController rc) throws GameActionException {
        if (teraformMode == 0) return;
        if (teraformMode == 1) buildOuterFort(rc);
        if (teraformMode == 2) buildInnerFort(rc);
    }

    private boolean isEvenOuter(RobotController rc, int index) throws GameActionException {
        MapLocation nextIndex;
        if (index != 4 && index != 9 && index != 14 && index != 19) {
            nextIndex = outerLoc[index+1].addWith(HQLocation);
        }
        else {
            nextIndex = rc.getLocation();
        }
        System.out.println("nextIndex position is: " + nextIndex.toString());
        if (rc.canSenseLocation(nextIndex)) {
            RobotInfo r = rc.senseRobotAtLocation(nextIndex);
            return r != null && r.getType() == RobotType.LANDSCAPER && r.getTeam() == rc.getTeam();
        }
        return false;
    }

    // try to build the outer layer, even is when we should try to build evenly
    private void buildOuterFort(RobotController rc) throws GameActionException {
        int index = positionOut(rc.getLocation());
        System.out.println("index is: " + index);
        if (index == -1) return;
        boolean even = isEvenOuter(rc, index);
        System.out.println("Even is " + even);
        Direction evenDir = rc.getLocation().directionTo(lowestElevationOuter(rc, index));
        System.out.println("Lowest elevation direction is " + evenDir.toString());
        if (index != 0 && index != 10) {
            MapLocation nextSpot = outerLoc[index-1].addWith(HQLocation);
            System.out.println("Next spot is " + nextSpot.toString());
            digOrMove(rc, even, nextSpot, evenDir);
        } else {
            // if no next spot, just dig dig dig
            if (rc.getDirtCarrying() == 0) {
                // dig
                System.out.println("I have no dirt, so digging");
                Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                if (rc.canDigDirt(digTo)) {
                    System.out.println("Digging towards " + digTo.toString());
                    rc.digDirt(digTo);
                }
            } else {
                // fill
                if (even && rc.canDepositDirt(evenDir)) {
                    System.out.println("Filling out " + evenDir.toString());
                    rc.depositDirt(evenDir);
                }
                if (rc.canDepositDirt(Direction.CENTER)) {
                    System.out.println("Filling out " + Direction.CENTER.toString());
                    rc.depositDirt(Direction.CENTER);
                }
            }
        }
    }

    // when building inner fort, automatically apply even option

    private void buildInnerFort(RobotController rc) throws GameActionException {
        int index = positionIn(rc.getLocation());
        if (index == -1) return;
        boolean even = isTurtle;
        Direction evenDir = rc.getLocation().directionTo(lowestElevationInner(rc));
        // if don't need to move don't move
        if (rc.getLocation().x == 0 || rc.getLocation().y == 0 || rc.getLocation().x == rc.getMapWidth()-1 || rc.getLocation().y == rc.getMapHeight()-1) {
            // then pretend this is last position
            if (rc.getDirtCarrying() == 0) {
                // dig
                System.out.println("I have no dirt");
                Direction digDir = getDig();
                if (rc.canDigDirt(digDir)) {
                    System.out.println("Digging towards " + digDir.toString());
                    rc.digDirt(digDir);
                }
            } else {
                // fill
                if (even && rc.canDepositDirt(evenDir)) {
                    System.out.println("Filling out at " + evenDir.toString());
                    rc.depositDirt(evenDir);
                }
                if (rc.canDepositDirt(Direction.CENTER)) {
                    rc.depositDirt(Direction.CENTER);
                }
            }
        }
        if (index == 0 || index == 4) {
            // last position
            if (rc.getDirtCarrying() == 0) {
                // dig
                System.out.println("I have no dirt");
                Direction optDir = getDig();
                if (rc.canDigDirt(optDir)) {
                    System.out.println("Digging towards " + optDir.toString());
                    rc.digDirt(optDir);
                }
            } else {
                // fill
                if (even && rc.canDepositDirt(evenDir)) {
                    System.out.println("Filling out at " + evenDir.toString());
                    rc.depositDirt(evenDir);
                }
                if (rc.canDepositDirt(Direction.CENTER)) {
                    rc.depositDirt(Direction.CENTER);
                }
            }
        }
        else if (index != 7) {
            MapLocation nextSpot = innerLoc[index-1].addWith(HQLocation);
            System.out.println("nextSpot is " + nextSpot.toString());
            if (rc.isReady()) {
                Direction optDir = rc.getLocation().directionTo(nextSpot);
                if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                    System.out.println("Moving towards " + optDir.toString());
                    rc.move(optDir);
                } else {
                    // if can't move, then check if we need to dig or bury
                    RobotInfo r = rc.senseRobotAtLocation(nextSpot);
                    if (r == null) {
                        if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(nextSpot)) {
                            System.out.println("The elevation is lower");
                            // if lower, fill
                            if (rc.getDirtCarrying() == 0) {
                                System.out.println("I don't have any dirt");
                                // dig
                                Direction digDir = getDig();
                                if (rc.canDigDirt(digDir)) {
                                    System.out.println("Digging towards " + digDir.toString());
                                    rc.digDirt(digDir);
                                }
                            } else {
                                // fill
                                if (rc.canDepositDirt(optDir)) {
                                    System.out.println("Filling out at " + optDir.toString());
                                    rc.depositDirt(optDir);
                                }
                            }
                        } else {
                            // if higher, dig the other thing
                            System.out.println("The elevation is higher");
                            if (rc.getDirtCarrying() == 0) {
                                // dig
                                if (rc.canDigDirt(optDir)) {
                                    System.out.println("Digging towards " + optDir.toString());
                                    rc.digDirt(optDir);
                                }
                            } else {
                                // fill
                                if (even && rc.canDepositDirt(evenDir)) {
                                    System.out.println("Filling out at " + evenDir.toString());
                                    rc.depositDirt(evenDir);
                                }
                                if (rc.canDepositDirt(Direction.CENTER)) {
                                    rc.depositDirt(Direction.CENTER);
                                }
                            }
                        }
                    }
                    else if (r.getType().isBuilding()) {
                        // if some other unit is in the way that is a building, bury it
                        System.out.println("Building is in the way");
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            Direction digDir = getDig();
                            if (rc.canDigDirt(digDir)) {
                                System.out.println("Digging towards " + digDir.toString());
                                rc.digDirt(digDir);
                            }
                        } else {
                            // fill
                            if (rc.canDepositDirt(optDir)) {
                                System.out.println("Filling out at " + evenDir.toString());
                                rc.depositDirt(optDir);
                            }
                        }
                    }
                    // if team is same or it's like an enemy unit or something
                    else if (!(r.getType() == RobotType.MINER && r.getTeam() == rc.getTeam())) {
                        System.out.println("A unit, so just dig normally");
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            System.out.println("I have no dirt");
                            Direction digDir = getDig();
                            if (rc.canDigDirt(digDir)) {
                                System.out.println("Digging towards " + digDir.toString());
                                rc.digDirt(digDir);
                            }
                        } else {
                            // fill
                            if (even && rc.canDepositDirt(evenDir)) {
                                System.out.println("Filling out at " + evenDir.toString());
                                rc.depositDirt(evenDir);
                            }
                            if (rc.canDepositDirt(Direction.CENTER)) {
                                rc.depositDirt(Direction.CENTER);
                            }
                        }
                    }
                }
            }
        } else {
            // index 7
            // if no next spot, just dig dig dig
            MapLocation nextSpot = innerLoc[index-1].addWith(HQLocation);
            System.out.println("nextSpot is " + nextSpot.toString());
            if (rc.isReady()) {
                Direction optDir = rc.getLocation().directionTo(nextSpot);
                if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                    System.out.println("Moving towards " + optDir.toString());
                    rc.move(optDir);
                } else {
                    // if can't move, then check if we need to dig or bury
                    RobotInfo r = rc.senseRobotAtLocation(nextSpot);
                    if (r == null) {
                        if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(nextSpot)) {
                            System.out.println("The elevation is lower");
                            // if lower, fill
                            if (rc.getDirtCarrying() == 0) {
                                System.out.println("I don't have any dirt");
                                // dig
                                Direction digDir = getDig();
                                if (rc.canDigDirt(digDir)) {
                                    System.out.println("Digging towards " + digDir.toString());
                                    rc.digDirt(digDir);
                                }
                            } else {
                                // fill
                                if (rc.canDepositDirt(optDir)) {
                                    System.out.println("Filling out at " + optDir.toString());
                                    rc.depositDirt(optDir);
                                }
                            }
                        } else {
                            // if higher, dig the other thing
                            System.out.println("The elevation is higher");
                            if (rc.getDirtCarrying() == 0) {
                                // dig
                                if (rc.canDigDirt(optDir)) {
                                    System.out.println("Digging towards " + optDir.toString());
                                    rc.digDirt(optDir);
                                }
                            } else {
                                // fill
                                if (even && rc.canDepositDirt(evenDir)) {
                                    System.out.println("Filling out at " + evenDir.toString());
                                    rc.depositDirt(evenDir);
                                }
                                if (rc.canDepositDirt(Direction.CENTER)) {
                                    rc.depositDirt(Direction.CENTER);
                                }
                            }
                        }
                    }
                    else if (r.getType().isBuilding()) {
                        // if some other unit is in the way that is a building, bury it
                        System.out.println("Building is in the way");
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            Direction digDir = getDig();
                            if (rc.canDigDirt(digDir)) {
                                System.out.println("Digging towards " + digDir.toString());
                                rc.digDirt(digDir);
                            }
                        } else {
                            // fill
                            if (rc.canDepositDirt(optDir)) {
                                System.out.println("Filling out at " + evenDir.toString());
                                rc.depositDirt(optDir);
                            }
                        }
                    }
                    // if team is same or it's like an enemy unit or something
                    else if (!(r.getType() == RobotType.MINER && r.getTeam() == rc.getTeam())){
                        System.out.println("A unit, so now check the other spot");
                        nextSpot = innerLoc[3].addWith(HQLocation);
                        if (rc.isReady()) {
                            optDir = rc.getLocation().directionTo(nextSpot);
                            if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                                System.out.println("Moving towards " + optDir.toString());
                                rc.move(optDir);
                            } else {
                                // if can't move, then check if we need to dig or bury
                                r = rc.senseRobotAtLocation(nextSpot);
                                if (r == null) {
                                    if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(nextSpot)) {
                                        System.out.println("The elevation is lower");
                                        // if lower, fill
                                        if (rc.getDirtCarrying() == 0) {
                                            System.out.println("I don't have any dirt");
                                            // dig
                                            Direction digDir = getDig();
                                            if (rc.canDigDirt(digDir)) {
                                                System.out.println("Digging towards " + digDir.toString());
                                                rc.digDirt(digDir);
                                            }
                                        } else {
                                            // fill
                                            if (rc.canDepositDirt(optDir)) {
                                                System.out.println("Filling out at " + optDir.toString());
                                                rc.depositDirt(optDir);
                                            }
                                        }
                                    } else {
                                        // if higher, dig the other thing
                                        System.out.println("The elevation is higher");
                                        if (rc.getDirtCarrying() == 0) {
                                            // dig
                                            if (rc.canDigDirt(optDir)) {
                                                System.out.println("Digging towards " + optDir.toString());
                                                rc.digDirt(optDir);
                                            }
                                        } else {
                                            // fill
                                            if (even && rc.canDepositDirt(evenDir)) {
                                                System.out.println("Filling out at " + evenDir.toString());
                                                rc.depositDirt(evenDir);
                                            }
                                            if (rc.canDepositDirt(Direction.CENTER)) {
                                                rc.depositDirt(Direction.CENTER);
                                            }
                                        }
                                    }
                                }
                                else if (r.getType().isBuilding()) {
                                    // if some other unit is in the way that is a building, bury it
                                    System.out.println("Building is in the way");
                                    if (rc.getDirtCarrying() == 0) {
                                        // dig
                                        Direction digDir = getDig();
                                        if (rc.canDigDirt(digDir)) {
                                            System.out.println("Digging towards " + digDir.toString());
                                            rc.digDirt(digDir);
                                        }
                                    } else {
                                        // fill
                                        if (rc.canDepositDirt(optDir)) {
                                            System.out.println("Filling out at " + evenDir.toString());
                                            rc.depositDirt(optDir);
                                        }
                                    }
                                }
                                // if team is same or it's like an enemy unit or something
                                else if (!(r.getType() == RobotType.MINER && r.getTeam() == rc.getTeam())) {
                                    System.out.println("A unit, so just dig normally");
                                    if (rc.getDirtCarrying() == 0) {
                                        // dig
                                        System.out.println("I have no dirt");
                                        Direction digDir = getDig();
                                        if (rc.canDigDirt(digDir)) {
                                            System.out.println("Digging towards " + digDir.toString());
                                            rc.digDirt(digDir);
                                        }
                                    } else {
                                        // fill
                                        if (even && rc.canDepositDirt(evenDir)) {
                                            System.out.println("Filling out at " + evenDir.toString());
                                            rc.depositDirt(evenDir);
                                        }
                                        if (rc.canDepositDirt(Direction.CENTER)) {
                                            rc.depositDirt(Direction.CENTER);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private MapLocation lowestElevationOuter(RobotController rc, int index) throws GameActionException {
        MapLocation lowest = rc.getLocation();
        MapLocation left, right;
        if (index == 5 || index == 9 || index == 10 || index == 14) {
            left = rc.getLocation().add(rotateDir(Direction.WEST));
            right = rc.getLocation().add(rotateDir(Direction.EAST));
        }
        else if (index == 0 || index == 4 || index == 15 || index == 19) {
            left = rc.getLocation().add(rotateDir(Direction.NORTH));
            right = rc.getLocation().add(rotateDir(Direction.SOUTH));
        }
        else {
            left = outerLoc[index-1].addWith(HQLocation);
            right = outerLoc[index+1].addWith(HQLocation);
        }
        if (rc.senseElevation(left) < rc.senseElevation(lowest)) {
            lowest = left;
        }
        if (rc.senseElevation(right) < rc.senseElevation(lowest)) {
            lowest = right;
        }
        System.out.println("Lowest is: " + lowest.toString());
        return lowest;
    }

    private MapLocation lowestElevationInner(RobotController rc) throws GameActionException {
        MapLocation lowest, left, right;
        ArrayList<MapLocation> posLoc = new ArrayList<>();
        Direction dir = HQLocation.directionTo(rc.getLocation());
        lowest = rc.getLocation();
        left = HQLocation.add(dir.rotateLeft());
        right = HQLocation.add(dir.rotateRight());
        if (rc.canSenseLocation(lowest)) posLoc.add(lowest);
        if (rc.canSenseLocation(left)) {
            if (!(left.x == 0 || left.x == rc.getMapWidth()-1 || left.y == 0 || left.y == rc.getMapHeight()-1)) posLoc.add(left);
        }
        if (rc.canSenseLocation(right)) {
            if (!(right.x == 0 || right.x == rc.getMapWidth()-1 || right.y == 0 || right.y == rc.getMapHeight()-1)) posLoc.add(right);
        }
        System.out.println("posLoc is: " + posLoc.toString());
        MapLocation pos = null;
        int curElevation = 0;
        for (MapLocation loc: posLoc) {
            if (pos == null || rc.senseElevation(loc) < curElevation) {
                pos = loc;
                curElevation = rc.senseElevation(loc);
            }
        }
        System.out.println("Lowest is: " + lowest.toString());
        return pos;
    }

    // call this to navigate to the right position initially
    public int positionOut(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < outerLoc.length; i++) {
            if (outerLoc[i].equals(vec)) return i;
        }
        return -1;
    }

    // call this to navigate to the right position initially
    public int positionIn(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < innerLoc.length; i++) {
            if (innerLoc[i].equals(vec)) return i;
        }
        return -1;
    }

    private void digOrMove(RobotController rc, boolean even, MapLocation nextSpot, Direction evenDir) throws GameActionException {
        if (rc.isReady()) {
            Direction optDir = rc.getLocation().directionTo(nextSpot);
            if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                // move if they really need to
                if (rc.getLocation().x == 0 || rc.getLocation().y == 0 || rc.getLocation().x == rc.getMapWidth()-1 || rc.getLocation().y == rc.getMapHeight()-1) {
                    // then pretend this is last position
                    if (rc.getDirtCarrying() == 0) {
                        // dig
                        System.out.println("I have no dirt");
                        if (rc.canDigDirt(Direction.CENTER)) {
                            System.out.println("Digging towards " + Direction.CENTER.toString());
                            rc.digDirt(Direction.CENTER);
                        }
                    } else {
                        // fill
                        if (rc.canDepositDirt(evenDir)) {
                            System.out.println("Filling out at " + evenDir.toString());
                            rc.depositDirt(evenDir);
                        }
                    }
                } else {
                    System.out.println("Moving towards " + optDir.toString());
                    rc.move(optDir);
                }
            } else {
                // if can't move, then check if we need to dig or bury
                RobotInfo r = rc.senseRobotAtLocation(nextSpot);
                if (r == null) {
                    if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(nextSpot)) {
                        System.out.println("Elevation is lower");
                        // if lower, fill
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            System.out.println("I have no dirt");
                            Direction digDir = rc.getLocation().directionTo(HQLocation).opposite();
                            if (rc.canDigDirt(digDir)) {
                                System.out.println("Digging towards " + optDir.toString());
                                rc.digDirt(digDir);
                            }
                        } else {
                            // fill
//                            if (even && rc.canDepositDirt(evenDir)) {
//                                System.out.println("Filling out " + evenDir.toString());
//                                rc.depositDirt(evenDir);
//                            }
                            if (rc.canDepositDirt(optDir)) {
                                System.out.println("Filling out " + optDir.toString());
                                rc.depositDirt(optDir);
                            }
                        }
                    } else {
                        // if higher, dig that place
                        System.out.println("Elevation is higher");
                        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                            // dig
                            if (rc.canDigDirt(optDir)) {
                                System.out.println("Digging towards " + optDir.toString());
                                rc.digDirt(optDir);
                            }
                        } else {
                            // fill
                            Direction fillDir = rc.getLocation().directionTo(HQLocation).opposite();
                            if (rc.canDepositDirt(fillDir)) {
                                System.out.println("Filling out " + fillDir.toString());
                                rc.depositDirt(fillDir);
                            }
                        }
                    }
                }
                else if (r.getType().isBuilding()) {
                    // if some other unit is in the way that is a building, bury it
                    System.out.println("There's a building in the way");
                    if (rc.getDirtCarrying() == 0) {
                        // dig
                        System.out.println("I have no dirt");
                        Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                        if (rc.canDigDirt(digTo)) {
                            System.out.println("Digging towards " + digTo.toString());
                            rc.digDirt(digTo);
                        }
                    } else {
                        // fill
                        if (rc.canDepositDirt(optDir)) {
                            System.out.println("Filling out " + optDir.toString());
                            rc.depositDirt(optDir);
                        }
                    }
                }
                // if team is same or it's like an enemy unit or something
                else {
                    System.out.println("Staying in place");
                    if (rc.getDirtCarrying() == 0) {
                        // dig
                        System.out.println("I have no dirt");
                        Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                        if (rc.canDigDirt(digTo)) {
                            System.out.println("Digging towards " + digTo.toString());
                            rc.digDirt(digTo);
                        }
                    } else {
                        // fill
                        if (even && rc.canDepositDirt(evenDir)) {
                            System.out.println("Filling out " + evenDir.toString());
                            rc.depositDirt(evenDir);
                        }
                        if (rc.canDepositDirt(Direction.CENTER)) {
                            System.out.println("Filling out " + Direction.CENTER.toString());
                            rc.depositDirt(Direction.CENTER);
                        }
                    }
                }
            }
        }
    }

    public void moveToTrail(RobotController rc) throws GameActionException {
        Vector offset = Vector.vectorSubtract(rc.getLocation(), HQLocation);
        System.out.println("I have offset of " + offset.getX() + ", " + offset.getY());
        if (teraformMode == 1) {
            if (offset.equals(new Vector(2, 2).rotate(rotateState))) {
                Direction dir1 = rotateDir(Direction.EAST);
                MapLocation loc1 = rc.getLocation().add(dir1);
                Direction dig = rotateDir(Direction.NORTHEAST);
                trytoMove(rc, dir1, dig, dig);
            } else {
                // should not reach here
                System.out.println("This landscaper is sad and wrong");
            }
        } else if (teraformMode == 2) {
            if (offset.equals(new Vector(2, 2).rotate(rotateState))) {
                Direction dir1 = rotateDir(Direction.SOUTHWEST);
                MapLocation loc1 = rc.getLocation().add(dir1);
                Direction dig = rotateDir(Direction.NORTHEAST);
                trytoMove(rc, dir1, dig, dig);
            } else {
                // should not reach here
                System.out.println("This landscaper is sad and wrong");
            }
        }
    }

    private void trytoMove(RobotController rc, Direction dir, Direction digFrom, Direction depositTo) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        if (rc.isReady()) {
            if (rc.canMove(dir) && !rc.senseFlooding(loc)) rc.move(dir);
            else {
                // if can't move, check if there is unit there
                RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r != null) return;
                else {
                    if (Math.abs(rc.senseElevation(rc.getLocation())-rc.senseElevation(loc)) > 20) return;
                    // check elevation
                    if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(loc)) {
                        // if lower
                        if (rc.getDirtCarrying() == 0) {
                            if (rc.canDigDirt(digFrom)) {
                                rc.digDirt(digFrom);
                            }
                        } else {
                            if (rc.canDepositDirt(dir)) {
                                rc.depositDirt(dir);
                            }
                        }
                    } else {
                        // if higher
                        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                            if (rc.canDigDirt(dir)) {
                                rc.digDirt(dir);
                            }
                        } else {
                            if (rc.canDepositDirt(depositTo)) {
                                rc.depositDirt(depositTo);
                            }
                        }
                    }
                }
            }
        }
    }

    public void attack(RobotController rc, MapLocation enemyHQLocation) throws GameActionException {
        // if next to enemy HQ bury it
        if (rc.getLocation().isAdjacentTo(enemyHQLocation)) {
            if (rc.getDirtCarrying() == 0) {
                // dig dirt
                Direction optDir = rc.getLocation().directionTo(enemyHQLocation).opposite();
                for (int i = 0; i < 8; i++) {
                    if (optDir != rc.getLocation().directionTo(enemyHQLocation)) {
                        if (rc.canDigDirt(optDir)) {
                            rc.digDirt(optDir);
                            break;
                        }
                    }
                }
            } else {
                Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
            }
        }
        else {
            // if not scan for surroundings and see if there's any open spots
            if (rc.canSenseLocation(enemyHQLocation)) {
                // empty spots
                ArrayList<MapLocation> emptySpots = new ArrayList<>();
                // non buildings and drones spot
                ArrayList<MapLocation> nonBuildingSpots = new ArrayList<>();
                // check enemy HQ surroundings and see if there's any openings
                for (Direction dir : directions) {
                    if (rc.canSenseLocation(enemyHQLocation.add(dir))) {
                        RobotInfo r = rc.senseRobotAtLocation(enemyHQLocation.add(dir));
                        if (r == null) emptySpots.add(enemyHQLocation.add(dir));
                        else if (r.getTeam() != rc.getTeam() && !r.getType().isBuilding() && r.getType() != RobotType.DELIVERY_DRONE) {
                            nonBuildingSpots.add(enemyHQLocation.add(dir));
                        }
                    }
                }
                // find the closest empty spot next to HQ
                MapLocation closestEmptySpot = null;
                for (MapLocation spots : emptySpots) {
                    if (closestEmptySpot == null || rc.getLocation().distanceSquaredTo(spots) < rc.getLocation().distanceSquaredTo(closestEmptySpot)) {
                        closestEmptySpot = spots;
                    }
                }
                MapLocation closestNonBuildingSpot = null;
                for (MapLocation spots : nonBuildingSpots) {
                    if (closestNonBuildingSpot == null || rc.getLocation().distanceSquaredTo(spots) < rc.getLocation().distanceSquaredTo(closestNonBuildingSpot)) {
                        closestNonBuildingSpot = spots;
                    }
                }
                if (closestEmptySpot != null) {
                    // move to that spot
                    // TODO: finish implementing attacking
                }
            } else {
                // try to move to enemyHQ
                // but i really doubt they can't see enemy HQ
                Direction dig = rc.getLocation().directionTo(enemyHQLocation);
                Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                for (int i = 0; i < 8; i++) {
                    trytoMove(rc, rc.getLocation().directionTo(enemyHQLocation), dig, dig);
                    optDir = optDir.rotateLeft();
                }
            }
        }
    }

    // get the digging position
    public Direction getDig() {
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            for (MapLocation digLoc: digLocation) {
                if (dig.equals(digLoc)) {
                    if (rc.canDigDirt(dir)) return dir;
                }
            }
        }
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            for (MapLocation digLoc: digLocation) {
                if (dig.equals(digLoc)) {
                    Direction left = dir.rotateLeft();
                    Direction right = dir.rotateRight();
                    if (rc.canDigDirt(left)) return left;
                    if (rc.canDigDirt(right)) return right;
                }
            }
        }
        // shouldn't reach here
        return rc.getLocation().directionTo(HQLocation).opposite();
    }

    private Direction rotateDir(Direction dir) {
        return Vector.getVec(dir).rotate(rotateState).getDir();
    }
}
