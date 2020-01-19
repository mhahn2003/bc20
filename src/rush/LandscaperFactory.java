package rush;

import battlecode.common.*;

import static rush.Cast.*;

public class LandscaperFactory extends Building {
    public LandscaperFactory(RobotController r) {
        super(r);

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (enemyHQLocation != null && rc.getLocation().distanceSquaredTo(enemyHQLocation) < 18) {
            // attacking factory

            // get count of how many landscapers and net guns we already have
            boolean netGunPlaced = false;
            int landscapers = 0;
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo r: robots) {
                if (r.getType() == RobotType.LANDSCAPER && r.getTeam() == rc.getTeam()) landscapers++;
                if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) netGunPlaced = true;
            }
            Direction optDir = rc.getLocation().directionTo(enemyHQLocation);
            Direction left = optDir.rotateLeft();
            Direction right = optDir.rotateRight();
            Direction leftLeft = left.rotateLeft();
            Direction rightRight = right.rotateRight();
            if (netGunPlaced) {
                tryPlace(optDir);
                tryPlace(left);
                tryPlace(right);
                tryPlace(leftLeft);
                tryPlace(rightRight);
            }
            else if (landscapers < 2) {
                tryPlace(optDir);
                tryPlace(left);
                tryPlace(right);
                tryPlace(leftLeft);
                tryPlace(rightRight);
            }

        } else {
            if (factoryLocation == null) {
                factoryLocation = rc.getLocation();
                infoQ.add(Cast.getMessage(Cast.InformationCategory.FACTORY, factoryLocation));
            }
            // spawning 5 turtle landscapers with a bit of leeway for refineries
            if (landscaperCount < 5 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 150 + rushCost) {
                Direction optDir = rc.getLocation().directionTo(HQLocation);
                if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    landscaperCount++;
                }
            }
            // spawning the other 3 to complete the turtle as soon as we have enough money
            if (landscaperCount < 8 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 75) {
                Direction optDir = rc.getLocation().directionTo(HQLocation);
                if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    landscaperCount++;
                }
            }
            // spawn the teraforming landscapers
            if (landscaperCount < 12 && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 300) {
                Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                for (int i = 0; i < 8; i++) {
                    MapLocation loc = rc.getLocation().add(optDir);
                    if (loc.x % 3 == HQLocation.x % 3 && loc.y % 3 == HQLocation.y % 3) {
                        optDir = optDir.rotateRight();
                        continue;
                    }
                    if (rc.getLocation().directionTo(HQLocation).equals(optDir)) {
                        optDir = optDir.rotateRight();
                        continue;
                    }
                    if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                        rc.buildRobot(RobotType.LANDSCAPER, optDir);
                        landscaperCount++;
                        break;
                    }
                    optDir = optDir.rotateRight();
                }
            }
            if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost + 300) {
                Direction optDir = rc.getLocation().directionTo(HQLocation);
                if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    landscaperCount++;
                }
            }
            if (landscaperCount < 25 && rc.getTeamSoup() >= 900) {
                Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                for (int i = 0; i < 3; i++) {
                    MapLocation loc = rc.getLocation().add(optDir);
                    if (loc.x % 3 == HQLocation.x % 3 && loc.y % 3 == HQLocation.y % 3) {
                        optDir = optDir.rotateRight();
                        continue;
                    }
                    if (rc.getLocation().directionTo(HQLocation).equals(optDir)) {
                        optDir = optDir.rotateRight();
                        continue;
                    }
                    if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                        rc.buildRobot(RobotType.LANDSCAPER, optDir);
                        landscaperCount++;
                        break;
                    }
                    optDir = optDir.rotateRight();
                }
            }
        }
    }

    public void tryPlace(Direction dir) throws GameActionException {
        if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) rc.buildRobot(RobotType.LANDSCAPER, dir);
    }
}
