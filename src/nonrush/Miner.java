package nonrush;

import battlecode.common.*;

import static nonrush.Cast.*;
import static nonrush.Util.directions;
import static nonrush.Util.refineryDist;

public class Miner extends Unit {

    static double expandVaporator = 14;
    static int vaporatorCount = 0;

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
            // if we can build vaporators now
            if (isVaporator) {
                expandVaporator += 0.1;
                for (Direction dir: directions) {
                    MapLocation loc = rc.getLocation().add(dir);
                    if (loc.x % 2 != HQLocation.x % 2 || loc.y % 2 != HQLocation.y % 2) continue;
                    if (rc.senseElevation(loc) >= 7) {
                        if (vaporatorCount == 0) if (rc.canBuildRobot(RobotType.VAPORATOR, dir)) {
                            rc.buildRobot(RobotType.VAPORATOR, dir);
                            vaporatorCount++;
                        } else {
                            // if you've already built a vaporator, then wait a bit until you build
                            if (rc.getTeamSoup() >= 700 && rc.canBuildRobot(RobotType.VAPORATOR, dir)) {
                                rc.buildRobot(RobotType.VAPORATOR, dir);
                                vaporatorCount++;
                            }
                        }
                    }
                }
                // if it can't build a vaporator
                if (rc.getLocation().distanceSquaredTo(HQLocation) >= expandVaporator) {
                    // if they're too far, move to factoryLocation
                    if (nav.needHelp(rc, turnCount, factoryLocation)) {
                        helpMode = 1;
                        System.out.println("Sending help!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        infoQ.add(Cast.getMessage(rc.getLocation(), factoryLocation));
                    } else {
                        nav.bugNav(rc, factoryLocation);
                    }
                } else {
                    // if not too far, try going out to enemyHQSuspect?
                    nav.bugNav(rc, enemyHQLocationSuspect);
                }
            } else {
                // build landscaper factory
                MapLocation LFLoc = new Vector(2, 2).rotate(rotateState).addWith(HQLocation);
                if (factoryLocation == null && isBuilder) {
                    System.out.println("rotateState is: " + rotateState);
                    System.out.println("LFLoc is: " + LFLoc.toString());
                    if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                        for (Direction dir : directions) {
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
                // TODO: if buildings are destroyed then rebuild
                // build refinery
                if (refineryLocation.isEmpty()) {
                    if (rc.getRoundNum() > 250 && rc.getTeamSoup() >= RobotType.REFINERY.cost) {
                        for (Direction dir : directions) {
                            MapLocation loc = rc.getLocation().add(dir);
                            if (loc.distanceSquaredTo(HQLocation) > 16) {
                                if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                                    MapLocation placeLoc = rc.getLocation().add(dir);
                                    if (placeLoc.x % 2 == HQLocation.x % 2 && placeLoc.y % 2 == HQLocation.y % 2)
                                        continue;
                                    rc.buildRobot(RobotType.REFINERY, dir);
                                    infoQ.add(getMessage(InformationCategory.NEW_REFINERY, placeLoc));
                                    refineryLocation.add(placeLoc);
                                    break;
                                }
                            }
                        }
                        if (factoryLocation != null) nav.bugNav(rc, factoryLocation);
                    }
                }
                // build drone factory
                if (droneFactoryLocation == null && isBuilder) {
                    if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost + 60) {
                        for (Direction dir : directions) {
                            MapLocation loc = rc.getLocation().add(dir);
                            if (loc.distanceSquaredTo(HQLocation) > 20) {
                                if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
                                    MapLocation placeLoc = rc.getLocation().add(dir);
                                    if (placeLoc.x % 2 == HQLocation.x % 2 && placeLoc.y % 2 == HQLocation.y % 2)
                                        continue;
                                    rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                                    droneFactoryLocation = rc.getLocation().add(dir);
                                    infoQ.add(getMessage(InformationCategory.DRONE_FACTORY, droneFactoryLocation));
                                    break;
                                }
                            }
                        }
                        if (factoryLocation != null) nav.bugNav(rc, factoryLocation);
                    }
                }
                if (rc.getRoundNum() > 300 && droneFactoryLocation == null && isTurtle) {
                    if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost+60) {
                        for (Direction dir : directions) {
                            MapLocation loc = rc.getLocation().add(dir);
                            if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
                                MapLocation placeLoc = rc.getLocation().add(dir);
                                if (placeLoc.x % 2 == HQLocation.x % 2 && placeLoc.y % 2 == HQLocation.y % 2)
                                    continue;
                                rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                                droneFactoryLocation = rc.getLocation().add(dir);
                                infoQ.add(getMessage(InformationCategory.DRONE_FACTORY, droneFactoryLocation));
                                break;
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
                    // TODO: remove refinery location if enemy destroys or is flooded
                    if (isTurtle) {
                        if (refineryLocation.isEmpty()) {
                            // build a refinery
                            Direction optDir = rc.getLocation().directionTo(HQLocation).opposite();
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
                                    break;
                                } else {
                                    optDir = optDir.rotateRight();
                                }
                            }
                        }
                        if (refineryLocation.isEmpty()) {
                            // if it's still empty, try to move back to HQ?
                            nav.bugNav(rc, HQLocation);
                            return;
                        }
                        closestRefineryLocation = refineryLocation.get(0);
                    } else closestRefineryLocation = HQLocation;
                    // check select a point as reference(might be edge case?)
                    MapLocation referencePoint = soupLoc;
                    if (referencePoint != null) {
                        //                System.out.println("reference to " + reference_point.toString());
                        int minRefineryDist = referencePoint.distanceSquaredTo(closestRefineryLocation);

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
                            infoQ.add(Cast.getMessage(InformationCategory.NEW_REFINERY, refineryLocation.get(refineryLocation.size() - 1)));
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
                        } else nav.bugNav(rc, closestRefineryLocation);
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
                    for (Direction d : directions) {
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
                            } else {
                                System.out.println("I'm going to soup");
                                System.out.println("Soup is at: " + soupLoc.toString());
                                nav.bugNav(rc, soupLoc);
                            }
                        }
                    } else {
                        System.out.println("Exploring!");
                        // check if getting close to flooded
                        // scout for soup
                        // explore
                        if (exploreTo == null || suspectsVisited.get(exploreTo)) {
                            nav.nextExplore();
                        }
                        System.out.println("I'm exploring to " + exploreTo.toString());
                        nav.bugNav(rc, exploreTo);
                    }
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
            if (rc.senseFlooding(check)) continue;
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
