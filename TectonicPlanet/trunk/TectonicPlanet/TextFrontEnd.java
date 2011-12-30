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
    // The World that we do all the work on
    private World m_world = null;

    // The limit to how many steps to take.
    private int m_stepLimit = 1;

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

    /**
     * Creates a new world and sets the limit to how many steps to iterate.
     *
     * @param stepCount The limit to how many steps to take.
     */
    public void init( String stepCount ){
        // Create a new world and run it for stepCount steps.
        createNewWorld();
        m_stepLimit = Integer.parseInt( stepCount );
        new Thread( this ).start();
    }

    /**
     * Loads an existing world file and sets the limit on how many steps to take.
     *
     * @param stepCount The limit to how many steps to take.
     * @param filename  The name of a file to load a world from.
     */
    public void init( String stepCount, String filename ){
        // Load the world saved in the given file and run it for stepCount steps.
        loadWorld( filename );
        m_stepLimit = Integer.parseInt( stepCount );
        new Thread( this ).start();
    }

    /**
     * Runs the simulation for as many steps as this was created to run.
     */
    @Override
    @SuppressWarnings( "CallToThreadYield" )
    public void run(){
        // Let the user and the program know the simulation is running.
        System.out.print( "Timestepping..." );

        // Step the simulation as many times as requested. Lets also keep track
        // of the time so the user has some idea of what to expect.
        long time = System.currentTimeMillis();
        for( int i = 0; i < m_stepLimit; ++i ){
            // Step.
            timeStep();
            Thread.yield();
            System.out.println(
                "*** Finished step " + (i + 1) + " of " + m_stepLimit + " ***"
            );

            // Compute the remaining time in seconds.
            double average = (System.currentTimeMillis() - time) / (1000.0f * (i + 1));
            double remaining = average * (m_stepLimit - i - 1);
            System.out.println(
                "*** Estimated time remaining: " + (remaining / 60f)
              + " minutes (" + ((int)remaining) + " seconds)\n"
            );

            // If something goes wrong, just kill it.
            // TODO: Why are we setting the steps to 1 here? Is it just to break
            //       out of the loop?
            if( Tet.goneBad ){
                m_stepLimit = 1;
                break;
            }
        }

        // We're done! Celebrate.
        final long   runTime = System.currentTimeMillis() - time;
        final double runTimeMinutes = runTime / 60000.0;
        System.out.println( "Done in " + runTimeMinutes + " minutes!" );
        Tet.goneBad = false;
    }

    /**
     * Creates a new world from scratch.
     */
    public void createNewWorld(){
        m_world = new World();

        // FIXME: This should be handled by the world class.
        for( int i = 0; i < m_world.getNumPoints(); ++i ){
            TecPoint point      = m_world.getPoint( i );
            final double height = point.getSurfaceHeight();
            final Color  color  = m_world.getColorMap().map( height );
            point.setColor( color );
        }
    }

    /**
     * Loads a world from a file.
     *
     * @param filename The name of the file to load the world from.
     */
    public void loadWorld( String filename ){
        try {
            File file = new File( filename );
            m_world   = new World( file.getCanonicalPath() );
        }
        catch( Exception e ){
            System.err.println( "Error loading world: " + filename );
            e.printStackTrace( System.out );
            System.exit( 1 );
        }
    }

    /**
     * Moves the simulation forward by one unit of time.
     */
    private void timeStep(){
        m_world.timeStep();
        //world.oceanTimeStep();

        // Set Colours
        for( int i = 0; i < m_world.getNumPoints(); i++ ){
            TecPoint point      = m_world.getPoint( i );
            final double height = point.getSurfaceHeight();
            final Color  color  = m_world.getColorMap().map( height );
            point.setColor( color );
        }

        // Save the files every 20 steps.
        final int step = m_world.getEpoch();
        if( step % 20 == 0 ){
            m_world.saveJPGsequence();
            m_world.save();
        }
    }
}