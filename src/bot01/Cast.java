package bot01;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.LinkedList;
import java.lang.Math;

// Navigation class
public class Cast {

    public static int ukey;

    public static enum information_catagory{
        // new building(needs coordinate and type)
        new_building,
        // destroyed building(needs coordinate)
        remove_building,
        // found a soup repository
        new_soup
    }

    

    public static int broadCast(information_catagory cat ) {
        int message=0;
        switch (cat) {
            case  new_building:
                message = 1;
            break;
            case  remove_building:
                message = 2;
            break;
            case  new_soup :
                message = 3;
            break;
        }

        return message;
    }

    public static int addCoord(int info_ori,int[] pos){
        return info_ori*10000+pos[0]*100+pos[1];
    }

    public static information_catagory get_cat(int message){
        switch((int)Math.floor(message/10000)){
            case 1: return Cast.new_building;
            case 2: return Cast.remove_building;
            case 3: return Cast.new_soup;
        }
    }


}