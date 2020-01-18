package teraform;

import battlecode.common.Direction;

// This is a file to accumulate all the random helper functions
// which don't interact with the game, but are common enough to be used in multiple places.
// For example, lots of logic involving MapLocations and Directions is common and ubiquitous.
public class Util {
    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };

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
    static int patrolRadiusMin = 34;
    static int patrolRadiusMax = 50;
    // help radius (super large for now because helping miners is pretty important)
    static int helpRadius = 800;
    // refinery radius
    static int refineryDist = 63;
    // drone count control
    static int maxDroneCount = 70;
    // landscaper count control
    static int attackLandscaperCount = 20;
    // attack control
    static int battlefieldRadius = 169;
    // teraform height
    static int teraformHeight = 10;

    // default cost of our transaction
    static int defaultCost = 2;
    // how much drones can wander
    static int wanderLimit = 5;
    // when builder returns
    static int builderReturn = 60;
    // when to explode drone
    static int explodeThresh = 10;

    public int floodRound(int level) {
        if (level == 1) return 230;
        if (level == 2) return 420;
        if (level == 3) return 620;
        if (level == 4) return 880;
        return 0;
    }
}
