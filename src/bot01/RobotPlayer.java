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
        // if turncount <= 11
        if (rc.getRobotCount() < 5) {
            for (Direction d : Direction.allDirections()) {
                if (rc.canBuildRobot(RobotType.MINER, d)) rc.buildRobot(RobotType.MINER, d);
            }
        }
        MapLocation soupLoc = findSoup();
        if (soupLoc != null) {
            // we found soup, so we want to broadcast so our miners can get to it faster
            // TODO: implement broadcasting
        }
    }

    static void runMiner() throws GameActionException {
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            // if the robot is full move back to HQ
            if (HQLocation != null) {
                // if HQ is next to miner deposit
                if (HQLocation.isAdjacentTo(rc.getLocation())) {
                    Direction soupDepositDir = rc.getLocation().directionTo(HQLocation);
                    if (rc.canDepositSoup(soupDepositDir)) {
                        System.out.println("Depositing " + rc.getSoupCarrying() + " soup!");
                        rc.depositSoup(soupDepositDir, rc.getSoupCarrying());
                    }
                    nav.bugOff();
                } else {
                    nav.bugNav(rc, HQLocation);
                }
            }
        } else {
            // if HQ is right next to miner and still have some soup left, keep depositing
            if (rc.getSoupCarrying() > 0 && HQLocation.isAdjacentTo(rc.getLocation())) {
                Direction soupDepositDir = rc.getLocation().directionTo(HQLocation);
                if (rc.canDepositSoup(soupDepositDir)) {
                    System.out.println("Depositing " + rc.getSoupCarrying() + " soup!");
                    rc.depositSoup(soupDepositDir, rc.getSoupCarrying());
                }
            } else {
                MapLocation soupLoc = findSoup();
                if (soupLoc != null) {
                    System.out.println("Soup is at: " + soupLoc.toString());
                    if (rc.canMineSoup(rc.getLocation().directionTo(soupLoc))) {
                        rc.mineSoup(rc.getLocation().directionTo(soupLoc));
                    }
                    // if we can't mine soup, go to other soups
                    if (rc.canMove(rc.getLocation().directionTo(soupLoc))) {
                        rc.move(rc.getLocation().directionTo(soupLoc));
                    }
                } else {
                    // if there is no soup nearby move randomly for now I guess?
                    // TODO: think of strategy for scouting for soup
                    tryMove(directions[(int) (Math.random() * directions.length)]);
                }
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
