package simbad;

import simbad.sim.Agent;

import javax.vecmath.Vector3d;

/**
 * Created by root on 11/24/15.
 */
public class WallFollower extends Agent {

    private boolean moveRandomly = false;
    private boolean followWall = true;

    private boolean missionComplete = false;
    private boolean searching = true; //searching for the target

    public WallFollower(Vector3d position, String name){
        super(position, name);
    }

    /** This method is called by the simulator engine on reset. */
    public void initBehavior() {

    }

    /** This method is call cyclically (20 times per second)  by the simulator engine. */
    public void performBehavior() {

        if(!missionComplete) {
            //if the item isnt found
            //  two behaviors: either follow a wall OR move randomly
            if (searching) {
                //follow wall-
                // choose either the right or left side
                // once side is chosen find a wall,
                //  keep wall at a certain distance
                //  continue until object is seen or time to move randomly has occurred

                //move randomly if the counter hits this
                if(getCounter() % 250 == 0){

                }else if(getCounter() % 49 == 0){

                }
            }

        }
        
    }
}
