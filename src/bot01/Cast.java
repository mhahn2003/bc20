package bot01;

import battlecode.common.RobotController;

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


    public static int broadCast(information_catagory cat,int[] coord ) {
        int message=0;
        switch (cat) {
            case NEW_BUILDING:      message+= 1;
            case REMOVE_BUILDING:   message+= 2;
            case NEW_SOUP:          message+= 3;
            default:                message+= 0;
        }
        message=addCoord(message, coord);
        return message;
    }

    public static int addCoord(int info_ori,int[] pos){
        return info_ori*10000+pos[0]*100+pos[1];
    }

    public static information_catagory get_cat(int message){
        switch((int)Math.floor(message/10000)){
            case 1:  return information_catagory.NEW_BUILDING;
            case 2:  return information_catagory.REMOVE_BUILDING;
            case 3:  return information_catagory.NEW_SOUP;
            default: return information_catagory.OTHER;
        }
    }

    public static int[] getCoord(int message){
        int[] coord={message%10000-message%100, message%100};
        return coord;
    }

}