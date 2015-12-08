package communication;

import javax.vecmath.Point3d;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by kilsuf on 12/6/15.
 */
public class RobotCommunication {

    private static Queue<Point3d> robot1Location = new LinkedList<Point3d>();
    private static Queue<Point3d> robot2Location = new LinkedList<Point3d>();

    private static boolean let1Read = false;
    private static boolean let2Read = false;

    private static ArrayList<String[]> robotIDs = new ArrayList<String[]>();

    /**
     * Creates an id - robotName pair and returns the robot's ID
     *
     * @param robotName     - the name of the robot
     * @return              - the id of the given robot
     * @throws Exception    - if the max # of registrations have been reached
     */
    public static int registerRobot(String robotName) throws Exception{
        int numberOfRobots = robotIDs.size();

        //can only have a max of 2 robots registered right now
        if(numberOfRobots < 2) {
            int currentRobotId = numberOfRobots + 1;

            String[] entry = {Integer.toString(currentRobotId), robotName};
            robotIDs.add(entry);

            return currentRobotId;

        }else {
            throw new Exception("Number of allowable registered robots has maxed out.");
        }

    }

    public static boolean canRead(int robotID){
        if(robotID == 1)
            return let1Read;
        else
            return let2Read;
    }

    public static void setRead(int robotID, boolean canRead){
        if(robotID == 1)
            let2Read = canRead;
        else
            let1Read = canRead;
    }

    public static void addLocation(int robotID, Point3d currentLocation){
        if(robotID == 1){
           // System.out.println("ROBOT 1\n\tcurrent location added - X:= " + currentLocation.getX() + ", Y: " + currentLocation.getZ());
            robot1Location.add(currentLocation);

        }else if(robotID == 2){
           // System.out.println("ROBOT 2\n\tcurrent location added - X:= " + currentLocation.getX() + ", Y: " + currentLocation.getZ());
            robot2Location.add(currentLocation);
        }
    }

    public static Point3d removeLocation(int robotID) throws Exception{

        if(robotID == 1 && let1Read) {
            if(robot2Location.size() == 1)
                setRead(2,false);
            return robot2Location.remove();
        }else if(robotID == 2 && let2Read) {
            if(robot1Location.size() == 1)
                setRead(1,false);
            return robot1Location.remove();
        }else
            throw new Exception("Other robots location queue is empty");
    }

}
