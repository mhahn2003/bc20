package rushdef;

import battlecode.common.*;

import java.util.ArrayList;

import static rushdef.Cast.infoQ;
import static rushdef.Util.directions;

public class Landscaper extends Unit {

    private ArrayList<MapLocation> visitedHole;
    private Vector[] untouchable;
    private Vector[] reinforce;
    private Vector[] digging;
    private MapLocation[] untouchableLoc;
    private MapLocation[] reinforceLoc;
    private MapLocation[] diggingLoc;
    private int untouchSize = 12;
    private int reinforceSize = 8;
    private Direction fill;
    private Direction digLoc;
    private int digState = -1;

    // rushing stuff
    private ArrayList<MapLocation> emptySpots;
    Direction LFdir = null;
    Direction DFdir = null;
    Direction HQdir = null;
    Direction RFdir = null;


    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        visitedHole = new ArrayList<>();
        emptySpots = new ArrayList<>();
    }

    public void initialize() throws GameActionException {
        super.initialize();
        untouchable = new Vector[]{new Vector(1, 0), new Vector(1, -1), new Vector(0, -1), new Vector(-1, -1), new Vector(-1, 0), new Vector(-1, 1), new Vector(0, 1), new Vector(1, 1), new Vector(0, 2), new Vector(2, 0), new Vector(0, -2), new Vector(-2, 0)};
        reinforce = new Vector[]{new Vector(1, 2), new Vector(2, 1), new Vector(-1, 2), new Vector(2, -1), new Vector(1, -2), new Vector(-2, 1), new Vector(-1, -2), new Vector(-2, -1)};
        digging = new Vector[]{new Vector(0, 2), new Vector(0, 2), new Vector(0, -2), new Vector(-2, 0)};
        untouchableLoc = new MapLocation[untouchSize];
        for (int i = 0; i < untouchSize; i++) {
            untouchableLoc[i] = untouchable[i].addWith(HQLocation);
        }
        reinforceLoc = new MapLocation[reinforceSize];
        for (int i = 0; i < reinforceSize; i++) {
            reinforceLoc[i] = reinforce[i].addWith(HQLocation);
        }
        diggingLoc = new MapLocation[4];
        for (int i = 0; i < 4; i++) {
            diggingLoc[i] = digging[i].addWith(HQLocation);
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (rc.getLocation().isAdjacentTo(HQLocation)) teraformMode = 2;
        if (teraformMode == 0) {
//            System.out.println("floods at: " + (Util.floodRound(factoryHeight)-40));
            if (rc.getLocation().distanceSquaredTo(HQLocation) == 5) {
                teraformMode = 4;
            }
            if (rc.getRoundNum() > Util.floodRound(factoryHeight)-40 && rc.getRoundNum() < Util.floodRound(factoryHeight)+40) {
//                System.out.println("Switching to teraform mode 4");
                teraformMode = 4;
            }
            // teraform mode
//            System.out.println("Initially I have: " + Clock.getBytecodesLeft());
//            if (teraformLoc[0] == null) {
////                System.out.println("My teraformLoc is: null");
//            } else {
//                for (MapLocation loc : teraformLoc) {
////                    System.out.println("My teraformLoc is: " + loc.toString());
//                }
//            }
            // if in position to build turtle, build it instead
            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 2) teraformMode = 2;
                // build the teraform
                // assume landscaper factory is distance 10 away from HQ
            if (rc.getLocation().distanceSquaredTo(HQLocation) > 2 && rc.getLocation().distanceSquaredTo(HQLocation) < 300 && (enemyHQLocation == null || !(rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36 && rc.getLocation().distanceSquaredTo(HQLocation) > 36))) {
//                System.out.println("Case 1");
                Direction dig = holeTo();
//                System.out.println("After checking hole locations, I have: " + Clock.getBytecodesLeft());
                fill = null;
                digLoc = null;
                checkFillAndDig(dig);
                MapLocation hole = closestHole();
//                System.out.println("After checking both locations, I have: " + Clock.getBytecodesLeft());
                if (fill == null) {
//                    System.out.println("No place to fill");
                    // no place to fill, check if we need to shave off height instead
                    if (digLoc == null) {
//                        System.out.println("No place to dig");
                        // nothing to do here, move onto another location after crossing this one out
                        MapLocation closeHole = rc.getLocation().add(dig);
                        if (visitedHole.contains(closeHole)) {
//                            System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                            if (hole != null) {
//                                System.out.println("closest hole is: " + hole);
                                moveTo(hole);
                            } else {
                                moveTo(HQLocation);
                            }
                        } else {
                            if (hole != null && hole.distanceSquaredTo(HQLocation) <= 32) {
//                                System.out.println("closest hole is: " + hole);
                                moveTo(hole);
                            } else if (fillMore(closeHole)) {
//                                System.out.println("There's more to do!");
                                moveTo(closeHole);
                            } else {
                                sendHole(closeHole);
//                                System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                                if (hole != null) {
//                                    System.out.println("closest hole is: " + hole);
                                    moveTo(hole);
                                } else {
                                    moveTo(enemyHQLocationSuspect);
                                }
                            }
                        }
                    } else {
                        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
//                            System.out.println("Digging at digLoc: " + digLoc.toString());
                            if (rc.canDigDirt(digLoc)) rc.digDirt(digLoc);
//                        } else {
//                            System.out.println("Depositing at dig: " + dig.toString());
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
//                System.out.println("After checking closest hole, I have: " + Clock.getBytecodesLeft());
                if (hole != null) {
//                    System.out.println("closest hole is: " + hole);
                    moveTo(hole);
                } else {
                    moveTo(enemyHQLocationSuspect);
                }
            }
        } else if (teraformMode == 1) {
            // rush mode
            // prioritize: getting to empty spot -> digging other landscaper factory out -> digging hq out -> digging other buildings out -> digging walls out
            RobotInfo[] robots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
            HQdir = null;
            LFdir = null;
            DFdir = null;
            RFdir = null;
            getDirections(robots);
//            if (RFdir != null) System.out.println("Rfdir is: " + RFdir.toString());
            Direction digTo = getAttackDig();
            if (rc.getLocation().isAdjacentTo(enemyHQLocation)) {
                if (LFdir != null) {
                    if (rc.getDirtCarrying() > 0) {
                        if (rc.canDepositDirt(LFdir)) rc.depositDirt(LFdir);
                    } else {
                        if (rc.canDigDirt(digTo)) rc.digDirt(digTo);
                    }
                }
                else if (HQdir != null) {
                    if (rc.getDirtCarrying() > 0) {
                        if (rc.canDepositDirt(HQdir)) rc.depositDirt(HQdir);
                    } else {
                        if (rc.canDigDirt(digTo)) rc.digDirt(digTo);
                    }
                }
            } else {
                // try to get to an empty spot
                getEmpty();
                if (emptySpots.isEmpty()) {
                    // try to dig down buildings if you can
                    if (digState == 1) {
                        // dig from our own building
                        if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                            // dig from there
                            if (rc.canDigDirt(digTo)) rc.digDirt(digTo);
                        }
                    }
                    if (LFdir != null) {
                        if (rc.getDirtCarrying() > 0) {
                            if (rc.canDepositDirt(LFdir)) rc.depositDirt(LFdir);
                        } else {
                            if (rc.canDigDirt(digTo)) rc.digDirt(digTo);
                        }
                    }
                    else if (DFdir != null) {
                        if (rc.getDirtCarrying() > 0) {
                            if (rc.canDepositDirt(DFdir)) rc.depositDirt(DFdir);
                        } else {
                            if (rc.canDigDirt(digTo)) rc.digDirt(digTo);
                        }
                    }
                    else if (RFdir != null) {
                        if (rc.getDirtCarrying() > 0) {
                            if (rc.canDepositDirt(RFdir)) {
//                                System.out.println("I'm depositing towards: " + RFdir.toString());
                                rc.depositDirt(RFdir);
                            }
                        } else {
                            if (rc.canDigDirt(digTo)) {
//                                System.out.println("I'm digging towards: " + digTo.toString());
                                rc.digDirt(digTo);
                            }
                        }
                    }
                    else {
                        // first check if there's any design school / fullfillment center
                        // if there is, navigate to those
                        MapLocation landscaperSpot = null;
                        MapLocation droneSpot = null;
                        MapLocation refinerySpot = null;
                        int landDist = 0;
                        int flyDist = 0;
                        int soupDist = 0;
                        RobotInfo[] fullRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                        for (RobotInfo r: fullRobots) {
                            if (r.getType() == RobotType.DESIGN_SCHOOL) {
                                int tempDist = rc.getLocation().distanceSquaredTo(r.getLocation());
                                if (landscaperSpot == null || tempDist < landDist) {
                                    landscaperSpot = r.getLocation();
                                    landDist = tempDist;
                                }
                            }
                            if (r.getType() == RobotType.FULFILLMENT_CENTER) {
                                int tempDist = rc.getLocation().distanceSquaredTo(r.getLocation());
                                if (droneSpot == null || tempDist < flyDist) {
                                    droneSpot = r.getLocation();
                                    flyDist = tempDist;
                                }
                            }
                            if (r.getType() == RobotType.REFINERY) {
                                int tempDist = rc.getLocation().distanceSquaredTo(r.getLocation());
                                if (refinerySpot == null || tempDist < soupDist) {
                                    refinerySpot = r.getLocation();
                                    soupDist = tempDist;
                                }
                            }
                        }
                        if (landscaperSpot != null) {
                            // go to landscaper
                            nav.bugNav(rc, landscaperSpot);
                        } else {
                            // go to drone center
                            if (droneSpot != null) {
                                nav.bugNav(rc, droneSpot);
                            } else {
                                if (refinerySpot != null) {
                                    nav.bugNav(rc, refinerySpot);
                                } else {
                                    // if nothing's there? dig miners out
                                    // check if there's any miners surrounding HQ, go to them, and wait
                                    // if there is none, then just dig down the walls
                                    MapLocation minerSpot = null;
                                    int curDist = 0;
                                    for (Direction dir : directions) {
                                        MapLocation loc = enemyHQLocation.add(dir);
                                        if (rc.canSenseLocation(loc)) {
                                            RobotInfo r = rc.senseRobotAtLocation(loc);
                                            if (r != null && r.getType() == RobotType.MINER && r.getTeam() != rc.getTeam()) {
                                                // go and try to steal that miners spot!
                                                // check elevation since you'll be digging his spot
                                                if (rc.canSenseLocation(enemyHQLocation)) {
                                                    if (Math.abs(rc.senseElevation(enemyHQLocation) - rc.senseElevation(r.getLocation())) > 3) continue;
                                                }
                                                int tempDist = rc.getLocation().distanceSquaredTo(r.getLocation());
                                                if (minerSpot == null || tempDist < curDist) {
                                                    minerSpot = r.getLocation();
                                                    curDist = tempDist;
                                                }
                                            }
                                        }
                                    }
                                    if (minerSpot != null) {
                                        // if adjacent to it, just wait there
                                        if (!rc.getLocation().isAdjacentTo(minerSpot)) {
                                            // if not, move there
                                            nav.bugNav(rc, minerSpot);
                                        } else {
                                            // if you're next to miner, dig him down / fill him up
                                            Direction minerDir = rc.getLocation().directionTo(minerSpot);
                                            // always lower it
                                            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                                                if (rc.canDigDirt(minerDir)) rc.digDirt(minerDir);
                                            } else {
                                                Direction optDir = rc.getLocation().directionTo(enemyHQLocation).opposite();
                                                if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                                            }
                                        }
                                    } else {
                                        // if there's no miner spots? (probably all landscapers)
                                        // we should try to dig them down, but for now we'll just navigate to enemy HQ
                                        // TODO: fix this
                                        nav.bugNav(rc, enemyHQLocation);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    MapLocation goTo = null;
                    int dist = 0;
                    for (MapLocation loc: emptySpots) {
                        int tempD = rc.getLocation().distanceSquaredTo(loc);
                        if (goTo == null || tempD < dist) {
                            goTo = loc;
                            dist = tempD;
                        }
                    }
//                    System.out.println("Going to: " + goTo.toString());
                    nav.bugNav(rc, goTo);
                }
            }
            // if after everything they're stuck, try to dig themselves out
            if (rc.isReady()) {
                if (!checkMove()) {
                    Direction high = highest();
                    if (rc.getDirtCarrying() == 0) {
                        if (rc.canDigDirt(high)) rc.digDirt(high);
                    } else {
                        if (high.equals(Direction.CENTER)) {
                            // place it somewhere random
                            Direction optDir = rc.getLocation().directionTo(enemyHQLocation).opposite();
                            if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                        } else {
                            // place it on you
                            if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                        }
                    }
                }
            }
        }
        else if (teraformMode == 2) {
            // turtle mode
            // dig hq if you can
            if (rc.canDigDirt(rc.getLocation().directionTo(HQLocation))) rc.digDirt(rc.getLocation().directionTo(HQLocation));
            // dig other locations if you can
            RobotInfo[] buildings = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo r: buildings) {
                if (r.getType().isBuilding()) {
                    Direction digDir = rc.getLocation().directionTo(r.getLocation());
                    if (rc.canDigDirt(digDir)) rc.digDirt(digDir);
                }
            }
            // if spawn location has bad height then dig it
            MapLocation spawn = HQLocation.add(rotateDir(Direction.NORTHEAST));
            if (rc.getLocation().isAdjacentTo(spawn) && rc.getRoundNum() < Util.floodRound(rc.senseElevation(HQLocation))-40) {
                if (rc.canSenseLocation(spawn) && Math.abs(rc.senseElevation(spawn) - factoryHeight) > 3) {
                    RobotInfo rob = rc.senseRobotAtLocation(spawn);
                    if (rob == null) {
                        if (rc.senseElevation(spawn) > factoryHeight) {
                            // if the spawn location is higher
                            if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                                Direction dig = rc.getLocation().directionTo(spawn);
                                if (rc.canDigDirt(dig)) rc.digDirt(dig);
                            } else {
                                if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                            }
                        } else {
                            // if the spawn location is lower
                            if (rc.getDirtCarrying() == 0) {
                                Direction dig = turtle.getDig();
                                if (rc.canDigDirt(dig)) rc.digDirt(dig);
                            } else {
                                Direction fill = rc.getLocation().directionTo(spawn);
                                if (rc.canDepositDirt(fill)) rc.depositDirt(fill);
                            }
                        }
                    }
                }
            }
            // go to opposite side if they can
            MapLocation op = new Vector(-1, -1).rotate(rotateState).addWith(HQLocation);
            if (rc.canSenseLocation(op)) {
                RobotInfo r = rc.senseRobotAtLocation(op);
                if (r == null && rc.getLocation().isAdjacentTo(op)) {
                    Direction moveDir = rc.getLocation().directionTo(op);
                    if (rc.canMove(moveDir)) rc.move(moveDir);
                }
            }
            // if not build the turtle
            if (rc.isReady()) turtle.buildFort(rc);
        }
        else if (teraformMode == 4) {
            System.out.println("In teraform mode 4!");
            // reinforce the turtle
            for (MapLocation loc: reinforceLoc) {
                if (rc.getLocation().equals(loc)) {
                    Direction dig = digReinforce();
                    RobotInfo rob = rc.senseRobotAtLocation(rc.getLocation().add(dig));
                    if (rc.senseElevation(rc.getLocation()) >= GameConstants.getWaterLevel(rc.getRoundNum())+3) {
                        System.out.println("Putting dirt on turtle");
                        // refinforce turtle
                        if (rc.getDirtCarrying() == 0 && (rob == null || rob.getTeam() != rc.getTeam() || rob.getType() == RobotType.DELIVERY_DRONE || rc.senseElevation(rc.getLocation().add(dig)) < -10)) {
                            if (rc.canDigDirt(dig)) rc.digDirt(dig);
                        } else {
                            Direction fill = lowestElevation();
                            if (rc.canDepositDirt(fill)) rc.depositDirt(fill);
                        }
                    } else {
                        System.out.println("Putting dirt on myself");
                        if (rc.getDirtCarrying() == 0 && (rob == null || rob.getTeam() != rc.getTeam() || rob.getType() == RobotType.DELIVERY_DRONE || rc.senseElevation(rc.getLocation().add(dig)) < -10)) {
                            if (rc.canDigDirt(dig)) rc.digDirt(dig);
                        } else {
                            if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                        }
                    }
                    return;
                }
            }
            if (rc.isReady()) {
                for (MapLocation loc: reinforceLoc) {
                    if (rc.getLocation().isAdjacentTo(loc) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc)) {
                        Direction dir = rc.getLocation().directionTo(loc);
                        if (rc.canMove(dir)) rc.move(dir);
                    }
                }
            }
            if (rc.isReady()) {
                // TODO: implement landscapers digging their way through
                Direction dig = holeTo();
//                System.out.println("After checking hole locations, I have: " + Clock.getBytecodesLeft());
                fill = null;
                digLoc = null;
                checkFillAndDig(dig);
//                MapLocation hole = closestHole();
//                System.out.println("After checking both locations, I have: " + Clock.getBytecodesLeft());
                if (fill == null) {
//                    System.out.println("No place to fill");
                    // no place to fill, move to available spots
                    MapLocation emptySpot = null;
                    for (MapLocation loc: reinforceLoc) {
                        if (rc.canSenseLocation(loc)) {
                            RobotInfo r = rc.senseRobotAtLocation(loc);
                            if (r == null) {
                                emptySpot = loc;
                            }
                        }
                    }
                    if (emptySpot != null) moveTo(emptySpot);
                    else nav.bugNav(rc, HQLocation);
                } else {
                    if (rc.getDirtCarrying() == 0) {
                        if (rc.canDigDirt(dig)) rc.digDirt(dig);
                    } else {
                        if (rc.canDepositDirt(fill)) rc.depositDirt(fill);
                    }
                }
            }
        }
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
//        System.out.println("ill be a grave bot");
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
        return pos.x%2 == HQLocation.x % 2 && pos.y%2 == HQLocation.y % 2;
    }

    // returns the optimal height of a location. Adds 2 to the height if near water.
    public int optHeight(MapLocation loc) throws GameActionException {
        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
        int droneHeight = 0;
        if (droneFactoryLocation != null) {
            if (loc.isAdjacentTo(droneFactoryLocation)) droneHeight+=2;
        }
        return Math.min(7, (int) (Math.floor(Math.sqrt(distFromFactory)/1.5)) + factoryHeight)+droneHeight;
    }

    public void checkFillAndDig(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            if (!rc.canSenseLocation(rc.getLocation().add(dir))) continue;
            if (dig.equals(dir) || isHole(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) continue;
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
                if ((rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill) && (rob == null || !rob.getType().isBuilding())) ||
                        (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam())) {
                    if (!(fill.distanceSquaredTo(HQLocation) == 5 && rob != null && rob.getType() == RobotType.LANDSCAPER && rob.getTeam() == rc.getTeam())) {
                        this.fill = dir;
                        return;
                    }
                }
                if ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40)
                        && (rob == null || !rob.getType().isBuilding() || (rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0))) {
                    if (!(fill.distanceSquaredTo(HQLocation) == 5 && rob != null && rob.getType() == RobotType.LANDSCAPER && rob.getTeam() == rc.getTeam())) {
                        this.digLoc = dir;
                        return;
                    }
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
            int holeH = rc.getLocation().distanceSquaredTo(hole)+10*HQLocation.distanceSquaredTo(hole);
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
            if (untouchableLoc[i].equals(loc)|| loc.x%2 == HQLocation.x%2 && loc.y%2 == HQLocation.y%2) return false;
        }
        return !hole.equals(dir) && rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }

    public boolean fillMore(MapLocation hole) throws GameActionException {
//        System.out.println("Before completing fillMore I have: " + Clock.getBytecodesLeft());
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
                if (((rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill) && (rob == null || !rob.getType().isBuilding())) ||
                        (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()))
                || ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40)
                        && (rob == null || !rob.getType().isBuilding() || (rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0)))) {
//                    if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)) System.out.println("first");
//                    if (rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) System.out.println("second");
//                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) System.out.println("third");
//                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0) System.out.println("fourth");
//                    System.out.println("Direction " + dir + " looks ok");
//                    System.out.println("optimal height is " + optHeight(fill));
//                    System.out.println("After completing fillMore I have: " + Clock.getBytecodesLeft());
                    if (!(fill.distanceSquaredTo(HQLocation) == 5 && rob != null && rob.getType() == RobotType.LANDSCAPER && rob.getTeam() == rc.getTeam())) {
                        return true;
                    }
                }
            }
        }
//        System.out.println("After completing fillMore I have: " + Clock.getBytecodesLeft());
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
//        System.out.println("Sending hole");
        infoQ.add(Cast.getMessage(Cast.InformationCategory.HOLE, closeHole));
    }

    public void replaceOrAdd(MapLocation loc) {
        if (visitedHole.size() >= 5) {
            visitedHole.remove(0);
        }
        visitedHole.add(loc);
    }



    public void getEmpty() throws GameActionException {
        // assuming enemyHQLocation is not null
        emptySpots = new ArrayList<>();
        for (Direction dir: directions) {
            MapLocation loc = enemyHQLocation.add(dir);
            if (isEmpty(loc)) emptySpots.add(loc);
        }
    }

    public boolean isEmpty(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            RobotInfo rob = rc.senseRobotAtLocation(loc);
            return rob == null && Math.abs(rc.senseElevation(loc)-rc.senseElevation(enemyHQLocation)) <= 3;
        }
        return false;
    }

    public void getDirections(RobotInfo[] robots) {
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.HQ && r.getTeam() != rc.getTeam()) {
                HQdir = rc.getLocation().directionTo(r.getLocation());
            }
            if (r.getType() == RobotType.DESIGN_SCHOOL && r.getTeam() != rc.getTeam()) {
                LFdir = rc.getLocation().directionTo(r.getLocation());
            }
            if (r.getType() == RobotType.FULFILLMENT_CENTER && r.getTeam() != rc.getTeam()) {
                DFdir = rc.getLocation().directionTo(r.getLocation());
            }
            if (r.getType() == RobotType.REFINERY && r.getTeam() != rc.getTeam()) {
                RFdir = rc.getLocation().directionTo(r.getLocation());
            }
        }
    }

    public Direction getAttackDig() throws GameActionException {
        // priorities: our design school, our net guns, and opponent miners
        Direction digDir;
        RobotInfo[] ourRobots = rc.senseNearbyRobots(2);
        digState = 1;
        for (RobotInfo r: ourRobots) {
            if (r.getType() == RobotType.DESIGN_SCHOOL && r.getTeam() == rc.getTeam()) {
                digDir = rc.getLocation().directionTo(r.getLocation());
                if (rc.canDigDirt(digDir)) {
                    return digDir;
                }
            }
        }
        for (RobotInfo r: ourRobots) {
            if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) {
                digDir = rc.getLocation().directionTo(r.getLocation());
                if (rc.canDigDirt(digDir)) {
                    return digDir;
                }
            }
        }
        digState = -1;
        for (RobotInfo r: ourRobots) {
            if (r.getType() == RobotType.MINER && r.getTeam() != rc.getTeam()) {
                digDir = rc.getLocation().directionTo(r.getLocation());
                if (rc.canDigDirt(digDir)) {
                    // check elevation difference
                    if (Math.abs(rc.senseElevation(enemyHQLocation)-rc.senseElevation(r.getLocation())) < 4) return digDir;
                }
            }
        }
        for (Direction dir: directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc)) {
                if (rc.senseFlooding(loc)) return dir;
            }
        }
        return Direction.CENTER;
    }

    public boolean checkMove() throws GameActionException {
        for (Direction dir: directions) {
            if (rc.canSenseLocation(rc.getLocation().add(dir)) && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) return true;
        }
        return false;
    }

    public Direction highest() throws GameActionException {
        Direction elevate = Direction.CENTER;
        int high = rc.senseElevation(rc.getLocation());
        for (Direction dir: directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc)) {
                if (rc.senseElevation(loc) > high) {
                    elevate = dir;
                    high = rc.senseElevation(loc);
                }
            }
        }
        return elevate;
    }

    public Direction digReinforce() throws GameActionException {
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            for (MapLocation digLoc: diggingLoc) {
                if (dig.equals(digLoc)) {
                    if (rc.canDigDirt(dir)) return dir;
                }
            }
        }
        return holeTo();
    }

    public Direction lowestElevation() throws GameActionException {
        int low = 100000;
        Direction lowest = rc.getLocation().directionTo(HQLocation);
        for (Direction dir: directions) {
            MapLocation fill = rc.getLocation().add(dir);
            if (fill.isAdjacentTo(HQLocation)) {
                if (rc.canSenseLocation(fill)) {
                    int el = rc.senseElevation(fill);
                    if (el < low) {
                        low = el;
                        lowest = dir;
                    }
                }
            }
        }
        return lowest;
    }
}