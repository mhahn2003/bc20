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

    static int turnCount;

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
    static int patrolRadiusMin = 36;
    static int patrolRadiusMax = 45;
    // help radius
    static int helpRadius = 80;
    // refinery radius
    static int refineryDist = 48;
    // default cost of our transaction
    private static int defaultCost = 2;
    // how much drones can wander
    static int wanderLimit = 5;

    // navigation object
    static Nav nav = new Nav();
    // turtle object
    static Turtle turtle;

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static ArrayList<MapLocation> soupLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocation = new ArrayList<MapLocation>();
    static boolean refineryInVision;
    static ArrayList<MapLocation> waterLocation = new ArrayList<MapLocation>();
    static MapLocation soupLoc = null;
    static MapLocation closestRefineryLocation = null;
    static ArrayList<Pair> helpLoc = new ArrayList<>();

    // states
    // for drones:   0: not helping   1: finding stranded   2: going to requested loc
    // for miners:   0: normal        1: requesting help
    static int helpMode = 0;
    static int helpIndex = -1;
    // is miner #1
    static boolean isBuilder;
    // is miner #2
    static boolean isAttacker;

    // booleans
    static boolean isCow = false;

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
                    getInfo(rc.getRoundNum()-1);
                    collectInfo();
                }
//                System.out.println("I have " + Clock.getBytecodesLeft() + " left!");
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        refineryInVision=false;
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
        // build all the miners we can get in the first few turns
        // maximum of 15 miners at 280th round
        Direction optDir = Direction.NORTH;
        if (minerCount < Math.max(5+rc.getRoundNum()/40, 12)) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
    }

    static void runMiner() throws GameActionException {
        System.out.println("I have " + Clock.getBytecodesLeft());
        System.out.println("I have " + rc.getSoupCarrying());
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
                MapLocation reference_point = soupLoc;
                if (reference_point != null) {
                    //                System.out.println("reference to " + reference_point.toString());
                    int minRefineryDist = reference_point.distanceSquaredTo(HQLocation);

                    //                System.out.println("before find min d i have" + Clock.getBytecodesLeft());
                    // check through refinery(redundancy here)
                    for (MapLocation refineryLoca : refineryLocation) {
                        int temp_d = reference_point.distanceSquaredTo(refineryLoca);
                        if (temp_d < minRefineryDist) {
                            closestRefineryLocation = refineryLoca;
                            minRefineryDist = temp_d;
                        }
                        //                    System.out.println("my memory contain " + refineryLoca.toString());
                    }
                    //                System.out.println("reference to " + reference_point.toString());
                    //                System.out.println("compare to " + closestRefineryLocation.toString());
                    //                System.out.println("after find min d i have " + Clock.getBytecodesLeft());
                    //                System.out.println("reference min distance to refinery " + minRefineryDist);
                    //                System.out.println("reference min distance to bot " + reference_point.distanceSquaredTo(rc.getLocation()));
                    //
                    if (minRefineryDist >= refineryDist && reference_point.distanceSquaredTo(rc.getLocation()) < 4 && rc.getTeamSoup() >= 200) {
                        //                    System.out.println("attempt build refinery");
                        for (Direction temp_dir : directions) {
                            if (rc.canBuildRobot(RobotType.REFINERY, temp_dir)) {
                                //                            System.out.println("can build refinery");
                                rc.buildRobot(RobotType.REFINERY, temp_dir);
                                //                            System.out.println("built refinery");
                                MapLocation robotLoc = rc.getLocation();
                                refineryLocation.add(robotLoc.add(temp_dir));
                                closestRefineryLocation = refineryLocation.get(refineryLocation.size() - 1);
                                break;
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
                } else {
                    if (nav.needHelp(rc, turnCount, closestRefineryLocation)) {
                        helpMode = 1;
//                        System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        infoQ.add(Cast.getMessage(rc.getLocation(), closestRefineryLocation));
                    }
                    else nav.bugNav(rc, closestRefineryLocation);
                }
            } else {
                if (soupLoc != null) {
                    System.out.println("Soup is at: " + soupLoc.toString());
                    Direction locDir = rc.getLocation().directionTo(soupLoc);
                    if (rc.canMineSoup(locDir)) {
                        rc.mineSoup(locDir);
                        // pollution might make miner skip this even though it's right next to soup
                        nav.navReset(rc, rc.getLocation());
                    }
                    // if we can't mine soup, go to other soups
                    else {
                        if (nav.needHelp(rc, turnCount, soupLoc)) {
                            helpMode = 1;
//                            System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            infoQ.add(Cast.getMessage(rc.getLocation(), soupLoc));
                        }
                        else nav.bugNav(rc, soupLoc);
                    }
                } else {
                    // scout for soup
                    if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                        nextExplore();
                    }
                    nav.bugNav(rc, exploreTo);
                }
            }
        }
    }

    static void runRefinery() throws GameActionException {
        // maybe refineries can request for help from drones?
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

        // produce 5 landscapers initially to guard hq
        Direction optDir = Direction.SOUTHEAST;
        if (landscaperCount < 5) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    landscaperCount++;
                    break;
                } else {
                    optDir = optDir.rotateRight();
                }
            }
        }
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
        if (isVaporator) {
            // produce outer layer
            if (landscaperCount < 28) {
                for (int i = 0; i < 8; i++) {
                    if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                        rc.buildRobot(RobotType.LANDSCAPER, optDir);
                        landscaperCount++;
                        break;
                    } else {
                        optDir = optDir.rotateRight();
                    }
                }
            }
        }
        // produce inner layer of minescapers
        if (netGunCount >= 4) {
            if (landscaperCount < 39) {
                for (int i = 0; i < 8; i++) {
                    if (rc.isReady() && rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                        rc.buildRobot(RobotType.LANDSCAPER, optDir);
                        landscaperCount++;
                        break;
                    } else {
                        optDir = optDir.rotateRight();
                    }
                }
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        // produce 8 drones
        Direction optDir = Direction.NORTHWEST;
        if (droneCount < 8) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    break;
                } else {
                    optDir = optDir.rotateRight();
                }
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        if (rc.getRoundNum() >= 600) {
            // convert to state 2 if needed
            // TODO: figure out when is optimal to change into state 2, or to change into state 3 where we can use the disruptWithCow strategy
            turtle.setLandscaperState(2);
        }
        if (turtle.getLandscaperState() == 0) {
            // TODO: maybe we can make a ENEMY_BUILDING category and have them attack in the future
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo r: robots) {
                if (r.getType() == RobotType.LANDSCAPER && r.getTeam() != rc.getTeam() && r.getLocation().distanceSquaredTo(HQLocation) <= 20) {
                    // if they're attacking HQ they're most likely attacking it or one of our building
                    MapLocation DFLoc = HQLocation.add(Direction.EAST);
                    MapLocation LFLoc = HQLocation.add(Direction.WEST);
                    RobotInfo LFr = rc.senseRobotAtLocation(LFLoc);
                    RobotInfo DFr = rc.senseRobotAtLocation(DFLoc);
                    if (rc.getLocation().isAdjacentTo(HQLocation) && rc.canDigDirt(rc.getLocation().directionTo(HQLocation))) rc.digDirt(rc.getLocation().directionTo(HQLocation));
                    else if (rc.getLocation().isAdjacentTo(LFLoc) && rc.canDigDirt(rc.getLocation().directionTo(LFLoc)) && LFr != null && LFr.getType() == RobotType.DESIGN_SCHOOL) rc.digDirt(rc.getLocation().directionTo(LFLoc));
                    else if (rc.getLocation().isAdjacentTo(DFLoc) && rc.canDigDirt(rc.getLocation().directionTo(DFLoc)) && DFr != null && DFr.getType() == RobotType.FULFILLMENT_CENTER) rc.digDirt(rc.getLocation().directionTo(DFLoc));
                    else {
                        // if not adjacent, stay there
                        if (!rc.getLocation().isAdjacentTo(HQLocation)) nav.bugNav(rc, HQLocation);
                    }
                }
                if (r.getTeam() != rc.getTeam() && (r.getType() != RobotType.MINER || r.getType() != RobotType.LANDSCAPER || r.getType() != RobotType.DELIVERY_DRONE || r.getType() != RobotType.COW)) {
                    // if it's an enemy building, bury it
                    if (rc.getLocation().isAdjacentTo(r.getLocation())) {
                        if (rc.getDirtCarrying() == 0) {
                            // dig anywhere but to that robot, and preferably not downward
                            for (Direction d: directions) {
                                if (rc.getLocation().add(d).equals(r.getLocation())) continue;
                                if (rc.canDigDirt(d)) rc.digDirt(d);
                            }
                        } else {
                            // fill up location
                            Direction optDir = rc.getLocation().directionTo(r.getLocation());
                            if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                        }
                    } else {
                        // move to that location
                        nav.bugNav(rc, r.getLocation());
                    }
                }
            }
            // if there's nothing nearby, patrol
            if (rc.isReady()) {
                MapLocation patrol = turtle.findPatrol(rc);
                if (!rc.getLocation().equals(patrol)) {
                    nav.bugNav(rc, patrol);
                }
            }
        } else if (turtle.getLandscaperState() == 1) {
            RobotInfo[] robots = rc.senseNearbyRobots(2);
            // check if there's anything adjacent to it that can bury
            for (RobotInfo r: robots) {
                if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                    // if it's an enemy building, bury it
                    if (rc.getDirtCarrying() == 0) {
                        // dig anywhere but to that robot, and preferably not downward
                        Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                        for (int i = 0; i < 8; i++) {
                            if (!rc.getLocation().add(optDir).equals(r.getLocation())) {
                                if (rc.canDigDirt(optDir)) {
                                    rc.digDirt(optDir);
                                    break;
                                }
                            }
                            optDir = optDir.rotateRight();
                        }
                    } else {
                        // fill up location
                        Direction optDir = rc.getLocation().directionTo(r.getLocation());
                        if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                    }
                }
                if (r.getTeam() == rc.getTeam() && r.getType().isBuilding()) {
                    // if our own building is getting buried (most likely net gun) dig out dirt
                    Direction optDir = rc.getLocation().directionTo(r.getLocation());
                    if (rc.canDigDirt(optDir)) rc.digDirt(optDir);
                }
            }
            // build outer wall if no other problems
            if (turtle.positionOut(rc.getLocation()) == -1) {
                MapLocation left = new Vector(-1, -3).addWith(HQLocation);
                MapLocation right = new Vector(0, -3).addWith(HQLocation);
                if (rc.canSenseLocation(left) && rc.senseRobotAtLocation(left) != null) {
                    nav.bugNav(rc, right);
                } else {
                    nav.bugNav(rc, left);
                }
            } else {
                turtle.buildFort(rc);
            }
        } else if (turtle.getLandscaperState() == 2) {
            RobotInfo[] robots = rc.senseNearbyRobots(2);
            // check if there's anything adjacent to it that can bury, but I really doubt it can happen at this state
            for (RobotInfo r: robots) {
                if (r.getTeam() != rc.getTeam() && r.getType().isBuilding()) {
                    // if it's an enemy building, bury it
                    if (rc.getDirtCarrying() == 0) {
                        // dig downward because you are alpha
                        if (rc.canDigDirt(Direction.CENTER)) rc.digDirt(Direction.CENTER);
                    } else {
                        // fill up location
                        Direction optDir = rc.getLocation().directionTo(r.getLocation());
                        if (rc.canDepositDirt(optDir)) rc.depositDirt(optDir);
                    }
                }
                if (r.getTeam() == rc.getTeam() && r.getType().isBuilding()) {
                    // if our own building is getting buried (most likely net gun) dig out dirt
                    Direction optDir = rc.getLocation().directionTo(r.getLocation());
                    if (rc.canDigDirt(optDir)) rc.digDirt(optDir);
                }
            }
            // build inner wall
            if (turtle.positionOut(rc.getLocation()) == -1) {
                MapLocation left = new Vector(0, -2).addWith(HQLocation);
                MapLocation right = new Vector(1, -2).addWith(HQLocation);
                if (rc.canSenseLocation(right) && rc.senseRobotAtLocation(right) != null) {
                    nav.bugNav(rc, left);
                } else {
                    nav.bugNav(rc, right);
                }
            } else {
                turtle.buildFort(rc);
            }
        }
    }

    static void runDeliveryDrone() throws GameActionException {
//        if (!helpLoc.isEmpty()) {
//            System.out.println("My help queue is: " + helpLoc.toString());
//        }
        // check for help mode
        if (helpMode == 0 && !rc.isCurrentlyHoldingUnit()) {
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
                helpMode = 1;
            }
        }
        if (helpMode != 0) {
            System.out.println("Currently on help mode " + helpMode);
            // helping mode on!
            if (helpMode == 1) {
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
            }
            else if (helpMode == 2) {
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
            // find opponent units and pick up
            if (!rc.isCurrentlyHoldingUnit()) {
                System.out.println("I'm not holding any units!");
                // find opponent units
                RobotInfo pickup = null;
                for (RobotInfo r : rc.senseNearbyRobots()) {
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
                        if (nav.getWander() >= wanderLimit) {
                            resetEnemyHQSuspect();
                        }
                        nav.bugNav(rc, enemyHQLocationSuspect);
                    }
                }
            } else {
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
            nav.bugNav(rc, enemyHQLocationSuspect);
            System.out.println("I'm at " + rc.getLocation().toString());
        }
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
            if (nav.canGo(rc, rotateDir)) rc.move(rotateDir);
            else rotateDir = rotateDir.rotateRight();
        }
        if (rc.canMove(rotateDir)) rc.move(rotateDir);
    }

    static void findState() throws GameActionException {
        if (rc.getType() == RobotType.MINER) {
            if (rc.getRoundNum() == 2) isBuilder = true;
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
            if (Cast.isMessageValid(stuff.getMessage())) {
                for (int i = 0; i < stuff.getMessage().length-1; i++) {
                    int message = stuff.getMessage()[i];
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
                                if (!refineryLocation.contains(loc)) refineryLocation.add(loc);
                                break;
                            case NEW_SOUP:
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
                                if (rc.getType() != RobotType.DELIVERY_DRONE) break;
                                MapLocation c1 = Cast.getC1(message);
                                MapLocation c2 = Cast.getC2(message);
                                helpLoc.add(new Pair(c1, c2));
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
                refineryInVision=true;
                rloc=r.getLocation();
                // check for matching
                for (MapLocation refineryLoca: refineryLocation){
                    if (refineryLoca==rloc){
                        saved=true;
                    }
                }
                // no matching => not saved => save it
                if (!saved){
                    refineryLocation.add(rloc);
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
                    if (rc.senseFlooding(check)) {
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
            info[blockSize] = Cast.hash(prepHash);
            if (rc.canSubmitTransaction(info, defaultCost)) {
//                System.out.println("Submitted transaction! Message is : " + info.toString());
                rc.submitTransaction(info, defaultCost);
            }
        }
    }
}

