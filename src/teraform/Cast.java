package teraform;

import battlecode.common.*;

import java.util.ArrayList;

import static teraform.Robot.*;
import static teraform.Util.*;

// Navigation class
public class Cast {

    static RobotController rc;

    // information queue waiting to be sent to blockchain
    static ArrayList<Integer> infoQ = new ArrayList<>();

    // number of possible cases for InfoCategory enum class
    private static int numCase = 13;
 
    public Cast(RobotController r) { rc = r; }

    public enum InformationCategory {
        // request help from drone
        HELP,
        // new refinery
        NEW_REFINERY,
        // information not there anymore
        REMOVE,
        // found a soup repository
        NEW_SOUP,
        // ENEMY HQ
        ENEMY_HQ,
        // OUR HQ
        HQ,
        // WATER
        WATER,
        // NET_GUN
        NET_GUN,
        // FORM SQUADS
        PREPARE,
        //RUN TO ENEMY HQ
        ATTACK,
        // LEAVE ENEMY HQ
        SURRENDER,
        // DEFENSE
        DEFENSE,
        // enemy?
        OTHER
    }
  


    public static int getMessage(InformationCategory cat, MapLocation coord) {
        int message=0;
        switch (cat) {
            case NEW_REFINERY:
                message += 2;
                break;
            case REMOVE:
                message += 3;
                break;
            case NEW_SOUP:
                message += 4;
                break;
            case ENEMY_HQ:
                message += 5;
                break;
            case HQ:
                message += 6;
                break;
            case WATER:
                message += 7;
                break;
            case NET_GUN:
                message += 8;
                break;
            case PREPARE:
                message += 9;
                break;
            case ATTACK:
                message += 10;
                break;
            case SURRENDER:
                message += 11;
                break;
            case DEFENSE:
                message += 12;
                break;
            default:
                message += 13;
                break;
        }
        message=addCoord(message, coord);
        return message;
    }

    public static int getMessage(MapLocation c1, MapLocation c2) {
        return addCoord(c1, c2);
    }

    private static int addCoord(int message, MapLocation coord) {
        return message*10000 + coord.x*100 + coord.y;
    }

    private static int addCoord(MapLocation c1, MapLocation c2) {
        return 100000000 + c1.x*1000000 + c1.y*10000 + c2.x*100 + c2.y;
    }

    public static InformationCategory getCat(int message){
        switch(message/10000) {
            case 2: return InformationCategory.NEW_REFINERY;
            case 3: return InformationCategory.REMOVE;
            case 4: return InformationCategory.NEW_SOUP;
            case 5: return InformationCategory.ENEMY_HQ;
            case 6: return InformationCategory.HQ;
            case 7: return InformationCategory.WATER;
            case 8: return InformationCategory.NET_GUN;
            case 9: return InformationCategory.PREPARE;
            case 10: return InformationCategory.ATTACK;
            case 11: return InformationCategory.SURRENDER;
            case 12: return InformationCategory.DEFENSE;
            default:
                if (message/100000000 == 1) return InformationCategory.HELP;
                return InformationCategory.OTHER;
        }
    }

    public static MapLocation getCoord(int message) {
        return new MapLocation((message%10000-message%100)/100, message%100);
    }

    public static MapLocation getC1(int message) {
        return new MapLocation((message%100000000-message%1000000)/1000000, (message%1000000-message%10000)/10000);
    }

    public static MapLocation getC2(int message) {
        return new MapLocation((message%10000-message%100)/100, message%100);
    }

    // check if it's our blockChain
    public static boolean isValid(int message, RobotController rc) throws GameActionException {
        if (getCat(message) != InformationCategory.HELP) return (message < 10000*(numCase+1) && onMap(getCoord(message), rc));
        else return (onMap(getC1(message), rc) && onMap(getC2(message), rc));
    }

    // check if location is on map
    public static boolean onMap(MapLocation loc, RobotController rc) {
        return (loc.x >= 0 && loc.x < rc.getMapWidth() && loc.y >= 0 && loc.y < rc.getMapHeight());
    }

    // check if given message is valid
    // Note: messageArr can be size 2, 3, 4, 5, 6, 7. If it's 1 or any other size, immediately return false
    public static boolean isMessageValid(RobotController rc, int[] messageArr) {
        int length = messageArr.length;
        if (length < 2 || length > 7){
            return false;
        }
        int hashedValue = 0;
        switch(messageArr.length){
            case 2:
                hashedValue = ((messageArr[0]%7345-9)^2+15)%65535;
                break;
            case 3:
                hashedValue = ((messageArr[0]%6555-23)+(messageArr[1]%998-1213)^2 )%65535;
                break;
            case 4:
                hashedValue = ((messageArr[0]%341-43)^2+(messageArr[1]%432-123)^2+(messageArr[2]%9653-2657)^2)%65535;
                break;
            case 5:
                hashedValue = ((messageArr[0]%2133-134)^2+(messageArr[1]%1341-4314)^2+(messageArr[2]%4312-324)^2+(messageArr[3]%42134-3242)^2)%65535;
                break;
            case 6:
                hashedValue = ((messageArr[0]%12344-341)^2+(messageArr[1]%1243-4134)^2+(messageArr[2]%41234-4432)^2+(messageArr[3]%4243-2144)^2+(messageArr[4]%4213-5432)^2)%65535;
                break;
            case 7:
                hashedValue = ((messageArr[0]%3143-1341)^2+(messageArr[1]%5465-7876)^2+(messageArr[2]%6752-5634)^2+(messageArr[3]%6754-2435)^2+(messageArr[4]%6345-5463)^2+(messageArr[5]%4314-5234)^2)%65535;
                break;
        }
        hashedValue += getTeamHash(rc);
//        System.out.println("Hashed value is supposed to be: " + hashedValue);
//        System.out.println("Our hash value is: " + messageArr[messageArr.length-1]);
        return hashedValue == messageArr[messageArr.length-1];

    }

    // hash function
    // Note: messageArr can be size 1, 2, 3, 4, 5, 6
    public static int hash(RobotController rc, int[] messageArr) {
        int hashedValue=0;
        switch(messageArr.length){
            case 1:

                hashedValue=((messageArr[0]%7345-9)^2+15)%65535;
                break;
            case 2:
                hashedValue=((messageArr[0]%6555-23)+(messageArr[1]%998-1213)^2 )%65535;
                break;
            case 3:
                hashedValue=((messageArr[0]%341-43)^2+(messageArr[1]%432-123)^2+(messageArr[2]%9653-2657)^2)%65535;
                break;
            case 4:
                hashedValue=((messageArr[0]%2133-134)^2+(messageArr[1]%1341-4314)^2+(messageArr[2]%4312-324)^2+(messageArr[3]%42134-3242)^2)%65535;
                break;
            case 5:
                hashedValue=((messageArr[0]%12344-341)^2+(messageArr[1]%1243-4134)^2+(messageArr[2]%41234-4432)^2+(messageArr[3]%4243-2144)^2+(messageArr[4]%4213-5432)^2)%65535;
                break;
            case 6:
                hashedValue=((messageArr[0]%3143-1341)^2+(messageArr[1]%5465-7876)^2+(messageArr[2]%6752-5634)^2+(messageArr[3]%6754-2435)^2+(messageArr[4]%6345-5463)^2+(messageArr[5]%4314-5234)^2)%65535;
                break;
        }
        return hashedValue + getTeamHash(rc);
    }

    private static int getTeamHash(RobotController rc) {
        if (rc.getTeam() == Team.A) return 1;
        else return 2;
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
            if (Cast.isMessageValid(rc, messageArr)) {
                for (int i = 0; i < messageArr.length-1; i++) {
                    int message = messageArr[i];
//                    System.out.println("message is: " + message);
//                    System.out.println("message validness is " + Cast.isValid(message, rc));
//                    System.out.println("message cat is" + Cast.getCat(message));
//                    System.out.println("message coord is" + Cast.getCoord(message));
                    if (Cast.isValid(message, rc)) {
                        // if valid message
                        MapLocation loc = Cast.getCoord(message);
                        boolean doAdd;
//                        System.out.println(Cast.getCat(message).toString());
                        switch (Cast.getCat(message)) {
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
                                MapLocation c1 = Cast.getC1(message);
                                MapLocation c2 = Cast.getC2(message);
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
            infoQ.add(Cast.getMessage(Cast.InformationCategory.HQ, HQLocation));
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
                infoQ.add(Cast.getMessage(Cast.InformationCategory.ENEMY_HQ, enemyHQLocation));
                infoQ.add(Cast.getMessage(Cast.InformationCategory.NET_GUN, enemyHQLocation));
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
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.NET_GUN, netGunLoc));
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
                            infoQ.add(Cast.getMessage(Cast.InformationCategory.NEW_SOUP, check));
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
                            infoQ.add(Cast.getMessage(Cast.InformationCategory.WATER, check));
                        }
                    }
                }
            }
            if (Clock.getBytecodesLeft() < 1000) break;
        }
        for (MapLocation water: waterLocation) {
            if (rc.canSenseLocation(water)) {
                if (!rc.senseFlooding(water)) infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, water));
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
                    infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, soup));
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
                        infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, l));
                    } else if (rc.canSenseLocation(l)) {
                        RobotInfo r = rc.senseRobotAtLocation(l);
                        if (r != null) {
                            RobotType t = r.getType();
                            if (r.getTeam() != rc.getTeam() && (t == RobotType.HQ || t == RobotType.NET_GUN)) {
                                suspectsVisited.replace(l, true);
                                infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, l));
                            } else if (rc.getLocation().distanceSquaredTo(l) < 9) {
                                suspectsVisited.replace(l, true);
                                infoQ.add(Cast.getMessage(Cast.InformationCategory.REMOVE, l));
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

    // TODO: put this function in only one place
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