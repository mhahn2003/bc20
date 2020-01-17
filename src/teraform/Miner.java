package teraform;
import battlecode.common.*;

import static teraform.Cast.infoQ;
import static teraform.Util.directions;
import static teraform.Util.refineryDist;

public class Miner extends Unit {

    public Miner(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // check if it's in help mode and it moved so it can go free
        if (helpMode == 1) {
            if (nav.outOfDrone(rc)) helpMode = 0;
        }
        if (helpMode == 0) {
            // try not to suffocate HQ
            if (rc.getLocation().isAdjacentTo(HQLocation) && rc.getSoupCarrying() == 0) {
                nav.bugNav(rc, rc.getLocation().add(rc.getLocation().directionTo(HQLocation).opposite()));
            }
            // build landscaper factory
            MapLocation LFLoc = new Vector(2, 2).rotate(rotateState).addWith(HQLocation);
            if (factoryLocation == null && rc.getLocation().distanceSquaredTo(HQLocation) <= 18 && isBuilder) {
                if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                    for (Direction dir: directions) {
                        MapLocation loc = rc.getLocation().add(dir);
                        if (loc.equals(LFLoc)) {
                            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                                rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                                factoryLocation = rc.getLocation().add(dir);
                                break;
                            }
                        }
                    }
                    nav.bugNav(rc, LFLoc);
                }
            }
            // build drone factory
            if (droneFactoryLocation == null && rc.getLocation().distanceSquaredTo(HQLocation) < 18 && isBuilder) {
                if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
                    for (Direction dir: directions) {
                        MapLocation loc = rc.getLocation().add(dir);
                        if (loc.distanceSquaredTo(HQLocation) == 10) {
                            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
                                rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                                droneFactoryLocation = rc.getLocation().add(dir);
                                break;
                            }
                        }
                    }
                }
            }

            if (soupLoc == null) {
                // if soup location is far or we just didn't notice
                //            System.out.println("In my soupLoc I have " + soupLocation.toString());
                findSoup();
            }
            //        System.out.println("After finding soup, I have " + Clock.getBytecodesLeft());
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || (soupLoc == null && rc.getSoupCarrying() > 0)) {
                // if the robot is full or has stuff and no more soup nearby, move back to HQ
                //            System.out.println("before going home i have " + Clock.getBytecodesLeft());
                // default hq
                // TODO: remove hqlocation as a possible refinery if outer layer is complete
                // TODO: remove refinery location if enemy destroys or is flooded
                closestRefineryLocation = HQLocation;
                // check select a point as reference(might be edge case?)
                MapLocation referencePoint = soupLoc;
                if (referencePoint != null) {
                    //                System.out.println("reference to " + reference_point.toString());
                    int minRefineryDist = referencePoint.distanceSquaredTo(HQLocation);

                    //                System.out.println("before find min d i have" + Clock.getBytecodesLeft());
                    // check through refinery(redundancy here)
                    for (MapLocation refineryLoca : refineryLocation) {
                        int temp_d = referencePoint.distanceSquaredTo(refineryLoca);
                        if (temp_d < minRefineryDist) {
                            closestRefineryLocation = refineryLoca;
                            minRefineryDist = temp_d;
                        }
                        //                    System.out.println("my memory contain " + refineryLoca.toString());
                    }
//                                    System.out.println("reference to " + referencePoint.toString());
//                                    System.out.println("compare to " + closestRefineryLocation.toString());
//                                    System.out.println("after find min d i have " + Clock.getBytecodesLeft());
//                                    System.out.println("reference min distance to refinery " + minRefineryDist);
//                                    System.out.println("reference min distance to bot " + referencePoint.distanceSquaredTo(rc.getLocation()));
                    //
                    if (minRefineryDist >= refineryDist && referencePoint.distanceSquaredTo(rc.getLocation()) < 13 && rc.getTeamSoup() >= 200) {
                        //                    System.out.println("attempt build refinery");
                        Direction optDir = rc.getLocation().directionTo(referencePoint);
                        for (int i = 0; i < 8; i++) {
                            if (rc.canBuildRobot(RobotType.REFINERY, optDir)) {
                                //                            System.out.println("can build refinery");
                                rc.buildRobot(RobotType.REFINERY, optDir);
                                //                            System.out.println("built refinery");
                                MapLocation robotLoc = rc.getLocation();
                                refineryLocation.add(robotLoc.add(optDir));
                                closestRefineryLocation = refineryLocation.get(refineryLocation.size() - 1);
                                break;
                            } else {
                                optDir = optDir.rotateRight();
                            }
                        }
                        infoQ.add(Cast.getMessage(Cast.InformationCategory.NEW_REFINERY, refineryLocation.get(refineryLocation.size() - 1)));
                    }
                    //                System.out.println("after new refinery procedures" + Clock.getBytecodesLeft());
                }
                // if HQ is next to miner deposit
                if (closestRefineryLocation.isAdjacentTo(rc.getLocation())) {
                    Direction soupDepositDir = rc.getLocation().directionTo(closestRefineryLocation);
                    tryRefine(soupDepositDir);
                    nav.navReset(rc, rc.getLocation());
                } else {
                    if (nav.needHelp(rc, turnCount, closestRefineryLocation)) {
                        helpMode = 1;
                        System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        infoQ.add(bot01.Cast.getMessage(rc.getLocation(), closestRefineryLocation));
                    }
                    else nav.bugNav(rc, closestRefineryLocation);
                }
            } else {
                // try to mine soup in all directions because miner finding soup can be slightly buggy
                boolean canMine = false;
                Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
                if (rc.canMineSoup(Direction.CENTER)) {
                    rc.mineSoup(Direction.CENTER);
                    canMine = true;
                }
                for (Direction d: directions) {
                    if (rc.canMineSoup(d)) {
                        rc.mineSoup(d);
                        canMine = true;
                        break;
                    }
                }
                if (soupLoc != null) {
//                    System.out.println("Soup is at: " + soupLoc.toString());
                    if (canMine) {
                        // pollution might make miner skip this even though it's right next to soup
                        nav.navReset(rc, rc.getLocation());
                    }
                    // if we can't mine soup, go to other soups
                    else {
                        if (nav.needHelp(rc, turnCount, soupLoc)) {
                            helpMode = 1;
                            System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            infoQ.add(Cast.getMessage(rc.getLocation(), soupLoc));
                        }
                        else nav.bugNav(rc, soupLoc);
                    }
                } else {
                    // scout for soup
                    nav.searchEnemyHQ(rc);
                }
            }
        }
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    // TODO: reimplement this function
    // stores the closest MapLocation of soup in the robot's stored soup locations in soupLoc
    // but if within vision range, just normally find the closest soup
    static void findSoup() throws GameActionException {
        // try to find soup very close
        MapLocation robotLoc = rc.getLocation();
        int maxV = 5;
        for (int x = -maxV; x <= maxV; x++) {
            for (int y = -maxV; y <= maxV; y++) {
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    if (rc.senseSoup(check) > 0) {
                        // find the closest maxmimal soup deposit
                        int checkDist = check.distanceSquaredTo(rc.getLocation());
                        if (soupLoc == null || checkDist < soupLoc.distanceSquaredTo(rc.getLocation())
                                || (checkDist == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                            soupLoc = check;
                    }
                }
            }
        }
        if (soupLoc != null) return;
        // if not, try to find closest soup according to stored soupLocation
        int closestDist = 0;
        if (soupLocation.isEmpty()) return;
        for (MapLocation soup: soupLocation) {
            // find the closest soup
            int soupDist = soup.distanceSquaredTo(rc.getLocation());
            if (soupLoc == null || soupDist < closestDist) {
                closestDist = soupDist;
                soupLoc = soup;
            }
        }
    }
}
