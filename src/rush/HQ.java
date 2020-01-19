package rush;

import battlecode.common.*;

import static rush.Cast.*;
import static rush.Util.directions;

public class HQ extends Shooter {
    static int numMiners = 0;

    public HQ(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // if HQ is surrounded by complete turtle then send turtle signal
        boolean oneSpace = false;
        int space;
        if ((HQLocation.x == 0 || HQLocation.x == rc.getMapWidth()-1) && (HQLocation.y == 0 || HQLocation.y == rc.getMapHeight()-1)) space = 3;
        else if ((HQLocation.x == 0 || HQLocation.x == rc.getMapWidth()-1) && (HQLocation.y == 1 || HQLocation.y == rc.getMapHeight()-2)) space = 4;
        else if ((HQLocation.x == 1 || HQLocation.x == rc.getMapWidth()-2) && (HQLocation.y == 0 || HQLocation.y == rc.getMapHeight()-1)) space = 4;
        else if ((HQLocation.x == 1 || HQLocation.x == rc.getMapWidth()-2) && (HQLocation.y == 1 || HQLocation.y == rc.getMapHeight()-2)) space = 5;
        else if (HQLocation.x == 0 || HQLocation.x == rc.getMapWidth()-1 || HQLocation.y == 0 || HQLocation.y == rc.getMapHeight()-1) space = 5;
        else if (HQLocation.x == 1 || HQLocation.x == rc.getMapWidth()-2 || HQLocation.y == 1 || HQLocation.y == rc.getMapHeight()-2) space = 7;
        else space = 8;
        System.out.println("space is: " + space);
        if (!isTurtle) {
            System.out.println("Checking turtle");
            int landscapers = 0;
            for (Direction dir : directions) {
                MapLocation loc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo rob = rc.senseRobotAtLocation(loc);
                    if (rob != null && rob.getType() == RobotType.LANDSCAPER && rob.getTeam() == rc.getTeam()) {
                        landscapers++;
                    }
                }
            }
            System.out.println("I have landscapers: " + landscapers);
            if (landscapers == space) {
                // broadcast turtle
                System.out.println("turtle!!!!!!!!!!");
                infoQ.add(getMessage(Cast.InformationCategory.TURTLE, HQLocation));
                isTurtle = true;
            }
            if (landscapers == space-1) oneSpace = true;
        }

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
        // TODO: spawn appropriate number of miners according to length of soupLoc
        Direction optDir = Direction.NORTH;
        if (!oneSpace && !isTurtle && minerCount < Math.min(4+rc.getRoundNum()/100, 7) && (minerCount < 4 || rc.getTeamSoup() >= RobotType.REFINERY.cost + RobotType.MINER.cost) && !isVaporator) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
    }
}