package simbad;

import communication.RobotCommunication;
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
    private int robotID;
    private String testNum;

    private RangeSensorBelt sonars, bumpers;
    private CameraSensor camera;

    private Point3d currentCoords = new Point3d();
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

    private enum Algorithm {FOLLOWWALL, MOVERADOMLY, GOTOROBOT, NONE};
    private Algorithm whosTurn = Algorithm.FOLLOWWALL;

    private String side;

    private int lastTurn = 0;
    private Point3d targetLocation = new Point3d();


    public WallFollower(Vector3d position, String name, String side, String testNumber){
        super(position, name);

        robotName = name;
        this.side = side;
        testNum = testNumber;

        //register the robot with the communicator
        try {
            robotID = RobotCommunication.registerRobot(name);
            System.out.println("Registered Robot: " + name + " with id: " + robotID);
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        // Add sensors
        bumpers = RobotFactory.addBumperBeltSensor(this, 12);
        sonars = RobotFactory.addSonarBeltSensor(this, 12);
        camera = RobotFactory.addCameraSensor(this);


    }

    /** This method is called by the simulator engine on reset. */
    public void initBehavior() {
        //have the camera and sonars update on every frame
        camera.setUpdateOnEachFrame(true);
        sonars.setUpdateOnEachFrame(true);

        //if this robot should follow a wall on its right side
        //  turn it initially
        if(side.equalsIgnoreCase("right"))
            rotateY(3 * Math.PI / 2);

    }

    public void performBehavior() {

        camera.setUpdatePerSecond(1);

        //only perform this the first time this function runs
        if(start){
            System.out.println("START");
            start = false;
            setTranslationalVelocity(1);
            setRotationalVelocity(Math.PI / 6);
            whosTurn = Algorithm.FOLLOWWALL;
        }

        //continue to perform the following as long as the target is not found
        if(!missionComplete) {

            //track the coordinates for mapping after simulation and to keep track of the robot
            trackCoordinates(testNum);

            //every 500 frames switch the algorithm to run
            if(getCounter() % 500 == 0 && getCounter() > 1){
                if(whosTurn == Algorithm.FOLLOWWALL){
                    whosTurn = Algorithm.MOVERADOMLY;
                }else{
                    whosTurn = Algorithm.FOLLOWWALL;
                }
            }

            //continue if still searching for target
            if (searching) {

                //get the camera's current image and scan for the target
                BufferedImage currentCameraImage = camera.createCompatibleImage();
                double targetColorRatio = scanCameraFeed(currentCameraImage);

                //check if the robot's front proximity sensor
                double frontRange = sonars.getFrontQuadrantMeasurement();

                //check if the robot can read message from queue that contains target's location
                if(RobotCommunication.canRead(robotID)) {

                    try {
                        targetLocation = RobotCommunication.removeLocation(robotID);

                        whosTurn = Algorithm.GOTOROBOT;
                        //translateTo(new Vector3d(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ()));
                        //RobotCommunication.setRead(robotID);
                    }catch (Exception e){
                        System.out.println(e.getMessage());
                    }

                }

                //the target has been found if 85% of the screen is the target
                //  or the robot is close to the target with at least 65% of its vision is the target
                if (targetColorRatio >= .85 || (targetColorRatio >= .65 && frontRange < .85)) {
                    searching = false;

                //if the target wasn't found, check if the target was at least seen.
                //  and attempt to move toward it
                // The target is the only blue object on the board
                } else if (currentBluePixels > 1 || seenBlue) {

                    seenBlue = true;

                    //obstacle avoidance
                    if (!checkIfStuck()) {


                        if (bumpers.oneHasHit() && targetColorRatio < .25) {
                            //System.out.println("\t seen less red - bumpers has one hit");
                            recoverFromCollision();

                        } else if (sonars.oneHasHit() && targetColorRatio < .25) {
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

                //Continue if the robot has received the location of the target
                //  and should be moving to the other robot's location
                } else if(whosTurn == Algorithm.GOTOROBOT) {

                    double left = sonars.getFrontLeftQuadrantMeasurement();
                    double right = sonars.getFrontRightQuadrantMeasurement();
                    double front = sonars.getFrontQuadrantMeasurement();

                    //If there is nothing to avoid move to target
                    if (bumpers.oneHasHit()) {
                        recoverFromCollision();

                    } else if (left < 0.7 || front < 0.7 || right < 0.7) {
                         // System.out.println("\t sonar has one hit");
                        avoidCollision();
                    } else if(isCloserToTarget()){
                        //Do nothing
                    }else if(getRotationalVelocity() == 0){

                        if (left < 1.5 || front < 1.5 || right < 1.5) {
                            if (left < right)
                                setRotationalVelocity(-5 * Math.PI / 6);
                            else
                                setRotationalVelocity(5 * Math.PI / 6);
                            setTranslationalVelocity(0);
                        }else{
                            if(lastTurn == 1) {
                                setRotationalVelocity(5 * Math.PI / 6);
                                lastTurn = 0;
                            }else {
                                setRotationalVelocity(-5 * Math.PI / 6);
                                lastTurn = 1;
                            }
                        }
                    }

                //Continue if the target hasnt been found
                }else if (!seenBlue) {

                    //every time it is reset to moveRandomly, chose a random direction
                    //  to start moving in
                    if(!findWall && whosTurn == Algorithm.MOVERADOMLY && !moveRandomly){

                        followWall = false;
                        findWall = false;
                        moveRandomly= true;

                        int rotateTo = ThreadLocalRandom.current().nextInt(1, 7 + 1);
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

                    //every time it is set to the FOLLOWWALL algorithm do the following
                    }else if(whosTurn == Algorithm.FOLLOWWALL){
                        //follow wall
                        followWall = true;
                        foundWall = false;
                        moveRandomly = false;
                        whosTurn = Algorithm.NONE;
                    }

                    //Check to see if a wall needs to be found
                    // if not follow the wall
                    if(followWall == true && foundWall == false)
                        findWall = true;
                    else if(followWall == true){
                        followWall(side);
                    }

                    //if a wall needs to be found, search for one
                    if(findWall) {
                        searchForWall(side);
                    }

                    //move randomly
                    if(moveRandomly){
                        moveRandomly();
                    }

                }

            }else{
                //robot was found, spew out data
                setTranslationalVelocity(0);
                setRotationalVelocity(0);
                double timeToGoal = getLifeTime();
                missionComplete = true;
                System.out.println("Robot: " + robotName + ", id: " + robotID + " is complete");
                System.out.println("Test number: " + testNum);
                System.out.println("Total Duration: " + timeToGoal);
                System.out.println("Total Distance: " + getOdometer());
                System.out.println("--------------------------------------------------------------------");

                //update other robot that the target has been found
                RobotCommunication.addLocation(robotID, currentCoords);
                RobotCommunication.setRead(robotID, true);
            }

        }
        
    }

    /**
     * Compares the last set of coordinates and the current set of coordinates to determine if the robot is
     *  moving closer to the target (used once the robot is given the location to th robot)
     * @return  true: if moving closer to target, false: if moving away from target
     */
    private boolean isCloserToTarget(){

        double lastX = lastCoords.getX();
        double lastZ = lastCoords.getZ();

        double currentX = currentCoords.getX();
        double currentZ = currentCoords.getZ();

        double targetX = targetLocation.getX();
        double targetZ = targetLocation.getZ();

        double lastDistance = getDistance(lastX, lastZ, targetX, targetZ);
        double thisDistance = getDistance(currentX, currentZ, targetX, targetZ);

        if(lastDistance <= thisDistance){
            return false;
        }else{
           return true;
        }

    }

    /**
     * Find the distance between 2 points.
     * @param x -> Target's X location
     * @param y -> Target's Y location
     * @param a -> Other point's X location
     * @param b -> Other point's Y location
     * @return -> The absolute difference between the points
     */
    private double getDistance(double x, double y, double a, double b){

        double euclideanDistance = Math.sqrt(Math.pow(x - a, 2.0) + Math.pow(y - b, 2.0));

        return Math.abs(euclideanDistance);
    }

    /**
     *  Have the robot use the Wall Following algorithm
     */
    private void followWall(String side){

        if(side.equalsIgnoreCase("left")) {
            double sonar2 = sonars.getMeasurement(2);
            double sonar3 = sonars.getMeasurement(3);
            double sonar4 = sonars.getMeasurement(4);

            //keep heading straight
            if (!((sonar4 - 0.2) <= sonar2 && sonar2 <= (sonar4 + 0.2)) && !adjusting) {
                setTranslationalVelocity(0);

                setRotationalVelocity(0);
                adjusting = true;
            } else if (((sonar4 - 0.1) <= sonar2 && sonar2 <= (sonar4 + 0.1)) && adjusting) {
                adjusting = false;
                setRotationalVelocity(0);
                setTranslationalVelocity(1);
            } else {
                if (Double.isInfinite(sonar2) || Double.isInfinite(sonar4)) {
                    setTranslationalVelocity(0.3);
                } else {
                }
            }

            //heading not straight adjust heading
            if (adjusting) {
                if (sonar2 > sonar4 + 0.1 || (Double.isInfinite(sonar4) && sonar2 > .75)) {
                    setRotationalVelocity(Math.PI / 6);
                } else if (sonar2 < sonar4 - 0.1 || (Double.isInfinite(sonar4) && sonar2 < .75) || (Double.isInfinite(sonar4) && sonars.hasHit(1))) {

                    if (Double.isInfinite(sonar4)) {
                        setTranslationalVelocity(0);
                    }
                    setRotationalVelocity(-Math.PI / 6);
                }
            }

            //stay close to the wall
            if (!(0.7 < sonar3 && sonar3 < 0.73) && getTranslationalVelocity() != 0) {
                if (sonar3 < 0.7) {
                    setRotationalVelocity(-Math.PI / 6);
                } else if (sonar3 > 0.73) {
                    setRotationalVelocity(Math.PI / 6);
                }
            } else if ((0.7 < sonar3 && sonar3 < 0.73) && !adjusting) {
                setRotationalVelocity(0);
            }
        }
        else if (side.equalsIgnoreCase("right")){
            double sonar10 = sonars.getMeasurement(10);
            double sonar9 = sonars.getMeasurement(9);
            double sonar8 = sonars.getMeasurement(8);

            //keep heading straight
            if (!((sonar8 - 0.2) <= sonar10 && sonar10 <= (sonar8 + 0.2)) && !adjusting) {

                setTranslationalVelocity(0);

                setRotationalVelocity(0);
                adjusting = true;
            } else if (((sonar8 - 0.1) <= sonar10 && sonar10 <= (sonar8 + 0.1)) && adjusting) {
                adjusting = false;
                setRotationalVelocity(0);
                setTranslationalVelocity(1);
            } else {
                if (Double.isInfinite(sonar10) || Double.isInfinite(sonar8)) {
                    setTranslationalVelocity(0.3);
                } else {
                }
            }

            //heading not straight adjust heading
            if (adjusting) {
                if (sonar10 > sonar8 + 0.1 || (Double.isInfinite(sonar8) && sonar10 > .75)) {
                    setRotationalVelocity(-Math.PI / 6);
                } else if (sonar10 < sonar8 - 0.1 || (Double.isInfinite(sonar8) && sonar10 < .75) || (Double.isInfinite(sonar8) && sonars.hasHit(1))) {

                    if (Double.isInfinite(sonar8)) {
                        setTranslationalVelocity(0);
                    }
                    setRotationalVelocity(Math.PI / 6);
                }
            }

            //stay close to the wall
            if (!(0.7 < sonar9 && sonar9 < 0.73) && getTranslationalVelocity() != 0) {
                if (sonar9 < 0.7) {
                    setRotationalVelocity(Math.PI / 6);
                } else if (sonar9 > 0.73) {
                    setRotationalVelocity(-Math.PI / 6);
                }
            } else if ((0.7 < sonar9 && sonar9 < 0.73) && !adjusting) {
                setRotationalVelocity(0);
            }

        }
    }

    /**
     * This allows for the robot to find a wall
     * @param side -> the side of the robot that should be closets to the wall
     */
    private void searchForWall(String side){
        if(side.equalsIgnoreCase("left")) {
            if (sonars.oneHasHit()) {

                boolean proceed = false;

                for (int i = 0; i < sonars.getNumSensors(); i++) {
                    if (sonars.hasHit(i) && sonars.getMeasurement(i) < .75)
                        proceed = true;
                }
                if (proceed) {
                    //rotate until wall has been found on the front right quadrant
                    setTranslationalVelocity(0);

                    if (sonars.getLeftQuadrantHits() > 1) {
                        foundWall = true;
                        findWall = false;
                        setRotationalVelocity(0);
                        setTranslationalVelocity(1);
                    } else if (getRotationalVelocity() == 0) {
                        setRotationalVelocity(Math.PI / 6);
                    }
                }
            } else {
                setRotationalVelocity(0);
                setTranslationalVelocity(1);
            }
        }
        else if(side.equalsIgnoreCase("right")){
            if (sonars.oneHasHit()) {

                boolean proceed = false;

                for (int i = 0; i < sonars.getNumSensors(); i++) {
                    if (sonars.hasHit(i) && sonars.getMeasurement(i) < .75)
                        proceed = true;
                }
                if (proceed) {
                    //rotate until wall has been found on the front right quadrant
                    setTranslationalVelocity(0);
                    if (sonars.getRightQuadrantHits() > 1) {
                        foundWall = true;
                        findWall = false;
                        setRotationalVelocity(0);
                        setTranslationalVelocity(1);
                    } else if (getRotationalVelocity() == 0) {
                        setRotationalVelocity(Math.PI / 6);
                    }
                }
            } else {
                setRotationalVelocity(0);
                setTranslationalVelocity(1);
            }
        }

    }

    /**
     * Measure the current camera feed for the color blue. Blue is the color of the target
     * @param currentCameraImage -> the current image of the robot's camera
     * @return the
     */
    private double scanCameraFeed(BufferedImage currentCameraImage){
        camera.copyVisionImage(currentCameraImage);

        int maxY = currentCameraImage.getHeight();
        int maxX = currentCameraImage.getWidth();

        int totalPixelcount = maxY * maxX;
        int totalBluePixels = 0;

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

        currentBluePixels = totalBluePixels;
        return (double)totalBluePixels / (double)totalPixelcount;
    }

    /**
     * This function updates the last and current coordinates and saves all coordinates in a file to be processed later.
     * @param testNum -> the test experiment
     */
    public void trackCoordinates(String testNum){
        String fileName = "Assignment4/WallFollow_Test_" + testNum + "_Robot_" + robotName + ".txt";

        lastCoords.set(currentCoords.getX(), currentCoords.getY(), currentCoords.getZ());
        getCoords(currentCoords);

        try {
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
            writer.println("x: " + currentCoords.getX() + ", y:" + currentCoords.getZ() + ", ");
            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    /**
     * Allows the robot to move around in a random direction
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

        if ((getCounter() % 150 == 0) && lastAction < getCounter()) {

            setRotationalVelocity(0.5 - (0.1 * Math.random()));
            setTranslationalVelocity(0);

            lastAction = getCounter();
        }
    }

    /**
     * This function has the robot recover if it has hit a wall
     */
    private void recoverFromCollision(){

        setTranslationalVelocity(-0.1);

        if(seenBlue){
            scanning = true;
            setRotationalVelocity(0.5 - (0.1 * Math.random()));
        }else {
            setRotationalVelocity(0.5 - (0.1 * Math.random()));
        }

    }

    /**
     * this function causes the robot to move away from a wall that is too close
     */
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
     * This function scans around the front of the robot to pick a direction that will put the robot in
     *  a more exact heading with the target
     */
    private void scanForTarget(){

        scanning = true;

        setRotationalVelocity(0);
        setTranslationalVelocity(0);

        switch (i) {
            case 0:
                rotateY(5 * Math.PI / 3);
                i = i + 1;

                break;
            case 1:
            case 2:
            case 3:
            case 4:
                rotateY(Math.PI / 6);
                highestPxlCntLoc[i - 1] = currentBluePixels;
                i = i + 1;
                break;
            case 5:
                rotateY(5 * Math.PI / 3);
                highestPxlCntLoc[i - 1] = currentBluePixels;

                int highest = 0;

                for (int i = 0; i < highestPxlCntLoc.length; i++) {
                    if (i == 0) {
                        highest = 0;
                    } else if (highestPxlCntLoc[i] > highestPxlCntLoc[highest]) {

                        highest = i;
                    }

                }


                switch (highest) {
                    case 0:
                        rotateY(5 * Math.PI / 3);
                        break;
                    case 1:
                        rotateY(11 * Math.PI / 6);
                        break;
                    case 2:
                        break;
                    case 3:
                        rotateY(Math.PI / 6);
                        break;
                    case 4:
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

    /**
     * This function checks to see if the robot has been stuck in the same location for a certain amount of cycles,
     * @return true: if stuck, false: if not
     */
    private boolean checkIfStuck(){

        if ((currentCoords.getX() == lastCoords.getX() && currentCoords.getZ() == lastCoords.getZ())) {
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
