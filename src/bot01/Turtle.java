package bot01;

import battlecode.common.*;

public class Turtle {

    // 0: patrolling and rushing enemyHQ with disruptWithCow
    // 1: building outer wall
    // 2: building inner wall
    // 3: disrupt with cow
    // TODO: implement state 3
    private int landscaperState;
    private MapLocation HQLocation;
    private Vector[] patrolLoc;
    private Vector[] outerLoc;
    private Vector[] innerLoc;

    // initialize landscaper state
    public Turtle(RobotController rc, MapLocation HQLocation) {
        this.HQLocation = HQLocation;
        boolean isVaporator = false;
        int netGunCount = 0;
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
                isVaporator = true;
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) {
                netGunCount++;
            }
        }
        if (netGunCount >= 4) {
            landscaperState = 2;
        }
        else if (isVaporator) {
            landscaperState = 1;
        }
        else {
            landscaperState = 0;
        }
        patrolLoc = new Vector[]{new Vector(-1, 2), new Vector(1, -2), new Vector(-2, 1), new Vector(2, -1), new Vector(1, 2)};
        outerLoc = new Vector[]{new Vector(-1, 3), new Vector(-2, 3), new Vector(-3, 3), new Vector(-3, 2), new Vector(-3, 1), new Vector(-3, 0), new Vector(-3, -1), new Vector(-3, -2), new Vector(-3, -3),
                new Vector(-2, -3), new Vector(-1, -3), new Vector(1, 3), new Vector(2, 3), new Vector(3, 3), new Vector(3, 2), new Vector(3, 1), new Vector(3, 0), new Vector(3, -1), new Vector(3, -2),
                new Vector(3, -3), new Vector(2, -3), new Vector(1, -3), new Vector(0, -3)};
        innerLoc = new Vector[]{new Vector(1,2), new Vector(2,1), new Vector(2, 0), new Vector(2, -1), new Vector(1, -2), new Vector(-1, 2), new Vector(-2, 1), new Vector(-2, 0), new Vector(-2, -1), new Vector(-1, -2), new Vector(0, -2)};
    }

    public int getLandscaperState() {
        return landscaperState;
    }

    public void setLandscaperState(int landscaperState) { this.landscaperState = landscaperState; }

    public void buildFort(RobotController rc) throws GameActionException {
        if (landscaperState == 0) return;
        if (landscaperState == 1) buildOuterFort(rc);
        if (landscaperState == 2) buildInnerFort(rc);
    }

    private boolean isEvenOuter(RobotController rc, int index) throws GameActionException {
        MapLocation nextIndex;
        if (index != 10 && index != 22) {
            nextIndex = outerLoc[index+1].addWith(HQLocation);
        } else if (index == 10) {
            nextIndex = rc.getLocation().add(Direction.EAST);
        } else {
            // index == 22
            nextIndex = rc.getLocation().add(Direction.WEST);
        }
        if (rc.canSenseLocation(nextIndex)) {
            RobotInfo r = rc.senseRobotAtLocation(nextIndex);
            return r != null && r.getType() == RobotType.LANDSCAPER && r.getTeam() == rc.getTeam();
        }
        return false;
    }

    // try to build the outer layer, even is when we should try to build evenly
    private void buildOuterFort(RobotController rc) throws GameActionException {
        int index = positionOut(rc.getLocation());
        if (index == -1) return;
        boolean even = isEvenOuter(rc, index);
        Direction evenDir = rc.getLocation().directionTo(lowestElevationOuter(rc, index));
        if (index != 0 && index != 11) {
            MapLocation nextSpot = outerLoc[index-1].addWith(HQLocation);
            digOrMove(rc, even, nextSpot, evenDir);
        } else {
            // if no next spot, just dig dig dig
            if (rc.getDirtCarrying() == 0) {
                // dig
                Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                if (rc.canDigDirt(digTo)) {
                    rc.digDirt(digTo);
                }
            } else {
                // fill
                if (even && rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
            }
        }
    }

    // when building inner fort, automatically apply even option
    private void buildInnerFort(RobotController rc) throws GameActionException {
        int index = positionIn(rc.getLocation());
        if (index == -1) return;
        Direction evenDir = rc.getLocation().directionTo(lowestElevationInner(rc));
        if (index != 0 && index != 5) {
            MapLocation nextSpot = innerLoc[index-1].addWith(HQLocation);
            if (rc.isReady()) {
                Direction optDir = rc.getLocation().directionTo(nextSpot);
                if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                    rc.move(optDir);
                } else {
                    // if can't move, then check if we need to dig or bury
                    RobotInfo r = rc.senseRobotAtLocation(nextSpot);
                    if (r == null) {
                        if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(nextSpot)) {
                            // if lower, fill
                            if (rc.getDirtCarrying() == 0) {
                                // dig
                                if (rc.canDigDirt(Direction.CENTER)) {
                                    rc.digDirt(Direction.CENTER);
                                }
                            } else {
                                // fill
                                if (rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                            }
                        } else {
                            // if higher, dig the other thing
                            if (rc.getDirtCarrying() == 0) {
                                // dig
                                if (rc.canDigDirt(optDir)) {
                                    rc.digDirt(optDir);
                                }
                            } else {
                                // fill
                                if (rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                            }
                        }
                    }
                    else if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                        // if some other unit is in the way that is a building, bury it
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            if (rc.canDigDirt(Direction.CENTER)) {
                                rc.digDirt(Direction.CENTER);
                            }
                        } else {
                            // fill
                            if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                        }
                    }
                    // if team is same or it's like an enemy unit or something
                    else {
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            if (rc.canDigDirt(Direction.CENTER)) {
                                rc.digDirt(Direction.CENTER);
                            }
                        } else {
                            // fill
                            if (rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                        }
                    }
                }
            }
        } else {
            // if no next spot, just dig dig dig
            if (rc.getDirtCarrying() == 0) {
                // dig
                if (rc.canDigDirt(Direction.CENTER)) {
                    rc.digDirt(Direction.CENTER);
                }
            } else {
                // fill
                if (rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
            }
        }
    }

    private MapLocation lowestElevationOuter(RobotController rc, int index) throws GameActionException {
        MapLocation lowest = rc.getLocation();
        MapLocation left, right;
        if (index == 0 || index == 10 || index == 11 || index == 22) {
            left = rc.getLocation().add(Direction.WEST);
            right = rc.getLocation().add(Direction.EAST);
        } else {
            left = outerLoc[index-1].addWith(HQLocation);
            right = outerLoc[index+1].addWith(HQLocation);
        }
        if (rc.senseElevation(left) < rc.senseElevation(lowest)) {
            lowest = left;
        }
        if (rc.senseElevation(right) < rc.senseElevation(lowest)) {
            lowest = right;
        }
        return lowest;
    }

    private MapLocation lowestElevationInner(RobotController rc) throws GameActionException {
        Vector v = Vector.vectorSubtract(rc.getLocation(), HQLocation);
        MapLocation lowest, left, right;
        if (v.getY() == 2) {
            // NORTH
            lowest = rc.getLocation().add(Direction.NORTH);
            left = rc.getLocation().add(Direction.NORTHWEST);
            right = rc.getLocation().add(Direction.NORTHEAST);
        }
        else if (v.getY() == -2) {
            // SOUTH
            lowest = rc.getLocation().add(Direction.SOUTH);
            left = rc.getLocation().add(Direction.SOUTHWEST);
            right = rc.getLocation().add(Direction.SOUTHEAST);
        }
        else if (v.getX() == 2) {
            // EAST
            lowest = rc.getLocation().add(Direction.EAST);
            left = rc.getLocation().add(Direction.NORTHEAST);
            right = rc.getLocation().add(Direction.SOUTHEAST);
        }
        else if (v.getX() == -2) {
            // WEST
            lowest = rc.getLocation().add(Direction.WEST);
            left = rc.getLocation().add(Direction.NORTHWEST);
            right = rc.getLocation().add(Direction.SOUTHWEST);
        }
        else {
            // this case should not happen
            System.out.println("Something is very very wrong with this landscaper");
            return rc.getLocation();
        }
        if (rc.senseElevation(left) < rc.senseElevation(lowest)) {
            lowest = left;
        }
        if (rc.senseElevation(right) < rc.senseElevation(lowest)) {
            lowest = right;
        }
        return lowest;
    }

    // call this to navigate to the right position initially
    public int positionOut(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < outerLoc.length; i++) {
            if (outerLoc[i].equals(vec)) return i;
        }
        landscaperState = 0;
        return -1;
    }

    // call this to navigate to the right position initially
    public int positionIn(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < innerLoc.length; i++) {
            if (innerLoc[i].equals(vec)) return i;
        }
        landscaperState = 0;
        return -1;
    }

    private void digOrMove(RobotController rc, boolean even, MapLocation nextSpot, Direction evenDir) throws GameActionException {
        if (rc.isReady()) {
            Direction optDir = rc.getLocation().directionTo(nextSpot);
            if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                rc.move(optDir);
            } else {
                // if can't move, then check if we need to dig or bury
                RobotInfo r = rc.senseRobotAtLocation(nextSpot);
                if (r == null) {
                    if (rc.senseElevation(rc.getLocation()) > rc.senseElevation(nextSpot)) {
                        // if lower, fill
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                            if (rc.canDigDirt(digTo)) {
                                rc.digDirt(digTo);
                            }
                        } else {
                            // fill
                            if (even && rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                            if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                        }
                    } else {
                        // if higher, place blocks on one self
                        if (rc.getDirtCarrying() == 0) {
                            // dig
                            if (rc.canDigDirt(optDir)) {
                                rc.digDirt(optDir);
                            }
                        } else {
                            // fill
                            if (even && rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                            if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                        }
                    }
                }
                else if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                    // if some other unit is in the way that is a building, bury it
                    if (rc.getDirtCarrying() == 0) {
                        // dig
                        Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                        if (rc.canDigDirt(digTo)) {
                            rc.digDirt(digTo);
                        }
                    } else {
                        // fill
                        if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                    }
                }
                // if team is same or it's like an enemy unit or something
                else {
                    if (rc.getDirtCarrying() == 0) {
                        // dig
                        Direction digTo = rc.getLocation().directionTo(HQLocation).opposite();
                        if (rc.canDigDirt(digTo)) {
                            rc.digDirt(digTo);
                        }
                    } else {
                        // fill
                        if (even && rc.canDepositDirt(evenDir)) rc.depositDirt(evenDir);
                        if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                    }
                }
            }
        }
    }



    public MapLocation findPatrol(RobotController rc) throws GameActionException {
        for (int i = 0; i < patrolLoc.length; i++) {
            MapLocation patLoc = patrolLoc[i].addWith(HQLocation);
            if (rc.canSenseLocation(patLoc)) {
                RobotInfo r = rc.senseRobotAtLocation(patLoc);
                if (r == null) return patLoc;
                // only if it's our team landscaper we don't move there
                if (r.getTeam() != rc.getTeam() || r.getType() != RobotType.LANDSCAPER) {
                    return patLoc;
                }
            }
        }
        // when they're far away and can't see anything
        return HQLocation;
    }
}
