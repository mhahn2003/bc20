package bot01;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

// Navigation class
public class Cast {

    private static int defaultCost = 5;
    // number of possible cases for InfoCategory enum class
    private static int numCase = 7;

    public Cast() {

    }

    public enum InformationCategory {
        // new building(needs coordinate and type)
        NEW_BUILDING,
        // destroyed building(needs coordinate)
        REMOVE,
        // found a soup repository
        NEW_SOUP,
        // request help from drone
        HELP,
        // ENEMY HQ
        ENEMY_HQ,
        // OUR HQ
        HQ,
        // WATER
        WATER,
        // enemy?
        OTHER
    }


    private static int[] broadCastSingle(InformationCategory cat, MapLocation coord) {
        int message=0;
        switch (cat) {
            case NEW_BUILDING:
                message += 1;
                break;
            case REMOVE:
                message += 2;
                break;
            case NEW_SOUP:
                message += 3;
                break;
            case HELP:
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
            default:
                message += 8;
                break;
        }
        message=addCoord(message, coord);
        return new int[]{message};
    }

    public static int getMessage(InformationCategory cat, MapLocation coord) {
        int message=0;
        switch (cat) {
            case NEW_BUILDING:
                message += 1;
                break;
            case REMOVE:
                message += 2;
                break;
            case NEW_SOUP:
                message += 3;
                break;
            case HELP:
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
            default:
                message += 8;
                break;
        }
        message=addCoord(message, coord);
        return message;
    }

    private static int addCoord(int message, MapLocation coord){
        return message*10000 + coord.x*100 + coord.y;
    }

    public static InformationCategory getCat(int message){
        switch(message/10000) {
            case 1: return InformationCategory.NEW_BUILDING;
            case 2: return InformationCategory.REMOVE;
            case 3: return InformationCategory.NEW_SOUP;
            case 4: return InformationCategory.HELP;
            case 5: return InformationCategory.ENEMY_HQ;
            case 6: return InformationCategory.HQ;
            case 7: return InformationCategory.WATER;
            default: return InformationCategory.OTHER;
        }
    }

    public static MapLocation getCoord(int message){
        return new MapLocation((message%10000-message%100)/100, message%100);
    }

    // submits blockChain with default cost
    public static void blockChain(InformationCategory info, MapLocation coord, RobotController rc) throws GameActionException {
        int[] stuff = broadCastSingle(info, coord);
        if (rc.canSubmitTransaction(stuff, defaultCost)) rc.submitTransaction(stuff, defaultCost);
    }

    // submits blockChain with specified cost
    public static void blockChain(InformationCategory info, MapLocation coord, RobotController rc, int cost) throws GameActionException {
        int[] stuff = broadCastSingle(info, coord);
        if (rc.canSubmitTransaction(stuff, defaultCost)) rc.submitTransaction(stuff, cost);
    }

    // check if it's our blockChain
    public static boolean isValid(int message, RobotController rc) throws GameActionException {
        return (message < 10000*(numCase+1) && onMap(getCoord(message), rc));
    }

    // check if location is on map
    public static boolean onMap(MapLocation loc, RobotController rc) {
        return (loc.x >= 0 && loc.x < rc.getMapWidth() && loc.y >= 0 && loc.y < rc.getMapHeight());
    }

    // check if given message is valid
    // Note: messageArr can be size 2, 3, 4, 5, 6, 7. If it's 1 or any other size, immediately return false
    public static boolean isMessageValid(int[] messageArr) {
        return false;
    }

    // hash function
    // Note: messageArr can be size 1, 2, 3, 4, 5, 6
    public static int hash(int[] messageArr) {
        return 0;
    }
}