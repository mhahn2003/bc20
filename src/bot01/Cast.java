package bot01;

// Navigation class
public class Cast {

    public static int ukey;

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

    

    public static int broadCast(information_catagory cat ) {
        switch (cat) {
            case NEW_BUILDING:      return 1;
            case REMOVE_BUILDING:   return 2;
            case NEW_SOUP:          return 3;
            case OTHER:             return 0;
            default:                return 0;
        }
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