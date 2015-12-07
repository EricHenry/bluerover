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
import java.util.concurrent.ThreadLocalRandom;

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
    private boolean findWall= false;

    private boolean adjusting = false;

    private boolean missionComplete = false;
    private boolean searching = true; //searching for the target
    boolean foundWall = false;

    boolean start = true;

    int lastAction = 0;

    private int originalRedPixelCount = 100;
    int i = 0;
    int[] highestPxlCntLoc = new int[5];
    private boolean scanning = false;
    private boolean seenBlue = false;
    private int currentBluePixels = 0;

    private int sameLocationCount = 0;

    private enum Algorithm {FOLLOWWALL, MOVERADOMLY, GOTOROBOT, BLINE};
    private Algorithm whosTurn = Algorithm.FOLLOWWALL;


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

        camera.setUpdatePerSecond(1);

        /*
        //find wall
        double left = sonars.getLeftQuadrantMeasurement();
        //double left = sonars.getQuadrantMeasurement(Math.PI / 2, Math.PI / 2);
        double right = sonars.getFrontRightQuadrantMeasurement();
        double front = sonars.getFrontQuadrantMeasurement();
*/
        if(start){
            System.out.println("START");
            start = false;
            setTranslationalVelocity(1);
            setRotationalVelocity(Math.PI / 6);
        }


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
                //check the current image to for the target
                BufferedImage currentCameraImage = camera.createCompatibleImage();
                double redRatio = scanCameraFeed(currentCameraImage);

                //check if the robot's front proximity sensor
                double frontRange = sonars.getFrontQuadrantMeasurement();

                //System.out.println("currentBLuePixels: " + currentBluePixels);

                //the target has been found if 85% of the screen is the target
                //  or the robot is close to the target with at least 65% of its vision is the target
                if (redRatio >= .85 || (redRatio >= .65 && frontRange < .85)) {
                    searching = false;
                    //System.out.println("HIT! RESULT: " + searching);

                    //if the target wasn't found, check if the target was at least seen.
                    //  and attempt to move toward it
                } else if (currentBluePixels > 1 || seenBlue) {

                    // System.out.println("INSIDE RED Pixel Count\n\tPixelcount is: " + currentRedPixels);

                    /**if (seenBlue)
                        System.out.println("SeenRed");
                    else
                        System.out.println("Current Red pixels > 1"); */

                    seenBlue = true;

                    if (!checkIfStuck()) {


                        if (bumpers.oneHasHit() && redRatio < .25) {
                            //System.out.println("\t seen less red - bumpers has one hit");
                            recoverFromCollision();

                        } else if (sonars.oneHasHit() && redRatio < .25) {
                            //System.out.println("\t seen less red - sonar has one hit");
                            avoidCollision();
                        }

                        if (getCounter() % 5 == 0) {

                            if (originalRedPixelCount >= currentBluePixels + 10 || scanning) {

                               // System.out.println("Seeing less red, React");
                                scanForTarget();

                            } else {

                                //System.out.println("Continuing to Red target");
                                setTranslationalVelocity(0.8);

                                if (!scanning) {
                                    originalRedPixelCount = currentBluePixels;
                                }
                            }

                        }


                    }
                } else if (!seenBlue) {
                    if(getCounter() % 500 == 0 && getCounter() > 1 && !findWall ){
                    /*if(moveRandomly){
                        randomizeMovement();
                        moveRandomly = false;
                    }else if(movingRandomly){
                        //detect for collisions
                    }*/
                        //System.out.println("MOVE RANDOMLY");
                        followWall = false;
                        findWall = false;
                        moveRandomly= true;


                        int rotateTo = ThreadLocalRandom.current().nextInt(1, 7 + 1);

                        //rotateY(0.5 - (0.1 * Math.random()));
                        switch(rotateTo){
                            case 1:
                                rotateY(11 * Math.PI / 6);
                                break;
                            case 2:
                                rotateY(7 * Math.PI / 4);
                                break;
                            case 3:
                                rotateY(5 * Math.PI / 3);
                                break;
                            case 4:
                                rotateY(3 * Math.PI / 2);
                                break;
                            case 5:
                                rotateY(4 * Math.PI / 3);
                                break;
                            case 6:
                                rotateY(5 * Math.PI / 4);
                                break;
                            case 7:
                                rotateY(7 * Math.PI / 6);
                                break;
                        }

                    }else if(getCounter() % 250 == 0){
                        //follow wall
                        followWall = true;
                        foundWall = false;
                        moveRandomly = false;
                    }

                    if(followWall == true && foundWall == false)
                        findWall = true;
                    else if(followWall == true){
                        //System.out.println("FOLLOW WALL");
                        //setTranslationalVelocity(1);
                        followWall();
                    }
                    if(findWall) {
                        searchForWall();
                    }

                    if(moveRandomly){
                        //System.out.println("INIT move randomly");
                        moveRandomly();
                    }
                }

            }else{
                setTranslationalVelocity(0);
                setRotationalVelocity(0);
                double timeToGoal = getLifeTime();
                missionComplete = true;
                //System.out.println("Test: " + boxCoordinates);
                System.out.println("Total Duration: " + timeToGoal);
                System.out.println("Total Distance: " + getOdometer());
            }

        }
        
    }

    /**
     *
     */
    private void followWall(){

        double sonar2 = sonars.getMeasurement(2);
        double sonar3 = sonars.getMeasurement(3);
        double sonar4 = sonars.getMeasurement(4);
        System.out.println("\tFollowing");
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
            }else if (sonar2 < sonar4-0.1 || (Double.isInfinite(sonar4) && sonar2 < .75) || (Double.isInfinite(sonar4) && sonars.hasHit(1))){
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
    }

    private void searchForWall(){

            if(sonars.oneHasHit()){

                boolean proceed = false;

                for(int i = 0; i < sonars.getNumSensors(); i++){
                    if(sonars.hasHit(i) && sonars.getMeasurement(i) < .75)
                        proceed = true;
                }
                if(proceed) {
                    //rotate until wall has been found on the front right quadrant
                    setTranslationalVelocity(0);
                    //setRotationalVelocity();
                    if (sonars.getLeftQuadrantHits() > 1) {
                        foundWall = true;
                        System.out.println("WALL FOUND");
                        findWall = false;
                        setRotationalVelocity(0);
                        setTranslationalVelocity(1);
                    } else if (getRotationalVelocity() == 0) {
                        System.out.println("rotaion not 0");
                        setRotationalVelocity(Math.PI / 6);
                    }
                }
            }else{
                setRotationalVelocity(0);
                setTranslationalVelocity(1);
            }


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

         System.out.println("height: " + currentCameraImage.getHeight() +
                 ", width: " + currentCameraImage.getWidth());

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

        System.out.println("total pixelcount: " + totalPixelcount +
                ", total Red pixels: " + totalBluePixels);
        currentBluePixels = totalBluePixels;
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

    /**
     *
     */
    private void moveRandomly(){
        if (bumpers.oneHasHit()) {

            recoverFromCollision();
            lastAction = getCounter();

        } else if (sonars.oneHasHit()) {
            avoidCollision();
            lastAction = getCounter();
        } else {
            setTranslationalVelocity(0.8);
            setRotationalVelocity(0);
        }

        // System.out.println("counter = " + getCounter() + ", lastAction = " + (lastAction));
        if ((getCounter() % 150 == 0) && lastAction < getCounter()) {
            System.out.println("HIT RANDOM");
                       /* double rand = Math.random() * 1;
                        if(Math.round(rand) == 1)
                            setRotationalVelocity(-1 - (0.1 * Math.random()));
                        else
                            setRotationalVelocity(1 - (0.1 * Math.random()));
                        */
            setRotationalVelocity(0.5 - (0.1 * Math.random()));
            setTranslationalVelocity(0);

            lastAction = getCounter();
        }
    }

    private void recoverFromCollision(){

        setTranslationalVelocity(-0.1);
        //setRotationalVelocity(0.5 - (0.1 * Math.random()));


        if(seenBlue){
            scanning = true;
            setRotationalVelocity(0.5 - (0.1 * Math.random()));
        }else {
            setRotationalVelocity(0.5 - (0.1 * Math.random()));
        }

    }

    private void avoidCollision(){
        // reads the three front quadrants
        double left = sonars.getFrontLeftQuadrantMeasurement();
        double right = sonars.getFrontRightQuadrantMeasurement();
        double front = sonars.getFrontQuadrantMeasurement();
        // if obstacle near
        if ((front < 0.7) || (left < 0.7) || (right < 0.7)) {
            if (left < right)
                setRotationalVelocity(-1 - (0.1 * Math.random()));
            else
                setRotationalVelocity(1 - (0.1 * Math.random()));
            setTranslationalVelocity(0);


        } else {
            setRotationalVelocity(0);
            setTranslationalVelocity(0.8);
        }
    }

    /**
     *
     */
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
                    seenBlue = false;
                i = 0;
                scanning = false;
                originalRedPixelCount = highestPxlCntLoc[highest];
                highestPxlCntLoc = new int[5];
                break;


        }




    }

    private boolean checkIfStuck(){

        if ((currentCords.getX() == lastCoords.getX() && currentCords.getZ() == lastCoords.getZ())) {
            System.out.println("IN SAME LOCATION: " + sameLocationCount);
            sameLocationCount += 1;
        }


        if(sameLocationCount == 20) {
            sameLocationCount = 0;
            return true;
        } else {
            return false;
        }

    }
}
