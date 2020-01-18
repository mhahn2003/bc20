package teraform;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static teraform.Cast.getMessage;
import static teraform.Cast.infoQ;
import static teraform.Util.directions;

public class Robot {
    static RobotController rc;

    // spawn variables
    static int turnCount = 0;
    static int factoryHeight;
    static int sizeX;
    static int sizeY;
    static int rotateState;


    // navigation object
    static Nav nav;
    // communication object
    static Cast cast;
    // turtle object
    static Turtle turtle;
    // blueprint object (only the first miner will have this)
    static Blueprint blueprint;

    // hole array
    static boolean[][] holeLocation;

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static MapLocation factoryLocation = null;
    static MapLocation droneFactoryLocation = null;
    static ArrayList<MapLocation> soupLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> refineryLocation = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> waterLocation = new ArrayList<MapLocation>();
    static MapLocation[] teraformLoc = new MapLocation[3];
    // only miners use the following
    static MapLocation soupLoc = null;
    static MapLocation closestRefineryLocation = null;
    // only drones use following
    static RobotInfo closestEnemyUnit;
    static ArrayList<Pair> helpLoc = new ArrayList<>();

    static RobotPlayer.actionPhase phase= RobotPlayer.actionPhase.NON_ATTACKING;

    // states
    // for drones:   0: not helping   1: finding stranded   2: going to requested loc
    // for miners:   0: normal        1: requesting help
    static int helpMode = 0;
    static int helpIndex = -1;

    // for landscapers:    0: building teraform   1: building 5x5 turtle
    static int teraformMode = 0;

    static boolean isBuilder;
    static boolean isAttacker;

    // booleans
    // is drone holding a cow
    static boolean isCow = false;
    // explode the unit
    static boolean explode = false;
    // is HQ under attack
    static boolean isUnderAttack = false;
    // is drone carrying another attacker unit
    static boolean isAttackerBuilder = false;
    // is the turtle around HQ done
    static boolean isTurtle = false;
    // have drones spawned yet
    static boolean areDrones = false;

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


    public Robot(RobotController r) {
        rc = r;
        cast = new Cast(rc);
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
        if (turnCount == 1) {
            initialize();
        } else {
            closestEnemyUnit=null;
            cast.getInfo(rc.getRoundNum()-1);
            cast.collectInfo();
        }
    }

    // when a unit is first created it calls this function
    public void initialize() throws GameActionException {
        nav = new Nav();
        if (rc.getType() == RobotType.HQ) {
            cast.collectInfo();
            findHoleSize();
        } else {
            cast.getAllInfo();
        }
        if (rc.getType() == RobotType.HQ) findRotate();
        exploreLoc();

        if (rc.getType() == RobotType.MINER) {
            if (rc.getRoundNum() == 2) {
                isBuilder = true;
            }
            if (rc.getRoundNum() == 3) isAttacker = true;
            return;
        }
        if (rc.getType() == RobotType.LANDSCAPER) {
            turtle = new Turtle(rc, HQLocation, rotateState);
            System.out.println("Initialized teraform!");
            teraformLoc[0] = null;
            teraformLoc[1] = null;
            teraformLoc[2] = null;
            // find design school and record location
            if (factoryLocation == null) {
                for (Direction dir : directions) {
                    MapLocation loc = rc.getLocation().add(dir);
                    if (rc.canSenseLocation(loc)) {
                        RobotInfo factory = rc.senseRobotAtLocation(loc);
                        if (factory != null && factory.getType() == RobotType.DESIGN_SCHOOL && factory.getTeam() == rc.getTeam()) {
                            factoryLocation = loc;
                            factoryHeight = rc.senseElevation(factoryLocation);
                            System.out.println("direction of factory is: " + dir.toString());
                            if (dir.equals(rotateDir(Direction.NORTHEAST))) {
                                System.out.println("My mode is 2!");
                                teraformMode = 2;
                                turtle = new Turtle(rc, HQLocation, rotateState);
                            }
                            break;
                        }
                    }
                }
            } else {
                if (rc.canSenseLocation(factoryLocation)) {
//                    System.out.println("direction to factory: " + rc.getLocation().directionTo(factoryLocation).toString());
//                    System.out.println("rotated direction: " + rotateDir(Direction.NORTHEAST));
//                    System.out.println("rotateState: " + rotateState);
                    if (rc.getLocation().directionTo(factoryLocation).equals(rotateDir(Direction.NORTHEAST))) {
                        System.out.println("My mode is 2!");
                        teraformMode = 2;
                        turtle = new Turtle(rc, HQLocation, rotateState);
                    }
                    factoryHeight = rc.senseElevation(factoryLocation);
                }
            }
        }
    }

    // find rotation
    static void findRotate() throws GameActionException {
        System.out.println("Finding rotation");
        // find rotation of map
        MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        Direction dirCenter = HQLocation.directionTo(center);
        if (dirCenter == Direction.EAST || dirCenter == Direction.NORTHEAST || dirCenter == Direction.NORTH) {
            rotateState = 0;
        }
        else if (dirCenter == Direction.WEST || dirCenter == Direction.SOUTHWEST || dirCenter == Direction.SOUTH) {
            rotateState = 2;
        }
        else if (dirCenter == Direction.SOUTHEAST) {
            rotateState = 1;
        }
        else {
            rotateState = 3;
        }
        if (HQLocation.x >= 9 && HQLocation.x <= rc.getMapWidth()-10 && HQLocation.y >= 9 && HQLocation.y <= rc.getMapHeight()-10) {
            rotateState += 2;
            rotateState %= 2;
        }
        System.out.println("Initial rotateState was: " + rotateState);
        Vector[] buildLoc = new Vector[]{new Vector(-2, -2), new Vector(2, -2), new Vector(-2, 2), new Vector(2, 2)};
        ArrayList<Vector> possibleBuilds = new ArrayList<>();
        int spawnHeight = rc.senseElevation(HQLocation);
        // check for buildable locations and rotate to that direction
        for (int i = 0; i < 4; i++) {
            buildLoc[i] = buildLoc[i].rotate(rotateState);
            MapLocation loc = buildLoc[i].addWith(HQLocation);
            if (rc.canSenseLocation(loc)) {
                if (Math.abs(rc.senseElevation(loc) - spawnHeight) < 6 && !rc.senseFlooding(loc)) {
                    boolean flood = false;
                    if (rc.senseElevation(loc) < 4) {
                        MapLocation hole = loc.add(HQLocation.directionTo(loc));
                        if (rc.canSenseLocation(hole)) {
                            if (rc.senseFlooding(hole)) flood = true;
                        }
                    }
                    if (!flood) possibleBuilds.add(buildLoc[i]);
                    System.out.println("I added: " + buildLoc[i].getX() + ", " + buildLoc[i].getY());
                }
            }
        }
        // TODO: what do we do if possibleBuilds is empty??
        dirCenter = HQLocation.directionTo(possibleBuilds.get(0).addWith(HQLocation));
        System.out.println("dirCenter is now: " + dirCenter.toString());
        if (dirCenter == Direction.EAST || dirCenter == Direction.NORTHEAST || dirCenter == Direction.NORTH) {
            rotateState = 0;
        } else if (dirCenter == Direction.WEST || dirCenter == Direction.SOUTHWEST || dirCenter == Direction.SOUTH) {
            rotateState = 2;
        } else if (dirCenter == Direction.SOUTHEAST) {
            rotateState = 1;
        } else {
            rotateState = 3;
        }
//        System.out.println("Sent rotation state of: " + rotateState);
        infoQ.add(getMessage(Cast.InformationCategory.ROTATION, new MapLocation(0, rotateState)));
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

    // generates locations we can explore
    static void exploreLoc() throws GameActionException {
        suspects = new ArrayList<>(Arrays.asList(horRef(HQLocation), verRef(HQLocation), horVerRef(HQLocation), new MapLocation(0, 0), new MapLocation(rc.getMapWidth()-1, 0), new MapLocation(0, rc.getMapHeight()), new MapLocation(rc.getMapWidth()-1, rc.getMapHeight()-1), new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2)));
        for (MapLocation loc: suspects) {
            suspectsVisited.put(loc, false);
        }
        enemyHQLocationSuspect = suspects.get(rc.getID() % 3);
    }

    // find how many holes can fit in the map and initialize the array
    static void findHoleSize() {
        int minX = HQLocation.x % 3;
        int minY = HQLocation.y % 3;
        int maxX = HQLocation.x;
        int maxY = HQLocation.y;
        while (maxX < rc.getMapWidth()) {
            maxX += 3;
        }
        while (maxY < rc.getMapHeight()) {
            maxY += 3;
        }
        System.out.println("maxX: " + maxX);
        System.out.println("minX: " + minX);
        System.out.println("maxY: " + maxY);
        System.out.println("minY: " + minY);

        sizeX = (maxX-minX+3)/3;
        sizeY = (maxY-minY+3)/3;
        System.out.println("sizeX: " + sizeX);
        System.out.println("sizeY: " + sizeY);
        holeLocation = new boolean[sizeX][sizeY];
    }

    // rotate direction orientated with center
    static Direction rotateDir(Direction dir) {
        return Vector.getVec(dir).rotate(rotateState).getDir();
    }
}