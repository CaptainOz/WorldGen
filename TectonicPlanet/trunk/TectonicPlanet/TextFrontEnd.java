package TectonicPlanet;

import java.io.*;
import java.awt.*;

/** 
 * TextFrontEnd is the main class that implements a text-based front end for
 * interacting with a {@link World} geological/tectonic simulation.
 *
 * @author Tom Groves
 * @author Nate Lillich
 */
public class TextFrontEnd implements Runnable {

    private World world=null;	// The World that we do all the work on

    // GUI bits
    private double rotation1=0, rotation2=0;
    private double zoom=6600;		// km^-1 scaling factor
    private boolean running=false, paintGuard=false, painting=false;
    private int steps=1;
    private PointViewer pointViewer=null;
    private ColorMap temperatureColorMap;

    /**
     * The <code>main</code> method is the entry-point for calls from the
     * command line. Sets up a new FrontEnd and starts the GUI.
     *
     * @param arg The array of command line arguments
     */
    public static void main( String[] arg ){
        try {
            TextFrontEnd tfe = new TextFrontEnd();
            if( arg.length == 1 )
                tfe.init( arg[0] );
            else if( arg.length == 2 )
                tfe.init( arg[0], arg[1] );
            else
                System.out.println(
                    "Usage: java TextFrontEnd <timesteps> [<filename>]"
                );
        }
        catch( Exception e ){
            e.printStackTrace( System.out );
        }
    }

    public void init( String stepCount ){
        // Create a new world and run it for stepCount steps.
        newWorld();
        steps = Integer.parseInt( stepCount );
        new Thread( this ).start();
    }

    public void init( String stepCount, String filename ){
        // Load the world saved in the given file and run it for stepCount steps.
        loadWorld( filename );
        steps = Integer.parseInt( stepCount );
        new Thread( this ).start();
    }

    @Override
    public void run(){
        // Let the user and the program know the simulation is running.
        running = true;
        System.out.print( "Timestepping..." );

        // Step the simulation as many times as requested. Lets also keep track
        // of the time so the user has some idea of what to expect.
        long time = System.currentTimeMillis();
        for( int i = 0; i < steps; ++i ){
            // Step.
            timeStep();
            Thread.yield();
            System.out.println(
                "*** Finished step " + (i + 1) + " of " + steps + " ***"
            );

            // Compute the remaining time in seconds.
            double average = (System.currentTimeMillis() - time) / (1000.0f * (i + 1));
            double remaining = average * (steps - i - 1);
            System.out.println(
                "*** Estimated time remaining: " + (remaining / 60f)
              + " minutes (" + ((int)remaining) + " seconds)\n"
            );

            // If something goes wrong, just kill it.
            // TODO: Why are we setting the steps to 1 here? Is it just to break
            //       out of the loop?
            if( Tet.goneBad ){
                steps = 1;
                break;
            }
        }

        // We're done! Celebrate.
        final long   runTime = System.currentTimeMillis() - time;
        final double runTimeMinutes = runTime / 60000.0;
        System.out.println( "Done in " + runTimeMinutes + " minutes!" );
        running = false;
        Tet.goneBad = false;
    }

    public double radiusSq( double x1, double y1, double x2, double y2 ){
        return Math.pow( x1 - x2, 2 ) + Math.pow( y1 - y2, 2 );
    }

    public void newWorld(){
        world=new World();
        // Sort out the colors
        for( int i = 0; i < world.getNumPoints(); ++i ){
            TecPoint point      = world.getPoint( i );
            final double height = point.getSurfaceHeight();
            final Color  color  = world.getColorMap().map( height );
            point.setColor( color );
        }
    }

    public void loadWorld( String filename ){
        try {
            File file = new File( filename );
            world     = new World( file.getCanonicalPath() );
        }
        catch( Exception e ){
            System.err.println( "Error loading world: " + filename );
            e.printStackTrace( System.out );
            System.exit( 1 );
        }
    }

    private void timeStep(){
        world.timeStep();
        //world.oceanTimeStep();

        // Set Colours
        for( int i = 0; i < world.getNumPoints(); i++ ){
            TecPoint point      = world.getPoint( i );
            final double height = point.getSurfaceHeight();
            final Color  color  = world.getColorMap().map( height );
            point.setColor( color );
        }

        // Save the files every 20 steps.
        final int step = world.getEpoch();
        if( step % 20 == 0 ){
            world.saveJPGsequence();
            world.save();
        }
    }
}