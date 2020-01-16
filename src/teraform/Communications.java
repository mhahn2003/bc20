package teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static teraform.Robot.*;
import static teraform.Util.*;

public class Communications {
    static RobotController rc;

    // information queue waiting to be sent to blockchain
    static ArrayList<Integer> infoQ = new ArrayList<>();

    public Communications(RobotController r, Robot robot) {
        rc = r;
    }


    // get information from the blocks
    public void getAllInfo() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++) {
            if (i % 10 == 1 || i % 10 == 2 || i % 10 == 3) getInfo(i);
        }
    }

    // get information from blockchain on that turn
    public void getInfo(int roundNum) throws GameActionException {
//        System.out.println("Getting info of round "+roundNum);
        Transaction[] info = rc.getBlock(roundNum);
        for (Transaction stuff: info) {
            int[] messageArr = removePad(stuff.getMessage());
            if (bot01.Cast.isMessageValid(rc, messageArr)) {
                for (int i = 0; i < messageArr.length-1; i++) {
                    int message = messageArr[i];
//                    System.out.println("message is: " + message);
//                    System.out.println("message validness is " + Cast.isValid(message, rc));
//                    System.out.println("message cat is" + Cast.getCat(message));
//                    System.out.println("message coord is" + Cast.getCoord(message));
                    if (bot01.Cast.isValid(message, rc)) {
                        // if valid message
                        MapLocation loc = bot01.Cast.getCoord(message);
                        boolean doAdd;
//                        System.out.println(Cast.getCat(message).toString());
                        switch (bot01.Cast.getCat(message)) {
                            case HQ:
                                HQLocation = loc;
                                break;
                            case ENEMY_HQ:
                                enemyHQLocation = loc;
                                if (!nav.isThreat(loc)) nav.addThreat(loc);
                                break;
                            case NET_GUN:
                                if (!nav.isThreat(loc)) nav.addThreat(loc);
                                break;
                            case NEW_REFINERY:
                                if (rc.getType() == RobotType.LANDSCAPER) break;
                                if (!refineryLocation.contains(loc)) refineryLocation.add(loc);
                                break;
                            case NEW_SOUP:
                                if (rc.getType() == RobotType.LANDSCAPER) break;
                                // add if it's far away enough from all the other soup coords
                                doAdd = true;
                                for (MapLocation soup : soupLocation) {
                                    if (soup.distanceSquaredTo(loc) <= soupClusterDist) {
                                        doAdd = false;
                                        break;
                                    }
                                }
                                if (doAdd) {
                                    soupLocation.add(loc);
                                }
                                break;
                            case WATER:
                                // add if it's far away enough from all the other water coords
                                if (rc.getType() != RobotType.DELIVERY_DRONE) break;
                                doAdd = true;
                                for (MapLocation water : waterLocation) {
                                    if (water.distanceSquaredTo(loc) <= waterClusterDist) {
                                        doAdd = false;
                                        break;
                                    }
                                }
                                if (doAdd) {
                                    waterLocation.add(loc);
                                }
                                break;
                            case REMOVE:
                                soupLocation.remove(loc);
                                waterLocation.remove(loc);
                                nav.removeThreat(loc);
                                if (suspects != null) {
                                    for (MapLocation l : suspects) {
                                        if (l.equals(loc)) {
                                            suspectsVisited.replace(l, true);
                                            break;
                                        }
                                    }
                                }
                                break;
                            case HELP:
                                if (rc.getType() != RobotType.DELIVERY_DRONE && rc.getType() != RobotType.FULFILLMENT_CENTER) break;
                                MapLocation c1 = bot01.Cast.getC1(message);
                                MapLocation c2 = bot01.Cast.getC2(message);
                                helpLoc.add(new Pair(c1, c2));
                                break;
                            case PREPARE:
                                phase = RobotPlayer.actionPhase.PREPARE;
                                break;
                            case ATTACK:
                                phase = RobotPlayer.actionPhase.ATTACK;
                                break;
                            case SURRENDER:
                                phase = RobotPlayer.actionPhase.SURRENDER;
                                break;
                            case DEFENSE:
                                phase = RobotPlayer.actionPhase.DEFENSE;
                                break;
                        }
                    }
                }
            }
        }
    }

    // send info when the turn number is 1 mod waitBlock (10), otherwise keep saving data
    public void collectInfo() throws GameActionException {
        MapLocation robotLoc = rc.getLocation();
        if (HQLocation == null && rc.getType() == RobotType.HQ) {
            HQLocation = rc.getLocation();
            infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.HQ, HQLocation));
        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        // location of the bot
        MapLocation rloc;
        // whether it is already in memory
        boolean saved;
        for (RobotInfo r : robots) {
            saved=false;
            if (enemyHQLocation == null && r.getType() == RobotType.HQ && r.getTeam() != rc.getTeam()) {
                enemyHQLocation = r.getLocation();
                infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.ENEMY_HQ, enemyHQLocation));
                infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.NET_GUN, enemyHQLocation));
                if (!nav.isThreat(enemyHQLocation)) nav.addThreat(enemyHQLocation);
            }
            // why is this an else if?
            else if (rc.getType() == RobotType.MINER && (r.getType() == RobotType.REFINERY || r.getType() == RobotType.HQ) && r.getTeam() == rc.getTeam()){
                rloc=r.getLocation();
                // check for matching
                for (MapLocation refineryLoca: refineryLocation){
                    if (refineryLoca.equals(rloc)) {
                        saved=true;
                    }
                }
                // no matching => not saved => save it
                if (!saved){
                    refineryLocation.add(rloc);
                }
            }
            if (r.getType() == RobotType.NET_GUN && r.getTeam() != rc.getTeam()) {
                MapLocation netGunLoc = r.getLocation();
                if (!nav.isThreat(netGunLoc)) {
                    infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.NET_GUN, netGunLoc));
                    nav.addThreat(netGunLoc);
                }
            }
        }
        boolean doAdd;
        soupLoc = null;
        for (int x = -maxV; x <= maxV; x+=2) {
            for (int y = -maxV; y <= maxV; y+=2) {
                // running out of bytecode so exiting early
                if (Clock.getBytecodesLeft() < 1000) break;
                MapLocation check = robotLoc.translate(x, y);
                if (rc.canSenseLocation(check)) {
                    // for now only check soup on dry land
                    if (rc.senseSoup(check) > 0 && !rc.senseFlooding(check)) {
                        int checkDist = check.distanceSquaredTo(rc.getLocation());
                        if (soupLoc == null || checkDist < soupLoc.distanceSquaredTo(rc.getLocation())
                                || (checkDist == soupLoc.distanceSquaredTo(rc.getLocation()) && rc.senseSoup(check) > rc.senseSoup(soupLoc)))
                            soupLoc = check;
                        doAdd = true;
                        for (MapLocation soup: soupLocation) {
                            if (soup.distanceSquaredTo(check) <= soupClusterDist) {
                                doAdd = false;
                                break;
                            }
                        }
                        if (doAdd) {
                            soupLocation.add(check);
                            infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.NEW_SOUP, check));
                        }
                    }
                    if ((rc.getRoundNum() <= 300 || (x % 3 == 0 && y % 3 == 0)) && rc.senseFlooding(check)) {
                        doAdd = true;
                        for (MapLocation water: waterLocation) {
                            if (water.distanceSquaredTo(check) <= waterClusterDist) {
                                doAdd = false;
                                break;
                            }
                        }
                        if (doAdd) {
                            waterLocation.add(check);
                            infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.WATER, check));
                        }
                    }
                }
            }
            if (Clock.getBytecodesLeft() < 1000) break;
        }
        for (MapLocation water: waterLocation) {
            if (rc.canSenseLocation(water)) {
                if (!rc.senseFlooding(water)) infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.REMOVE, water));
            }
        }
        for (MapLocation soup: soupLocation) {
            if (rc.getLocation().equals(soup)) {
                // check if robot is on the soup location and there is no soup around him
                // if there isn't any soup around it then remove
                // TODO: reimplement with the new documentation
                findSoup();
                if (rc.senseSoup(soup) == 0 && (soupLoc == null || rc.getLocation().distanceSquaredTo(soupLoc) >= soupClusterDist || soup.equals(soupLoc))) {
                    System.out.println("There's no soup!");
                    infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.REMOVE, soup));
                    soupLocation.remove(soup);
                }
                break;
            }
        }
        if (suspects != null) {
            for (MapLocation l: suspects) {
                if (suspectsVisited.get(l)) {
                    if (rc.getLocation().equals(l)) {
                        suspectsVisited.replace(l, true);
                        infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.REMOVE, l));
                    } else if (rc.canSenseLocation(l)) {
                        RobotInfo r = rc.senseRobotAtLocation(l);
                        if (r != null) {
                            RobotType t = r.getType();
                            if (r.getTeam() != rc.getTeam() && (t == RobotType.HQ || t == RobotType.NET_GUN)) {
                                suspectsVisited.replace(l, true);
                                infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.REMOVE, l));
                            } else if (rc.getLocation().distanceSquaredTo(l) < 9) {
                                suspectsVisited.replace(l, true);
                                infoQ.add(bot01.Cast.getMessage(bot01.Cast.InformationCategory.REMOVE, l));
                            }
                        }
                    }
                }
            }
        }
        if (rc.getRoundNum() % waitBlock == 1) sendInfo();
    }

    // send information collected to the blockchain
    public void sendInfo() throws GameActionException {
        if (!infoQ.isEmpty())  {
            int blockSize = Math.min(6, infoQ.size());
            int[] info = new int[blockSize+1];
            int[] prepHash = new int[blockSize];
            for (int i = 0; i < blockSize; i++) {
                info[i] = infoQ.get(0);
                prepHash[i] = infoQ.get(0);
                infoQ.remove(0);
            }
            // add the hash
            info[blockSize] = Cast.hash(rc, prepHash);
            if (rc.canSubmitTransaction(padMessage(info), defaultCost)) {
//                System.out.println("Submitted transaction! Message is : " + info.toString());
                rc.submitTransaction(padMessage(info), defaultCost);
            }
        }
    }

    private int[] padMessage(int[] arr) throws GameActionException {
        int[] message = new int[7];
        for (int i = 0; i < 7; i++) {
            if (i < arr.length) {
                message[i] = arr[i];
            } else {
                message[i] = 0;
            }
        }
        return message;
    }

    private int[] removePad(int[] arr) throws GameActionException {
        int nonZero = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 0) nonZero++;
        }
        int[] actualMesssage = new int[nonZero];
        int index = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != 0) {
                actualMesssage[index] = arr[i];
                index++;
            }
        }
        return actualMesssage;
    }
}
