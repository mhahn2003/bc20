package rngform;
import battlecode.common.*;

import static rngform.Cast.getMessage;
import static rngform.Cast.infoQ;
import static rngform.Util.directions;
import static rngform.Util.refineryDist;

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
            // build landscaper factory

            if (soupLoc == null) {
                // if soup location is far or we just didn't notice
                findSoup();
            }

            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || (soupLoc == null && rc.getSoupCarrying() > 0)) {
                // TODO: remove refinery location if enemy destroys or is flooded

                // check select a point as reference(might be edge case?)
                MapLocation referencePoint = soupLoc;
                if (referencePoint != null) {
                    // System.out.println("reference to " + reference_point.toString());
                    int minRefineryDist = referencePoint.distanceSquaredTo(closestRefineryLocation );

                    // System.out.println("before find min d i have" + Clock.getBytecodesLeft());
                    // check through refinery(redundancy here)
                    for (MapLocation refineryLoca : refineryLocation) {
                        int temp_d = referencePoint.distanceSquaredTo(refineryLoca);
                        if (temp_d < minRefineryDist) {
                            closestRefineryLocation = refineryLoca;
                            minRefineryDist = temp_d;
                        }
                    }
                    // System.out.println("reference to " + referencePoint.toString());
                    // System.out.println("compare to " + closestRefineryLocation.toString());
                    // System.out.println("after find min d i have " + Clock.getBytecodesLeft());
                    // System.out.println("reference min distance to refinery " + minRefineryDist);
                    // System.out.println("reference min distance to bot " + referencePoint.distanceSquaredTo(rc.getLocation()));
                    if (minRefineryDist >= refineryDist && referencePoint.distanceSquaredTo(rc.getLocation()) < 13 && rc.getTeamSoup() >= 200) {
                        // System.out.println("attempt build refinery");
                        Direction optDir = rc.getLocation().directionTo(referencePoint);
                        for (int i = 0; i < 8; i++) {
                            MapLocation robotLoc = rc.getLocation();
                            MapLocation placeLoc = robotLoc.add(optDir);
                            if (rc.canBuildRobot(RobotType.REFINERY, optDir)) {
                                //                            System.out.println("can build refinery");
                                rc.buildRobot(RobotType.REFINERY, optDir);
                                //                            System.out.println("built refinery");
                                refineryLocation.add(placeLoc);
                                closestRefineryLocation = refineryLocation.get(refineryLocation.size() - 1);
                                break;
                            } else {
                                optDir = optDir.rotateRight();
                            }
                        }
                        infoQ.add(Cast.getMessage(Cast.InformationCategory.NEW_REFINERY, refineryLocation.get(refineryLocation.size() - 1)));
                    }
                    // System.out.println("after new refinery procedures" + Clock.getBytecodesLeft());
                }
                // if HQ is next to miner deposit
                if (closestRefineryLocation.isAdjacentTo(rc.getLocation())) {
                    Direction soupDepositDir = rc.getLocation().directionTo(closestRefineryLocation);
                    tryRefine(soupDepositDir);
                    nav.navReset(rc, rc.getLocation());
                } else {
                    if (nav.needHelp(rc, turnCount, closestRefineryLocation)) {
                        // just build a refinery?
                        if (rc.getTeamSoup() >= RobotType.REFINERY.cost) {
                            Direction optDir = rc.getLocation().directionTo(referencePoint);
                            for (int i = 0; i < 8; i++) {
                                MapLocation robotLoc = rc.getLocation();
                                MapLocation placeLoc = robotLoc.add(optDir);
                                if (placeLoc.x % 3 == HQLocation.x && placeLoc.y % 3 == HQLocation.y) {
                                    optDir = optDir.rotateRight();
                                    continue;
                                }
                                if (rc.canBuildRobot(RobotType.REFINERY, optDir)) {
                                    //                            System.out.println("can build refinery");
                                    rc.buildRobot(RobotType.REFINERY, optDir);
                                    //                            System.out.println("built refinery");
                                    refineryLocation.add(placeLoc);
                                    closestRefineryLocation = refineryLocation.get(refineryLocation.size() - 1);
                                    return;
                                } else {
                                    optDir = optDir.rotateRight();
                                }
                            }
                        }
                        helpMode = 1;
                        System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        infoQ.add(Cast.getMessage(rc.getLocation(), closestRefineryLocation));
                    }
                    else nav.bugNav(rc, closestRefineryLocation);
                }
            } else {
                System.out.println("I'm looking for soup");
                // try to mine soup in all directions because miner finding soup can be slightly buggy
                boolean canMine = false;
                MapLocation op = rc.getLocation().add(rc.getLocation().directionTo(HQLocation).opposite());
                if (rc.canMineSoup(Direction.CENTER)) {
                    System.out.println("I can mine " + Direction.CENTER.toString());
                    if (rc.getLocation().isAdjacentTo(HQLocation)) {
                        System.out.println("I'm bugnaving!");
                        nav.bugNav(rc, op);
                    } else {
                        rc.mineSoup(Direction.CENTER);
                    }
                    canMine = true;
                }
                for (Direction d: directions) {
                    if (rc.canMineSoup(d)) {
                        if (rc.getLocation().isAdjacentTo(HQLocation)) {
                            System.out.println("I'm bugnaving!");
                            nav.bugNav(rc, op);
                        } else {
                            System.out.println("I can mine " + d.toString());
                            rc.mineSoup(d);
                        }
                        canMine = true;
                        break;
                    }
                }
                if (soupLoc != null) {

                    System.out.println("Soup is at: " + soupLoc.toString());
                    if (canMine) {
                        System.out.println("I mined soup!");
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
                        else {
                            System.out.println("I'm going to soup");
                            System.out.println("Soup is at: " + soupLoc.toString());
                            nav.bugNav(rc, soupLoc);
                        }
                    }
                } else {
                    System.out.println("Going to enemyHQ!");
                    // check if getting close to flooded
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
        System.out.println("Before calling I have: " + Clock.getBytecodesLeft());
        MapLocation[] soups = rc.senseNearbySoup();
        for (MapLocation check: soups) {
            int checkDist = check.distanceSquaredTo(rc.getLocation());
            if (soupLoc == null || checkDist < soupLoc.distanceSquaredTo(rc.getLocation())
                    || (checkDist == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                soupLoc = check;
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
        System.out.println("After calling I have: " + Clock.getBytecodesLeft());
    }
}
