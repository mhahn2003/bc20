package rushdef;

import battlecode.common.*;

import static rushdef.Cast.*;
import static rushdef.Util.directions;

public class HQ extends Shooter {
    static int numMiners = 0;
    static int plusMiners = 0;

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
        int landscapers = 0;
        System.out.println("space is: " + space);
        if (!isTurtle) {
            System.out.println("Checking turtle");
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
                infoQ.add(getMessage(InformationCategory.TURTLE, HQLocation));
                isTurtle = true;
            }
            if (landscapers == space-1) oneSpace = true;
        }
        if (isTurtle) {
//            System.out.println("Checking turtle");
            for (Direction dir : directions) {
                MapLocation loc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo rob = rc.senseRobotAtLocation(loc);
                    if (rob != null && rob.getType() == RobotType.LANDSCAPER && rob.getTeam() == rc.getTeam()) {
                        landscapers++;
                    }
                }
            }
//            System.out.println("I have landscapers: " + landscapers);
            if (landscapers < space) {
                // broadcast turtle
                System.out.println("turtle broken!!!!!!!!!!");
                infoQ.add(getMessage(InformationCategory.TURTLE, HQLocation));
                isTurtle = false;
            }
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
                if ((r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.MINER || (r.getType() == RobotType.DELIVERY_DRONE && r.currentlyHoldingUnit)) && r.getTeam() != rc.getTeam()) {
                    infoQ.add(Cast.getMessage(InformationCategory.DEFENSE, HQLocation));
                    isUnderAttack = true;
                    break;
                }
            }
        } else {
            isUnderAttack = false;
            for (RobotInfo r : robots) {
                if ((r.getType() == RobotType.LANDSCAPER || r.getType() == RobotType.MINER || (r.getType() == RobotType.DELIVERY_DRONE && r.currentlyHoldingUnit)) && r.getTeam() != rc.getTeam()) {
                    isUnderAttack = true;
                    break;
                }
            }
            if (!isUnderAttack) {
                infoQ.add(Cast.getMessage(InformationCategory.SURRENDER, HQLocation));
            }
        }
        if (rc.getMapHeight() > 47 && rc.getMapWidth() > 47) plusMiners = 2;
        else if (rc.getMapHeight() > 36 && rc.getMapWidth() > 36) plusMiners = 1;
        else plusMiners = 0;
        // build all the miners we can get in the first few turns
        MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        Direction optDir = rc.getLocation().directionTo(center);
        if (minerCount < 4) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
        if (minerCount < 4 + plusMiners && rc.getRoundNum() > 150) {
            for (int i = 0; i < 8; i++) {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
                    rc.buildRobot(RobotType.MINER, optDir);
                    minerCount++;
                } else optDir = optDir.rotateLeft();
            }
        }
//        optDir = rc.getLocation().directionTo(center);
//        if (minerCount < 8 && !oneSpace && !isTurtle && space-landscapers < 5 && rc.getRoundNum() > 250) {
//            for (int i = 0; i < 8; i++) {
//                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, optDir)) {
//                    rc.buildRobot(RobotType.MINER, optDir);
//                    minerCount++;
//                } else optDir = optDir.rotateLeft();
//            }
//        }
        // attack
        if (rc.getRoundNum() == 2300) {
            infoQ.add(getMessage(InformationCategory.PREPARE, HQLocation));
        }
        if (phase == RobotPlayer.actionPhase.PREPARE && rc.getRoundNum() == 2400) {
            infoQ.add(getMessage(InformationCategory.ATTACK, HQLocation));
        }
        if (rc.getRoundNum() == 2450) {
            infoQ.add(getMessage(InformationCategory.SURRENDER, HQLocation));
        }
    }
}