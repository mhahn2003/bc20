package bot01;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

// Navigation class
public class Cast {

    private static int defaultCost = 5;
    // number of possible cases for InfoCategory enum class
    private static int numCase = 5;

    public enum InformationCategory {
        // new building(needs coordinate and type)
        NEW_BUILDING,
        // destroyed building(needs coordinate)
        REMOVE_BUILDING,
        // found a soup repository
        NEW_SOUP,
        // request help from drone
        HELP,
        // ENEMY HQ
        ENEMY_HQ,
        // enemy?
        OTHER
    }


    public static int[] broadCast(InformationCategory cat, MapLocation coord) {
        int message=0;
        switch (cat) {
            case NEW_BUILDING:      message += 1;
            case REMOVE_BUILDING:   message += 2;
            case NEW_SOUP:          message += 3;
            case HELP:              message += 4;
            case ENEMY_HQ:          message += 5;
            default:                message += 6;
        }
        message=addCoord(message, coord);
        return new int[]{message};
    }

    public static int addCoord(int message, MapLocation coord){
        return message*10000 + coord.x*100 + coord.y;
    }

    public static InformationCategory getCat(int message){
        switch(message/10000) {
            case 1: return InformationCategory.NEW_BUILDING;
            case 2: return InformationCategory.REMOVE_BUILDING;
            case 3: return InformationCategory.NEW_SOUP;
            case 4: return InformationCategory.HELP;
            case 5: return InformationCategory.ENEMY_HQ;
            default: return InformationCategory.OTHER;
        }
    }

    public static MapLocation getCoord(int message){
        return new MapLocation(message%10000-message%100, message%100);
    }

    // submits blockChain with default cost
    public static void blockChain(InformationCategory info, MapLocation coord, RobotController rc) throws GameActionException {
        int[] stuff = broadCast(info, coord);
        if (rc.canSubmitTransaction(stuff, defaultCost)) rc.submitTransaction(stuff, defaultCost);
    }

    // submits blockChain with specified cost
    public static void blockChain(InformationCategory info, MapLocation coord, RobotController rc, int cost) throws GameActionException {
        int[] stuff = broadCast(info, coord);
        if (rc.canSubmitTransaction(stuff, defaultCost)) rc.submitTransaction(stuff, cost);
    }

    // check if it's our blockChain
    public static boolean isValid(int message, RobotController rc) throws GameActionException {
        return (message < 10000*(numCase+1) && rc.onTheMap(getCoord(message)));
    }
}