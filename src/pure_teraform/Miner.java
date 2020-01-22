package pure_teraform;
import battlecode.common.*;

import static pure_teraform.Cast.getMessage;
import static pure_teraform.Cast.infoQ;
import static pure_teraform.Util.directions;
import static pure_teraform.Util.refineryDist;

public class Miner extends Unit {

    private static boolean isLF = false;
    private static boolean isDF = false;

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
            // build drone factory
            if (!isDF && isBuilder) {
                if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
                    for (Direction dir: directions) {
                        MapLocation loc = rc.getLocation().add(dir);
                        if (loc.isAdjacentTo(HQLocation)) {
                            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
                                rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                                isDF = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!isLF && isBuilder) {
                if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                    for (Direction dir: directions) {
                        MapLocation loc = rc.getLocation().add(dir);
                        if (loc.isAdjacentTo(HQLocation)) {
                            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                                rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                                isLF = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!completeTeraform && isBuilder) {
                if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                    MapLocation closestVap = null;
                    int dist = 0;
                    int curHeight = rc.senseElevation(rc.getLocation());
                    for (MapLocation loc: vapInsideLoc) {
                        if (rc.canSenseLocation(loc) && Math.abs(rc.senseElevation(loc)-curHeight) <= 3) {
                            RobotInfo r = rc.senseRobotAtLocation(loc);
                            if (r == null) {
                                int tempD = rc.getLocation().distanceSquaredTo(loc);
                                if (closestVap == null || tempD < dist) {
                                    closestVap = loc;
                                    dist = tempD;
                                }
                            }
                        }
                    }
                    if (closestVap != null) {
                        if (rc.getLocation().isAdjacentTo(closestVap)) {
                            Direction buildDir = rc.getLocation().directionTo(closestVap);
                            if (rc.canBuildRobot(RobotType.VAPORATOR, buildDir)) rc.buildRobot(RobotType.VAPORATOR, buildDir);
                        } else {
                            // navigate to that spot
                            nav.bugNav(rc, closestVap);
                        }
                    } else {
                        if (nav.needHelp(rc, turnCount, HQLocation)) {
                            helpMode = 1;
                            System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            infoQ.add(Cast.getMessage(rc.getLocation(), HQLocation));
                        }
                        else nav.bugNav(rc, HQLocation);
                    }
                }
            }
            if (completeTeraform) {
                // then build vaporators randomly i guess
                MapLocation closestVap = null;
                int dist = 0;
                int maxV = 3;
                for (int i = -maxV; i <= maxV; i++) {
                    for (int j = -maxV; j <= maxV; j++) {
                        MapLocation loc = rc.getLocation().translate(i, j);
                        if (rc.canSenseLocation(loc) && rc.senseElevation(loc) == 8 && loc.x % 2 != HQLocation.x % 2 && loc.y % 2 != HQLocation.y % 2) {
                            RobotInfo r = rc.senseRobotAtLocation(loc);
                            if (r == null) {
                                int tempD = rc.getLocation().distanceSquaredTo(loc);
                                if (closestVap == null || tempD < dist) {
                                    closestVap = loc;
                                    dist = tempD;
                                }
                            }
                        }
                    }
                }
                if (closestVap != null) {
                    if (rc.getLocation().isAdjacentTo(closestVap)) {
                        Direction buildDir = rc.getLocation().directionTo(closestVap);
                        if (rc.canBuildRobot(RobotType.VAPORATOR, buildDir)) rc.buildRobot(RobotType.VAPORATOR, buildDir);
                    } else {
                        // navigate to that spot
                        if (nav.needHelp(rc, turnCount, closestVap)) {
                            helpMode = 1;
                            System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            infoQ.add(Cast.getMessage(rc.getLocation(), closestVap));
                        }
                        else nav.bugNav(rc, closestVap);
                        nav.bugNav(rc, closestVap);
                    }
                } else {
                    // go to enemy location suspect and spread out?
                    nav.bugNav(rc, enemyHQLocationSuspect);
                }
            }
//            if (factoryLocation == null && isBuilder) {
//                if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
//                    for (Direction dir: directions) {
//                        MapLocation loc = rc.getLocation().add(dir);
//                        if (loc.x % 2 != HQLocation.x % 2 && loc.y % 2 == HQLocation.y % 2) continue;
//                        if (loc.isAdjacentTo(HQLocation)) {
//                            if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
//                                rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
//                                factoryLocation = rc.getLocation().add(dir);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//            // TODO: if buildings are destroyed then rebuild
//            // build refinery
//            if (refineryLocation.isEmpty()) {
//                System.out.println("refinery loc is empty!");
//            } else {
//                System.out.println("refinery loc is: " + refineryLocation.toString());
//            }
//
//            // build drone factory
//            if (droneFactoryLocation == null && isBuilder ) {
//                if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost+60) {
//                    for (Direction dir: directions) {
//                        MapLocation loc = rc.getLocation().add(dir);
//                        if (loc.distanceSquaredTo(HQLocation) > 20) {
//                            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
//                                rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
//                                droneFactoryLocation = rc.getLocation().add(dir);
//                                break;
//                            }
//                        }
//                    }
//                }
//            }

            if (soupLoc == null) {
                // if soup location is far or we just didn't notice
                //            System.out.println("In my soupLoc I have " + soupLocation.toString());
                findSoup();
            }
            //        System.out.println("After finding soup, I have " + Clock.getBytecodesLeft());
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit || (soupLoc == null && rc.getSoupCarrying() > 0)) {
                // if the robot is full or has stuff and no more soup nearby, move back to HQ
                // TODO: remove refinery location if enemy destroys or is flooded

                // check select a point as reference(might be edge case?)
                if (closestRefineryLocation==null){
                    closestRefineryLocation=HQLocation;
                }
                
                MapLocation referencePoint = soupLoc;
                if (referencePoint != null) {
                    int minRefineryDist = referencePoint.distanceSquaredTo(closestRefineryLocation);
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
                        // just build a refinery?
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
