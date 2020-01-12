package bot01;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Turtle {

    // 0: patrolling and rushing enemyHQ with disruptWithCow
    // 1: building outer wall
    // 2: building inner wall
    private int landScaperState;
    private MapLocation HQLocation;
    private Vector[] patrolLoc;
    private Vector[] outerLoc;
    private Vector[] innerLoc;

    // initialize landscaper state
    public Turtle(RobotController rc, MapLocation HQLocation) {
        this.HQLocation = HQLocation;
        boolean isVaporator = false;
        int netGunCount = 0;
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r: robots) {
            if (r.getType() == RobotType.VAPORATOR && r.getTeam() == rc.getTeam()) {
                isVaporator = true;
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() == rc.getTeam()) {
                netGunCount++;
            }
        }
        if (netGunCount >= 4) {
            landScaperState = 2;
        }
        else if (isVaporator) {
            landScaperState = 1;
        }
        else {
            landScaperState = 0;
        }
        patrolLoc = new Vector[]{new Vector(-1, 2), new Vector(1, -2), new Vector(-2, 1), new Vector(2, -1), new Vector(1, 2)};
        outerLoc = new Vector[]{new Vector(-1, 3), new Vector(-2, 3), new Vector(-3, 3), new Vector(-3, 2), new Vector(-3, 1), new Vector(-3, 0), new Vector(-3, -1), new Vector(-3, -2), new Vector(-3, -3),
                new Vector(-2, -3), new Vector(-1, -3), new Vector(1, 3), new Vector(2, 3), new Vector(3, 3), new Vector(3, 2), new Vector(3, 1), new Vector(3, 0), new Vector(3, -1), new Vector(3, -2),
                new Vector(3, -3), new Vector(2, -3), new Vector(1, -3), new Vector(0, -3)};
        innerLoc = new Vector[]{new Vector(-1, 2), new Vector(-2, 1), new Vector(-2, 0), new Vector(-2, -1), new Vector(-1, -2), new Vector(1, 2), new Vector(2, 1), new Vector(2, 0), new Vector(2, -1), new Vector(1, -2), new Vector(0, -2)};
    }

    public int getLandscaperState() {
        return landScaperState;
    }

    public void buildFort(RobotController rc) {
        if (landScaperState == 0) return;
        if (landScaperState == 1) buildOuterFort(rc);
        if (landScaperState == 2) buildInnerFort(rc);
    }

    private void buildOuterFort(RobotController rc) {
        int index = positionOut(rc.getLocation());
        if (index == -1) return;
        if (index <= 11) {
            if (index != 1) {

            }
        } else {
            // right side
        }
    }

    private void buildInnerFort(RobotController rc) {
        int index = positionIn(rc.getLocation());
        if (index == -1) return;
        if (index <= 5) {
            // left side
        } else {
            // right side
        }
    }

    public int positionOut(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < outerLoc.length; i++) {
            if (outerLoc[i].equals(vec)) return i;
        }
        landScaperState = 0;
        return -1;
    }

    public int positionIn(MapLocation loc) {
        Vector vec = Vector.vectorSubtract(loc, HQLocation);
        for (int i = 0; i < innerLoc.length; i++) {
            if (innerLoc[i].equals(vec)) return i;
        }
        landScaperState = 0;
        return -1;
    }
}
