package bot01;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};


    public enum actionPhase{
        NON_ATTACKING,
        PREPARE,
        ATTACK,
        SURRENDER,
        DEFENSE
    }

    static int turnCount;
    static int spawnHeight;

    // game map, not implemented yet
    static int[][] map;

    // information queue waiting to be sent to blockchain
    static ArrayList<Integer> infoQ = new ArrayList<>();

    // constants
    // max vision of all units
    static int maxV = 6;
    // how many turns we wait before blockChain
    static int waitBlock = 10;
    // how far can soup be away from each other
    static int soupClusterDist = 24;
    // how far can water be away from each other
    static int waterClusterDist = 120;
    // patrol radius
    static int patrolRadiusMin = 25;
    static int patrolRadiusMax = 34;
    // help radius (super large for now because helping miners is pretty important)
    static int helpRadius = 800;
    // refinery radius
    static int refineryDist = 63;
    // drone count control
    static int maxDroneCount = 70;
    // landscaper count control
    static int attackLandscaperCount = 20;
    // attack control
    static int battlefieldRadius =169;

    // default cost of our transaction
    private static int defaultCost = 2;
    // how much drones can wander
    static int wanderLimit = 5;
    // when builder returns
    static int builderReturn = 100;
    // when to explode drone
    static int explodeThresh = 10;

    // navigation object
    static Nav nav = new Nav();
    // turtle object
    static Turtle turtle;
    // blueprint object (only used by first miner)
    static Blueprint blueprint;

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static ArrayList<MapLocation> soupLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> waterLocation = new ArrayList<MapLocation>();
    // only miners use the following
    static MapLocation soupLoc = null;
    static MapLocation closestRefineryLocation = null;
    // only drones use following
    static RobotInfo closestEnemyUnit;
    static ArrayList<Pair> helpLoc = new ArrayList<>();
    // only landscapers use the following
    static Vector[] spawnPos = new Vector[]{new Vector(0, 0), new Vector(1, 0), new Vector(-1, 0), new Vector(0, 1), new Vector(-1, 1), new Vector(-2, 1), new Vector(-2, 0), new Vector(-2, -1), new Vector(-1, -1), new Vector(0, -1), new Vector(1, -1), new Vector(2, -1), new Vector(2, 0), new Vector(2, 1), new Vector(1, 1), new Vector(-1, 2), new Vector(0, 2), new Vector(1, 2), new Vector(-1, -2), new Vector(0, -2), new Vector(1, -2)};

    static actionPhase phase= actionPhase.NON_ATTACKING;

    // states
    // for drones:   0: not helping   1: finding stranded   2: going to requested loc
    // for miners:   0: normal        1: requesting help
    static int helpMode = 0;
    static int helpIndex = -1;
    // is miner #1
    static boolean isBuilder;
    // is miner #2
    // TODO: implement rush if it's not nerfed after sprint with this miner and the 5 state 0 landscapers we have
    static boolean isAttacker;

    // booleans
    // is drone holding a cow
    static boolean isCow = false;
    // explode the unit
    static boolean explode = false;
    // is HQ under attack
    static boolean isUnderAttack = false;
    // is outer layer and inner layer done
    static boolean isOuterLayer = false;
    static boolean isInnerLayer = false;
    // is drone carrying another attacker unit
    static boolean isAttackerBuilder = false;

    // used for exploring enemy HQ locations
    static int idIncrease = 0;

    // suspected enemy HQ location
    static MapLocation enemyHQLocationSuspect;
    // possible navigation locations
    static ArrayList<MapLocation> suspects = null;
    static Map<MapLocation, Boolean> suspectsVisited = new HashMap<>();
    // currently navigating to
    static MapLocation exploreTo;

    // unit counter
    static int minerCount = 0;
    static int droneCount = 0;
    static int landscaperCount = 0;

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
                } else {
                    closestEnemyUnit=null;
                    getInfo(rc.getRoundNum()-1);
                    collectInfo();
                }
//                System.out.println("I have " + Clock.getBytecodesLeft() + " left!");
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
                }
                // if the unit wants to explode, kill it
                if (explode) return;
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        // find drones and shoot them
//        System.out.println("enemy hq might be at " + enemyHQLocationSuspect.toString());
//        if (enemyHQLocation!=null){
//        System.out.println("enemy hq is at" + enemyHQLocation.toString());
//        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        boolean isVaporator = false;
        int netGunCount = 0;
        for (RobotInfo r : robots) {
            if (r.getTeam() != rc.getTeam() && r.getType() == RobotType.DELIVERY_DRONE) {
                System.out.println("Shot a drone at " + r.getLocation());
                if (rc.canShootUnit(r.getID())) {
                    rc.shootUnit(r.getID());
                    break;
                }
            }
            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
                isVaporator = true;
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) {
                netGunCount++;
            }
        }
        if (!isUnderAttack) {
            for (RobotInfo r : robots) {
                if (r.getType() == RobotType.LANDSCAPER && r.getTeam() != rc.getTeam()) {
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.DEFENSE, HQLocation));
                    isUnderAttack = true;
                    break;
                }
            }
        } else {
            isUnderAttack = false;
            for (RobotInfo r : robots) {
                if (r.getType() == RobotType.LANDSCAPER && r.getTeam() != rc.getTeam()) {
                    isUnderAttack = true;
                    break;
                }
            }
            if (!isUnderAttack) {
                infoQ.add(Cast.getMessage(Cast.InformationCategory.SURRENDER, HQLocation));
            }
        }
        // build all the miners we can get in the first few turns
        // maximum of 10 miners at 250th round
        Direction optDir = Direction.NORTH;
        if (minerCount < Math.min(5+rc.getRoundNum()/50, 10) && (minerCount < 5 || rc.getTeamSoup() >= RobotType.REFINERY.cost + RobotType.MINER.cost) && !isVaporator) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
    }

    static void runMiner() throws GameActionException {
//        System.out.println("I have " + Clock.getBytecodesLeft());
//        System.out.println("I have " + rc.getSoupCarrying());
        if (isBuilder && rc.getRoundNum() >= builderReturn) {
            if (rc.getRoundNum() == builderReturn) helpMode = 0;
            runBuilder();
            return;
        }
        // check if it's in help mode and it moved so it can go free
        if (helpMode == 1) {
            if (nav.outOfDrone(rc)) helpMode = 0;
        }
        if (helpMode == 0) {
            // build drone factory if there isn't one
            MapLocation DFLoc = HQLocation.add(Direction.EAST);
            MapLocation LFLoc = HQLocation.add(Direction.WEST);
            if (rc.getRobotCount() > 4 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && rc.getLocation().isAdjacentTo(DFLoc) && !rc.getLocation().equals(DFLoc)) {
                // build a drone factory east of hq
                tryBuild(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(DFLoc));
            }
            if (rc.getRobotCount() > 6 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && rc.getLocation().isAdjacentTo(LFLoc) && !rc.getLocation().equals(LFLoc)) {
                // build a landscaper factory west of hq
                tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(LFLoc));
            }
            if (soupLoc == null) {
                // if soup location is far or we just didn't notice
                //            System.out.println("In my soupLoc I have " + soupLocation.toString());
                findSoup();
            }
            //        System.out.println("After finding soup, I have " + Clock.getBytecodesLeft());
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || (soupLoc == null && rc.getSoupCarrying() > 0)) {
                // if the robot is full or has stuff and no more soup nearby, move back to HQ
                //            System.out.println("before going home i have " + Clock.getBytecodesLeft());
                // default hq
                closestRefineryLocation = HQLocation;
                // check select a point as reference(might be edge case?)
                MapLocation referencePoint = soupLoc;
                if (referencePoint != null) {
                    //                System.out.println("reference to " + reference_point.toString());
                    int minRefineryDist = referencePoint.distanceSquaredTo(HQLocation);

                    //                System.out.println("before find min d i have" + Clock.getBytecodesLeft());
                    // check through refinery(redundancy here)
                    for (MapLocation refineryLoca : refineryLocation) {
                        int temp_d = referencePoint.distanceSquaredTo(refineryLoca);
                        if (temp_d < minRefineryDist) {
                            closestRefineryLocation = refineryLoca;
                            minRefineryDist = temp_d;
                        }
                        //                    System.out.println("my memory contain " + refineryLoca.toString());
                    }
//                                    System.out.println("reference to " + referencePoint.toString());
//                                    System.out.println("compare to " + closestRefineryLocation.toString());
//                                    System.out.println("after find min d i have " + Clock.getBytecodesLeft());
//                                    System.out.println("reference min distance to refinery " + minRefineryDist);
//                                    System.out.println("reference min distance to bot " + referencePoint.distanceSquaredTo(rc.getLocation()));
                    //
                    if (minRefineryDist >= refineryDist && referencePoint.distanceSquaredTo(rc.getLocation()) < 13 && rc.getTeamSoup() >= 200) {
                        //                    System.out.println("attempt build refinery");
                        Direction optDir = rc.getLocation().directionTo(referencePoint);
                        for (int i = 0; i < 8; i++) {
                            if (rc.canBuildRobot(RobotType.REFINERY, optDir)) {
                                //                            System.out.println("can build refinery");
                                rc.buildRobot(RobotType.REFINERY, optDir);
                                //                            System.out.println("built refinery");
                                MapLocation robotLoc = rc.getLocation();
                                refineryLocation.add(robotLoc.add(optDir));
                                closestRefineryLocation = refineryLocation.get(refineryLocation.size() - 1);
                                break;
                            } else {
                                optDir = optDir.rotateRight();
                            }
                        }
                        infoQ.add(Cast.getMessage(Cast.InformationCategory.NEW_REFINERY, refineryLocation.get(refineryLocation.size() - 1)));
                    }  
                    //                System.out.println("after new refinery procedures" + Clock.getBytecodesLeft());
                }
                // if HQ is next to miner deposit
                if (closestRefineryLocation.isAdjacentTo(rc.getLocation())) {
                    Direction soupDepositDir = rc.getLocation().directionTo(closestRefineryLocation);
                    tryRefine(soupDepositDir);
                    nav.navReset(rc, rc.getLocation());
                } else {
                    if (nav.needHelp(rc, turnCount, closestRefineryLocation)) {
                        helpMode = 1;
                        System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        infoQ.add(Cast.getMessage(rc.getLocation(), closestRefineryLocation));
                    }
                    else nav.bugNav(rc, closestRefineryLocation);
                }
            } else {
                // try to mine soup in all directions because miner finding soup can be slightly buggy
                boolean canMine = false;
                for (Direction d: directions) {
                    if (rc.canMineSoup(d)) {
                        rc.mineSoup(d);
                        canMine = true;
                        break;
                    }
                }
                if (rc.canMineSoup(Direction.CENTER)) {
                    rc.mineSoup(Direction.CENTER);
                    canMine = true;
                }
                if (soupLoc != null) {
//                    System.out.println("Soup is at: " + soupLoc.toString());
                    if (canMine) {
                        // pollution might make miner skip this even though it's right next to soup
                        nav.navReset(rc, rc.getLocation());
                    }
                    // if we can't mine soup, go to other soups
                    else {
                        if (nav.needHelp(rc, turnCount, soupLoc)) {
                            helpMode = 1;
                            System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            infoQ.add(Cast.getMessage(rc.getLocation(), soupLoc));
                        }
                        else nav.bugNav(rc, soupLoc);
                    }
                } else {
                    // scout for soup
                    if (nav.getWander() >= wanderLimit) {
                        resetEnemyHQSuspect();
                    }
                    nav.bugNav(rc, enemyHQLocationSuspect);
                }
            }
        }
    }

    static void runBuilder() throws GameActionException {
//        if (blueprint == null) {
//            System.out.println("blueprint is null");
//        }
        if (helpMode == 1) {
            if (nav.outOfDrone(rc)) helpMode = 0;
        }
        if (helpMode == 0) {
            // check if it's on the miner trail
            if (blueprint.getIndex(rc.getLocation()) == -1) {
                // if off the rail, try to move to HQLoc + EAST 2 times
//                System.out.println("Off the trail right now");
                MapLocation onTrail = new Vector(2, 0).addWith(HQLocation);
                if (nav.needHelp(rc, turnCount, onTrail)) {
                    helpMode = 1;
//                            System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    infoQ.add(Cast.getMessage(rc.getLocation(), onTrail));
                }
                else nav.bugNav(rc, onTrail);
            } else {
                // on the trail
//                System.out.println("On the trail!");
                if (!blueprint.build(rc)) {
                    explode = true;
                    return;
                }
            }
            // if it can deposit then do deposit
            if (rc.canDepositSoup(rc.getLocation().directionTo(HQLocation))) rc.depositSoup(rc.getLocation().directionTo(HQLocation), rc.getSoupCarrying());
        }
    }

    static void runRefinery() throws GameActionException {
        // maybe refineries can request for help from drones?
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        System.out.println("isOuterLayer is: " + isOuterLayer);
        System.out.println("isInnerLayer is: " + isInnerLayer);

        // produce 5 landscapers initially to guard hq
        Direction[] spawnDir = new Direction[]{Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST};
        if (landscaperCount < 5 && rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.LANDSCAPER.cost) {
            for (int i = 0; i < 3; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, spawnDir[i])) {
                    rc.buildRobot(RobotType.LANDSCAPER, spawnDir[i]);
                    landscaperCount++;
                    break;
                }
            }
        }
        boolean isVaporator = false;
        int netGunCount = 0;
        RobotInfo[] robots = rc.senseNearbyRobots();
        Vector[] outerLayerConfirm = new Vector[6];
        Vector[] innerLayerConfirm = new Vector[]{new Vector(-2, 1), new Vector(-2, 0), new Vector(-2, -1), new Vector(-1, -2), new Vector(0, -2), new Vector(1, -2), new Vector(2, 1), new Vector(2, 0), new Vector(2, -1)};
        for (int i = 0; i < 6; i++) {
            outerLayerConfirm[i] = new Vector(i-3, -3);
        }
        if (!isOuterLayer) {
            isOuterLayer = true;
            for (Vector v : outerLayerConfirm) {
                MapLocation loc = v.addWith(HQLocation);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r == null || r.getTeam() != rc.getTeam() || r.getType() != RobotType.LANDSCAPER) {
                        isOuterLayer = false;
                        break;
                    }
                } else {
                    isOuterLayer = false;
                    break;
                }
            }
            if (isOuterLayer) {
                infoQ.add(Cast.getMessage(Cast.InformationCategory.OUTER_LAYER, HQLocation));
                System.out.println("Sent outer layer info!");
            }
        }
        if (!isInnerLayer) {
            isInnerLayer = true;
            for (Vector v : innerLayerConfirm) {
                MapLocation loc = v.addWith(HQLocation);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo r = rc.senseRobotAtLocation(loc);
                    if (r == null || r.getTeam() != rc.getTeam() || r.getType() != RobotType.LANDSCAPER) {
                        isInnerLayer = false;
                        break;
                    }
                } else {
                    isOuterLayer = false;
                    break;
                }
            }
            if (isInnerLayer) {
                infoQ.add(Cast.getMessage(Cast.InformationCategory.INNER_LAYER, HQLocation));
                System.out.println("Sent inner layer info!");
            }
        }
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
                isVaporator = true;
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) {
                netGunCount++;
            }
        }
        if (isVaporator && !isOuterLayer) {
            // produce outer layer
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.LANDSCAPER.cost) {
                for (int i = 0; i < 2; i++) {
                    if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, spawnDir[i])) {
                        rc.buildRobot(RobotType.LANDSCAPER, spawnDir[i]);
                        landscaperCount++;
                        break;
                    }
                }
            }
        }
        // produce inner layer of minescapers
        if (netGunCount >= 4 && isOuterLayer && !isInnerLayer) {
            if (rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.LANDSCAPER.cost) {
                for (int i = 0; i < 2; i++) {
                    if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, spawnDir[i])) {
                        rc.buildRobot(RobotType.LANDSCAPER, spawnDir[i]);
                        landscaperCount++;
                        break;
                    }
                }
            }
        }
        if (phase == actionPhase.PREPARE) {
            if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, Direction.NORTH)) {
                rc.buildRobot(RobotType.LANDSCAPER, Direction.NORTH);
                landscaperCount++;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        System.out.println("helpLoc length is: " + helpLoc.size());
        // produce 5 drones
        Direction optDir = Direction.NORTHWEST;
        if (droneCount < 5 && rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.DELIVERY_DRONE.cost) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) && rc.getTeamSoup()>350 ){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    break;
                } else {
                    optDir = optDir.rotateRight(); 
                }
            }
        }
        if (droneCount < 8 && rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.DELIVERY_DRONE.cost && helpLoc.size() >= 5) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) && rc.getTeamSoup()>350 ){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    break;
                } else {
                    optDir = optDir.rotateRight();
                }
            }
        }
        boolean isVaporator = false;
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
                isVaporator = true;
            }
        }
        // spam drones if we have a ton of soup
        if (rc.getTeamSoup() >= 1000 && isVaporator && droneCount < maxDroneCount) {
            optDir = Direction.NORTHWEST;
            if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
                rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                droneCount++;
            }
            // if drones are almost max then prepare attack
            if (droneCount == maxDroneCount-attackLandscaperCount) {
                infoQ.add(Cast.getMessage(Cast.InformationCategory.PREPARE, HQLocation));
            }
            if (droneCount == maxDroneCount) {
                infoQ.add(Cast.getMessage(Cast.InformationCategory.ATTACK, HQLocation));
            }
        }

    }

    static void runLandscaper() throws GameActionException {
        System.out.println("isOuterLayer is: " + isOuterLayer);
        System.out.println("isInnerLayer is: " + isInnerLayer);
        RobotInfo[] robots = rc.senseNearbyRobots();
        RobotInfo[] robotsmall = rc.senseNearbyRobots(2);
        if (turtle.getLandscaperState() < 2 && isOuterLayer) {
            System.out.println("Checking outer layer");
            if (turtle.getLandscaperState() == 0 || (turtle.getLandscaperState() == 1 && turtle.positionOut(rc.getLocation()) == -1)) {
                turtle.setLandscaperState(2);
            }
        }
        // explode if stuck on the inner layer trying to get to outer layer
        if (isInnerLayer) {
            if (Vector.vectorSubtract(rc.getLocation(), HQLocation).equals(new Vector(-1, -1)) || Vector.vectorSubtract(rc.getLocation(), HQLocation).equals(new Vector(0, -1))) {
                explode = true;
                return;
            }
        }
        // explode if command is attack but they just spawned and nobody next to them
        if (phase == actionPhase.ATTACK && Vector.vectorSubtract(rc.getLocation(), HQLocation).equals(new Vector(-1, 1)) && rc.senseRobotAtLocation(rc.getLocation().add(Direction.EAST)) == null) {
            explode = true;
            return;
        }
        System.out.println("My state is: " + turtle.getLandscaperState());
        if (turtle.getLandscaperState() == 0) {
            // TODO: maybe we can make a ENEMY_BUILDING category and have them attack in the future
            boolean enemyLandscaper = false;
            for (RobotInfo r: robots) {
                if (r.getType() == RobotType.LANDSCAPER && r.getTeam() != rc.getTeam() && r.getLocation().distanceSquaredTo(HQLocation) <= 20) {
                    enemyLandscaper = true;
                    System.out.println("Enemy landscapers incoming!!");
                    // if they're attacking HQ they're most likely attacking it or one of our building
                    MapLocation DFLoc = HQLocation.add(Direction.EAST);
                    MapLocation LFLoc = HQLocation.add(Direction.WEST);
                    RobotInfo LFr = rc.senseRobotAtLocation(LFLoc);
                    RobotInfo DFr = rc.senseRobotAtLocation(DFLoc);
                    if (rc.getLocation().isAdjacentTo(HQLocation) && rc.canDigDirt(rc.getLocation().directionTo(HQLocation))) {
                        System.out.println("Digging dirt from HQ");
                        rc.digDirt(rc.getLocation().directionTo(HQLocation));
                    } else if (rc.getLocation().isAdjacentTo(LFLoc) && rc.canDigDirt(rc.getLocation().directionTo(LFLoc)) && LFr != null && LFr.getType() == RobotType.DESIGN_SCHOOL) {
                        System.out.println("Digging dirt from landscaper factory");
                        rc.digDirt(rc.getLocation().directionTo(LFLoc));
                    } else if (rc.getLocation().isAdjacentTo(DFLoc) && rc.canDigDirt(rc.getLocation().directionTo(DFLoc)) && DFr != null && DFr.getType() == RobotType.FULFILLMENT_CENTER) {
                        System.out.println("Digging dirt from drone factory");
                        rc.digDirt(rc.getLocation().directionTo(DFLoc));
                    } else {
                        // if adjacent, stay there
                        if (!rc.getLocation().isAdjacentTo(HQLocation)) {
                            System.out.println("Moving towards HQ");
                            nav.bugNav(rc, HQLocation);
                        } else {
                            System.out.println("Not moving from HQ");
                        }
                    }
                }
            }
            if (!enemyLandscaper) {
                for (RobotInfo r: robots) {
                    if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                        // if it's an enemy building, bury it
                        System.out.println("Enemy building targeted");
                        if (rc.getLocation().isAdjacentTo(r.getLocation())) {
                            if (rc.getDirtCarrying() == 0) {
                                // dig anywhere but to that robot, and preferably not downward
                                System.out.println("I have no dirt");
                                for (Direction d : directions) {
                                    if (rc.getLocation().add(d).equals(r.getLocation())) continue;
                                    if (rc.canDigDirt(d)) {
                                        System.out.println("Digging towards: " + d.toString());
                                        rc.digDirt(d);
                                        break;
                                    }
                                }
                            } else {
                                System.out.println("I have dirt");
                                // fill up location
                                Direction optDir = rc.getLocation().directionTo(r.getLocation());
                                if (rc.canDepositDirt(optDir)) {
                                    System.out.println("Filling up: " + optDir.toString());
                                    rc.depositDirt(optDir);
                                }
                            }
                        } else {
                            // move to that location
                            System.out.println("Moving towards enemy building");
                            nav.bugNav(rc, r.getLocation());
                        }
                    }
                }
            }
            // even out the spawn area
            if (rc.isReady()) {
                for (Vector v: spawnPos) {
                    MapLocation modify = v.addWith(HQLocation);
                    if (modify.isAdjacentTo(rc.getLocation())) {
                        if (rc.canSenseLocation(modify)) {
                            if (Math.abs(rc.senseElevation(modify)-spawnHeight) > 3) {
                                if (rc.senseElevation(modify) > spawnHeight) {
                                    // if higher
                                    if (rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                                        // dig
                                        if (rc.canDigDirt(rc.getLocation().directionTo(modify))) {
                                            rc.digDirt(rc.getLocation().directionTo(modify));
                                            break;
                                        }
                                    } else {
                                        Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                                        if (rc.canDepositDirt(optDir)) {
                                            rc.depositDirt(optDir);
                                            break;
                                        }
                                    }
                                } else {
                                    // if lower
                                    if (rc.getDirtCarrying() == 0) {
                                        // dig
                                        Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                                        if (rc.canDigDirt(optDir)) {
                                            rc.digDirt(optDir);
                                            break;
                                        }
                                    } else {
                                        if (rc.canDigDirt(rc.getLocation().directionTo(modify))) {
                                            rc.digDirt(rc.getLocation().directionTo(modify));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // if there's nothing nearby, patrol
            if (rc.isReady()) {
                MapLocation patrol = turtle.findPatrol(rc);
                if (!rc.getLocation().equals(patrol)) {
                    System.out.println("Patrolling towards: " + patrol.toString());
                    nav.bugNav(rc, patrol);
                } else {
                    System.out.println("Staying at: " + patrol.toString());
                }
            }
        } else if (turtle.getLandscaperState() == 1) {
            // check if there's anything adjacent to it that can bury
//            System.out.println("I have bytecode: " + Clock.getBytecodesLeft());
            for (RobotInfo r: robotsmall) {
                if (r.getTeam() == rc.getTeam() && r.getType().isBuilding()) {
                    // if our own building is getting buried (most likely net gun) dig out dirt
                    Direction optDir = rc.getLocation().directionTo(r.getLocation());
                    if (rc.canDigDirt(optDir)) {
                        System.out.println("Building getting buried, digging out " + optDir.toString());
                        rc.digDirt(optDir);
                    }
                }
            }
            MapLocation[] netGunLoc = new MapLocation[]{new Vector(2, 2).addWith(HQLocation), new Vector(2, -2).addWith(HQLocation), new Vector(-2, 2).addWith(HQLocation), new Vector(-2, -2).addWith(HQLocation)};
//            System.out.println("After checking if any building is getting buried, there is " + Clock.getBytecodesLeft());
            // check if any net guns are unbuildable by the miner
            for (MapLocation loc: netGunLoc) {
                if (loc.isAdjacentTo(rc.getLocation())) {
                    if (Math.abs(spawnHeight-rc.senseElevation(loc)) > 3) {
                        // can't spawn
                        if (spawnHeight > rc.senseElevation(loc)) {
                            // if lower
                            if (rc.getDirtCarrying() == 0) {
                                System.out.println("I have no dirt");
                                Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                                if (rc.canDigDirt(optDir)) {
                                    System.out.println("Digging dirt from: " + optDir.toString());
                                    rc.digDirt(optDir);
                                    break;
                                }
                            } else {
                                // fill up location
                                System.out.println("I have dirt");
                                Direction optDir = rc.getLocation().directionTo(loc);
                                if (rc.canDepositDirt(optDir)) {
                                    System.out.println("Depositing dirt at: " + optDir.toString());
                                    rc.depositDirt(optDir);
                                    break;
                                }
                            }
                        } else {
                            // if higher
                            Direction optDir = rc.getLocation().directionTo(loc);
                            if (rc.canDigDirt(optDir)) {
                                rc.digDirt(optDir);
                                break;
                            }
                        }
                    }
                }
            }
            if (rc.isReady()) {
                for (RobotInfo r : robotsmall) {
                    if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                        System.out.println("I see enemy building");
                        // if it's an enemy building, bury it
                        if (rc.getDirtCarrying() == 0) {
                            // dig anywhere but to that robot, and preferably not downward
                            System.out.println("I have no dirt");
                            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                            for (int i = 0; i < 8; i++) {
                                if (!rc.getLocation().add(optDir).equals(r.getLocation())) {
                                    if (rc.canDigDirt(optDir)) {
                                        System.out.println("Digging dirt from: " + optDir.toString());
                                        rc.digDirt(optDir);
                                        break;
                                    }
                                }
                                optDir = optDir.rotateRight();
                            }
                        } else {
                            // fill up location
                            System.out.println("I have dirt");
                            Direction optDir = rc.getLocation().directionTo(r.getLocation());
                            if (rc.canDepositDirt(optDir)) {
                                System.out.println("Depositing dirt at: " + optDir.toString());
                                rc.depositDirt(optDir);
                            }
                        }
                    }
                }
//                System.out.println("After checking if any enemy building, there is " + Clock.getBytecodesLeft());
                // build outer wall if no other problems
                if (turtle.positionOut(rc.getLocation()) == -1) {
                    turtle.moveToTrail(rc);
                } else {
                    System.out.println("Building fort");
//                    System.out.println("Right before building fort, there is " + Clock.getBytecodesLeft());
                    turtle.buildFort(rc);
                }
            }
        } else if (turtle.getLandscaperState() == 2) {
            // check if there's anything adjacent to it that can bury, but I really doubt it can happen at this state
            for (RobotInfo r: robotsmall) {
                if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                    System.out.println("I sense enemy building");
                    // if it's an enemy building, bury it
                    if (rc.getDirtCarrying() == 0) {
                        // dig downward because you are alpha
                        System.out.println("Digging towards center");
                        if (rc.canDigDirt(Direction.CENTER)) rc.digDirt(Direction.CENTER);
                    } else {
                        // fill up location
                        Direction optDir = rc.getLocation().directionTo(r.getLocation());
                        if (rc.canDepositDirt(optDir)) {
                            System.out.println("Filling up dirt at: "+ optDir.toString());
                            rc.depositDirt(optDir);
                        }
                    }
                }
                if (r.getTeam() == rc.getTeam() && r.getType().isBuilding()) {
                    // if our own building is getting buried (most likely net gun) dig out dirt
                    Direction optDir = rc.getLocation().directionTo(r.getLocation());
                    if (rc.canDigDirt(optDir)) {
                        System.out.println("Our building is getting buried! Digging out at: " + optDir.toString());
                        rc.digDirt(optDir);
                    }
                }
            }
            // build inner wall
            if (turtle.positionIn(rc.getLocation()) == -1) {
                turtle.moveToTrail(rc);
            } else {
                System.out.println("Building fort");
                turtle.buildFort(rc);
            }
        }
        else if (turtle.getLandscaperState() == 3) {
            turtle.attack(rc, enemyHQLocation);
        }
    }

    static void runDeliveryDrone() throws GameActionException {

        System.out.println("My wander value is " + nav.getWander());
        System.out.println("My stuck value is " + nav.getStuck());
        // check if it needs to explode
        if (nav.getStuck() >= explodeThresh) {
            explode = true;
            return;
        }
        // check if it is going to be carrying a landscaper for attack mode
        if (phase == actionPhase.ATTACK && Vector.vectorSubtract(rc.getLocation(), HQLocation).equals(new Vector(0, 1))) {
            RobotInfo r = rc.senseRobotAtLocation(rc.getLocation().add(Direction.WEST));
            if (r != null && r.getType() == RobotType.LANDSCAPER && r.getTeam() != rc.getTeam()) {
                isAttackerBuilder = true;
                if (rc.canPickUpUnit(r.getID())) {
                    rc.pickUpUnit(r.getID());
                }
            }
        }
        // check if inner loc is completed then never go inside again
        if (isInnerLayer) {
            System.out.println("inner layer is completed!");
            System.out.println("My distance is: " + rc.getLocation().distanceSquaredTo(HQLocation));
            // if within distance 13 of HQ first things first move away
            if (rc.getLocation().distanceSquaredTo(HQLocation) <= 13) {
                Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                for (int i = 0; i < 8; i++) {
                    if (rc.canMove(optDir)) {
                        rc.move(optDir);
                        break;
                    } else {
                        optDir = optDir.rotateRight();
                    }
                }
            }
        }
        // check for help mode
        if (helpMode == 0 && !rc.isCurrentlyHoldingUnit()) {
            // check for unit to help
            // check for all of helpLoc if there's anything, if so, change helpMode and order of helpLoc
            helpIndex = -1;
            int closestDist = helpRadius;
            // first prune the helpLoc list
            ArrayList<Pair> removeHelp = new ArrayList<>();
            for (Pair value : helpLoc) {
                MapLocation helpAt = value.getKey();
                if (rc.canSenseLocation(helpAt)) {
                    RobotInfo r = rc.senseRobotAtLocation(helpAt);
                    if (r == null || r.getType() != RobotType.MINER || r.getTeam() != rc.getTeam()) {
                        removeHelp.add(value);
                    }
                }
            }
            for (Pair pair : removeHelp) {
                helpLoc.remove(pair);
            }
            for (int i = 0; i < helpLoc.size(); i++) {
                int helpDist = helpLoc.get(i).getKey().distanceSquaredTo(rc.getLocation());
                if (helpDist <= closestDist) {
                    closestDist = helpDist;
                    helpIndex = i;
                }
            }
            if (helpIndex != -1) {
                // only help people if you don't know enemy hq location or you're not bugging around enemyHQ
                if (enemyHQLocation == null || rc.getLocation().distanceSquaredTo(enemyHQLocation) > patrolRadiusMax) helpMode = 1;
            }
        }
        // if helping
        if (helpMode != 0) {
            System.out.println("Currently on help mode " + helpMode);
            // helping mode on!
            if (helpMode == 1) {
                // 
                MapLocation strandLoc = helpLoc.get(helpIndex).getKey();
                if (rc.canSenseLocation(strandLoc)) {
                    RobotInfo r = rc.senseRobotAtLocation(strandLoc);
                    if (r == null) {
                        helpLoc.remove(helpIndex);
                        helpMode = 0;
                    } else {
                        if (rc.getLocation().isAdjacentTo(strandLoc)) {
                            if (rc.canPickUpUnit(r.getID())) {
                                rc.pickUpUnit(r.getID());
                                helpMode = 2;
                            }
                        }
                    }
                }
                System.out.println("Navigating to " + strandLoc.toString());
                nav.bugNav(rc, strandLoc);
            } else if (helpMode == 2) {
                MapLocation requestLoc = helpLoc.get(helpIndex).getValue();
                if (rc.getLocation().isAdjacentTo(requestLoc)) {
                    Direction optDir = rc.getLocation().directionTo(requestLoc);
                    for (int i = 0; i < 8; i++) {
                        if (rc.canDropUnit(optDir) && !rc.senseFlooding(rc.getLocation().add(optDir))) {
                            rc.dropUnit(optDir);
                            helpLoc.remove(helpIndex);
                            helpMode = 0;
                            break;
                        } else {
                            optDir = optDir.rotateRight();
                        }
                    }
                } else {
                    System.out.println("Navigating to " + requestLoc);
                    nav.bugNav(rc, requestLoc);
                }
            }
        } else {
            // if not helping and no one to help
            switch(phase) {
                case NON_ATTACKING:
                    // find opponent units and pick up
                    if (!rc.isCurrentlyHoldingUnit()) {
                        System.out.println("I'm not holding any units!");
                        // find opponent units
                        RobotInfo pickup = null;
                        for (RobotInfo r : rc.senseNearbyRobots()) {
                            // check if it's inside the barrier somehow
                            if (rc.getRoundNum() >= 450 && r.getLocation().distanceSquaredTo(HQLocation) < 8) continue;
                            if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.COW)) {
                                if (pickup == null || r.getLocation().distanceSquaredTo(rc.getLocation()) < pickup.getLocation().distanceSquaredTo(rc.getLocation())) {
                                    if (r.getType() == RobotType.COW) {
                                        if (enemyHQLocation == null || r.getLocation().distanceSquaredTo(enemyHQLocation) > 48)
                                            pickup = r;
                                    } else {
                                        if (enemyHQLocation == null || r.getLocation().distanceSquaredTo(enemyHQLocation) > GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)
                                            pickup = r;
                                    }
                                }
                            }
                        }
                        if (pickup != null) {
                            // if can pickup do pickup
                            if (pickup.getLocation().isAdjacentTo(rc.getLocation())) {
                                System.out.println("Just picked up a " + pickup.getType());
                                if (rc.canPickUpUnit(pickup.getID())) {
                                    isCow = pickup.getType() == RobotType.COW;
                                    rc.pickUpUnit(pickup.getID());
                                }
                            } else {
                                // if not navigate to that unit
                                nav.bugNav(rc, pickup.getLocation());
                                System.out.println("Navigating to unit at " + pickup.getLocation().toString());
                            }
                        } else {
                            // if there are no robots nearby
                            if (enemyHQLocation != null) {
                                if (rc.getID() % 6 == 0) {
                                    // let some drones patrol
                                    nav.bugNav(rc, enemyHQLocationSuspect);
                                } else if (rc.getID() % 2 == 1) {
                                    patrolHQ();
                                } else nav.bugNav(rc, enemyHQLocation);
                            } else {
                                // too many rushes that we can't deal with without drones
                                if (rc.getID() % 2 == 1) {
                                    System.out.println("I'm patrolling!");
                                    patrolHQ();
                                } else {
                                    if (nav.getWander() >= wanderLimit) {
                                        resetEnemyHQSuspect();
                                    }
                                    System.out.println("going to enemy HQ suspect loc");
                                    nav.bugNav(rc, enemyHQLocationSuspect);
                                }
                            }
                        }
                    } else {
                        System.out.println("I am adjacent to my self is " + rc.getLocation().isAdjacentTo(rc.getLocation()));
                        System.out.println("I'm here and distance is: " + rc.getLocation().distanceSquaredTo(HQLocation));
                        // if within distance 13 of HQ first things first move away
                        if (rc.getLocation().distanceSquaredTo(HQLocation) <= 13) {
                            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                            optDir = optDir.rotateLeft();
                            for (int i = 0; i < 8; i++) {
                                if (rc.canMove(optDir)) {
                                    rc.move(optDir);
                                    break;
                                } else {
                                    optDir = optDir.rotateLeft();
                                }
                            }
                        }
                        // find water if not cow
                        System.out.println("I'm holding a unit!");
                        if (isCow) {
                            // go to enemyHQ
                            boolean canPlace = false;
                            if (enemyHQLocation != null) {
                                if (rc.getLocation().distanceSquaredTo(enemyHQLocation) < 24) {
                                    Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                                    for (int i = 0; i < 8; i++) {
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            canPlace = true;
                                            break;
                                        } else optDir = optDir.rotateRight();
                                    }
                                }
                            }
                            if (enemyHQLocation != null && !canPlace) {
                                nav.bugNav(rc, enemyHQLocation);
                            }
                            if (enemyHQLocation == null) {
                                if (nav.getWander() >= wanderLimit) {
                                    resetEnemyHQSuspect();
                                }
                                nav.bugNav(rc, enemyHQLocationSuspect);
                            }
                        } else {
                            MapLocation water = findWater();
                            MapLocation robotLoc = rc.getLocation();
                            if (water != null) {
                                if (water.isAdjacentTo(robotLoc)) {
                                    System.out.println("Dropping off unit!");
                                    // drop off unit
                                    Direction dropDir = robotLoc.directionTo(water);
                                    if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                                } else {
                                    System.out.println("Navigating to water at " + water.toString());
                                    nav.bugNav(rc, water);
                                }
                            } else {
                                // explore
                                if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                                    nextExplore();
                                }
                                System.out.println("I'm exploring to " + exploreTo.toString());
                                nav.bugNav(rc, exploreTo);
                            }
                        }
                    }
                    break;
                case PREPARE:
                    if (enemyHQLocation.distanceSquaredTo(rc.getLocation()) >= patrolRadiusMax) {
                        // if too far, move in
                        nav.bugNav(rc, enemyHQLocation);
                        break;
                    } else if (enemyHQLocation.distanceSquaredTo(rc.getLocation()) < patrolRadiusMin) {
                        // if too close, move out
                        nav.bugNav(rc, HQLocation);
                    }
                    // with in the area, move to closest possible position around enemy hq
                    // TODO: this ignores potential net guns around HQ
                    MapLocation minDistancedSafe = rc.getLocation();
                    int min_dist = enemyHQLocation.distanceSquaredTo(minDistancedSafe);
                    MapLocation nextPrepareLocation;
                    for (Direction dir : directions) {
                        nextPrepareLocation = rc.getLocation().add(dir);
                        if (min_dist > enemyHQLocation.distanceSquaredTo(nextPrepareLocation) &&
                                enemyHQLocation.distanceSquaredTo(nextPrepareLocation) >= 25 &&
                                nav.canGoDrone(rc, dir, true)) {
                            rc.move(dir);
                            break;
                        }
                    }
                    break;
                case ATTACK:
                    if (rc.isCurrentlyHoldingUnit() && !isAttackerBuilder) {
                        // throw unit out if it's not our unit
                        // check the 8 adjacent tiles and see if there's any water
                        boolean unitDropped = false;
                        for (Direction d : directions) {
                            if (rc.senseFlooding(rc.getLocation().add(d))) {
                                if (rc.canDropUnit(d)) {
                                    rc.dropUnit(d);
                                    unitDropped = true;
                                    break;
                                }
                            }
                        }
                        if (!unitDropped) {
                            // else, move out using even manhattan distance
                            MapLocation nextEscapeLocation;
                            int currentDistToEnemyHQ = rc.getLocation().distanceSquaredTo(enemyHQLocation);
                            for (Direction dir : directions) {
                                nextEscapeLocation = rc.getLocation().add(dir);
                                // manhattan distaance is odd makes a lattice
                                // even or closer make sure no dense positions
                                // check if empty
                                if (manhattanDistance(enemyHQLocation, nextEscapeLocation) % 2 == 0 &&
                                        (manhattanDistance(enemyHQLocation, rc.getLocation()) % 2 == 1 || nextEscapeLocation.distanceSquaredTo(enemyHQLocation) <= currentDistToEnemyHQ) &&
                                        rc.canMove(dir)) {
                                    rc.move(dir);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (isAttackerBuilder) {
                        // first, check if they can see enemyHQ
                            if (rc.canSenseLocation(enemyHQLocation)) {
                                // empty spots
                                ArrayList<MapLocation> emptySpots = new ArrayList<>();
                                // non buildings and drones spot
                                ArrayList<MapLocation> nonBuildingSpots = new ArrayList<>();
                                // check HQ surroundings and see if there's any openings
                                for (Direction dir: directions) {
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
                                for (MapLocation spots: emptySpots) {
                                    if (closestEmptySpot == null || rc.getLocation().distanceSquaredTo(spots) < rc.getLocation().distanceSquaredTo(closestEmptySpot)) {
                                        closestEmptySpot = spots;
                                    }
                                }
                                MapLocation closestNonBuildingSpot = null;
                                for (MapLocation spots: nonBuildingSpots) {
                                    if (closestNonBuildingSpot == null || rc.getLocation().distanceSquaredTo(spots) < rc.getLocation().distanceSquaredTo(closestNonBuildingSpot)) {
                                        closestNonBuildingSpot = spots;
                                    }
                                }
                                if (closestEmptySpot != null) {
                                    // if there's an empty spot navigate there and place your unit
                                    if (rc.getLocation().isAdjacentTo(closestEmptySpot)) {
                                        Direction optDir = rc.getLocation().directionTo(closestEmptySpot);
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            isAttackerBuilder = false;
                                        }
                                    } else {
                                        // if you're placing a unit move in even manhattan distance
                                        moveManhattan(closestEmptySpot, 0);
                                    }
                                }
                                else if (closestNonBuildingSpot != null) {
                                    // if there's an non building spot then hope that some other drone will come pick that unit up and then it'll convert to closestEmptySpot
                                    if (rc.getLocation().isAdjacentTo(closestNonBuildingSpot)) {
                                        Direction optDir = rc.getLocation().directionTo(closestNonBuildingSpot);
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            isAttackerBuilder = false;
                                        }
                                    } else {
                                        // if you're placing a unit move in even manhattan distance
                                        moveManhattan(closestNonBuildingSpot, 0);
                                    }
                                }
                                else {
                                    // if all of HQ is enclosed, then place them somewhere on land and make them dig the buildings (just deploying will do)
                                    if (rc.getLocation().distanceSquaredTo(enemyHQLocation) > 8) {
                                        // if they're not close enough, move in more
                                        moveManhattan(enemyHQLocation, 0);
                                    } else {
                                        // check all adjacent locations and see if they're land and placable
                                        Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                                        for (int i = 0; i < 8; i++) {
                                            if (!rc.senseFlooding(rc.getLocation().add(optDir))) {
                                                if (rc.canDropUnit(optDir)) {
                                                    rc.dropUnit(optDir);
                                                    break;
                                                }
                                            }
                                            optDir = optDir.rotateRight();
                                        }
                                        // if there's no available locations, still move manhattan
                                        moveManhattan(enemyHQLocation, 0);
                                    }
                                }
                            } else {
                                // otherwise move in
                                // they move with even parity because they have priority
                                moveManhattan(enemyHQLocation, 0);
                            }
                        } else {
                            // if next to any units pick them up
                            RobotInfo[] robotsmall = rc.senseNearbyRobots(2);
                            for (RobotInfo r : robotsmall) {
                                if (r.getTeam() != rc.getTeam() && (r.getType() == RobotType.MINER || r.getType() == RobotType.LANDSCAPER)) {
                                    if (rc.canPickUpUnit(r.getID())) {
                                        rc.pickUpUnit(r.getID());
                                        break;
                                    }
                                }
                            }
                            // get into attack radius of enemy netgun
                            moveManhattan(enemyHQLocation, 1);
                        }
                    }
                    break;
                case SURRENDER:
                    if (enemyHQLocation != null && rc.getLocation().distanceSquaredTo(enemyHQLocation) < patrolRadiusMin) {
                            // if close, move away
                            nav.bugNav(rc, HQLocation);
                    } else {
                        // if far, change back to normal state
                        phase = actionPhase.NON_ATTACKING;
                    }
                    break;
                case DEFENSE:
                    if (rc.isCurrentlyHoldingUnit()) {
                        // find water if not cow
                        System.out.println("I'm holding a unit!");
                        if (isCow) {
                            // go to enemyHQ
                            boolean canPlace = false;
                            if (enemyHQLocation != null) {
                                if (rc.getLocation().distanceSquaredTo(enemyHQLocation) < 24) {
                                    Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                                    for (int i = 0; i < 8; i++) {
                                        if (rc.canDropUnit(optDir)) {
                                            rc.dropUnit(optDir);
                                            canPlace = true;
                                            break;
                                        } else optDir = optDir.rotateRight();
                                    }
                                }
                            }
                            if (enemyHQLocation != null && !canPlace) {
                                nav.bugNav(rc, enemyHQLocation);
                            }
                            if (enemyHQLocation == null) {
                                if (nav.getWander() >= wanderLimit) {
                                    resetEnemyHQSuspect();
                                }
                                nav.bugNav(rc, enemyHQLocationSuspect);
                            }
                        } else {
                            MapLocation water = findWater();
                            MapLocation robotLoc = rc.getLocation();
                            if (water != null) {
                                if (water.isAdjacentTo(robotLoc)) {
                                    System.out.println("Dropping off unit!");
                                    // drop off unit
                                    Direction dropDir = robotLoc.directionTo(water);
                                    if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                                } else {
                                    System.out.println("Navigating to water at " + water.toString());
                                    nav.bugNav(rc, water);
                                }
                            } else {
                                // explore
                                if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                                    nextExplore();
                                }
                                System.out.println("I'm exploring to " + exploreTo.toString());
                                nav.bugNav(rc, exploreTo);
                            }
                        }
                    } else patrolHQ();

            }
        }
        System.out.println("I'm at " + rc.getLocation().toString());
    }

    static void runNetGun() throws GameActionException {
        // find drones and shoot them
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots) {
            if (r.getTeam() != rc.getTeam() && r.getType() == RobotType.DELIVERY_DRONE) {
                System.out.println("Shot a drone at " + r.getLocation());
                if (rc.canShootUnit(r.getID())) {
                    rc.shootUnit(r.getID());
                    break;
                }
            }
        }
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    // stores the closest MapLocation of soup in the robot's stored soup locations in soupLoc
    // but if within vision range, just normally find the closest soup
    static void findSoup() throws GameActionException {
        // try to find soup very close
        MapLocation robotLoc = rc.getLocation();
        int maxV = 5;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    if (rc.senseSoup(check) > 0) {
                        // find the closest maxmimal soup deposit
                        int checkDist = check.distanceSquaredTo(rc.getLocation());
                        if (soupLoc == null || checkDist < soupLoc.distanceSquaredTo(rc.getLocation())
                                || (checkDist == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                            soupLoc = check;
                    }
                }
            }
        }
        if (soupLoc != null) return;
        // if not, try to find closest soup according to stored soupLocation
        int closestDist = 0;
        if (soupLocation.isEmpty()) return;
        for (MapLocation soup: soupLocation) {
            // find the closest soup
            int soupDist = soup.distanceSquaredTo(rc.getLocation());
            if (soupLoc == null || soupDist < closestDist) {
                closestDist = soupDist;
                soupLoc = soup;
            }
        }
    }

    static MapLocation findWater() throws GameActionException {
        MapLocation water = null;
        int maxV = 2;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                MapLocation check = rc.getLocation().translate(x, y);
                if (rc.canSenseLocation(check)) {
                    if (rc.senseFlooding(check)) {
                        // find the closest maxmimal soup deposit
                        int checkDist = check.distanceSquaredTo(rc.getLocation());
                        if (water == null || checkDist < water.distanceSquaredTo(rc.getLocation()))
                            water = check;
                    }
                }
            }
        }
        if (water != null) return water;
        int closestDist = 0;
        for (MapLocation w : waterLocation) {
            // find the closest soup
            int waterDist = w.distanceSquaredTo(rc.getLocation());
            if (water == null || waterDist < closestDist) {
                closestDist = waterDist;
                water = w;
            }
        }
        return water;
    }







    // finds the next exploring location
    // if there is none left, return to HQ
    static void nextExplore() throws GameActionException {
        int closestDist = 1000000;
        for (int i = 0; i < 8; i++) {
            if (!suspectsVisited.get(suspects.get(i))) {
                if (rc.getLocation().distanceSquaredTo(suspects.get(i)) < closestDist) {
                    closestDist = rc.getLocation().distanceSquaredTo(suspects.get(i));
                    exploreTo = suspects.get(i);
                }
            }
        }
        if (closestDist == 1000000) exploreTo = HQLocation;
    }

    // generates locations we can explore
    static void exploreLoc() throws GameActionException {
        suspects = new ArrayList<>(Arrays.asList(horRef(HQLocation), verRef(HQLocation), horVerRef(HQLocation), new MapLocation(0, 0), new MapLocation(rc.getMapWidth()-1, 0), new MapLocation(0, rc.getMapHeight()), new MapLocation(rc.getMapWidth()-1, rc.getMapHeight()-1), new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2)));
        for (MapLocation loc: suspects) {
            suspectsVisited.put(loc, false);
        }
        enemyHQLocationSuspect = suspects.get(rc.getID() % 3);
    }

    // when a unit is first created it calls this function
    static void initialize() throws GameActionException {
        spawnHeight = rc.senseElevation(rc.getLocation());
        if (rc.getType() == RobotType.HQ) {
            collectInfo();
        } else {
            getAllInfo();
        }
        exploreLoc();
        if (rc.getType() == RobotType.MINER) {
            findState();
        }
        if (rc.getType() == RobotType.LANDSCAPER) {
            turtle = new Turtle(rc, HQLocation);
        }
    }

    static void resetEnemyHQSuspect() throws GameActionException {
        idIncrease++;
        enemyHQLocationSuspect = suspects.get((rc.getID()+idIncrease) % 3);
    }

    static void patrolHQ() throws GameActionException {
        Direction rotateDir = rc.getLocation().directionTo(HQLocation);
        int distHQ = rc.getLocation().distanceSquaredTo(HQLocation);
        if (distHQ < patrolRadiusMin) {
            rotateDir = rotateDir.opposite();
        } else if (distHQ <= patrolRadiusMax) {
            rotateDir = rotateDir.rotateLeft();
            rotateDir = rotateDir.rotateLeft();
        }
        for (int i = 0; i < 8; i++) {
            if (nav.canGo(rc, rotateDir, true)) rc.move(rotateDir);
            else rotateDir = rotateDir.rotateRight();
        }
        if (rc.canMove(rotateDir)) rc.move(rotateDir);
    }

    static void findState() throws GameActionException {
        if (rc.getType() == RobotType.MINER) {
            if (rc.getRoundNum() == 2) {
                if (HQLocation == null) {
                    System.out.println("HQloc is null");
                }
                isBuilder = true;
                blueprint = new Blueprint(HQLocation);
            }
            if (rc.getRoundNum() == 3) isAttacker = true;
            return;
        }
    }







    // reflect horizontally
    static MapLocation horRef(MapLocation loc) {
        return new MapLocation(rc.getMapWidth() - 1 - loc.x, loc.y);
    }
    // reflect vertically
    static MapLocation verRef(MapLocation loc) {
        return new MapLocation(loc.x, rc.getMapHeight() - 1 - loc.y);
    }
    // reflect vertically and horizontally
    static MapLocation horVerRef(MapLocation loc) {
        return new MapLocation(rc.getMapWidth() - 1 - loc.x, rc.getMapHeight() - 1 - loc.y);
    }




    static int manhattanDistance(MapLocation loc1, MapLocation loc2){
        return  Math.abs(loc1.x-loc2.x) + Math.abs(loc1.y-loc2.y);
    }



    // get information from the blocks
    static void getAllInfo() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++) {
            if (i % 10 == 1 || i % 10 == 2 || i % 10 == 3) getInfo(i);
        }
    }

    // get information from blockchain on that turn
    static void getInfo(int roundNum) throws GameActionException {
//        System.out.println("Getting info of round "+roundNum);
        Transaction[] info = rc.getBlock(roundNum);
        for (Transaction stuff: info) {
            int[] messageArr = removePad(stuff.getMessage());
            if (Cast.isMessageValid(rc, messageArr)) {
                for (int i = 0; i < messageArr.length-1; i++) {
                    int message = messageArr[i];
//                    System.out.println("message is: " + message);
//                    System.out.println("message validness is " + Cast.isValid(message, rc));
//                    System.out.println("message cat is" + Cast.getCat(message));
//                    System.out.println("message coord is" + Cast.getCoord(message));
                    if (Cast.isValid(message, rc)) {
                        // if valid message
                        MapLocation loc = Cast.getCoord(message);
                        boolean doAdd;
//                        System.out.println(Cast.getCat(message).toString());
                        switch (Cast.getCat(message)) {
                            case HQ:
                                HQLocation = loc;
                                break;
                            case ENEMY_HQ:
                                enemyHQLocation = loc;
                                if (!nav.isThreat(loc)) nav.addThreat(loc);
                                break;
                            case NET_GUN:
                                if (!nav.isThreat(loc)) nav.addThreat(loc);
                                break;
                            case NEW_REFINERY:
                                if (rc.getType() == RobotType.LANDSCAPER) break;
                                if (!refineryLocation.contains(loc)) refineryLocation.add(loc);
                                break;
                            case NEW_SOUP:
                                if (rc.getType() == RobotType.LANDSCAPER) break;
                                // add if it's far away enough from all the other soup coords
                                doAdd = true;
                                for (MapLocation soup : soupLocation) {
                                    if (soup.distanceSquaredTo(loc) <= soupClusterDist) {
                                        doAdd = false;
                                        break;
                                    }
                                }
                                if (doAdd) {
                                    soupLocation.add(loc);
                                }
                                break;
                            case WATER:
                                // add if it's far away enough from all the other water coords
                                if (rc.getType() != RobotType.DELIVERY_DRONE) break;
                                doAdd = true;
                                for (MapLocation water : waterLocation) {
                                    if (water.distanceSquaredTo(loc) <= waterClusterDist) {
                                        doAdd = false;
                                        break;
                                    }
                                }
                                if (doAdd) {
                                    waterLocation.add(loc);
                                }
                                break;
                            case REMOVE:
                                soupLocation.remove(loc);
                                waterLocation.remove(loc);
                                nav.removeThreat(loc);
                                if (suspects != null) {
                                    for (MapLocation l : suspects) {
                                        if (l.equals(loc)) {
                                            suspectsVisited.replace(l, true);
                                            break;
                                        }
                                    }
                                }
                                break;
                            case HELP:
                                if (rc.getType() != RobotType.DELIVERY_DRONE && rc.getType() != RobotType.FULFILLMENT_CENTER) break;
                                MapLocation c1 = Cast.getC1(message);
                                MapLocation c2 = Cast.getC2(message);
                                helpLoc.add(new Pair(c1, c2));
                                break;
                            case PREPARE:
                                phase = actionPhase.PREPARE;
                                break;
                            case ATTACK:
                                phase = actionPhase.ATTACK;
                                break;
                            case SURRENDER:
                                phase = actionPhase.SURRENDER;
                                break;
                            case DEFENSE:
                                phase = actionPhase.DEFENSE;
                                break;
                            case OUTER_LAYER:
                                isOuterLayer = true;
                                break;
                            case INNER_LAYER:
                                isInnerLayer = true;
                                break;
                        }
                    }
                }
            }
        }
    }

    // send info when the turn number is 1 mod waitBlock (10), otherwise keep saving data
    static void collectInfo() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        if (HQLocation == null && rc.getType() == RobotType.HQ) {
            HQLocation = rc.getLocation();
            infoQ.add(Cast.getMessage(Cast.InformationCategory.HQ, HQLocation));
        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        // location of the bot
        MapLocation rloc;
        // whether it is already in memory
        boolean saved;
        for (RobotInfo r : robots) {
            saved=false;
            if (enemyHQLocation == null && r.getType() == RobotType.HQ && r.getTeam() != rc.getTeam()) {
                enemyHQLocation = r.getLocation(); 
                infoQ.add(Cast.getMessage(Cast.InformationCategory.ENEMY_HQ, enemyHQLocation));
                infoQ.add(Cast.getMessage(Cast.InformationCategory.NET_GUN, enemyHQLocation));
                if (!nav.isThreat(enemyHQLocation)) nav.addThreat(enemyHQLocation);
            } 
            // why is this an else if?
            else if (rc.getType() == RobotType.MINER && (r.getType() == RobotType.REFINERY || r.getType() == RobotType.HQ) && r.getTeam() == rc.getTeam()){
                rloc=r.getLocation();
                // check for matching
                for (MapLocation refineryLoca: refineryLocation){
                    if (refineryLoca.equals(rloc)) {
                        saved=true;
                    }
                }
                // no matching => not saved => save it
                if (!saved){
                    refineryLocation.add(rloc);
                }
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() != rc.getTeam()) {
                MapLocation netGunLoc = r.getLocation();
                if (!nav.isThreat(netGunLoc)) {
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.NET_GUN, netGunLoc));
                    nav.addThreat(netGunLoc);
                }
            }
        }
        boolean doAdd;
        soupLoc = null;
        for (int x = -maxV; x <= maxV; x+=2) {
            for (int y = -maxV; y <= maxV; y+=2) {
                // running out of bytecode so exiting early
                if (Clock.getBytecodesLeft() < 1000) break;
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    // for now only check soup on dry land
                    if (rc.senseSoup(check) > 0 && !rc.senseFlooding(check)) {
                        int checkDist = check.distanceSquaredTo(rc.getLocation());
                        if (soupLoc == null || checkDist < soupLoc.distanceSquaredTo(rc.getLocation())
                                || (checkDist == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                            soupLoc = check;
                        doAdd = true;
                        for (MapLocation soup: soupLocation) {
                            if (soup.distanceSquaredTo(check) <= soupClusterDist) {
                                doAdd = false;
                                break;
                            }
                        }
                        if (doAdd) {
                            soupLocation.add(check);
                            infoQ.add(Cast.getMessage(Cast.InformationCategory.NEW_SOUP, check));
                        }
                    }
                    if ((rc.getRoundNum() <= 300 || (x % 3 == 0 && y % 3 == 0)) && rc.senseFlooding(check)) {
                        doAdd = true;
                        for (MapLocation water: waterLocation) {
                            if (water.distanceSquaredTo(check) <= waterClusterDist) {
                                doAdd = false;
                                break;
                            }
                        }
                        if (doAdd) {
                            waterLocation.add(check);
                            infoQ.add(Cast.getMessage(Cast.InformationCategory.WATER, check));
                        }
                    }
                }
            }
            if (Clock.getBytecodesLeft() < 1000) break;
        }
        for (MapLocation water: waterLocation) {
            if (rc.canSenseLocation(water)) {
                if (!rc.senseFlooding(water)) infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, water));
            }
        }
        for (MapLocation soup: soupLocation) {
            if (rc.getLocation().equals(soup)) {
                // check if robot is on the soup location and there is no soup around him
                // if there isn't any soup around it then remove
                findSoup();
                if (rc.senseSoup(soup) == 0 && (soupLoc == null || rc.getLocation().distanceSquaredTo(soupLoc) >= soupClusterDist || soup.equals(soupLoc))) {
                    System.out.println("There's no soup!");
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, soup));
                    soupLocation.remove(soup);
                }
                break;
            }
        }
        if (suspects != null) {
            for (MapLocation l: suspects) {
                if (suspectsVisited.get(l)) {
                    if (rc.getLocation().equals(l)) {
                        suspectsVisited.replace(l, true);
                        infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, l));
                    } else if (rc.canSenseLocation(l)) {
                        RobotInfo r = rc.senseRobotAtLocation(l);
                        if (r != null) {
                            RobotType t = r.getType();
                            if (r.getTeam() != rc.getTeam() && (t == RobotType.HQ || t == RobotType.NET_GUN)) {
                                suspectsVisited.replace(l, true);
                                infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, l));
                            } else if (rc.getLocation().distanceSquaredTo(l) < 9) {
                                suspectsVisited.replace(l, true);
                                infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, l));
                            }
                        }
                    }
                }
            }
        }
        if (rc.getRoundNum() % waitBlock == 1) sendInfo();
    }

    // send information collected to the blockchain
    static void sendInfo() throws GameActionException {
        if (!infoQ.isEmpty())  {
            int blockSize = Math.min(6, infoQ.size());
            int[] info = new int[blockSize+1];
            int[] prepHash = new int[blockSize];
            for (int i = 0; i < blockSize; i++) {
                info[i] = infoQ.get(0);
                prepHash[i] = infoQ.get(0);
                infoQ.remove(0);
            }
            // add the hash
            info[blockSize] = Cast.hash(rc, prepHash);
            if (rc.canSubmitTransaction(padMessage(info), defaultCost)) {
//                System.out.println("Submitted transaction! Message is : " + info.toString());
                rc.submitTransaction(padMessage(info), defaultCost);
            }
        }
    }

    static int[] padMessage(int[] arr) throws GameActionException {
        int[] message = new int[7];
        for (int i = 0; i < 7; i++) {
            if (i < arr.length) {
                message[i] = arr[i];
            } else {
                message[i] = 0;
            }
        }
        return message;
    }

    static int[] removePad(int[] arr) throws GameActionException {
        int nonZero = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 0) nonZero++;
        }
        int[] actualMesssage = new int[nonZero];
        int index = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 0) {
                actualMesssage[index] = arr[i];
                index++;
            }
        }
        return actualMesssage;
    }

    // move to location with the desired parity of manhattan distance

    // NOTE: PARITY WITH ENEMYHQ
    static void moveManhattan(MapLocation loc, int parity) throws GameActionException {
        int nonparity;
        if (parity == 0) nonparity = 1;
        else nonparity = 0;
        int currentDist = rc.getLocation().distanceSquaredTo(loc);
        MapLocation nextAttackLocation;
        for (Direction dir : directions) {
            nextAttackLocation = rc.getLocation().add(dir);
            // manhattan distance is odd makes a lattice
            // even or closer make sure no dense positions
            // check if empty
            if (manhattanDistance(enemyHQLocation, nextAttackLocation) % 2 == parity &&
                    (manhattanDistance(enemyHQLocation, rc.getLocation()) % 2 == nonparity || nextAttackLocation.distanceSquaredTo(loc) <= currentDist) &&
                    rc.canMove(dir)) {
                rc.move(dir);
                break;
            }
        }
    }
}

