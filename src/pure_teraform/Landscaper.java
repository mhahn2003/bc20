package pure_teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static pure_teraform.Cast.getMessage;
import static pure_teraform.Cast.infoQ;
import static pure_teraform.Util.directions;

public class Landscaper extends Unit {

    private ArrayList<MapLocation> visitedHole;
    private Vector[] untouchable;
    private MapLocation[] untouchableLoc;
    private int untouchSize = 12;
    private Direction fill;
    private Direction digLoc;


    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        visitedHole = new ArrayList<>();

    }

    public void initialize() throws GameActionException {
        super.initialize();

    }

    public void takeTurn() throws GameActionException {

    }

    public Direction holeTo() throws GameActionException {
        int modX = HQLocation.x % 2;
        int modY = HQLocation.y % 2;
        for (Direction dir: directions) {
            MapLocation dig = rc.getLocation().add(dir);
            if (dig.x % 2 == modX && dig.y % 2 == modY && ! rc.senseFlooding(dig) && surroundedLand(dig)) {
                return dir;
            }
        }
        // this shouldn't happen
        return Direction.CENTER;
    }

    // returns the optimal height of a location. Adds 2 to the height if near water.
    public int optHeight(MapLocation loc) throws GameActionException {
        return 9;
//        int distFromFactory = loc.distanceSquaredTo(factoryLocation);
//        return Math.min(8, (int) (Math.floor(Math.sqrt(distFromFactory)*2)) + factoryHeight);
    }

    public void checkFillAndDig(Direction dig) throws GameActionException {
        for (Direction dir: directions) {
            MapLocation fill = rc.getLocation().add(dir);
            if (dig.equals(dir) || isHole(fill) && ! rc.senseFlooding(fill)) continue;

            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    continue;
                }
            }

            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (rc.senseElevation(fill) > -30 && 
                    rc.senseElevation(fill) < optHeight(fill) && 
                    (rob == null || rob.getTeam() == rc.getTeam() && !(rob.getType().isBuilding() )) ) {
                    this.fill = dir;
                    return;
                }
                if ((rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) || 
                    (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying < 25)) {
                    this.digLoc = dir;
                    return;
                }
            }
        }
    }

    // scans teraformLoc and checks
    public MapLocation closestHole() throws GameActionException {
        if (teraformLoc[0] == null) return null;
        MapLocation closest = null;
        int heuristic = 0;
        for (MapLocation hole: teraformLoc) {
            if (visitedHole.contains(hole)) continue;
            int holeH = rc.getLocation().distanceSquaredTo(hole)+HQLocation.distanceSquaredTo(hole);
            if (closest == null || holeH < heuristic) {
                closest = hole;
                heuristic = holeH;
            }
        }
        return closest;
    }

    // because we reduced everything (except deep tiles or super tall tiles) we should be able to move pretty freely
    public void moveTo(MapLocation loc) throws GameActionException {
        Direction optDir = rc.getLocation().directionTo(loc);
        Direction left = optDir.rotateLeft();
        Direction right = optDir.rotateRight();
        Direction leftLeft = left.rotateLeft();
        Direction rightRight = right.rotateRight();
        Direction leftLeftLeft = leftLeft.rotateLeft();
        Direction rightRightRight = rightRight.rotateRight();
        Direction op = optDir.opposite();
        if (canMove(optDir)) rc.move(optDir);
        else if (canMove(left)) rc.move(left);
        else if (canMove(right)) rc.move(right);
        else if (canMove(leftLeft)) rc.move(leftLeft);
        else if (canMove(rightRight)) rc.move(rightRight);
        else if (canMove(leftLeftLeft)) rc.move(leftLeftLeft);
        else if (canMove(rightRightRight)) rc.move(rightRightRight);
        else if (canMove(op)) rc.move(op);
        else {
            // TODO: call for drone help
        }
    }

    public boolean canMove(Direction dir) throws GameActionException {
        Direction hole = holeTo();
        MapLocation loc = rc.getLocation().add(dir);
        for (int i = 0; i < untouchSize; i++) {
            if (untouchableLoc[i].equals(loc)|| loc.x%2 == HQLocation.x%2 && loc.y%2 == HQLocation.y%2) return false;
        }
        return !hole.equals(dir) && rc.canMove(dir) && rc.canSenseLocation(loc) && !rc.senseFlooding(loc);
    }

    public boolean fillMore(MapLocation hole) throws GameActionException {
        System.out.println("Before completing fillMore I have: " + Clock.getBytecodesLeft());
        for (Direction dir: directions) {
            MapLocation fill = hole.add(dir);
            boolean bad = false;
            for (int i = 0; i < untouchSize; i++) {
                if (fill.equals(untouchableLoc[i])) {
                    bad = true;
                    break;
                }
            }
            if (bad) continue;
            if (rc.canSenseLocation(fill)) {
                RobotInfo rob = rc.senseRobotAtLocation(fill);
                if (((rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < 40 && rc.senseElevation(fill) != optHeight(fill)) ||
                        (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) || (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0))
                && !(rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying == 0)) {
//                    if (rc.senseElevation(fill) > -30 && rc.senseElevation(fill) < optHeight(fill)) System.out.println("first");
//                    if (rc.senseElevation(fill) > optHeight(fill) && rc.senseElevation(fill) < 40) System.out.println("second");
//                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() != rc.getTeam()) System.out.println("third");
//                    if (rob != null && rob.getType().isBuilding() && rob.getTeam() == rc.getTeam() && rob.dirtCarrying > 0) System.out.println("fourth");
//                    System.out.println("Direction " + dir + " looks ok");
//                    System.out.println("optimal height is " + optHeight(fill));
//                    System.out.println("After completing fillMore I have: " + Clock.getBytecodesLeft());
                    return true;
                }
            }
        }
        System.out.println("After completing fillMore I have: " + Clock.getBytecodesLeft());
        return false;
    }

    public void sendHole(MapLocation closeHole) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(8, rc.getTeam());
        for (MapLocation loc: visitedHole) {
            if (loc.equals(closeHole)) return;
        }
        replaceOrAdd(closeHole);
        boolean alreadySent = false;
        for (RobotInfo rob: robots) {
            if (rob.getType() == RobotType.LANDSCAPER && rob.getLocation().isAdjacentTo(closeHole) && rob.getID() < rc.getID()) {
                alreadySent = true;
                break;
            }
        }
        if (alreadySent) return;
        System.out.println("Sending hole");
        infoQ.add(getMessage(Cast.InformationCategory.HOLE, closeHole));
    }

    public void replaceOrAdd(MapLocation loc) {
        if (visitedHole.size() >= 5) {
            visitedHole.remove(0);
        }
        visitedHole.add(loc);
    }
}