package rush;

import battlecode.common.*;

import java.util.ArrayList;

import static rush.Cast.getMessage;
import static rush.Cast.infoQ;
import static rush.Robot.*;
import static rush.Util.directions;

public class Rush {

    private boolean isRush = true;
    private boolean landscaperPlaced = false;
    private boolean netGunPlaced = false;
    private MapLocation landscaperFactory = null;
    private MapLocation[] suspects;
    private MapLocation currentDest;
    private MapLocation lastLoc;
    private boolean isBugging;
    private int closestDist;
    private boolean[] visited;
    private ArrayList<MapLocation> emptySpots;
    private int enemyHQHeight;

    public Rush() {
        isRush = true;
        suspects = new MapLocation[]{verRef(HQLocation), horVerRef(HQLocation), horRef(HQLocation)};
        visited = new boolean[]{false, false, false};
    }

    public boolean getRush() {
        return isRush;
    }

    public void killEnemy() throws GameActionException {
        if (enemyHQLocation != null) {
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
                        // build net gun
                        if (!netGunPlaced) {
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
                    } else {
                        System.out.println("Going to my location!");
                        bugNav(goTo);
                    }
                }
            } else {
                System.out.println("Currently navigating to enemy hq");
                getEmpty();
                if (emptySpots.isEmpty()) {
                    System.out.println("No empty spots");
                    bugNav(enemyHQLocation);
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
            int curDist = 0;
            for (MapLocation loc: nonVisited) {
                int tempD = rc.getLocation().distanceSquaredTo(loc);
                if (goTo == null || tempD < curDist) {
                    goTo = loc;
                    curDist = tempD;
                }
            }
            System.out.println("Going to pos: " + goTo);
            // go to pos
            bugNav(goTo);
        }
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
            return rob == null && Math.abs(rc.senseElevation(loc)-enemyHQHeight) <= 3;
        }
        return false;
    }

    public boolean engaged() {
        return landscaperPlaced;
    }

    public void turnOff() {
        isRush = false;
    }

    public void bugNav(MapLocation dest) throws GameActionException {
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

    private boolean canGo(Direction dir) throws GameActionException {
        MapLocation moveTo = rc.getLocation().add(dir);
        if (!rc.canMove(dir)) return false;
        if (moveTo.equals(lastLoc)) return false;
        if (rc.canSenseLocation(moveTo) && rc.senseFlooding(moveTo)) return false;
        return true;
    }
}
