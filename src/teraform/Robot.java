package teraform;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Robot {
    static RobotController rc;

    // spawn variables
    static int turnCount = 0;

    // navigation object
    static Nav nav = new Nav();
    // communication object
    static Cast cast;

    // important locations
    static MapLocation HQLocation = null;
    static MapLocation enemyHQLocation = null;
    static MapLocation factoryLocation = null;
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

    static RobotPlayer.actionPhase phase= RobotPlayer.actionPhase.NON_ATTACKING;

    // states
    // for drones:   0: not helping   1: finding stranded   2: going to requested loc
    // for miners:   0: normal        1: requesting help
    static int helpMode = 0;
    static int helpIndex = -1;

    // for landscapers:    0: building teraform   1: building 5x5 turtle
    static int teraformMode = 0;

    // booleans
    // is drone holding a cow
    static boolean isCow = false;
    // explode the unit
    static boolean explode = false;
    // is HQ under attack
    static boolean isUnderAttack = false;
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
        if (rc.getType() == RobotType.HQ) {
            cast.collectInfo();
        } else {
            cast.getAllInfo();
        }
        exploreLoc();
//        if (rc.getType() == RobotType.MINER) {
//            findState();
//        }
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
}