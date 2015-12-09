package environment;

import simbad.gui.Simbad;

import javax.vecmath.Vector3d;

/**
 * Created by kilsuf on 11/28/15.
 */
public class SimRunner {

    /**
     * Runs the simulation
     * @param args
     */
    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");
        // create Simbad instance with given environment
        //Simbad frame = new Simbad(new LRoom(new Vector3d(-9, 0, -3), "1"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(8, 0, -3), "2"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(0, 0, -5), "3"), false);
        //Simbad frame2 = new Simbad(new LRoom(new Vector3d(5, 0, 5), "4"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(-5, 0, -2), "5"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(8, 0, 3), "6"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(-6, 0, -7), "7"), false);
        Simbad frame = new Simbad(new LRoom(new Vector3d(6, 0, 7), "8"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(2, 0, 2), "9"), false);
        //Simbad frame = new Simbad(new LRoom(new Vector3d(-7, 0, -4), "10"), false);

    }


}
