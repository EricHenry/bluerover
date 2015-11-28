package environment;

import simbad.gui.Simbad;

import javax.vecmath.Vector3d;

/**
 * Created by kilsuf on 11/28/15.
 */
public class SimRunner {

    public static void main(String[] args) {
        // request antialising
        System.setProperty("j3d.implicitAntialiasing", "true");
        // create Simbad instance with given environment
        //Simbad frame = new Simbad(new MowerEnvironment(new Vector3d(2, 0, 2), 2, "2, 0, 2"), false);
        //Simbad frame2 = new Simbad(new MowerEnvironment(new Vector3d(6, 0, 7), 2, "6, 0, 7"), false);


        //Simbad frame = new Simbad(new MowerEnvironment())
    }


}
