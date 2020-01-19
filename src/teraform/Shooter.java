package teraform;
import battlecode.common.*;

public class Shooter extends Building {

    public Shooter(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        // TODO: prioritize enemy drones holding units / closer
        // shoot nearby enemies
        Team enemy = rc.getTeam().opponent();
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, enemy);

        for (RobotInfo e : enemiesInRange) {
            if (e.type == RobotType.DELIVERY_DRONE) {
                if (rc.canShootUnit(e.ID)){
                    rc.shootUnit(e.ID);
                    break;
                }
            }
        }
    }
}