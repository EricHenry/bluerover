package environment;

import communication.RobotCommunication;
import simbad.WallFollower;
import simbad.sim.Box;
import simbad.sim.EnvironmentDescription;
import simbad.sim.Wall;

import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * Created by kilsuf on 11/28/15.
 */
public class LRoom extends EnvironmentDescription {

    public LRoom(Vector3d boxLocation, int robotType, String coordinates) {
        light1IsOn = true;
        light2IsOn = false;

        lShapedRoom(boxLocation);

        WallFollower robot1 = new WallFollower(new Vector3d(-9,0,-8), "chappie", "right");
        add(robot1);

        //RobotCommunication robCom

        WallFollower robot2 = new WallFollower(new Vector3d(-9,0,-9), "machina", "left");
        add(robot2);

    }

    public void lShapedRoom(Vector3d boxLocation) {
        Wall bottomOfL = new Wall(new Vector3d(10, 0, 0), 20, 1, this);
        bottomOfL.setColor(new Color3f(0, 0, 0));
        bottomOfL.rotate90(1);
        add(bottomOfL);

        Wall topOfL = new Wall(new Vector3d(-10, 0, -5), 10, 1, this);
        topOfL.setColor(new Color3f(0,0,0));
        topOfL.rotate90(1);
        add(topOfL);

        Wall middleWall = new Wall(new Vector3d(0, 0, 5), 10, 1, this);
        middleWall.setColor(new Color3f(0, 0, 0));
        middleWall.rotate90(1);
        add(middleWall);

        Wall leftWall1 = new Wall(new Vector3d(5, 0, 10), 10, 1, this);
        leftWall1.setColor(new Color3f(0,0,0));
        add(leftWall1);

        Wall leftWall2 = new Wall(new Vector3d(-5, 0, 0), 10, 1, this);
        leftWall2.setColor(new Color3f(0,0,0));
        add(leftWall2);

        Wall rightWall = new Wall(new Vector3d(0, 0, -10), 20, 1, this);
        rightWall.setColor(new Color3f(0,0,0));
        add(rightWall);


        Box target = new Box(boxLocation, new Vector3f(1, 1, 1),
                this);
        target.setColor(new Color3f(0,0,256));
        add(target);
/*
        Box b2 = new Box(new Vector3d(1,0,-1), new Vector3f(1, 1, 1), this);
        b2.setColor(new Color3f(0,256,0));

        Box b3 = new Box(new Vector3d(-4,0,-4), new Vector3f(1, 1, 1), this);
        b3.setColor(new Color3f(0,256,0));

        Box b4 = new Box(new Vector3d(3,0,5), new Vector3f(1, 1, 1), this);
        b4.setColor(new Color3f(0,256,0));

        Box b5 = new Box(new Vector3d(7,0,-5), new Vector3f(1, 1, 1), this);
        b5.setColor(new Color3f(0,256,0));


        add(b2);
        add(b3);
        add(b4);
        add(b5);
        */
    }
}
