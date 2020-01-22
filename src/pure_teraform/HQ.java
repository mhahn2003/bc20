package pure_teraform;

import battlecode.common.*;

import static pure_teraform.Cast.getMessage;
import static pure_teraform.Cast.infoQ;
import static pure_teraform.Util.directions;

public class HQ extends Shooter {
    static int numMiners = 0;

    public HQ(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // find drones and shoot them
//        System.out.println("enemy hq might be at " + enemyHQLocationSuspect.toString());
//        if (enemyHQLocation!=null){
//        System.out.println("enemy hq is at" + enemyHQLocation.toString());
//        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        boolean isVaporator = false;
        int netGunCount = 0;
        for (RobotInfo r : robots) {
            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
                isVaporator = true;
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) {
                netGunCount++;
            }
        }
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
        // build all the miners we can get in the first few turns
        // maximum of 10 miners at 250th round
        // TODO: fix this
        Direction optDir = Direction.NORTH;
        if (minerCount < Math.min(3+rc.getRoundNum()/100, 7) && (minerCount < 3 || rc.getTeamSoup() >= RobotType.REFINERY.cost + RobotType.MINER.cost) && !isVaporator) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
    }
}