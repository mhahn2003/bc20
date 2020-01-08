package bot01;

import battlecode.common.MapLocation;

// Navigation class
public class Cast {

    public static enum information_catagory{
        // new building(needs coordinate and type)
        NEW_BUILDING,
        // destroyed building(needs coordinate)
        REMOVE_BUILDING,
        // found a soup repository
        NEW_SOUP,
        // enemy?
        OTHER
    }


    public static int broadCast(information_catagory cat, MapLocation coord) {
        int message=0;
        switch (cat) {
            case NEW_BUILDING:      message += 1;
            case REMOVE_BUILDING:   message += 2;
            case NEW_SOUP:          message += 4;
            default:                message += 8;
        }
        message=addCoord(message, coord);
        return message;
    }

    public static int addCoord(int message, MapLocation coord){
        return message*10000 + coord.x*100 + coord.y;
    }

    public static information_catagory get_cat(int message){
        switch(message/10000) {
            // We might be able to do combination of cases with binary?
            case 1:  return information_catagory.NEW_BUILDING;
            case 2:  return information_catagory.REMOVE_BUILDING;
            case 4:  return information_catagory.NEW_SOUP;
            default: return information_catagory.OTHER;
        }
    }

    public static MapLocation getCoord(int message){
        return new MapLocation(message%10000-message%100, message%100);
    }

}