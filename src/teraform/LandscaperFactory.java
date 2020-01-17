package teraform;

import battlecode.common.*;

import static teraform.Cast.infoQ;

public class LandscaperFactory extends Building {
    public LandscaperFactory(RobotController r) {
        super(r);

    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if (factoryLocation == null) {
            factoryLocation = rc.getLocation();
            infoQ.add(Cast.getMessage(Cast.InformationCategory.FACTORY, factoryLocation));
        }
        if (rc.getTeamSoup() >= RobotType.LANDSCAPER.cost+100) {
            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
            for (int i = 0; i < 8; i++) {
                MapLocation loc = rc.getLocation().add(optDir);
                if (loc.x % 3 == HQLocation.x % 3 && loc.y % 3 == HQLocation.y % 3) continue;
                if (rc.canBuildRobot(RobotType.LANDSCAPER, optDir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, optDir);
                    break;
                }
                optDir = optDir.rotateRight();
            }
        }
    }
}
