package bot01;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;

    // game map, not implemented yet
    static int[][] map;

    // navigation object
    static Nav nav = new Nav();

    static MapLocation HQLocation;
    // suspected enemy HQ location
    static MapLocation enemyHQLocationSuspect;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;
        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

                if (turnCount == 1) {
                    initialize();
                }
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        // build all the miners we can get in the first few turns
        if (rc.getRobotCount() < 5) {
            for (Direction d : Direction.allDirections()) {
                tryBuild(RobotType.MINER, d);
            }
        }
        MapLocation soupLoc = findSoup();
        if (soupLoc != null) {
            // we found soup, so we want to broadcast so our miners can get to it faster
            // TODO: implement broadcasting
        }

        // TODO: shoot drones in range
    }

    static void runMiner() throws GameActionException {
        // build drone factory if there isn't one
        if (rc.getRobotCount() == 5 && rc.getLocation().distanceSquaredTo(HQLocation) < 15 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
            for (Direction d : Direction.allDirections()) {
                tryBuild(RobotType.FULFILLMENT_CENTER, d);
            }
        }
        if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost+200 && rc.getLocation().distanceSquaredTo(HQLocation) < 15) {
            for (Direction d : Direction.allDirections()) {
                tryBuild(RobotType.VAPORATOR, d);
            }
        }
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || (findSoup() == null && rc.getSoupCarrying() > 0)) {
            // if the robot is full or has stuff and no more soup nearby, move back to HQ
            if (HQLocation != null) {
                // if HQ is next to miner deposit
                if (HQLocation.isAdjacentTo(rc.getLocation())) {
                    Direction soupDepositDir = rc.getLocation().directionTo(HQLocation);
                    tryRefine(soupDepositDir);
                    nav.navReset();
                } else {
                    nav.bugNav(rc, HQLocation);
                }
            }
        } else {
            MapLocation soupLoc = findSoup();
            if (soupLoc != null) {
                System.out.println("Soup is at: " + soupLoc.toString());
                Direction locDir = rc.getLocation().directionTo(soupLoc);
                if (rc.canMineSoup(locDir)) {
                    rc.mineSoup(locDir);
                    nav.navReset();
                }
                // if we can't mine soup, go to other soups
                else nav.bugNav(rc, soupLoc);
            } else {
                // TODO: think of strategy for scouting for soup
                // move to suspected enemy HQ
                nav.bugNav(rc, enemyHQLocationSuspect);
            }
        }
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {
        // no drones -> 6 units
        // produce 4 drones
        if (rc.getRobotCount() < 25) {
            for (Direction dir : directions)
                tryBuild(RobotType.DELIVERY_DRONE, dir);
        }
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        // find opponent units and pick up
        if (!rc.isCurrentlyHoldingUnit()) {
            // find opponent units
            RobotInfo pickup = null;
            for (RobotInfo r: rc.senseNearbyRobots()) {
                if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER || r.getType()  == RobotType.COW)) {
                    pickup = r;
                }
            }
            if (pickup != null) {
                // if can pickup do pickup
                if (pickup.getLocation().isAdjacentTo(rc.getLocation())) {
                    if (rc.canPickUpUnit(pickup.getID())) rc.pickUpUnit(pickup.getID());
                    nav.navReset();
                } else {
                    // if not navigate to that unit
                    nav.bugNav(rc, pickup.getLocation());
                }
            } else {
                // if there are no robots nearby
                nav.bugNav(rc, enemyHQLocationSuspect);
            }
        } else {
            // find water if not cow
            MapLocation water = null;
            MapLocation robotLoc = rc.getLocation();
            int maxV = 5;
            for (int x = -maxV; x <= maxV; x++) {
                for (int y = -maxV; y <= maxV; y++) {
                    MapLocation check = robotLoc.translate(x, y);
                    if (rc.canSenseLocation(check)) {
                        if (rc.senseFlooding(check)) {
                            // find the closest maxmimal soup deposit
                            if (water == null || check.distanceSquaredTo(rc.getLocation()) < water.distanceSquaredTo(rc.getLocation())) water = check;
                        }
                    }
                }
            }
            if (water != null) {
                if (water.isAdjacentTo(robotLoc)) {
                    // drop off unit
                    Direction dropDir = robotLoc.directionTo(water);
                    if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                    nav.navReset();
                } else {
                    nav.bugNav(rc, water);
                }
            } else {
                // TODO: find water
                // for now, move randomly to try find water
                tryMove(directions[(int) (Math.random()*directions.length)]);
            }
        }
        nav.bugNav(rc, enemyHQLocationSuspect);
    }

    static void runNetGun() throws GameActionException {

    }

    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[10];
            for (int i = 0; i < 10; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }


    // returns the current water level given turn count
    static double waterLevel() {
        double x = (double) turnCount;
        return Math.exp(0.0028*x-1.38*Math.sin(0.00157*x-1.73)+1.38*Math.sin(-1.73))-1;
    }

    static int distSqr(int dx, int dy) {
        return dx * dx + dy * dy;
    }

    // returns the closest MapLocation of soup in the robot's vision radius
    static MapLocation findSoup() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        int maxV = 6;
        MapLocation soupLoc = null;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    if (rc.senseSoup(check) > 0) {
                        // find the closest maxmimal soup deposit
                        if (soupLoc == null || check.distanceSquaredTo(rc.getLocation()) < soupLoc.distanceSquaredTo(rc.getLocation())
                        || (check.distanceSquaredTo(rc.getLocation()) == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                        soupLoc = check;
                    }
                }
            }
        }
        return soupLoc;
    }

    // finds HQ and guesses enemy HQ
    static void findHQ() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo ri: robots){
            if (ri.getType() == RobotType.HQ && ri.getTeam() == rc.getTeam()) HQLocation = ri.getLocation();
        }
        MapLocation[] suspects = new MapLocation[]{horRef(HQLocation, rc), verRef(HQLocation, rc), horVerRef(HQLocation, rc)};
        enemyHQLocationSuspect = suspects[rc.getID() % 3];
        rc.setIndicatorDot(enemyHQLocationSuspect, 255, 0, 0);
    }

    // when a unit is first created it calls this function
    static void initialize() throws GameActionException {
        findHQ();
    }

    // reflect horizontally
    static MapLocation horRef(MapLocation loc, RobotController rc) {
        return new MapLocation(rc.getMapWidth()-1-loc.x, loc.y);
    }

    // reflect vertically
    static MapLocation verRef(MapLocation loc, RobotController rc) {
        return new MapLocation(loc.x, rc.getMapHeight()-1-loc.y);
    }

    // reflect vertically and horizontally
    static MapLocation horVerRef(MapLocation loc, RobotController rc) {
        return new MapLocation(rc.getMapWidth()-1-loc.x, rc.getMapHeight()-1-loc.y);
    }
}
