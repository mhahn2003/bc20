package bot01;


import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.HashMap;
import java.util.Map;

public class Blueprint {
    // only the first miner will have access to this object

    private Vector[] netGunLoc;
    private Vector[] vaporatorLoc;
    private Vector[] minerTrail;
    private Map<Vector, Boolean> netGunComplete;
    private Map<Vector, Boolean> vaporatorComplete;
    private MapLocation HQLocation;

    public Blueprint(MapLocation HQLocation) {
        this.HQLocation = HQLocation;
        netGunComplete = new HashMap<>();
        vaporatorComplete = new HashMap<>();
        netGunLoc = new Vector[]{new Vector(-2, 2), new Vector(-2, -2), new Vector(2, 2), new Vector(2, -2)};
        vaporatorLoc = new Vector[]{new Vector(1, 1), new Vector(-1,1), new Vector(-1,-1), new Vector(1, -1)};
        for (Vector v: netGunLoc) {
            netGunComplete.put(v, false);
        }
        for (Vector v: vaporatorLoc) {
            vaporatorComplete.put(v, false);
        }
        minerTrail = new Vector[]{new Vector(2, 1), new Vector(2, 0), new Vector(1, -1), new Vector(0, -1), new Vector(-1, -1), new Vector(-2, 0), new Vector(-1, 1)};
    }

    // called every turn, build necessary things
    public void build(RobotController rc) {
        return;
    }

    // get the index of the miner in minerTrail
    private int getIndex(MapLocation loc) {
        for (int i = 0; i < minerTrail.length; i++) {
            if (loc.equals(minerTrail[i].addWith(HQLocation))) return i;
        }
        return -1;
    }
}
