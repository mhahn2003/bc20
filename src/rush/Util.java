package rush;

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
    static int friendlyPatrolRadiusMin = 13;
    static int friendlyPatrolRadiusMax = 25;
    static int enemyPatrolRadiusMin = 34;
    static int enemyPatrolRadiusMax = 50;
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

    public int floodRound (int elevation) {
        //returns the round it gets flooded the given elevation. Last one is for 30
        switch(elevation) {
            case 0: return 0;
            case 1: return 256;
            case 2: return 464;
            case 3: return 677;
            case 4: return 931;
            case 5: return 1210;
            case 6: return 1413;
            case 7: return 1546;
            case 8: return 1640;
            case 9: return 1713;
            case 10: return 1771;
            case 11: return 1819;
            case 12: return 1861;
            case 13: return 1897;
            case 14: return 1929;
            case 15: return 1957;
            case 16: return 1983;
            case 17: return 2007;
            case 18: return 2028;
            case 19: return 2048;
            case 20: return 2067;
            case 21: return 2084;
            case 22: return 2100;
            case 23: return 2115;
            case 24: return 2129;
            case 25: return 2143;
            case 26: return 2155;
            case 27: return 2168;
            case 28: return 2179;
            case 29: return 2190;
            default: return 0;
        }
    }
}
