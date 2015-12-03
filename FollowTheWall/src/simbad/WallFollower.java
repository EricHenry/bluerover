package simbad;

import simbad.sim.Agent;
import simbad.sim.CameraSensor;
import simbad.sim.RangeSensorBelt;
import simbad.sim.RobotFactory;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Created by root on 11/24/15.
 */
public class WallFollower extends Agent {

    private String robotName;

    private RangeSensorBelt sonars, bumpers;
    private CameraSensor camera;

    private Point3d currentCords = new Point3d();
    private Point3d lastCoords = new Point3d();

    private boolean moveRandomly = false;
    private boolean followWall = true;
    private boolean followingWall = false;
    private boolean movingRandomly = false;

    private boolean adjusting = false;

    private boolean missionComplete = false;
    private boolean searching = true; //searching for the target
    boolean foundWall = false;

    boolean start = true;

    public WallFollower(Vector3d position, String name){
        super(position, name);

        robotName = name;

        // Add sensors
        bumpers = RobotFactory.addBumperBeltSensor(this, 12);
        sonars = RobotFactory.addSonarBeltSensor(this, 12);
        camera = RobotFactory.addCameraSensor(this);


    }

    /** This method is called by the simulator engine on reset. */
    public void initBehavior() {
        camera.setUpdateOnEachFrame(true);
        sonars.setUpdateOnEachFrame(true);

       // rotateY(Math.PI / 4);

        //setRotationalVelocity(Math.PI / 6);

    }

    /** This method is call cyclically (20 times per second)  by the simulator engine. */
    public void performBehavior() {


        //find wall
        double left = sonars.getLeftQuadrantMeasurement();
        //double left = sonars.getQuadrantMeasurement(Math.PI / 2, Math.PI / 2);
        double right = sonars.getFrontRightQuadrantMeasurement();
        double front = sonars.getFrontQuadrantMeasurement();

        if(start){
            System.out.println("START");
            start = false;
            setRotationalVelocity(Math.PI / 6);
        }

        if(foundWall){
            setTranslationalVelocity(0);
            if(sonars.oneHasHit()){
                //rotate until wall has been found on the front right quadrant
                if(sonars.getLeftQuadrantHits() > 1 ) {
                    foundWall = true;
                    System.out.println("WALL FOUND");
                    followWall = true;
                    setRotationalVelocity(0);
                }else if(getRotationalVelocity() == 0) {
                    System.out.println("rotaion not 0");
                    setRotationalVelocity(Math.PI / 6);
                }
            }

        }

        //setRotationalVelocity(Math.PI / 6);
        //System.out.println(sonars.getSensorAngle(3));
        System.out.println(sonars.getMeasurement(2));
        System.out.println(sonars.getMeasurement(3));
        System.out.println(sonars.getMeasurement(4));

        double sonar2 = sonars.getMeasurement(2);
        double sonar3 = sonars.getMeasurement(3);
        double sonar4 = sonars.getMeasurement(4);

        //keep heading straight
        if(!((sonar4-0.2) <= sonar2 && sonar2 <= (sonar4 + 0.2)) && !adjusting){
            //outside courner case
            System.out.println("Off Heading! Adjust");
                setTranslationalVelocity(0);

            setRotationalVelocity(0);
            adjusting = true;
        }else if(((sonar4-0.1) <= sonar2 && sonar2 <= (sonar4 + 0.1)) && adjusting){
            adjusting = false;
            System.out.println("ON Heading! Stop Adjusting");
            setRotationalVelocity(0);
            setTranslationalVelocity(1);
        }else {
            if(Double.isInfinite(sonar2) || Double.isInfinite(sonar4)){
                System.out.println("Sonar 2 or 4 is infinite");
               setTranslationalVelocity(0.3);
            }else {}
        }

        //heading not straight adjust heading
        if(adjusting){
            System.out.println("ADJUSTING");
            if(sonar2 > sonar4+0.1 || (Double.isInfinite(sonar4) && sonar2 > .75)) {
                System.out.println("Rotate left");
                setRotationalVelocity(Math.PI / 6);
            }else if (sonar2 < sonar4-0.1 || (Double.isInfinite(sonar4) && sonar2 < .75)){
                System.out.println("Rotate right");
                if(Double.isInfinite(sonar4)){
                    setTranslationalVelocity(0);
                }
                setRotationalVelocity(-Math.PI / 6);
            }else{
                //adjusting = false;
            }
        }

        //stay close to the wall
        if(!(0.7 < sonar3 && sonar3 < 0.73) && getTranslationalVelocity() != 0){
            if(sonar3 < 0.7) {
                setRotationalVelocity(-Math.PI / 6);
            }else if(sonar3 > 0.73){
                setRotationalVelocity(Math.PI / 6);
            }
        }else if((0.7 < sonar3 && sonar3 < 0.73) && !adjusting){
            setRotationalVelocity(0);
        }

        /*
        if(followWall){
            //System.out.println("followWall");
            setTranslationalVelocity(1);
            //avoid obsticles
            if(front < 1.5){
              //  System.out.println("hit front");
                //setRotationalVelocity(11*Math.PI / 6);
            }else {

                if (left > 1 && left < 5) {
                    //turn towards the wall
                    System.out.println("Turn towards the wall, front left is: " + left);
                    setRotationalVelocity(Math.PI / 6);
                } else if (left < 0.75) {
                    //turn away from the wall
                    System.out.println("Turn away the wall, front left is: " + left +"\n\t sensor angle: " + sonars.getSensorAngle(3));
                    setRotationalVelocity(-Math.PI / 6);
                }
            }

        }
*/
        //follow wall
        /*if(!followingWall && (getCounter() % 50 == 0)){
            setRotationalVelocity(0.5 - (0.1 * Math.random()));

        }else{



        }

        /*
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
                if(getCounter() % 250 == 0 || movingRandomly){
                    if(moveRandomly){
                        randomizeMovement();
                        moveRandomly = false;
                    }else if(movingRandomly){
                        //detect for collisions
                    }

                }else if(getCounter() % 49 == 0){
                    //follow wall

                }
            }else{
                setTranslationalVelocity(0);
                double timeToGoal = getLifeTime();
                missionComplete = true;
                //System.out.println("Test: " + boxCoordinates);
                System.out.println("Total Duration: " + timeToGoal);
                System.out.println("Total Distance: " + getOdometer());
            }

        }*/
        
    }

    public void randomizeMovement(){

    }

    /**
     *
     * @param currentCameraImage
     * @return
     */
    private double scanCameraFeed(BufferedImage currentCameraImage){
        camera.copyVisionImage(currentCameraImage);

        int maxY = currentCameraImage.getHeight();
        int maxX = currentCameraImage.getWidth();

        int totalPixelcount = maxY * maxX;
        int totalBluePixels = 0;

        // System.out.println("height: " + currentCameraImage.getHeight() +
        //         ", width: " + currentCameraImage.getWidth());

        for (int x = 0; x < maxX; x++) {

            for (int y = 0; y < maxY; y++) {
                Color imageColor = new Color(currentCameraImage.getRGB(x, y));
                int red = imageColor.getRed();
                int green = imageColor.getGreen();
                int blue = imageColor.getBlue();

                if (blue > green && blue > red){
                    totalBluePixels += 1;

                }
            }
        }

        //System.out.println("total pixelcount: " + totalPixelcount +
        //        ", total Red pixels: " + totalRedPixels);
        //currentBluePixels = totalBluePixels;
        return (double)totalBluePixels / (double)totalPixelcount;
    }

    public void trackCoordinates(String testNum){
        String fileName = "Assignment3/Random_Test" + testNum + ".txt";

        Point3d currentCoords = new Point3d();

        getCoords(currentCoords);

        //Point3d tempCords = new Point3d();
        lastCoords.set(currentCords.getX(), currentCords.getY(), currentCords.getZ());
        getCoords(currentCords);

        //System.out.println("x= " + currentCoords.getX() + " z= " + currentCoords.getZ());

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
            writer.println("x: " + currentCoords.getX() + ", y:" + currentCoords.getZ() + ", ");
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    /**s
    private void scanForTarget(){

        scanning = true;
        //System.out.println("I == "+ i);
        setRotationalVelocity(0);
        setTranslationalVelocity(0);

        switch (i) {
            case 0:
                rotateY(5 * Math.PI / 3);
                //highestPxlCntLoc[i] = currentRedPixels;
                //System.out.println("location array " + i + ": " + highestPxlCntLoc[i]);
                i = i + 1;

                break;
            case 1:
            case 2:
            case 3:
            case 4:
                rotateY(Math.PI / 6);
                highestPxlCntLoc[i - 1] = currentBluePixels;
                System.out.println("location array " + i + ": " + highestPxlCntLoc[i - 1]);
                i = i + 1;
                break;
            case 5:
                rotateY(5 * Math.PI / 3);
                highestPxlCntLoc[i - 1] = currentBluePixels;
                System.out.println("location array " + i + ": " + highestPxlCntLoc[i - 1]);

                int highest = 0;

                for (int i = 0; i < highestPxlCntLoc.length; i++) {
                    if (i == 0) {
                        highest = 0;
                    } else if (highestPxlCntLoc[i] > highestPxlCntLoc[highest]) {
                        System.out.println("num @ " + i + ": " + highestPxlCntLoc[i] + "\t num @ highest: " + highestPxlCntLoc[highest]);

                        highest = i;
                        //System.out.println("HIGHEST: " + highest);
                    }

                }


                switch (highest) {
                    case 0:
                        System.out.println("HIGHEST: " + highest + " rotate to 5 * Math.PI / 3 ");
                        rotateY(5 * Math.PI / 3);
                        break;
                    case 1:
                        System.out.println("HIGHEST: " + highest + " rotate to 11 * Math.PI / 6 ");
                        rotateY(11 * Math.PI / 6);
                        break;
                    case 2:
                        System.out.println("HIGHEST: " + highest + " rotate to nothing ");
                        break;
                    case 3:
                        System.out.println("HIGHEST: " + highest + " rotate to Math.PI / 6 ");
                        rotateY(Math.PI / 6);
                        break;
                    case 4:
                        System.out.println("HIGHEST: " + highest + " rotate to  Math.PI / 3 ");
                        rotateY(Math.PI / 3);
                        break;

                }

                if(highestPxlCntLoc[highest] == 0)
                    seenRed = false;
                i = 0;
                scanning = false;
                originalRedPixelCount = highestPxlCntLoc[highest];
                highestPxlCntLoc = new int[5];
                break;


        }




    }
    */
}
