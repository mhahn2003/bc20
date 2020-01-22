package pure_teraform;

import battlecode.common.*;

import static pure_teraform.Cast.getMessage;
import static pure_teraform.Cast.infoQ;
import static pure_teraform.Util.directions;

public class HQ extends Shooter {
    static Vector[] complete;
    static MapLocation[] completeLoc;

    public HQ(RobotController r) {
        super(r);
        complete = new Vector[]{new Vector(0, 3), new Vector(1, 3), new Vector(2, 3), new Vector(3, 3), new Vector(3, 2), new Vector(3, 1), new Vector(3, 0), new Vector(3, -1), new Vector(3, -2), new Vector(3, -3), new Vector(2, -3), new Vector(1, -3), new Vector(0, -3), new Vector(-1, -3), new Vector(-2, -3), new Vector(-3, -3), new Vector(-3, -2), new Vector(-3, -1), new Vector(-3, 0), new Vector(-3, 1), new Vector(-3, 2), new Vector(-3, 3), new Vector(-2, 3), new Vector(-1, 3)};
        completeLoc = new MapLocation[24];
        for (int i = 0; i < 24; i++) {
            completeLoc[i] = complete[i].addWith(rc.getLocation());
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // find drones and shoot them
//        System.out.println("enemy hq might be at " + enemyHQLocationSuspect.toString());
//        if (enemyHQLocation!=null){
//        System.out.println("enemy hq is at" + enemyHQLocation.toString());
//        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        if (!isUnderAttack) {
            for (RobotInfo r : robots) {
                if ((r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.MINER) && r.getTeam() != rc.getTeam()) {
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.DEFENSE, HQLocation));
                    isUnderAttack = true;
                    break;
                }
            }
        } else {
            isUnderAttack = false;
            for (RobotInfo r : robots) {
                if ((r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.MINER) && r.getTeam() != rc.getTeam()) {
                    isUnderAttack = true;
                    break;
                }
            }
            if (!isUnderAttack) {
                infoQ.add(Cast.getMessage(Cast.InformationCategory.SURRENDER, HQLocation));
            }
        }
        if (!completeTeraform) {
            // check all the surrounding locations and check if they're either infinite water tiles or 8 and higher
            System.out.println("teraform is not complete");
            completeTeraform = true;
            for (MapLocation loc: completeLoc) {
                if (rc.canSenseLocation(loc)) {
                    int height = rc.senseElevation(loc);
                    if (height >= -10000 && height < 5) {
                        completeTeraform = false;
                        break;
                    }
                }
            }
            if (completeTeraform) {
                // signal that teraform is complete
                System.out.println("TERAFORM COMPLETE!!!!");
                infoQ.add(getMessage(Cast.InformationCategory.TERAFORM_COMPLETE, HQLocation));
            }
        }
        // build all the miners we can get in the first few turns
        // maximum of 10 miners at 250th round
        // TODO: fix this
        Direction optDir = Direction.NORTH;
        if (minerCount < Math.min(5+rc.getRoundNum()/125, 7)) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
    }
}