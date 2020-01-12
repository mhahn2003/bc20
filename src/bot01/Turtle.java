package bot01;

import battlecode.common.*;

public class Turtle {

    // 0: patrolling and rushing enemyHQ with disruptWithCow
    // 1: building outer wall
    // 2: building inner wall
    private int landScaperState;
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
            landScaperState = 2;
        }
        else if (isVaporator) {
            landScaperState = 1;
        }
        else {
            landScaperState = 0;
        }
        patrolLoc = new Vector[]{new Vector(-1, 2), new Vector(1, -2), new Vector(-2, 1), new Vector(2, -1), new Vector(1, 2)};
        outerLoc = new Vector[]{new Vector(-1, 3), new Vector(-2, 3), new Vector(-3, 3), new Vector(-3, 2), new Vector(-3, 1), new Vector(-3, 0), new Vector(-3, -1), new Vector(-3, -2), new Vector(-3, -3),
                new Vector(-2, -3), new Vector(-1, -3), new Vector(1, 3), new Vector(2, 3), new Vector(3, 3), new Vector(3, 2), new Vector(3, 1), new Vector(3, 0), new Vector(3, -1), new Vector(3, -2),
                new Vector(3, -3), new Vector(2, -3), new Vector(1, -3), new Vector(0, -3)};
        innerLoc = new Vector[]{new Vector(-1, 2), new Vector(-2, 1), new Vector(-2, 0), new Vector(-2, -1), new Vector(-1, -2), new Vector(1, 2), new Vector(2, 1), new Vector(2, 0), new Vector(2, -1), new Vector(1, -2), new Vector(0, -2)};
    }

    public int getLandscaperState() {
        return landScaperState;
    }

    public void buildFort(RobotController rc, boolean even) throws GameActionException {
        if (landScaperState == 0) return;
        if (landScaperState == 1) buildOuterFort(rc, even);
        if (landScaperState == 2) buildInnerFort(rc, even);
    }

    // try to build the outer layer, even is when we should try to build evenly
    private void buildOuterFort(RobotController rc, boolean even) throws GameActionException {
        int index = positionOut(rc.getLocation());
        if (index == -1) return;
        if (index != 0 && index != 11) {
            MapLocation nextSpot = outerLoc[index-1].addWith(HQLocation);
            if (rc.isReady()) {
                Direction optDir = rc.getLocation().directionTo(nextSpot);
                if (rc.canMove(optDir) && !rc.senseFlooding(nextSpot)) {
                    rc.move(optDir);
                } else {
                    // if can't move, then check if we need to dig or bury
                    Direction evenDir = rc.getLocation().directionTo(lowestElevationOuter(rc, index));
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
                    else if (r.getTeam() != rc.getTeam() && (r.getType() != RobotType.MINER && r.getType() != RobotType.DELIVERY_DRONE && r.getType() != RobotType.LANDSCAPER && r.getType() != RobotType.COW)) {
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
    }

    private MapLocation lowestElevationOuter(RobotController rc, int index) throws GameActionException {
        MapLocation lowest = rc.getLocation();
        MapLocation left;
        MapLocation right;
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

    private void buildInnerFort(RobotController rc, boolean even) {
        int index = positionIn(rc.getLocation());
        if (index == -1) return;
        if (index <= 4) {
            // left side
        } else {
            // right side
        }
    }

    public int positionOut(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < outerLoc.length; i++) {
            if (outerLoc[i].equals(vec)) return i;
        }
        landScaperState = 0;
        return -1;
    }

    public int positionIn(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < innerLoc.length; i++) {
            if (innerLoc[i].equals(vec)) return i;
        }
        landScaperState = 0;
        return -1;
    }
}
