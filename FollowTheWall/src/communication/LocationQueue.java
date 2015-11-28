package communication;

import java.util.ArrayList;

/**
 * Created by kilsuf on 11/28/15.
 */
public class LocationQueue {

    private ArrayList<String[]> robotIDs = new ArrayList<String[]>();

    //register robot

    /**
     * Creates an id - robotName pair and returns the robot's ID
     *
     * @param robotName     - the name of the robot
     * @return              - the id of the given robot
     * @throws Exception    - if the max # of registrations have been reached
     */
    public int registerRobot(String robotName) throws Exception{
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

    //get robot ID

    /**
     * Given a robot name returns the robot's id
     *
     * @param robotName     - the robot to search for
     * @return              - the id of the given robot
     * @throws Exception    - if the robot's name was not found
     */
    public int getID(String robotName)throws Exception{

        for(int i = 0; i < robotIDs.size(); i++){
            String[] currentArray = robotIDs.get(i);

            //the robot name should be the second element in the array
            if(currentArray[1].equals(robotName)){
                int id = Integer.getInteger(currentArray[0]);

                return id;
            }

        }

        throw new Exception("the robot's name was not found, that robot name was not registered" +
                            " or spelled incorrectly");

    }

    /**
     * Write to file. Robot passes its id and message. The message is written to the file.
     *
     */
    public static void writeMessage(int id, String message){

    }


    /**
     * Read from file. Robot passes its id, returns the oldest message from the opposite robot's input.
     *
     */
    public static String readMessage(int id){
        return new String();
    }

}
