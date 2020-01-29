package rushdef;

import battlecode.common.*;

import static rushdef.Cast.*;
import static rushdef.Util.*;

public class DroneFactory extends Building {
    public DroneFactory(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
//        System.out.println("helpLoc length is: " + helpLoc.size());
        // produce 5 drones
        int minElevation = rc.senseElevation(rc.getLocation());
        for (Direction dir: directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canSenseLocation(loc)) {
                minElevation = Math.min(minElevation, rc.senseElevation(loc));
            }
        }
        boolean netGun = false;
        MapLocation netGunLoc = null;
        RobotInfo[] netGuns = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo net: netGuns) {
            if (net.getType() == RobotType.NET_GUN) {
                netGun = true;
                netGunLoc = net.getLocation();
            }
        }
        minElevation = Math.max(3, minElevation);
        if (rc.getRoundNum() > Util.floodRound(minElevation)-60 && droneCount < 27 && isTurtle) {
            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (netGun && netGunLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    optDir = optDir.rotateRight();
                    continue;
                }
                if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    break;
                }
                optDir = optDir.rotateRight();
            }
        }

        Direction optDir = Direction.NORTHEAST;
        if (droneCount < 1) {
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (netGun && netGunLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    optDir = optDir.rotateLeft();
                    continue;
                }
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    if (droneCount == 1) {
                        infoQ.add(getMessage(InformationCategory.DRONE_SPAWN, HQLocation));
                        areDrones = true;
                    }
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }
        optDir = Direction.NORTHEAST;
        if (droneCount < 2 && rc.getRoundNum() > 135) {
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (netGun && netGunLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    optDir = optDir.rotateLeft();
                    continue;
                }
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    if (droneCount == 1) {
                        infoQ.add(getMessage(InformationCategory.DRONE_SPAWN, HQLocation));
                        areDrones = true;
                    }
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }
        optDir = Direction.NORTHEAST;
        if (droneCount < 5 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 150+rushCost/2 && isTurtle) {
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (netGun && netGunLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    optDir = optDir.rotateLeft();
                    continue;
                }
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) && rc.getTeamSoup() > 400){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    if (droneCount == 1) {
                        infoQ.add(getMessage(InformationCategory.DRONE_SPAWN, HQLocation));
                        areDrones = true;
                    }
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }
        optDir = Direction.NORTHEAST;
        if (droneCount < 8 && rc.getTeamSoup() >= RobotType.REFINERY.cost+RobotType.DELIVERY_DRONE.cost+rushCost/2 && helpLoc.size() >= 5 && isTurtle) {
            for (int i = 0; i < 2; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (netGun && netGunLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    optDir = optDir.rotateLeft();
                    continue;
                }
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir) && rc.getTeamSoup() > 350){
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }

        // spam drones
        if (vaporatorCount > 0 && rc.getTeamSoup() >= 600 && isTurtle) {
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (netGun && netGunLoc.distanceSquaredTo(loc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
                    optDir = optDir.rotateLeft();
                    continue;
                }
                if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
                    droneCount++;
                    if (droneCount == maxDroneCount-attackLandscaperCount) {
                        infoQ.add(Cast.getMessage(InformationCategory.PREPARE, HQLocation));
                    }
                    if (droneCount == maxDroneCount) {
                        infoQ.add(Cast.getMessage(InformationCategory.ATTACK, HQLocation));
                    }
                    break;
                } else {
                    optDir = optDir.rotateLeft();
                }
            }
        }


//        boolean isVaporator = false;
//        RobotInfo[] robots = rc.senseNearbyRobots();
//        for (RobotInfo r: robots) {
//            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
//                isVaporator = true;
//            }
//        }
//        // spam drones if we have a ton of soup
//        if (rc.getTeamSoup() >= 1000 && isVaporator) {
//            optDir = Direction.NORTH;
//            if (rc.isReady() && rc.canBuildRobot(RobotType.DELIVERY_DRONE, optDir)) {
//                rc.buildRobot(RobotType.DELIVERY_DRONE, optDir);
//                droneCount++;
//            }
//            // if drones are almost max then prepare attack
//            if (droneCount == maxDroneCount-attackLandscaperCount) {
//                infoQ.add(Cast.getMessage(Cast.InformationCategory.PREPARE, HQLocation));
//            }
//            if (droneCount == maxDroneCount) {
//                infoQ.add(Cast.getMessage(Cast.InformationCategory.ATTACK, HQLocation));
//            }
//        }
    }
}
