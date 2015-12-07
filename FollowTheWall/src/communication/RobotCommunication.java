package communication;

import javax.vecmath.Point3d;
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
            robot1Location.add(currentLocation);
        }else if(robotID == 2){
            robot2Location.add(currentLocation);
        }
    }

    public static Point3d removeLocation(int robotID) throws Exception{

        if(robotID == 1 && let1Read)
            return robot2Location.remove();
        else if(robotID == 2 && let2Read)
            return robot1Location.remove();
        else
            throw new Exception("Other robots location queue is empty");
    }

}
