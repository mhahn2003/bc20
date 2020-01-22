package rushdef;

import battlecode.common.*;

import java.util.ArrayList;

import static rushdef.Cast.getMessage;
import static rushdef.Cast.infoQ;
import static rushdef.Robot.*;
import static rushdef.Util.directions;

public class Rush {

    static private boolean isRush = true;
    static private boolean landscaperPlaced = false;
    static private boolean netGunPlaced = false;
    static private boolean flyingDetected = false;
    static private MapLocation landscaperFactory = null;
    static private MapLocation[] suspects = new MapLocation[]{horRef(HQLocation), horVerRef(HQLocation), verRef(HQLocation)};
    static private MapLocation currentDest;
    static private MapLocation lastLoc;
    static private boolean isBugging;
    static private int closestDist;
    static private boolean[] visited = new boolean[]{false, false, false};
    static private ArrayList<MapLocation> emptySpots;
    static private int enemyHQHeight;
    static private int[][] heights;


    static boolean getRush() {
        return isRush;
    }

    static void killEnemy() throws GameActionException {
        if (enemyHQLocation != null) {
            if (!flyingDetected) {
                checkFlying();
            }
            if (rc.canSenseLocation(enemyHQLocation)) enemyHQHeight = rc.senseElevation(enemyHQLocation);
            System.out.println("I see enemy hq!");
            // if enemy HQ location is seen
            if (rc.getLocation().isAdjacentTo(enemyHQLocation)) {
                System.out.println("I'm right next to it!");
                if (!landscaperPlaced) {
                    System.out.println("Haven't placed factory down yet");
                    Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                    Direction left = optDir.rotateLeft();
                    Direction right = optDir.rotateRight();
                    if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, left)) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, left);
                            landscaperPlaced = true;
                            landscaperFactory = rc.getLocation().add(left);

                        } else if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, right)) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, right);
                            landscaperPlaced = true;
                            landscaperFactory = rc.getLocation().add(right);
                        } else {
                            for (int i = 0; i < 8; i++) {
                                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, optDir)) {
                                    rc.buildRobot(RobotType.DESIGN_SCHOOL, optDir);
                                    landscaperPlaced = true;
                                    landscaperFactory = rc.getLocation().add(optDir);
                                } else {
                                    optDir = optDir.rotateRight();
                                }
                            }
                        }
                    }
                    if (landscaperPlaced) {
                        System.out.println("I placed factory down!");
                    }
                } else {
                    System.out.println("I've already placed factory down!");
                    System.out.println("Factory is at: " + landscaperFactory);
                    // check if landscaper factory has been destroyed
                    RobotInfo factoryCheck = rc.senseRobotAtLocation(landscaperFactory);
                    if (factoryCheck == null) {
                        isRush = false;
                        // broadcast that the rush ended
                        infoQ.add(getMessage(Cast.InformationCategory.RUSH, enemyHQLocation));
                    }
                    // landscaper factory already placed
                    getEmpty();
                    System.out.println("My empty list is: " + emptySpots.toString());
                    MapLocation goTo = rc.getLocation();
                    int curDist = goTo.distanceSquaredTo(landscaperFactory);
                    // get away from the design school
                    for (MapLocation loc: emptySpots) {
                        int dist = loc.distanceSquaredTo(landscaperFactory);
                        if (dist > curDist) {
                            goTo = loc;
                            curDist = dist;
                            System.out.println("Updated");
                            System.out.println("goTo is now: " + goTo.toString());
                            System.out.println("curDist is now: " + curDist);
                        }
                    }
                    System.out.println("I need to go to: " + goTo.toString());
                    if (rc.getLocation().equals(goTo)) {
                        System.out.println("Already there!");
                        // don't move
                    } else {
                        // only move if it's adjacent or there's a lot of empty spots
                        System.out.println("Going to my location!");
                        if (goTo.isAdjacentTo(rc.getLocation()) || emptySpots.size() > 2) bugNav(goTo);
                    }
                    // build net gun if we need to and can
                    if (!netGunPlaced && flyingDetected) {
                        System.out.println("I don't have a net gun yet!");
                        // check if there are any landscapers first before spawning
                        boolean isLandscaper = false;
                        RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
                        for (RobotInfo r: robots) {
                            if (r.getType() == RobotType.LANDSCAPER) {
                                isLandscaper = true;
                                break;
                            }
                        }
                        if (isLandscaper && rc.getTeamSoup() >= RobotType.NET_GUN.cost) {
                            Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
                            for (int i = 0; i < 8; i++) {
                                if (rc.canBuildRobot(RobotType.NET_GUN, optDir)) {
                                    rc.buildRobot(RobotType.NET_GUN, optDir);
                                    netGunPlaced = true;
                                } else optDir = optDir.rotateRight();
                            }
                        }
                    }
                }
            } else {
                System.out.println("Currently navigating to enemy hq");
                getEmpty();
                if (emptySpots.isEmpty()) {
                    System.out.println("No empty spots");
                    // bug nav if not close
                    if (rc.getLocation().distanceSquaredTo(enemyHQLocation) > 5) bugNav(enemyHQLocation);
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
                    System.out.println("Going to: " + goTo.toString());
                    bugNav(goTo);
                }
            }
        } else {
            System.out.println("I don't see enemy HQ");
            // navigate to enemy HQ (don't see it right now)
            ArrayList<MapLocation> nonVisited = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                if (rc.canSenseLocation(suspects[i])) {
                    System.out.println("I can see pos: " + i);
                    visited[i] = true;
                }
                if (!visited[i]) {
                    nonVisited.add(suspects[i]);
                }
            }
            // go to the closest location
            MapLocation goTo = null;
            goTo = nonVisited.get(0);
//            int curDist = 0;
//            for (MapLocation loc: nonVisited) {
//                int tempD = rc.getLocation().distanceSquaredTo(loc);
//                if (goTo == null || tempD < curDist) {
//                    goTo = loc;
//                    curDist = tempD;
//                }
//            }
            System.out.println("Going to pos: " + goTo);
            // go to pos
            if (rc.isReady()) {
                nav.bugNav(rc, goTo);
            }
        }
    }

    static void getEmpty() throws GameActionException {
        // assuming enemyHQLocation is not null
        emptySpots = new ArrayList<>();
        for (Direction dir: directions) {
            MapLocation loc = enemyHQLocation.add(dir);
            if (isEmpty(loc)) emptySpots.add(loc);
        }
    }

    static boolean isEmpty(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            RobotInfo rob = rc.senseRobotAtLocation(loc);
            return rob == null && Math.abs(rc.senseElevation(loc)-enemyHQHeight) <= 3;
        }
        return false;
    }

    static boolean engaged() {
        return landscaperPlaced;
    }

    static void turnOff() {
        isRush = false;
    }

    static void bugNav(MapLocation dest) throws GameActionException {
        System.out.println("I'm at: " + rc.getLocation().toString());
        if (currentDest == null || !dest.isAdjacentTo(currentDest)) {
            System.out.println("Resetting");
            currentDest = dest;
            lastLoc = null;
            isBugging = false;
            closestDist = rc.getLocation().distanceSquaredTo(dest);
        }
        Direction optDir = rc.getLocation().directionTo(dest);
        System.out.println("optimal direction is: " + optDir.toString());
        if (!isBugging) {
            System.out.println("Not bugging right now");
            if (canGo(optDir)) {
                System.out.println("I moved!");
                lastLoc = rc.getLocation();
                rc.move(optDir);
            }
            else {
                System.out.println("Couldn't move, switching to bugging");
                isBugging = true;
            }
        }
        if (isBugging) {
            System.out.println("Bugging right now");
            for (int i = 0; i < 8; i++) {
                if (canGo(optDir)) {
                    System.out.println("I can move to: " + optDir.toString());
                    lastLoc = rc.getLocation();
                    rc.move(optDir);
                    break;
                } else {
                    optDir = optDir.rotateRight();
                }
            }
        }
        if (rc.getLocation().distanceSquaredTo(dest) < closestDist) {
            System.out.println("I'm closer now, so going back to bugging");
            closestDist = rc.getLocation().distanceSquaredTo(dest);
            isBugging = false;
        }
    }

    // optimized bugnav
    static void bugNavOpt(MapLocation dest) throws GameActionException {
        System.out.println("I'm at: " + rc.getLocation().toString());
        if (currentDest == null || !dest.isAdjacentTo(currentDest)) {
            System.out.println("Resetting");
            currentDest = dest;
            lastLoc = null;
            isBugging = false;
            closestDist = rc.getLocation().distanceSquaredTo(dest);
        }
        Direction optDir = getOptimalDirection(dest);
        System.out.println("optimal direction is: " + optDir.toString());
        if (!isBugging) {
            System.out.println("Not bugging right now");
            if (canGo(optDir)) {
                System.out.println("I moved!");
                lastLoc = rc.getLocation();
                rc.move(optDir);
            }
            else {
                System.out.println("Couldn't move, switching to bugging");
                isBugging = true;
            }
        }
        if (isBugging) {
            if (optDir.equals(Direction.CENTER)) optDir = rc.getLocation().directionTo(dest);
            System.out.println("Bugging right now");
            for (int i = 0; i < 8; i++) {
                if (canGo(optDir)) {
                    System.out.println("I can move to: " + optDir.toString());
                    lastLoc = rc.getLocation();
                    rc.move(optDir);
                    break;
                } else {
                    optDir = optDir.rotateRight();
                }
            }
        }
        if (rc.getLocation().distanceSquaredTo(dest) < closestDist) {
            System.out.println("I'm closer now, so going back to bugging");
            closestDist = rc.getLocation().distanceSquaredTo(dest);
            isBugging = false;
        }
    }

    static boolean canGo(Direction dir) throws GameActionException {
        MapLocation moveTo = rc.getLocation().add(dir);
        if (!rc.canMove(dir)) return false;
        if (moveTo.equals(lastLoc)) return false;
        if (rc.canSenseLocation(moveTo) && rc.senseFlooding(moveTo)) return false;
        return true;
    }

    static void checkFlying() {
        RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.DELIVERY_DRONE || r.getType() == RobotType.FULFILLMENT_CENTER) {
                flyingDetected = true;
                break;
            }
        }
    }

    static Direction getOptimalDirection(MapLocation dest) throws GameActionException {
        System.out.println("Initially I have: " + Clock.getBytecodesLeft());
        boolean[][] firstMove = new boolean[3][3];
        Direction[][] secondMove = new Direction[5][5];
        heights = new int[5][5];
        for (int i = 4; i >= 0; i--) {
            for (int j = 4; j >= 0; j--) {
                secondMove[i][j] = Direction.CENTER;
                MapLocation loc = rc.getLocation().translate(i-2, j-2);
                if (rc.canSenseLocation(loc)) {
                    if (rc.senseFlooding(loc)) heights[i][j] = -10000;
                    else heights[i][j] = rc.senseElevation(loc);
                } else heights[i][j] = 10000;
                if (i == 2 && j == 2) heights[i][j] = rc.senseElevation(rc.getLocation());
                System.out.println("Height at i: " + i + " j: " + j + " is at: " + heights[i][j]);
//                System.out.println("One itereation takes: " + Clock.getBytecodesLeft());
            }
        }

//        System.out.println("After initializing arrays, I have: " + Clock.getBytecodesLeft());
        Direction optDir = rc.getLocation().directionTo(dest);
//        System.out.println("optDir is: " + optDir.toString());
        Direction left = optDir.rotateLeft();
        Direction right = optDir.rotateRight();
        Direction leftLeft = left.rotateLeft();
        Direction rightRight = right.rotateRight();
        if (canGo(optDir) && canGo(left) && canGo(right)) {
//            System.out.println("I can go anywhere");
            return optDir;
        }
//        System.out.println("Still computing now");
        for (int i = 2; i >= 0; i--) {
            for (int j = 2; j >= 0; j--) {
                Direction d = new Vector(i-1, j-1).getDir();
                if (optDir.equals(d) || left.equals(d) || right.equals(d) || leftLeft.equals(d) || rightRight.equals(d)) firstMove[i][j] = true;
                if (!canMove(2, 2, i+1, j+1)) firstMove[i][j] = false;
                if (firstMove[i][j]) {
                    System.out.println("First move for i: " + (i-1) + ", j: " + (j-1));
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            d = new Vector(x, y).getDir();
                            if (!(optDir.equals(d) || left.equals(d) || right.equals(d) || leftLeft.equals(d) || rightRight.equals(d))) continue;
                            // check the second iteration
                            if (canMove(i+1, j+1, i+1+x, j+1+y)) {
                                System.out.println("Can move to x: " + x + ", y: " + y);
                                secondMove[i+1+x][j+1+y] = d;
                            }
                        }
                    }
                }
            }
        }
//        System.out.println("After doing 2 steps, I have: " + Clock.getBytecodesLeft());
        optDir = Direction.CENTER;
        // first check if we can just get there with only one move
        for (int i = 2; i >= 0; i--) {
            for (int j = 2; j >= 0; j--) {
                if (firstMove[i][j]) {
                    MapLocation loc = rc.getLocation().translate(i-1, j-1);
                    int tempD = loc.distanceSquaredTo(dest);
                    if (tempD < closestDist) {
                        closestDist = tempD;
                        optDir = new Vector(i-1, j-1).getDir();
                    }
                }
            }
        }
//        System.out.println("After checking 1 step, I have: " + Clock.getBytecodesLeft());

        for (int i = 4; i >= 0; i--) {
            for (int j = 4; j >= 0; j--) {
                System.out.println("For i: " + i + ", j: " + j + ", we have secondMove: " + secondMove[i][j].toString());
                if (!secondMove[i][j].equals(Direction.CENTER)) {
                    MapLocation loc = rc.getLocation().translate(i-2, j-2);
                    int tempD = loc.distanceSquaredTo(dest);
                    if (tempD < closestDist) {
                        closestDist = tempD;
                        optDir = rc.getLocation().directionTo(loc.add(secondMove[i][j].opposite()));
                    }
                }
            }
        }
        System.out.println("After checking 2 steps, I have: " + Clock.getBytecodesLeft());
        return optDir;
    }

    static boolean canMove(int fi, int fj, int si, int sj) {
        return Math.abs(heights[fi][fj]-heights[si][sj]) <= 3;
    }
}
