package bot01;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.math.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    public enum actionPhase{
        NON_ATTACKING,
        PREPARE,
        ATTACK,
        SURRENDER
    }
        
        
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
    // default cost of our transaction
    private static int defaultCost = 2;

    // navigation object
    static Nav nav = new Nav();

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static ArrayList<MapLocation> waterLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> soupLocation = new ArrayList<MapLocation>();
    // only miners use following
    static ArrayList<MapLocation> refineryLocation = new ArrayList<MapLocation>();
    static boolean refineryInVision;
    // only drones use following
    static RobotInfo closestEnemyUnit;

    static MapLocation soupLoc = null;
    static MapLocation closestRefineryLocation = null;

    // booleans
    static boolean isCow = false;

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

    // strategy
    static actionPhase phase = actionPhase.NON_ATTACKING;

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
                    refineryInVision=false;
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
        System.out.println("enemy hq might be at " + enemyHQLocationSuspect.toString());
        if (enemyHQLocation!=null){
        System.out.println("enemy hq is at" + enemyHQLocation.toString());
        }
        // below are debugging
        if (turnCount == 600 && enemyHQLocation== null){
            infoQ.add(Cast.getMessage(Cast.InformationCategory.ENEMY_HQ, enemyHQLocationSuspect ));
            enemyHQLocation=enemyHQLocationSuspect;
        }
        if (turnCount == 600 && enemyHQLocation!= null) {
            infoQ.add(Cast.getMessage(Cast.InformationCategory.PREPARE, enemyHQLocation));
            System.out.println("prepare");
        }
        // above are debugging
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
        if (minerCount < 5+rc.getRoundNum()/100) {
            for (Direction d : Direction.allDirections()) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, d)) {
                    rc.buildRobot(RobotType.MINER, d);
                    minerCount++;
                }
            }
        }
    }

    static void runMiner() throws GameActionException {
        System.out.println("I have " + Clock.getBytecodesLeft());
        // build drone factory if there isn't one
        if (rc.getRobotCount() > 4 && rc.getLocation().distanceSquaredTo(HQLocation) < 15 && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && rc.getRoundNum() <= 200) {
            // first check if there's a fullfillment center nearby
            RobotInfo[] robots = rc.senseNearbyRobots();
            boolean alreadyBuilt = false;
            for (RobotInfo r: robots) {
                if (r.getType() == RobotType.FULFILLMENT_CENTER && r.getTeam() == rc.getTeam()) alreadyBuilt = true;
            }
            if (!alreadyBuilt) {
                for (Direction d : Direction.allDirections()) {
                    tryBuild(RobotType.FULFILLMENT_CENTER, d);
                }
            }
        }
//        if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 200 && rc.getLocation().distanceSquaredTo(HQLocation) < 15) {
//            for (Direction d : Direction.allDirections()) {
//                tryBuild(RobotType.VAPORATOR, d);
//            }
//        }
//        System.out.println("Before finding soup, I have "+ Clock.getBytecodesLeft());
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
//                System.out.println("after find min d i have " + Clock.getBytecodesLeft());
//                System.out.println("soup at " + reference_point.toString());
//                System.out.println("closest refinery at " + closestRefineryLocation.toString());
//                System.out.println("im at " + rc.getLocation().toString());
//                System.out.println("soup min distance to refinery " + minRefineryDist);
//                System.out.println("soup min distance to bot " + reference_point.distanceSquaredTo(rc.getLocation()));
                //
                if (minRefineryDist >= 81 && reference_point.distanceSquaredTo(rc.getLocation()) < 4 && rc.getTeamSoup() >= 200) {
                    System.out.println("attempt build refinery");
                    for (Direction temp_dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, temp_dir)) {
                            System.out.println("can build refinery");
                            rc.buildRobot(RobotType.REFINERY, temp_dir);
                            System.out.println("built refinery");
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
                nav.bugNav(rc, closestRefineryLocation);
            }
        } else {
            if (soupLoc != null) {
//                System.out.println("Soup is at: " + soupLoc.toString());
                Direction locDir = rc.getLocation().directionTo(soupLoc);
                if (rc.canMineSoup(locDir)) {
                    rc.mineSoup(locDir);
                }
                // if we can't mine soup, go to other soups
                else nav.bugNav(rc, soupLoc);
            } else {
                // scout for soup
                if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                    nextExplore();
                }
                nav.bugNav(rc, exploreTo);
            }
        }
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {
        // produce 8 drones
        Direction optDir = Direction.NORTHWEST;
        if (droneCount < 80) {
            for (int i=0; i<8 ; i++) {
                if ( rc.getTeamSoup()<350){
                    break;
                }
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) ) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                }else{
                    optDir=optDir.rotateRight();
                }
            }
        }
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        // find opponent units and pick up
        if(closestEnemyUnit != null){
            System.out.println("there is a "+closestEnemyUnit.getType()+" nearby");
        }

        if (!rc.isCurrentlyHoldingUnit() && closestEnemyUnit != null){
            // System.out.println("I'm not holding any units!");
                // if can pickup do pickup
            if (closestEnemyUnit.getLocation().isAdjacentTo(rc.getLocation())) {
                // System.out.println("Just picked up a " + closestEnemyUnit.getType());
                if (rc.canPickUpUnit(closestEnemyUnit.getID())) {
                    isCow = closestEnemyUnit.getType() == RobotType.COW;
                    rc.pickUpUnit(closestEnemyUnit.getID());
                }
            } else {
                // if not navigate to that unit
                nav.bugNav(rc, closestEnemyUnit.getLocation());
                // System.out.println("Navigating to unit at " + closestEnemyUnit.getLocation().toString());
            }
        } else if(rc.isCurrentlyHoldingUnit()){
            // find water if not cow
            // System.out.println("I'm holding a unit!");
            if (isCow) {
                // go to enemyHQ
                boolean canPlace = false;
                if (enemyHQLocation != null) {
                    if (rc.getLocation().distanceSquaredTo(enemyHQLocation) < 36) {
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
        System.out.println("I'm at " + phase.toString() +" phase");
        switch(phase){
            case NON_ATTACKING:
                // if there are no robots nearby
                if (enemyHQLocation != null) {
                    if (rc.getID() % 4 == 0) {
                        // let some drones patrol
                        nav.bugNav(rc, enemyHQLocationSuspect);
                    }
                    else if (rc.getID() % 2 == 1) {
                        // patrol HQ
                        Direction rotateDir = rc.getLocation().directionTo(HQLocation);
                        int distHQ = rc.getLocation().distanceSquaredTo(HQLocation);
                        if (distHQ < patrolRadiusMin) {
                            rotateDir = rotateDir.opposite();
                        }
                        else if (distHQ <= patrolRadiusMax) {
                            rotateDir = rotateDir.rotateLeft();
                            rotateDir = rotateDir.rotateLeft();
                        }
                        for (int i = 0; i < 8; i++) {
                            if (nav.canGoDrone(rc, rotateDir)) rc.move(rotateDir);
                            else rotateDir = rotateDir.rotateRight();
                        }
                        if (rc.canMove(rotateDir)) rc.move(rotateDir);
                    }
                    else nav.bugNav(rc, enemyHQLocation);
                } else nav.bugNav(rc, enemyHQLocationSuspect);
                break;
            case PREPARE:
                if (enemyHQLocation.distanceSquaredTo(rc.getLocation()) > 169 ){
                    nav.bugNav(rc, enemyHQLocation);
                    break;
                }else if(enemyHQLocation.distanceSquaredTo(rc.getLocation()) < 16 ){
                    nav.bugNav(rc, HQLocation);
                }
                
                MapLocation minminManhattan = rc.getLocation();
                int minManhattanDist = manhattanDistance(enemyHQLocation, minminManhattan);
                MapLocation nextLocation;
                for (Direction dir: directions){
                    nextLocation=rc.getLocation().add(dir);
                    if (manhattanDistance(enemyHQLocation, nextLocation)%2==1 &&
                        (manhattanDistance(enemyHQLocation, minminManhattan)%2==0 || manhattanDistance(enemyHQLocation, nextLocation) <= minManhattanDist) &&
                        enemyHQLocation.distanceSquaredTo(nextLocation) >=25 &&
                        rc.canMove(dir)){
                        rc.move(dir);
                        break;
                    }
                }
            break;
            case ATTACK:
            break;
            case SURRENDER:
            break;

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

    // returns the current water level given turn count
    static int waterLevel() {
        double x = (double) turnCount;
        return (int) Math.exp(0.0028 * x - 1.38 * Math.sin(0.00157 * x - 1.73) + 1.38 * Math.sin(-1.73)) - 1;
    }

    static int distSqr(int dx, int dy) {
        return dx * dx + dy * dy;
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
                        System.out.println(Cast.getCat(message).toString());
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
                            case PREPARE:
                                System.out.println("prepare to attack");
                                phase=actionPhase.PREPARE;
                                break;
                            case ATTACK:
                                phase=actionPhase.ATTACK;
                                break;
                            case SURRENDER:
                                phase=actionPhase.SURRENDER;
                                break;
                            // TODO: other cases we need to figure out
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
        // type of the bot
        RobotType enemytype;
        //
        int r_rc_distance=1000000;
        // whether it is already in memory
        boolean saved;
        for (RobotInfo r : robots) {
            saved=false;
            enemytype=r.getType();
            if (rc.getType()==RobotType.DELIVERY_DRONE && r.getTeam() != rc.getTeam()) {
                if (enemyHQLocation!=null){
                System.out.println( "enemy base is at " +enemyHQLocation.toString());
                }
                System.out.println( "enemy base is at " +enemyHQLocationSuspect.toString());
                System.out.println( "distance from cow to enemy base is " +r.getLocation().distanceSquaredTo( enemyHQLocationSuspect));
                if (enemyHQLocation == null && enemytype == RobotType.HQ){
                    enemyHQLocation = r.getLocation(); 
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.ENEMY_HQ, enemyHQLocation));
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.NET_GUN, enemyHQLocation));
                    if (!nav.isThreat(enemyHQLocation)) nav.addThreat(enemyHQLocation);
                }else if (enemytype == RobotType.MINER || enemytype== RobotType.LANDSCAPER || enemytype== RobotType.COW && phase==actionPhase.NON_ATTACKING && !(enemyHQLocation == null && r.getLocation().distanceSquaredTo( enemyHQLocationSuspect)< 49 || enemyHQLocation != null && r.getLocation().distanceSquaredTo( enemyHQLocation)< 49) ) {
                    if (closestEnemyUnit==null || rc.getLocation().distanceSquaredTo(r.getLocation())<r_rc_distance){
                        closestEnemyUnit=r;
                        r_rc_distance=rc.getLocation().distanceSquaredTo(r.getLocation());
                    }
                }
            } 
            // why is this an else if?
            else if (rc.getType() == RobotType.MINER && (enemytype == RobotType.REFINERY || enemytype == RobotType.HQ) && r.getTeam() == rc.getTeam()){
                refineryInVision=true;
                rloc=r.getLocation();
                // check for matching
                for (MapLocation refineryLoca: refineryLocation){
                    if (refineryLoca==rloc){
                        saved=true;
                        break;
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
                if (soupLoc == null || rc.getLocation().distanceSquaredTo(soupLoc) >= soupClusterDist || soup.equals(soupLoc)) {
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

