package rngform;

import battlecode.common.*;

import static rngform.Cast.getMessage;
import static rngform.Cast.infoQ;
import static rngform.Util.directions;

public class HQ extends Shooter {
    static int numMiners = 0;

    public HQ(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        while (numMiners<10 && rc.getTeamSoup()>70){
            for (int i=0 ; i<8 ; i++){
                if (rc.canBuildRobot(RobotType.MINER , directions[i])){
                    rc.buildRobot(RobotType.MINER , directions[i]);
                    numMiners++;
                    break;
                }
            }
        }
    }
}