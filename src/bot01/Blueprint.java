package bot01;


import battlecode.common.*;

public class Blueprint {
    // only the first miner will have access to this object

    // 0, 5, 6 are vaporators, 1, 2, 3, 4 are net guns
    private Boolean[] buildComplete;
    private Vector[] minerTrail;
    private MapLocation HQLocation;

    public Blueprint(MapLocation HQLocation) {
        this.HQLocation = HQLocation;
        buildComplete = new Boolean[7];
        for (int i = 0; i < 7; i++) buildComplete[i] = false;
        minerTrail = new Vector[]{new Vector(2, 1), new Vector(2, 0), new Vector(1, -1), new Vector(0, -1), new Vector(-1, -1), new Vector(-2, 0), new Vector(-1, 1)};
    }

    // called every turn, build necessary things
    // if there is nothing more to build, return false and explode
    public boolean build(RobotController rc) throws GameActionException {
        int currentIndex = getIndex(rc.getLocation());
        int buildNext = getBuildNext();
        // explode
        if (buildNext == -1) return false;
        if (buildNext == 0 || buildNext > 4) {
            if (rc.getTeamSoup() < RobotType.VAPORATOR.cost+50) {
                // only move if you have enough soup
                // best position to be in is 2
                goTo(rc, 2);
            }
            // building vaporators
            else if (buildNext == 0) {
                // move to index 0 or 1
                if (currentIndex == 0) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, Direction.WEST)) {
                            System.out.println("Built vaporator 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, Direction.WEST);
                        }
                    }
                }
                else if (currentIndex == 1) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost+100) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, Direction.NORTHWEST)) {
                            System.out.println("Built vaporator 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, Direction.NORTHWEST);
                        }
                    }
                } else {
                    goTo(rc, 1);
                }
            }
            else if (buildNext == 5) {
                // move to index 3
                if (currentIndex == 3) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost+100) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, Direction.WEST)) {
                            System.out.println("Built vaporator 3");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, Direction.WEST);
                        }
                    }
                } else {
                    goTo(rc, 3);
                }
            }
            else if (buildNext == 6) {
                // move to index 3
                if (currentIndex == 3) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost+100) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, Direction.EAST)) {
                            System.out.println("Built vaporator 3");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, Direction.EAST);
                        }
                    }
                } else {
                    goTo(rc, 3);
                }
            }
        }
        else {
            // building net guns
            if (rc.getTeamSoup() < RobotType.NET_GUN.cost+100) {
                // only move if you have enough soup
                // best position to be in is 2
                goTo(rc, 2);
            }
            else if (buildNext == 1) {
                // move to index 6
                if (currentIndex == 6) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, Direction.NORTHWEST)) {
                            System.out.println("Built net gun 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, Direction.NORTHWEST);
                        }
                    }
                } else {
                    goTo(rc, 6);
                }
            }
            else if (buildNext == 2) {
                // move to index 4
                if (currentIndex == 4) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, Direction.SOUTHWEST)) {
                            System.out.println("Built net gun 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, Direction.SOUTHWEST);
                        }
                    }
                } else {
                    goTo(rc, 4);
                }
            }
            else if (buildNext == 3) {
                // move to index 0
                if (currentIndex == 0) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, Direction.NORTH)) {
                            System.out.println("Built net gun 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, Direction.NORTH);
                        }
                    }
                } else {
                    goTo(rc, 0);
                }
            }
            else if (buildNext == 4) {
                // move to index 2
                if (currentIndex == 2) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, Direction.SOUTHEAST)) {
                            System.out.println("Built net gun 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, Direction.SOUTHEAST);
                        }
                    }
                } else {
                    goTo(rc, 2);
                }
            }
        }
        return true;
    }

    // get the index of the miner in minerTrail
    public int getIndex(MapLocation loc) {
        for (int i = 0; i < minerTrail.length; i++) {
            if (loc.equals(minerTrail[i].addWith(HQLocation))) return i;
        }
        return -1;
    }

    // assuming the miner is already on minerTrail
    // returns whether the miner has moved or not
    public boolean goTo(RobotController rc, int index) throws GameActionException {
        int rcIndex = getIndex(rc.getLocation());
        Direction optDir;
        if (rcIndex == index) return false;
        if (rcIndex < index) {
            optDir = rc.getLocation().directionTo(minerTrail[rcIndex+1].addWith(HQLocation));
        } else {
            optDir = rc.getLocation().directionTo(minerTrail[rcIndex-1].addWith(HQLocation));
        }
        if (rc.canMove(optDir)) {
            System.out.println("Going to: " + rc.getLocation().add(optDir));
            rc.move(optDir);
            return true;
        } else {
            System.out.println("I can't move there!");
            return false;
        }
    }

    // return the index of what to build next
    public int getBuildNext() {
        for (int i = 0; i < 7; i++) {
            if (!buildComplete[i]) return i;
        }
        return -1;
    }
}
