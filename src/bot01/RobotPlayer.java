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

    static MapLocation HQLocation;

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
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

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
        if (turnCount <= 11) {
            for (Direction d: Direction.allDirections()) {
                if (rc.canBuildRobot(RobotType.MINER, d)) rc.buildRobot(RobotType.MINER, d);
            }
        }
        MapLocation[] soupLoc = findSoup();
        if (soupLoc.length != 0) {
            // we found soup, so we want to broadcast so our miners can get to it faster
            // TODO: implement broadcasting
        }
    }

    static void runMiner() throws GameActionException {
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            // if the robot is full move back to HQ
            // TODO: implement bug nav
            if (HQLocation != null) {
                if (rc.canMove(rc.getLocation().directionTo(HQLocation))) {
                    rc.move(rc.getLocation().directionTo(HQLocation));
                }
            }
        } else {
            MapLocation[] soupLoc = findSoup();
            for (MapLocation soup: soupLoc) {
                if (rc.canMineSoup(rc.getLocation().directionTo(soup))) {
                    rc.mineSoup(rc.getLocation().directionTo(soup));
                }
            }
            // if we can't mine soup, go to other soups
            for (MapLocation soup: soupLoc) {
                if (rc.canMove(rc.getLocation().directionTo(soup))) {
                    rc.move(rc.getLocation().directionTo(soup));
                }
            }

            if (soupLoc.length == 0) {
                // if there is no soup nearby move randomly for now I guess?
                // TODO: think of strategy for scouting for soup
                tryMove(directions[(int) (Math.random()*directions.length)]);
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
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
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

    static double distSqr(int dx, int dy) {
        return dx*dx+dy*dy;
    }

    // returns a list of MapLocations of soup in the robot's vision radius
    static MapLocation[] findSoup() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        int sensorR = rc.getType().sensorRadiusSquared;
        // currently, HQ can see 48 distance squared, so we only need to check 6x6 square
        int maxV = 6;
        MapLocation[] soupLoc = new MapLocation[36];
        int index = 0;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                if (distSqr(x, y) < sensorR) {
                    MapLocation check = robotLoc.translate(x, y);
                    if (rc.senseSoup(check) > 0) {
                        soupLoc[index] = check;
                        index++;
                    }
                }
            }
        }
        return soupLoc;
    }

    static void findHQ() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo ri: robots){
            if (ri.getType() == RobotType.HQ) HQLocation = ri.getLocation();
        }
    }

    // when a unit is first created it calls this function
    static void initialize() throws GameActionException {
        findHQ();
    }
}
