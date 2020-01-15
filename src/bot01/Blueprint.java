package bot01;


import battlecode.common.*;

public class Blueprint {
    // only the first miner will have access to this object

    // 0, 5, 6 are vaporators, 1, 2, 3, 4 are net guns
    private Boolean[] buildComplete;
    private Vector[] minerTrail;
    private MapLocation HQLocation;
    private MapLocation DFLoc;
    private MapLocation LFLoc;
    private int rotateState;
    private boolean isShifted;

    public Blueprint(MapLocation HQLocation, int rotateState, boolean isShifted) {
        this.HQLocation = HQLocation;
        this.rotateState = rotateState;
        DFLoc = new Vector(2, 1).rotate(rotateState).addWith(HQLocation);
        LFLoc = new Vector(1, 2).rotate(rotateState).addWith(HQLocation);
        this.isShifted = isShifted;
        buildComplete = new Boolean[10];
        for (int i = 0; i < 10; i++) buildComplete[i] = false;
        if (isShifted) {
            minerTrail = new Vector[]{new Vector(0, 0), new Vector(0, -1), new Vector(-1, 0), new Vector(0, 1), new Vector(1, 0)};
        } else {
            minerTrail = new Vector[]{new Vector(-1, -1), new Vector(0, -1), new Vector(-1, 0), new Vector(0, 1), new Vector(1, 0)};
        }
        for (int i = 0; i < minerTrail.length; i++) {
            minerTrail[i] = minerTrail[i].rotate(rotateState);
        }
    }

    // called every turn, build necessary things
    // if there is nothing more to build, return false and explode
    public boolean build(RobotController rc) throws GameActionException {
        int currentIndex = getIndex(rc.getLocation());
        int buildNext = getBuildNext();
        // explode
        if (buildNext == -1) return false;
        if (rc.canSenseLocation(DFLoc)) {
            RobotInfo r = rc.senseRobotAtLocation(DFLoc);
            if (r == null) {
                if (currentIndex == 4) {
                    if (rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost+100) {
                        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, rotateDir(Direction.NORTHEAST))) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, rotateDir(Direction.NORTHEAST));
                        }
                    }
                } else {
                    goTo(rc,4);
                }
            }
        }
        if (rc.canSenseLocation(LFLoc)) {
            RobotInfo r = rc.senseRobotAtLocation(LFLoc);
            if (r == null) {
                if (currentIndex == 3) {
                    if (rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost+100) {
                        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, rotateDir(Direction.NORTHEAST))) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, rotateDir(Direction.NORTHEAST));
                        }
                    }
                } else {
                    goTo(rc,3);
                }
            }
        }
        if (buildNext == 0 || buildNext > 4) {
            if (rc.getTeamSoup() < RobotType.VAPORATOR.cost-50) {
                // only move if you have enough soup
                // best position to be in is 2
                goTo(rc, 0);
            }
            // building vaporators
            else if (buildNext == 0) {
                // move to index 0 or 1
                if (currentIndex == 1) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, rotateDir(Direction.EAST))) {
                            System.out.println("Built vaporator 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, rotateDir(Direction.EAST));
                        }
                    }
                }
                else if (currentIndex == 4) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, rotateDir(Direction.SOUTH))) {
                            System.out.println("Built vaporator 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, rotateDir(Direction.SOUTH));
                        }
                    }
                } else {
                    goTo(rc, 1);
                }
            }
            else if (buildNext == 5) {
                // move to index 3
                if (currentIndex == 2) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, rotateDir(Direction.NORTH))) {
                            System.out.println("Built vaporator 2");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, rotateDir(Direction.NORTH));
                        }
                    }
                }
                else if (currentIndex == 3) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, rotateDir(Direction.WEST))) {
                            System.out.println("Built vaporator 2");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, rotateDir(Direction.WEST));
                        }
                    }
                } else {
                    goTo(rc, 3);
                }
            }
            else if (buildNext == 6) {
                // move to index 1
                if (currentIndex == 1) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, rotateDir(Direction.NORTHEAST))) {
                            System.out.println("Built vaporator 3");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, rotateDir(Direction.NORTHEAST));
                        }
                    }
                } else {
                    goTo(rc, 1);
                }
            }
            else if (buildNext == 7) {
                // move to index 2
                if (currentIndex == 2) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        if (rc.canBuildRobot(RobotType.VAPORATOR, rotateDir(Direction.NORTHEAST))) {
                            System.out.println("Built vaporator 4");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, rotateDir(Direction.NORTHEAST));
                        }
                    }
                } else {
                    goTo(rc, 2);
                }
            }
            else if (buildNext == 8) {
                // move to index 0
                if (currentIndex == 0) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        Direction buildDir = rc.getLocation().directionTo(new Vector(-1, 0).rotate(rotateState).addWith(HQLocation));
                        if (rc.canBuildRobot(RobotType.VAPORATOR, buildDir)) {
                            System.out.println("Built vaporator 5");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, buildDir);
                        }
                    }
                } else {
                    goTo(rc, 0);
                }
            }
            else if (buildNext == 9) {
                // move to index 0
                if (currentIndex == 0) {
                    if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost) {
                        Direction buildDir = rc.getLocation().directionTo(new Vector(0, -1).rotate(rotateState).addWith(HQLocation));
                        if (rc.canBuildRobot(RobotType.VAPORATOR, buildDir)) {
                            System.out.println("Built vaporator 6");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.VAPORATOR, buildDir);
                        }
                    }
                } else {
                    goTo(rc, 0);
                }
            }
        }
        else {
            // building net guns
            if (rc.getTeamSoup() < RobotType.NET_GUN.cost+100) {
                // only move if you have enough soup
                // best position to be in is 2
                goTo(rc, 0);
            }
            else if (buildNext == 1) {
                // move to index 1
                if (currentIndex == 1) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, rotateDir(Direction.SOUTH))) {
                            System.out.println("Built net gun 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, rotateDir(Direction.SOUTH));
                        }
                    }
                } else {
                    goTo(rc, 1);
                }
            }
            else if (buildNext == 2) {
                // move to index 2
                if (currentIndex == 2) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, rotateDir(Direction.WEST))) {
                            System.out.println("Built net gun 2");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, rotateDir(Direction.WEST));
                        }
                    }
                } else {
                    goTo(rc, 2);
                }
            }
            else if (buildNext == 3) {
                // move to index 4
                if (currentIndex == 4) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, rotateDir(Direction.EAST))) {
                            System.out.println("Built net gun 3");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, rotateDir(Direction.EAST));
                        }
                    }
                } else {
                    goTo(rc, 4);
                }
            }
            else if (buildNext == 4) {
                // move to index 3
                if (currentIndex == 3) {
                    if (rc.getTeamSoup() >= RobotType.NET_GUN.cost+150) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, rotateDir(Direction.NORTH))) {
                            System.out.println("Built net gun 1");
                            buildComplete[buildNext] = true;
                            rc.buildRobot(RobotType.NET_GUN, rotateDir(Direction.NORTH));
                        }
                    }
                } else {
                    goTo(rc, 3);
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
        for (int i = 0; i < 10; i++) {
            if (!buildComplete[i]) return i;
        }
        return -1;
    }

    private Direction rotateDir(Direction dir) {
        return Vector.getVec(dir).rotate(rotateState).getDir();
    }
}
