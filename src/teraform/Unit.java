package teraform;
import battlecode.common.*;

public class Unit extends Robot {

    Nav nav;

    MapLocation hqLoc;

    public Unit(RobotController r) {
        super(r);
        nav = new Nav();
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
    }
}