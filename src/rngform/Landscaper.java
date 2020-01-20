package rngform;

import battlecode.common.*;

import java.util.ArrayList;

import static rngform.Cast.getMessage;
import static rngform.Cast.infoQ;
import static rngform.Util.directions;

public class Landscaper extends Unit {

    private ArrayList<MapLocation> visitedHole;
    private Vector[] untouchable;
    private MapLocation[] untouchableLoc;
    private int untouchSize = 12;
    private Direction fill;
    private Direction digLoc;


    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        visitedHole = new ArrayList<>();

    }

    public void initialize() throws GameActionException {

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
      
    }


    // returns the optimal height of a location. Adds 2 to the height if near water.
    //  TODO: change this
    public int optHeight(MapLocation loc) throws GameActionException {
        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
        int waterHeight = 0;
        for (Direction dir: directions) {
            MapLocation posWater = loc.add(dir);
            if (rc.canSenseLocation(posWater) && rc.senseFlooding(posWater)) waterHeight = 2;
        }
        return Math.min(12, (int) (Math.floor(Math.sqrt(distFromFactory)/1.3)) + 10)+waterHeight;
    }

    public void checkFillAndDig(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            if (dig.equals(dir)) continue;
            MapLocation fill = rc.getLocation().add(dir);
            boolean bad = false;
            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    bad = true;
                    break;
                }
            }
            if (bad) continue;
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)
                        && (rob == null || !(rob.getType().isBuilding() && rob.getTeam() == rc.getTeam()))) {
                    this.fill = dir;
                    return;
                }
                if ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40)
                        || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0)) {
                    this.digLoc = dir;
                    return;
                }
            }
        }
    }
}