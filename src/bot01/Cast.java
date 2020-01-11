package bot01;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

// Navigation class
public class Cast {

    private int x_bot;

    private int y_bit;

    private static int defaultCost = 3;
    // number of possible cases for InfoCategory enum class
    private static int numCase = 8;

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
        // NET_GUN
        NET_GUN,
        // enemy?
        OTHER
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
            case NET_GUN:
                message += 8;
            default:
                message += 9;
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
            case 8: return InformationCategory.NET_GUN;
            default: return InformationCategory.OTHER;
        }
    }

    public static MapLocation getCoord(int message){
        return new MapLocation((message%10000-message%100)/100, message%100);
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
//        System.out.println("Hashed value is supposed to be: " + hashedValue);
//        System.out.println("Our hash value is: " + messageArr[messageArr.length-1]);
        return hashedValue == messageArr[messageArr.length-1];

    }

    // hash function
    // Note: messageArr can be size 1, 2, 3, 4, 5, 6
    public static int hash(int[] messageArr) {
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
        return hashedValue;
    }
}