package TectonicPlanet;

// Standard Java imports
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// Java3d imports
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.media.j3d.Transform3D;

/**
 * World is the main class that implements a geological/tectonic simulation.
 * 
 * @author Tom Groves
 * @author Nate Lillich
 */
public class World {
    // Physics bits

    private ArrayList m_points = null;  // The points of the surface
    private ArrayList m_plates = null;  // The plates of the surface
    private ArrayList m_tets = null;    // The tetrahedrons of the surface
    private int m_epoch = 0;            // "Date". Basically, how many cycles have been run.
    private double m_planetRadius = 6400.0, m_planetSurfaceArea;
    private int m_pointSpacing = 360;   // The default distance between surface points
    private TecPoint m_planetCenter = null;
    private Vector3d m_omega = null;    // Speed of planet rotation
    // Delaunay bits
    private int m_gridSize;
    private HashSet[][][] m_gridBox;
    private LinkSystem m_linkSystem;
    // Mantle flow arrows
    private double[] m_mantleFlowStrength;
    private Point3d[] m_mantlePoint;
    private int m_numMantlePoints;
    // Map saving bits
    private BufferedImage m_imageBuffer;
    private int m_imageSequenceNumber = -1;
    private ImageSettings m_imageSettings;
    private ColorMap m_colorMap;
    private boolean m_altered = false;
    private File m_saveFile = null;
    // Tet gridbox bits (for fast fluids)
    private int m_tetGridSize;
    private HashSet[][][] m_tetGridBox;

    /**
     * Default constructor - makes a new World using default settings.
     */
    public World(){
        m_numMantlePoints = 10;
        int numPlates = 10;
        initStorage();
        initColors();
        _initPoints( numPlates );
    }

    /**
     * Loads the World from the given file.
     * 
     * @param file  The filename of the file to load the World from
     */
    public World( String file ){
        load( file );
        initColors();
    }

    /**
     * Builds a new World using the given parameters.
     *
     * @param mantlePoints  The number of mantle up-welling points to make
     * @param numPlates     The number of tectonic plates to make
     */
    public World( int mantlePoints, int numPlates ){
        this.m_numMantlePoints = mantlePoints;
        initStorage();
        initColors();
        _initPoints( numPlates );
    }

    /**
     * Initializes the various arrays used to store the data about the world.
     */
    private void initStorage(){
        m_points = new ArrayList();
        m_plates = new ArrayList();
        m_mantlePoint   = new Point3d[ m_numMantlePoints ];
        m_linkSystem    = new LinkSystem();
        m_imageSettings = new ImageSettings();
        m_mantleFlowStrength = new double[ m_numMantlePoints ];
    }

    /**
     * Initializes the default color map.
     */
    private void initColors(){
        // Defines the colour ranges used for displaying/saving images
        m_colorMap = new ColorMap();

        // TODO: Figure out why these are commented out. Is this some debugging
        //       code?
        /*colorMap.add(-3.6f, Color.black);
        colorMap.add(-0.1f, Color.blue);
        colorMap.add(0.0f, Color.green);
        colorMap.add(1.4f, Color.yellow);
        colorMap.add(9.4f, Color.orange);
        colorMap.add(15.4f, Color.red);*/
        /*colorMap.add(-3.6f, Color.black);
        colorMap.add(-0.1f, Color.blue);
        colorMap.add(0f, new Color(0.305882f, 0.698039f, 1.000000f));
        colorMap.add(00.19608f, new Color(0f, 0.501961f, 0.121569f));
        colorMap.add(01.17647f, new Color(0.184314f, 0.569078f, 0.254902f));
        colorMap.add(02.15686f, new Color(0.482353f, 0.729412f, 0.539216f));
        colorMap.add(03.13725f, new Color(0.662745f, 0.662745f, 0.776471f));
        colorMap.add(04.11765f, new Color(0.745098f, 0.717647f, 0.698039f));
        colorMap.add(05.09804f, new Color(0.890196f, 0.784314f, 0.678431f));
        colorMap.add(06.07843f, new Color(0.984314f, 0.792157f, 0.513725f));
        colorMap.add(07.05882f, new Color(0.850980f, 0.615686f, 0.415686f));
        colorMap.add(08.03922f, new Color(1.000000f, 0.800000f, 0.666667f));
        colorMap.add(09.01961f, new Color(1.000000f, 0.929412f, 0.815686f));
        colorMap.add(10.00000f, new Color(1.000000f, 1.000000f, 0.870588f));
        colorMap.add(15.4f, Color.red);*/
        m_colorMap.add( -5.6f, Color.black );
        //colorMap.add(-1f, new Color(20,20,120));
        m_colorMap.add( -0.01f, Color.blue );
        m_colorMap.add( 0, new Color( 50, 70, 25 ) );
        m_colorMap.add( 1.5f, new Color( 75, 155, 74 ) );
        m_colorMap.add( 2.5f, new Color( 125, 190, 80 ) );
        m_colorMap.add( 4, new Color( 175, 140, 100 ) );
        m_colorMap.add( 7, new Color( 213, 185, 135 ) );
        m_colorMap.add( 10, new Color( 255, 255, 255 ) );
    }

    /**
     * Creates the points that define the shape of the tectonic plates.
     *
     * @param numPlates The number of tectonic plates to create points for.
     */
    private void _initPoints( int numPlates ){
        // Build a new planet
        Tet.planetRadius = m_planetRadius;

        // First place numPlates random plates
        for( int i = 0; i < numPlates; ++i ){
            Vector3d vector3d = getRandomVector();
            vector3d.scale( m_planetRadius );
            TecPlate plate = new TecPlate( vector3d.x, vector3d.y, vector3d.z );
            plate.densityTweak = Math.random() * 0.15;
            addPlate( plate );
        }

        // The totalLatCount is the number of latitude steps there are from the
        // south pole up to the north pole.
        double dblPointSpace = (double)(m_pointSpacing * 2);
        double planetCircum  = 2 * Math.PI * m_planetRadius;
        double totalLatCount = (int)(planetCircum / dblPointSpace);

        // Figure out the location of each latitude line and calculate all the.
        // points along it.
        System.out.println( "Creating latitude points." );
        for( int latCount = 0; latCount <= totalLatCount; ++latCount ){
            // Calculate the latitude angle of offset from the equator and its
            // circumference.
            double latAngle  = -90.0 + (double)latCount / totalLatCount * 180.0;
            double latRadius = Math.cos( Math.toRadians( latAngle ) ) * m_planetRadius;
            double latCircum = (2 * Math.PI * latRadius);
            _calcLatPoints( latCircum, latAngle );
        }

        // Make some of the plates continental, the others will be ocean and
        // then re-center the plates.
        System.out.println( "Adjusting plate types." );
        for( int i = 0; i < numPlates; ++i ){
            boolean isOcean = Math.random() < 0.75;
            TecPlate plate  = getPlate( i );
            _setPlateType( plate, isOcean );
            plate.center();
        }

        // The rotation vector of the planet (for Coriolis Effect) in
        // radians/sec, should be 0.000072722
        // FIXME: This is currently unused.
        m_omega = new Vector3d( 0.0, 0.072722, 1.0E-13 );

        // Initialise the gridBox system
        System.out.println( "Allocating grid boxes." );
        initGridBoxSystems();

        // Delaunay the whole planet (for the first time)
        System.out.println( "Delaunaying planet." );
        Tet.biggestError = 0.0;
        delaunay();

        // Stick all the tets into the tetGridBox system
        for( int i = 0; i < m_tets.size(); ++i )
            gridBoxAdd( (Tet)m_tets.get( i ) );

        // Mantle upwelling points
        System.out.println( "Randomizing mantle upwellings." );
        randomiseUpwellings();

        // And done.
        System.out.println( "Initialized." );
        m_altered = true;
    }

    /**
     * Places the mantel upwellings in random locations.
     */
    public void randomiseUpwellings(){
        for( int i = 0; i < m_numMantlePoints; ++i ){
            // Shuffle the upwelling point randomly, until it _isn't_ too close
            // to the other points.
            double closestUpwelling;
            do {
                Vector3d pointPos = getRandomVector();
                pointPos.scale( m_planetRadius );
                Point3d pointI   = new Point3d( pointPos.x, pointPos.y, pointPos.z );
                m_mantlePoint[i] = pointI;

                // Check for other points close by
                closestUpwelling = m_planetRadius * 3; // Impossibly far.
                for( int k = 0; k < i; ++k ){
                    Point3d pointK  = m_mantlePoint[k];
                    double distance = pointK.distance( pointI );
                    if( distance < closestUpwelling ){
                        closestUpwelling = distance;
                    }
                }
            } while( closestUpwelling < 3000.0 );
        }
    }

    /**
     * Adjusts the positions of mantle upwellings such that they are more likely
     * to cause supercontinents to break apart.
     */
    public void breakUpSuperContinents(){
        // Move the mantle upwellings and try to place them underneath major
        // landmasses to initiate breakup of any supercontinents
        for( int i = 0; i < m_numMantlePoints; ++i ){
            System.out.println( "Moving mantle point " + (i + 1) + " of " + m_numMantlePoints );

            // Shuffle the upwelling point randomly, until it _isn't_ too close
            // to the other points
            boolean ok = true;
            double closestUpwelling;
            double volume;
            do {
                ok = true;
                Vector3d pointPos = getRandomVector();
                pointPos.scale( m_planetRadius );
                Point3d pointI   = new Point3d( pointPos.x, pointPos.y, pointPos.z );
                m_mantlePoint[i] = pointI;

                // Check for other points close by
                closestUpwelling = m_planetRadius * 3; // Impossibly far.
                for( int k = 0; k < i; ++k ){
                    Point3d pointK  = m_mantlePoint[k];
                    double  distance = pointK.distance( pointI );
                    if( distance < closestUpwelling ){
                        closestUpwelling = distance;
                    }
                }
                
                // Check for the amount of deep rock overhead
                volume = 0;
                for( int p = 0; p < m_points.size(); ++p ){
                    TecPoint tp = getPoint( p );
                    double distance = pointI.distance( tp.getPos() );
                    if( distance < 3000 ){
                        double area  = tp.getArea();
                        double depth = tp.getDepth();
                        volume += area * depth * (3000 - distance) / 3000;
                    }
                }

                // Rough estimate of the average depth of rock (ocean floor is
                // ~7km thick).
                // Make sure we don't get stuck forever on a young world
                // TODO: What units is this in? Millimeters?
                volume /= 10000000;
                if( Math.random() < Math.pow( 8 / volume, 2 ) && Math.random() < 0.7 )
                    ok = false;
            } while( !ok && closestUpwelling < 3000.0 );
        }
        System.out.println( "Done adjusting mantle points." );
    }

    /**
     * Initialize the two grid box systems.
     */
    public void initGridBoxSystems(){
        // Allocate the normal gridBox.
        m_gridSize = (int)Math.ceil( m_planetRadius / (m_pointSpacing * 1.1) - 1 );
        m_gridBox  = new HashSet[m_gridSize][m_gridSize][m_gridSize];
        for( int z = 0; z < m_gridSize; ++z ){
            for( int y = 0; y < m_gridSize; ++y ){
                for( int x = 0; x < m_gridSize; ++x ){
                    m_gridBox[z][y][x] = null;
                }
            }
        }

        // Initialise the tetGridBox system
        m_tetGridSize = 15;
        m_tetGridBox  = new HashSet[m_tetGridSize][m_tetGridSize][m_tetGridSize];
        for( int z = 0; z < m_tetGridSize; z++ ){
            for( int y = 0; y < m_tetGridSize; y++ ){
                for( int x = 0; x < m_tetGridSize; x++ )
                    m_tetGridBox[z][y][x] = null;
            }
        }
    }

    /**
     * Loads a World from the given file.
     *
     * @param filename  The filename of the file to load the World from.
     */
    final public void load( String filename ){
        // The world files are plain binary files with the data laid out as
        // follows:
        //
        // epoch           (int)
        // planetRadius    (double)
        // pointSpacing    (int)
        // numMantlePoints (int)
        // mantle data
        //   mantleFlowStrength (double)
        //   mantlePointX       (double)
        //   mantlePointY       (double)
        //   mantlePointZ       (double)
        // numColorPoints  (int)
        // color data
        //   value              (double)
        //   rgb                (int)
        // numPlates       (int)
        // numPoints       (int)
        // tec point data
        //   pointX             (double)
        //   pointY             (double)
        //   pointZ             (double)
        //   pointDepth         (int)
        //   pointSize          (double)
        //   plateId            (int)
        //   magmaDensity       (double)
        //   baseDepthOffset    (double)
        //   rockThickness      (double)
        //   density            (double)

        System.out.println( "Loading file " + filename + "..." );
        try {
            int numPlates = _readFile( filename );
            m_altered = false;

            // Now sort out the bare planet we just loaded...
            Tet.planetRadius = m_planetRadius;
            m_linkSystem     = new LinkSystem();
            m_imageSettings  = new ImageSettings();
            System.out.println( "Centering plates." );
            for( int i = 0; i < numPlates; ++i ){
                getPlate( i ).center();
            }

            // Init the gridBox system
            System.out.println( "Allocating grid boxes." );
            initGridBoxSystems();

            // Delaunay the whole planet
            System.out.println( "Delaunaying planet." );
            Tet.biggestError = 0.0;
            m_tets = null;
            delaunay();
            resetTetGridSystem();
            pourOnWater();

            // Set Colors
            for( int i = 0; i < getNumPoints(); ++i ){
                TecPoint point  = getPoint( i );
                double   height = point.getSurfaceHeight();
                point.setColor( m_colorMap.map( height - TecPoint.seaLevel ) );
            }

            // Store interpolated colors in the links.
            Color c1, c2;
            ArrayList tempVec = new ArrayList( getLinkSystem().getCollection() );
            for( int i = 0; i < tempVec.size(); ++i ){
                LinkPair lp = (LinkPair)tempVec.get( i );
                c1 = lp.getA().getColor();
                c2 = lp.getB().getColor();
                lp.col = new Color( (c1.getRed() + c2.getRed()) / 2, (c1.getGreen() + c2.getGreen()) / 2, (c1.getBlue() + c2.getBlue()) / 2 );
            }
        }
        catch( Exception exception ){
            System.out.println( "Error while loading the world - " + exception );
            exception.printStackTrace( System.out );
        }
    }

    /**
     * Saves the world to file.
     */
    public void save(){
        System.out.print( "Saving..." );
        if( m_saveFile == null ){
            _openNewSaveFile();
        }

        try {
            // Back up the existing save file.
            if( m_saveFile.exists() ){
                // Remove the backup, if it exists
                File backup = new File( m_saveFile.getAbsolutePath() + ".bak" );
                if( backup.exists() ){
                    backup.delete();
                }
                m_saveFile.renameTo( backup );
            }

            // Now write the file.
            _writeFile();

        }
        catch( Exception e ){
            System.out.println( "Error saving the world - " + e.toString() );
            e.printStackTrace( System.out );
        }
        System.out.println( "done!" );
    }
    
    /**
     * Detects if this world has been altered since its last save.
     *
     * @return True if it has been altered.
     */
    public boolean isAltered(){
        return m_altered;
    }

    /**
     * Adds a single tectonic point to the world.
     *
     * @param tecpoint The point to add.
     */
    public void addPoint( TecPoint tecpoint ){
        m_points.add( tecpoint );
    }

    /**
     * Retrieves a single point specified by ID.
     *
     * @param id The ID of the point to retrieve.
     *
     * @return The retrieved point.
     */
    public TecPoint getPoint( int id ){
        return (TecPoint)m_points.get( id );
    }

    /**
     * Grants access to the whole array of TecPoints in the world.
     *
     * @return The World's TecPoint array.
     */
    public ArrayList getPoints(){
        return m_points;
    }

    /**
     * Grants access to the whole array of Tets in the world.
     *
     * TODO: What is a Tet!?
     *
     * @return The World's Tets array.
     */
    public ArrayList getTets(){
        return m_tets;
    }

    /**
     * Fetches the number of points in the world.
     *
     * @return How many points are known to the world.
     */
    public int getNumPoints(){
        return m_points.size();
    }

    /**
     * Fetches the planet's radius.
     *
     * @return The radius of the World.
     */
    public double getPlanetRadius(){
        return m_planetRadius;
    }

    /**
     * Fetches the location of the planet center.
     *
     * @return The coordinates of the World's center.
     */
    public TecPoint getCenterOfPlanet(){
        return m_planetCenter;
    }

    // Delaunay the whole planet for the first time!
    public void delaunay(){
        long time = System.currentTimeMillis();

        // Clear all the gridboxes
        for( int i = 0; i < m_gridSize; i++ )
            for( int j = 0; j < m_gridSize; j++ )
                for( int k = 0; k < m_gridSize; k++ )
                    if( m_gridBox[i][j][k] != null )
                        m_gridBox[i][j][k].clear();

        // Put all the points in the gridBoxes
        for( int i = 0; i < m_points.size(); i++ )
            gridBoxAdd( (TecPoint)m_points.get( i ) );

        // Make sure there's a point at the center of the planet. We use it for the tets
        if( m_planetCenter == null )
            m_planetCenter = new TecPoint( 0.0, 0.0, 0.0, m_epoch );

        // If this isn't the first time we've delaunay'd the whole planet, find a suitable starting tet...
        ArrayList activeTets = new ArrayList( 2000 );
        if( m_tets != null ){
            activeTets = new ArrayList( m_tets.size() );
            boolean goodTet;
            Tet startTet;
            // Make random tets, then check if they're ok
            do {
                goodTet = true;
                startTet = (Tet)m_tets.get( (int)(Math.random() * (double)m_tets.size()) );
                // Check if tet is ok
                for( int i = 0; i < m_points.size() && goodTet; i++ ){
                    if( startTet.contains( getPoint( i ).getPos() ) )
                        goodTet = false;
                }
            } while( !goodTet );
            // Found a good tet, start a new list based on it
            m_tets = new ArrayList();
            m_linkSystem = new LinkSystem();
            m_tets.add( startTet );
            m_linkSystem.addLink( startTet.b, startTet.c );
            m_linkSystem.addLink( startTet.b, startTet.d );
            m_linkSystem.addLink( startTet.c, startTet.d );
            activeTets.add( startTet );
        }
        else {
            // First time Delaunay
            // Reset the list of tets
            m_tets = new ArrayList();
            m_linkSystem = new LinkSystem();

            // Try to make the first tet from the known pole points...
            Tet tet1 = new Tet( m_planetCenter, getPoint( 0 ), getPoint( 1 ), getPoint( 2 ) );
            tet1.calc();
            // Check if it's ok
            System.out.print( "Checking first tet..." );
            boolean ok = true;
            for( int i = 3; i < m_points.size(); i++ ){
                if( ok && tet1.contains( getPoint( i ).getPos() ) )
                    ok = false;
            }
            if( ok ){
                // Cool, we've got our first tet
                m_tets.add( tet1 );
                m_linkSystem.addLink( tet1.b, tet1.c );
                m_linkSystem.addLink( tet1.b, tet1.d );
                m_linkSystem.addLink( tet1.c, tet1.d );
                activeTets.add( tet1 );
                System.out.println( "ok!" );
            }
            else {
                // Nope, we'll have to use brute force
                System.out.println( "bad :(" );
                System.out.println( "Using brute force to find first tet..." );
                Object object = null;
                do {
                    ok = true;
                    ArrayList nearby;
                    do {
                        nearby = new ArrayList( pointsSurrounding( getPoint( (int)(Math.random() * (double)(m_points.size() - 1)) ).getPos() ) );
                    } while( nearby == null || nearby.size() < 4 );
                    // OK, we've found some points, now pick 3 (plus centerOfPlanet) and make a tetrahedron from them
                    int p1, p2, p3;
                    p1 = (int)(Math.random() * nearby.size());
                    do {
                        p2 = (int)(Math.random() * nearby.size());
                    } while( p2 == p1 );
                    do {
                        p3 = (int)(Math.random() * nearby.size());
                    } while( p3 == p1 && p3 == p2 );
                    tet1 = new Tet( m_planetCenter, (TecPoint)nearby.get( p1 ),
                                    (TecPoint)nearby.get( p2 ),
                                    (TecPoint)nearby.get( p3 ) );
                    tet1.calc();
                    for( int i_56_ = 0; i_56_ < m_points.size(); i_56_++ ){
                        if( ok
                                && !getPoint( i_56_ ).equals( (TecPoint)nearby.get( p1 ) )
                                && !getPoint( i_56_ ).equals( (TecPoint)nearby.get( p2 ) )
                                && !getPoint( i_56_ ).equals( (TecPoint)nearby.get( p3 ) )
                                && tet1.contains( getPoint( i_56_ ).getPos() ) )
                            ok = false;
                    }
                } while( !ok );
                // Cool, we've got our first tet (by brute force!)
                m_tets.add( tet1 );
                m_linkSystem.addLink( tet1.b, tet1.c );
                m_linkSystem.addLink( tet1.b, tet1.d );
                m_linkSystem.addLink( tet1.c, tet1.d );
                activeTets.add( tet1 );
                System.out.println( tet1.b + "   " + tet1.c + "   " + tet1.d );
            }
        }

        System.out.print( "Doing Delaunay tets" );
        Tet t1 = null;
        Tet t2 = null;
        Tet t3 = null;
        int tetNum = 0;
        Iterator iter, iter2;
        while( activeTets.size() > 0 && tetNum < activeTets.size() ){
            Tet tet = (Tet)activeTets.get( tetNum );//(Tet) activeTets.get( (int) (Math.random() * (double) activeTets.size()));//
            if( Math.random() < 0.01 )
                System.out.print( "." );
            t1 = null;
            t2 = null;
            t3 = null;

            // Take the centre point and two of the surface points as the start of a new tet

            // Can we use link tet.b<->tet.c?
            if( m_linkSystem.getCount( tet.b, tet.c ) <= 1 ){
                HashSet nearby = pointsSurrounding( tet.b.getPos(), tet.c.getPos() );
                iter = nearby.iterator();
                while( iter.hasNext() && t1 == null ){
                    //for (int i = 0; t1 == null && i < nearby.size(); i++) {
                    TecPoint tecpoint = (TecPoint)iter.next();//nearby.get(i);
                    if( !tecpoint.equals( tet.a ) && !tecpoint.equals( tet.b ) && !tecpoint.equals( tet.c ) && !tecpoint.equals( tet.d ) ){
                        Tet tet_63_ = new Tet( m_planetCenter, tet.b, tet.c, tecpoint );
                        tet_63_.calc();
                        // Now check against all nearby points
                        boolean ok = true;
                        iter2 = nearby.iterator();
                        while( ok && iter2.hasNext() ){
                            //for (int i_64_ = 0; ok && i_64_ < nearby.size(); i_64_++) {
                            TecPoint tecpoint_65_ = (TecPoint)iter2.next();//nearby.get(i_64_);
                            if( tecpoint != tecpoint_65_ && tecpoint_65_ != tet.b && tecpoint_65_ != tet.c && tet_63_.contains( tecpoint_65_.getPos() ) )
                                ok = false;
                        }
                        if( ok ){
                            t1 = tet_63_;
                            m_tets.add( t1 );
                            activeTets.add( t1 );
                            m_linkSystem.addLink( t1.b, t1.c );
                            m_linkSystem.addLink( t1.b, t1.d );
                            m_linkSystem.addLink( t1.c, t1.d );
                        }
                    }
                }
            }
            // Can we use link tet.c<->tet.d?
            if( m_linkSystem.getCount( tet.c, tet.d ) <= 1 ){
                HashSet nearby = pointsSurrounding( tet.c.getPos(), tet.d.getPos() );
                iter = nearby.iterator();
                while( iter.hasNext() && t2 == null ){
                    //for (int i = 0; t2 == null && i < nearby.size(); i++) {
                    TecPoint tecpoint = (TecPoint)iter.next();//nearby.get(i);
                    if( !tecpoint.equals( tet.a ) && !tecpoint.equals( tet.b ) && !tecpoint.equals( tet.c ) && !tecpoint.equals( tet.d ) ){
                        Tet tet_67_ = new Tet( m_planetCenter, tet.c, tet.d, tecpoint );
                        tet_67_.calc();
                        // Now check against all nearby points
                        boolean ok = true;
                        iter2 = nearby.iterator();
                        while( ok && iter2.hasNext() ){
                            //for (int i_68_ = 0; ok && i_68_ < nearby.size(); i_68_++) {
                            TecPoint tecpoint_69_ = (TecPoint)iter2.next();//nearby.get(i_68_);
                            if( tecpoint != tecpoint_69_ && tecpoint_69_ != tet.c && tecpoint_69_ != tet.d && tet_67_.contains( tecpoint_69_.getPos() ) )
                                ok = false;
                        }
                        if( ok ){
                            t2 = tet_67_;
                            m_tets.add( t2 );
                            activeTets.add( t2 );
                            m_linkSystem.addLink( t2.b, t2.c );
                            m_linkSystem.addLink( t2.b, t2.d );
                            m_linkSystem.addLink( t2.c, t2.d );
                        }
                    }
                }
            }
            // Can we use link tet.b<->tet.d?
            if( m_linkSystem.getCount( tet.b, tet.d ) <= 1 ){
                HashSet nearby = pointsSurrounding( tet.b.getPos(), tet.d.getPos() );
                iter = nearby.iterator();
                while( iter.hasNext() && t3 == null ){
                    //for (int i = 0; t3 == null && i < vector_70_.size(); i++) {
                    TecPoint tecpoint = (TecPoint)iter.next();//vector_70_.get(i);
                    if( !tecpoint.equals( tet.a ) && !tecpoint.equals( tet.b ) && !tecpoint.equals( tet.c ) && !tecpoint.equals( tet.d ) ){
                        Tet tet_71_ = new Tet( m_planetCenter, tet.b, tet.d, tecpoint );
                        tet_71_.calc();
                        // Now check against all nearby points
                        boolean ok = true;
                        iter2 = nearby.iterator();
                        while( ok && iter2.hasNext() ){
                            //for (int i_72_ = 0; ok && i_72_ < vector_70_.size(); i_72_++) {
                            TecPoint tecpoint_73_ = (TecPoint)iter2.next();//vector_70_.get(i_72_);
                            if( tecpoint != tecpoint_73_ && tecpoint_73_ != tet.b && tecpoint_73_ != tet.d && tet_71_.contains( tecpoint_73_.getPos() ) )
                                ok = false;
                        }
                        if( ok ){
                            t3 = tet_71_;
                            m_tets.add( t3 );
                            activeTets.add( t3 );
                            m_linkSystem.addLink( t3.b, t3.c );
                            m_linkSystem.addLink( t3.b, t3.d );
                            m_linkSystem.addLink( t3.c, t3.d );
                        }
                    }
                }
            }
            if( m_linkSystem.getCount( tet.b, tet.c ) >= 2 && m_linkSystem.getCount( tet.b, tet.d ) >= 2 && m_linkSystem.getCount( tet.c, tet.d ) >= 2 )
                tetNum++;//activeTets.remove(tet);
            else {
                System.out.println( "Completely failed to expand from tet." );
                System.exit( 1 );
            }
        }
        System.out.println( "done! In " + (System.currentTimeMillis() - time) / 1000.0f + " seconds" );

        // Calc the areas of all points
        calculateAreas();
        pourOnWater();

        // Set Colours
        for( int i_39_ = 0; i_39_ < getNumPoints(); i_39_++ )
            getPoint( i_39_ ).setColor( m_colorMap.map( getPoint( i_39_ ).getSurfaceHeight() - TecPoint.seaLevel ) );
        // store interpolated colours in the links
        Color c1, c2;
        ArrayList tempVec = new ArrayList( getLinkSystem().getCollection() );
        for( int i = 0; i < tempVec.size(); i++ ){
            LinkPair lp = (LinkPair)tempVec.get( i );
            c1 = lp.getA().getColor();
            c2 = lp.getB().getColor();
            lp.col = new Color( (c1.getRed() + c2.getRed()) / 2, (c1.getGreen() + c2.getGreen()) / 2, (c1.getBlue() + c2.getBlue()) / 2 );
        }

        System.out.println( "Whole planet Delaunay in " + (System.currentTimeMillis() - time) / 60000.0f + " minutes! (" + (System.currentTimeMillis() - time) / 1000.0f + " seconds)" );
    }

    public void addPlate( TecPlate tecplate ){
        m_plates.add( tecplate );
    }

    public int getPlateNum( TecPlate tp ){
        for( int i = 0; i < m_plates.size(); i++ )
            if( tp.equals( m_plates.get( i ) ) )
                return i;
        return -1;
    }

    public TecPlate getPlate( int i ){
        return (TecPlate)m_plates.get( i );
    }

    public int getNumPlates(){
        return m_plates.size();
    }

    public Vector3d getMantleFlow( Point3d point3d ){
        return getMantleFlow( point3d.x, point3d.y, point3d.z );
    }

    public Vector3d getMantleFlow( double x, double y, double z ){
        // Find the closest and second-closest upwelling points
        Point3d point3d = new Point3d( x, y, z );
        int cp = -1;
        int scp = -1;
        double scd;
        double cd = scd = Math.pow( m_planetRadius * 2.0 + 100.0, 2.0 );
        for( int i_80_ = 0; i_80_ < m_numMantlePoints; i_80_++ ){
            double dist = m_mantlePoint[i_80_].distanceSquared( point3d );
            if( dist <= cd ){
                // Move cd down to scd
                scd = cd;
                scp = cp;
                cd = dist;
                cp = i_80_;
            }
            else if( dist < scd ){
                scd = dist;
                scp = i_80_;
            }
        }
        if( cp == -1 || scp == -1 ){
            System.out.println( "Error in getMantleFlow: cp=" + cp + ", scp=" + scp );
            System.out.println( "Failed to find closest mantlePoints for point=(" + x + ", " + y + ", " + z + ")" );
            new Exception().printStackTrace( System.out );
            System.exit( 1 );
        }
        double ratio = cd / (cd + scd);
        double speed;
        if( ratio <= 0.1 )
            speed = ratio * 10.0;
        else
            speed = (0.4 - (ratio - 0.1)) / 0.4;
        Vector3d out = new Vector3d( x - m_mantlePoint[cp].x, y - m_mantlePoint[cp].y, z - m_mantlePoint[cp].z );
        out.normalize();
        out.scale( speed * 100 );
        return out;
    }

    // GridBox system
    private int getGridBoxX( TecPoint tecpoint ){
        return getGridBoxX( tecpoint.getX() );
    }

    private int getGridBoxX( double d ){
        return (int)Math.round( (d + m_planetRadius) * (double)(m_gridSize - 1) / (m_planetRadius * 2.0) );
    }

    private int getGridBoxY( TecPoint tecpoint ){
        return getGridBoxY( tecpoint.getY() );
    }

    private int getGridBoxY( double d ){
        return (int)Math.round( (d + m_planetRadius) * (double)(m_gridSize - 1) / (m_planetRadius * 2.0) );
    }

    private int getGridBoxZ( TecPoint tecpoint ){
        return getGridBoxZ( tecpoint.getZ() );
    }

    private int getGridBoxZ( double d ){
        return (int)Math.round( (d + m_planetRadius) * (double)(m_gridSize - 1) / (m_planetRadius * 2.0) );
    }

    public void gridBoxAdd( TecPoint tecpoint ){
        int x = getGridBoxX( tecpoint );
        int y = getGridBoxY( tecpoint );
        int z = getGridBoxZ( tecpoint );
        try {
            if( m_gridBox[x][y][z] == null )
                m_gridBox[x][y][z] = new HashSet( 100 );
            m_gridBox[x][y][z].add( tecpoint );
        }
        catch( Exception e ){
            System.out.println( "Problem adding to gridBox[" + x + "][" + y + "][" + z + "] :" + e );
            e.printStackTrace( System.out );
            System.exit( 1 );
        }
    }

    private HashSet getGridBox( Point3d p ){
        int x = getGridBoxX( p.x );
        int y = getGridBoxY( p.y );
        int z = getGridBoxZ( p.z );
        return m_gridBox[x][y][z];
    }

    private void gridBoxRemove( TecPoint p ){
        int x = getGridBoxX( p );
        int y = getGridBoxY( p );
        int z = getGridBoxZ( p );
        if( m_gridBox[x][y][z] != null )
            m_gridBox[x][y][z].remove( p );
    }

    // The "pointsSurrounding" methods return a vector of points: those found in the gridbox containing the input point, and the gridboxes surrounding it
    private HashSet pointsSurrounding( Point3d point3d ){
        return pointsSurrounding( getGridBoxX( point3d.x ), getGridBoxX( point3d.y ), getGridBoxX( point3d.z ) );
    }

    private HashSet pointsSurrounding( int x, int y, int z ){
        HashSet out = new HashSet( 100 );
        for( int i = -1; i <= 1; i++ )
            for( int j = -1; j <= 1; j++ )
                for( int k = -1; k <= 1; k++ )
                    if( x + i >= 0 && x + i < m_gridSize && y + j >= 0 && y + j < m_gridSize && z + k >= 0 && z + k < m_gridSize && m_gridBox[x + i][y + j][z + k] != null )
                        out.addAll( m_gridBox[x + i][y + j][z + k] );
        return out;
    }

    private HashSet pointsSurrounding( Point3d p1, Point3d p2 ){
        HashSet out = new HashSet( 100 );

        int x1 = getGridBoxX( p1.x );
        int y1 = getGridBoxY( p1.y );
        int z1 = getGridBoxZ( p1.z );
        int x2 = getGridBoxX( p2.x );
        int y2 = getGridBoxY( p2.y );
        int z2 = getGridBoxZ( p2.z );

        for( int i = Math.min( x1, x2 ) - 1; i <= Math.max( x1, x2 ) + 1; i++ )
            for( int j = Math.min( y1, y2 ) - 1; j <= Math.max( y1, y2 ) + 1; j++ )
                for( int k = Math.min( z1, z2 ) - 1; k <= Math.max( z1, z2 ) + 1; k++ )
                    if( i >= 0 && i < m_gridSize && j >= 0 && j < m_gridSize && k >= 0 && k < m_gridSize && m_gridBox[i][j][k] != null )
                        out.addAll( m_gridBox[i][j][k] );
        return out;
    }

    private HashSet pointsSurrounding( Point3d p1, Point3d p2, Point3d p3 ){
        HashSet out = new HashSet( 100 );

        int x1 = getGridBoxX( p1.x );
        int y1 = getGridBoxY( p1.y );
        int z1 = getGridBoxZ( p1.z );
        int x2 = getGridBoxX( p2.x );
        int y2 = getGridBoxY( p2.y );
        int z2 = getGridBoxZ( p2.z );
        int x3 = getGridBoxX( p3.x );
        int y3 = getGridBoxY( p3.y );
        int z3 = getGridBoxZ( p3.z );

        for( int i = Math.min( x3, Math.min( x1, x2 ) ) - 1; i <= Math.max( x3, Math.max( x1, x2 ) ) + 1; i++ )
            for( int j = Math.min( y3, Math.min( y1, y2 ) ) - 1; j <= Math.max( y3, Math.max( y1, y2 ) ) + 1; j++ )
                for( int k = Math.min( z3, Math.min( z1, z2 ) ) - 1; k <= Math.max( z3, Math.max( z1, z2 ) ) + 1; k++ )
                    if( i >= 0 && i < m_gridSize && j >= 0 && j < m_gridSize && k >= 0 && k < m_gridSize && m_gridBox[i][j][k] != null )
                        out.addAll( m_gridBox[i][j][k] );
        return out;
    }

    private boolean checkIfValid( Tet tet ){
        // Work out which gridboxes overlap the circumcircle of this tet,
        // and check if any of the points in them are inside the circumcircle.
        // If any is, the tet is invalid.
        double ccRadius = Math.sqrt( tet.radiussq );
        int minx = getGridBoxX( tet.center.x - ccRadius );
        int miny = getGridBoxY( tet.center.y - ccRadius );
        int minz = getGridBoxZ( tet.center.z - ccRadius );
        int maxx = getGridBoxX( tet.center.x + ccRadius );
        int maxy = getGridBoxY( tet.center.y + ccRadius );
        int maxz = getGridBoxZ( tet.center.z + ccRadius );
        TecPoint checkpoint;
        Iterator iter;
        for( int i = minx; i <= maxx; i++ )
            for( int j = miny; j <= maxy; j++ )
                for( int k = minz; k <= maxz; k++ )
                    if( i >= 0 && i < m_gridSize && j >= 0 && j < m_gridSize && k >= 0 && k < m_gridSize && m_gridBox[i][j][k] != null ){
                        iter = m_gridBox[i][j][k].iterator();
                        while( iter.hasNext() ){
                            checkpoint = (TecPoint)iter.next();
                            if( !tet.uses( checkpoint ) && tet.contains( checkpoint.getPos() ) )
                                return false; // Tet is invalid, so we can stop checking now
                        }
                    }
        return true;  // Checked all possible invalidating points: this tet is fine.
    }

    private int getGridBoxSize( int i, int i_117_, int i_118_ ){
        if( m_gridBox[i][i_117_][i_118_] == null )
            return 0;
        return m_gridBox[i][i_117_][i_118_].size();
    }
    //private TecPoint getGridBoxPoint(int x, int y, int z, int p) {
    //  return (TecPoint)gridBox[x][y][z].get(p);
    //}

    public LinkSystem getLinkSystem(){
        return m_linkSystem;
    }

    public int getNumMantlePoints(){
        return m_numMantlePoints;
    }

    public Point3d getMantlePoint( int i ){
        return m_mantlePoint[i];
    }

    public int getNumTets(){
        return m_tets.size();
    }

    public int getEpoch(){
        return m_epoch;
    }

    private void calcMantleForceForTecPoint( TecPoint p ){
        p.mantleFlow = getMantleFlow( p.getPos() );
        if( p.mantleForce == null )
            p.mantleForce = new Vector3d();
        p.mantleForce.set( p.mantleFlow );
        Vector3d pointMove = new Vector3d( p.getPos() );
        pointMove.sub( p.getOldpos() );
        //pointMove.scale(0.1);
        p.mantleForce.sub( pointMove );    // The force is due to the _difference_ in speed between the mantle and the point
        p.mantleForce.scale( p.getArea() * 0.00003 );
    }

    public void timeStep(){
        ////////////////////////////////////////////////
        // First do the tectonics: move the plates, and tidy up //
        ///////////////////////////////////////////////
        // Check which way up all the triangles are
        // Note: I'm not sure this system gets used any more (17/2/2008)
        System.out.println( "Checking triangle way-upness..." );
        ArrayList tris = new ArrayList();
        for( int i = 0; i < m_tets.size(); i++ ){
            Triangle triangle = ((Tet)m_tets.get( i )).getTopTriangle();
            triangle.cacheIsFacingUp();
            triangle.cacheArea();
            triangle.cacheCenter();
            if( triangle.isEdge() )
                tris.add( triangle );
        }

        // Check for plate with no points in them!
        System.out.println( "Checking for null plates..." );
        for( int i = 0; i < m_plates.size(); i++ ){
            TecPlate tecplate = (TecPlate)m_plates.get( i );
            if( tecplate.getPoints().isEmpty() ){
                m_plates.remove( i );
                i--;
            }
        }

        // Check for plates splitting
        // --------------------
        // First make all the point mantle flows
        System.out.println( "Calculating point mantle flows..." );
        for( int i = 0; i < m_points.size(); i++ ){
            calcMantleForceForTecPoint( getPoint( i ) );
        }

        // Then sort out the edgeLinkPair lists
        System.out.println( "CalculateEdgeLinkPairs..." );
        calculateEdgeLinkPairs();

        // While they're fresh, check for any plates which have fragmented (Thanks to Jeremy Hussell for this bit, which is faster than my version was)
        System.out.println( "Check for fragmented plates..." );
        boolean foundBadness = false;
        for( int i = 0; i < m_plates.size(); i++ ){
            TecPlate tecplate = (TecPlate)m_plates.get( i );
            HashSet tempPoints = new HashSet( tecplate.getPoints() );
            LinkedList addedPoints = new LinkedList();
            TecPoint tecpoint = tecplate.getPoint( 0 );
            addedPoints.add( tecpoint );
            tempPoints.remove( tecpoint );
            while( !addedPoints.isEmpty() ){
                tecpoint = (TecPoint)addedPoints.removeFirst();
                ArrayList linkedPoints = (ArrayList)m_linkSystem.getPointLinks( tecpoint );
                if( linkedPoints != null )
                    for( int j = 0; j < linkedPoints.size(); j++ ){
                        TecPoint tp = (TecPoint)linkedPoints.get( j );
                        if( tp.getPlate() == tecplate && tempPoints.remove( tp ) )
                            addedPoints.add( tp );
                    }
            }
            // OK, we've moved all the ones linked to the original point. Are there any left?
            if( tempPoints.size() > 0 ){
                // Yes, there are! The plate must be fragmented.
                System.out.println( "Fragmented plate!!! Splitting into 2 plates!!!" );
                foundBadness = true;
                // Give one set of the points a new plate
                TecPlate newPlate = new TecPlate( tecplate.getPos().x, tecplate.getPos().y, tecplate.getPos().z );
                addPlate( newPlate );
                Iterator iter = tempPoints.iterator();
                while( iter.hasNext() ){
                    TecPoint tp = (TecPoint)iter.next();
                    tp.setPlate( newPlate );
                }
                newPlate.center();
                newPlate.linkRemoved = false;
                tecplate.center();
                System.out.format( "New plate has %d points\nOld plate has %d points\n", tempPoints.size(), tecplate.getPoints().size() );
            }
            tecplate.linkRemoved = false;
        }


        // If we changed anything, redo the edge lists
        if( foundBadness )
            calculateEdgeLinkPairs();

        // Then check the strain on the plates
  	/*System.out.println("Check platesplitting...");
        double wiggle = 0.1;
        for (int i = 0; i < plates.size(); i++) {
        TecPlate tecplate = (TecPlate) plates.get(i);
        tecplate.splitVector.x = tecplate.getPos().x;
        tecplate.splitVector.y = tecplate.getPos().z;
        tecplate.splitVector.z = -tecplate.getPos().y;
        tecplate.splitVector.normalize();
        if (tecplate.getPoints().size() > 10) {
        double score = plateSplitScore(tecplate);
        Vector3d backup = new Vector3d(tecplate.splitVector);
        for (int j = 0; j < 30; j++) {
        tecplate.splitVector.x	+= wiggle * (Math.random() - Math.random());
        tecplate.splitVector.y	+= wiggle * (Math.random() - Math.random());
        tecplate.splitVector.z	+= wiggle * (Math.random() - Math.random());
        tecplate.splitVector.normalize();
        double tempScore = plateSplitScore(tecplate);
        if (tempScore <= score)	tecplate.splitVector.set(backup);
        else score = tempScore;
        }
        if (score > 5.0E7) {
        splitPlate(tecplate);
        System.out.println("Split plate!!!!");
        }
        }
        }*/

        // Prototype k-nearest-means system
        // Probably obsolete now I've invented the FEA system (17/2/2008)
    /*for (int i=0; i<plates.size(); i++) {
        TecPlate plate=(TecPlate)plates.get(i);
        findMeans(plate);
        }*/

        //pointsCheck();

        // Move all the plates
        for( int i = 0; i < m_plates.size(); i++ ){
            TecPlate tecPlate = (TecPlate)m_plates.get( i );
            // Exert the force occuring at each point
            Point3d point3d = new Point3d( 0.0, 0.0, 0.0 );
            for( int i_136_ = 0; i_136_ < tecPlate.getPoints().size(); i_136_++ ){
                TecPoint tecPoint = tecPlate.getPoint( i_136_ );
                calcMantleForceForTecPoint( tecPoint );
                tecPlate.force( tecPoint.getPos(), tecPoint.mantleForce );
                // Don't forget to exert the collision forces
                tecPlate.force( tecPoint.getPos(), tecPoint.collisionForce );
            }
            tecPlate.move();
            tecPlate.resetForces();
        }

        // Invalidate every edge point
        Iterator iterator = m_linkSystem.getIterator();
        while( iterator.hasNext() ){
            LinkPair linkpair = (LinkPair)iterator.next();
            if( linkpair.getA().getPlate() != linkpair.getB().getPlate() ){
                // Link crosses two plates!
                linkpair.getA().setValid( false );
                linkpair.getB().setValid( false );
            }
        }

        //pointsCheck();

        // Make a list of all the landmass sections (chunks of nice thick continental crust)
        System.out.print( "Listing landmasses..." );
        // Reset all the lms markers
        for( int i = 0; i < m_points.size(); i++ )
            getPoint( i ).lms = -1;

        ArrayList landmassSections = new ArrayList();
        TecPoint firstPoint, nextPoint, checkPoint;
        boolean added = false;
        for( int p = 0; p < m_points.size(); p++ ){
            firstPoint = getPoint( p );
            if( firstPoint.lms == -1 && firstPoint.isContinental() ){
                // The first unclaimed continental point we find will become the start of a new land mass section...
                ArrayList lms = new ArrayList();
                landmassSections.add( lms );
                lms.add( firstPoint );
                firstPoint.lms = landmassSections.size() - 1;
                HashSet possiblePoints = new HashSet( firstPoint.getPlate().getPoints().size() );
                // Add all the _continental_ points from this plate to the list of possible points
                for( int i = 0; i < firstPoint.getPlate().getPoints().size(); i++ )
                    if( firstPoint.getPlate().getPoint( i ).lms == -1 && firstPoint.getPlate().getPoint( i ).isContinental() )
                        possiblePoints.add( firstPoint.getPlate().getPoint( i ) );
                for( int i = 0; i < lms.size(); i++ ){
                    HashSet pointsToAdd = m_linkSystem.getLinkedPoints( (TecPoint)lms.get( i ), pointsSurrounding( ((TecPoint)lms.get( i )).getPos() ) );
                    Iterator iter = pointsToAdd.iterator();
                    while( iter.hasNext() ){
                        //for (int j=0; j<pointsToAdd.size(); j++) {
                        nextPoint = (TecPoint)iter.next();  //pointsToAdd.get(j);
                        if( nextPoint.lms == -1 && possiblePoints.contains( nextPoint ) ){
                            possiblePoints.remove( nextPoint );
                            lms.add( nextPoint );
                            nextPoint.lms = landmassSections.size() - 1;
                        }
                    }
                }
            }
        }
        System.out.println( "done! Counted " + landmassSections.size() + " landmass sections" );
        double[] lmsSize = new double[landmassSections.size()];
        for( int i = 0; i < landmassSections.size(); i++ ){
            ArrayList lms = (ArrayList)landmassSections.get( i );
            for( int j = 0; j < lms.size(); j++ ){
                TecPoint tecpoint = (TecPoint)lms.get( j );
                lmsSize[i] += tecpoint.getArea();//3.141592653589793 * Math.pow(tecpoint.getSize() / 2.0, 2.0); // Crappy old way of calculating area!!
            }
        }

        //pointsCheck();

        // Examine the length of every link
        double collisionForce = 0.4;
        double squash = 0.004;
        double areaLimit = 1.0E7;		// How large an area is allowed to obduct
        ArrayList subductionLinks = new ArrayList();
        Iterator iterator_152_ = m_linkSystem.getIterator();
        while( iterator_152_.hasNext() ){
            LinkPair linkpair = (LinkPair)iterator_152_.next();
            if( linkpair.getA().getPlate() != linkpair.getB().getPlate() ){
                // Link crosses two plates!
                TecPoint tecpoint = linkpair.getA();
                TecPoint tecpoint2 = linkpair.getB();
                double naturalLength = tecpoint.getSize() + tecpoint2.getSize();
                double length = tecpoint.getPos().distance( tecpoint2.getPos() );
                if( length < naturalLength ){
                    if( tecpoint.isContinental() && tecpoint2.isContinental() ){
                        // Squashed link between continental crusts, exert force
                        Vector3d vector3d = new Vector3d( tecpoint2.getPos() );
                        vector3d.sub( tecpoint.getPos() );
                        vector3d.scale( collisionForce * naturalLength / length );
                        //tecpoint2.getPlate().force(tecpoint2.getPos(), vector3d);
                        tecpoint.collisionForce.scaleAdd( 0.1, vector3d, tecpoint.collisionForce );
                        vector3d.scale( -1.0 );
                        //tecpoint2.getPlate().force(tecpoint2.getPos(), vector3d);
                        tecpoint2.collisionForce.scaleAdd( 0.1, vector3d, tecpoint2.collisionForce );
                        // Check for obduction
                        boolean obducted = false;
                        if( length < naturalLength * 0.8 && tecpoint.lms != -1 && tecpoint2.lms != -1 ){	// If two continental bits are colliding LOTS!
                            if( lmsSize[tecpoint.lms] < lmsSize[tecpoint2.lms] ){	// and tp1 is smaller
                                if( lmsSize[tecpoint.lms] <= areaLimit ){
                                    obduct( ((ArrayList)landmassSections.get( tecpoint.lms )), ((ArrayList)landmassSections.get( tecpoint2.lms )) );
                                    obducted = true;
                                }
                            }
                            else if( lmsSize[tecpoint2.lms] <= areaLimit ){ // if tp2 is the smaller
                                obduct( ((ArrayList)landmassSections.get( tecpoint2.lms )), (ArrayList)landmassSections.get( tecpoint.lms ) );
                                obducted = true;
                            }
                        }
                        if( !obducted && length < naturalLength * 0.9 ){
                            // Too much squashing, move the points (deform the plates)

                            // First exert even more collision force
              /*double fScale=0.1*(Math.min(tecpoint.getDepth(), tecpoint2.getDepth())-12);
                            vector3d.scale(-1.0);
                            tecpoint.collisionForce.scaleAdd(0.1*fScale,vector3d, tecpoint.collisionForce);
                            vector3d.scale(-1.0);
                            tecpoint2.collisionForce.scaleAdd(0.1*fScale,vector3d, tecpoint2.collisionForce);*/
                            ////////
                            Vector3d move = new Vector3d();
                            TecPlate tecplate = tecpoint.getPlate();
                            TecPlate tecplate2 = tecpoint2.getPlate();
                            vector3d = new Vector3d( tecpoint.getPos() );
                            vector3d.sub( tecpoint2.getPos() );
                            vector3d.scale( squash * naturalLength / length );
                            double squashSize = 300.0;
                            // Make a list of all the points we are about to move (in a  HashSet so we can do .contains(point) really fast)
                            HashSet mPoints = new HashSet( tecplate.getPoints().size() + tecplate2.getPoints().size() );
                            for( int i = 0; i < tecplate.getPoints().size(); i++ ){
                                TecPoint tecpoint_159_ = (TecPoint)tecplate.getPoints().get( i );
                                double dist = tecpoint.getPos().distance( tecpoint_159_.getPos() );
                                if( dist < squashSize )
                                    mPoints.add( tecpoint_159_ );
                            }
                            for( int i = 0; i < tecplate2.getPoints().size(); i++ ){
                                TecPoint tecpoint_162_ = ((TecPoint)tecplate2.getPoints().get( i ));
                                double dist = tecpoint2.getPos().distance( tecpoint_162_.getPos() );
                                if( dist < squashSize )
                                    mPoints.add( tecpoint_162_ );
                            }
                            // Make a list of all the tets which involve any of the points we are about to move
                            ArrayList squishTets = new ArrayList();
                            for( int i = 0; i < m_tets.size(); i++ ){
                                Tet tet = (Tet)m_tets.get( i );
                                if( tet.getPlate() == tecplate || tet.getPlate() == tecplate2 )
                                    if( mPoints.contains( tet.b ) || mPoints.contains( tet.c ) || mPoints.contains( tet.d ) )
                                        squishTets.add( tet );
                            }
                            // Place the 'area' of each tet into its 'oldArea'
                            for( int i = 0; i < squishTets.size(); i++ ){
                                Tet tet = (Tet)squishTets.get( i );
                                tet.calcArea();
                                tet.oldArea = tet.area;
                            }

                            for( int i = 0; i < tecplate.getPoints().size(); i++ ){
                                TecPoint tecpoint_159_ = (TecPoint)tecplate.getPoints().get( i );
                                double dist = tecpoint.getPos().distance( tecpoint_159_.getPos() );
                                if( dist < squashSize ){
                                    move.scale( 1.0 - dist / squashSize, vector3d );
                                    //tecpoint_159_.scale(1.0 + (0.05 * squash * (1.0 - (dist / squashSize))));
                                    tecpoint_159_.move( move );
                                    tecpoint_159_.setHeight( m_planetRadius );
                                    tecpoint_159_.setValid( false );
                                }
                            }
                            vector3d.scale( -1.0 );
                            for( int i = 0; i < tecplate2.getPoints().size(); i++ ){
                                TecPoint tecpoint_162_ = ((TecPoint)tecplate2.getPoints().get( i ));
                                double dist = tecpoint2.getPos().distance( tecpoint_162_.getPos() );
                                if( dist < squashSize ){
                                    move.scale( 1 - dist / squashSize, vector3d );
                                    //tecpoint_162_.scale(1+0.05*squash*(1-dist/squashSize));
                                    tecpoint_162_.move( move );
                                    tecpoint_162_.setHeight( m_planetRadius );
                                    tecpoint_162_.setValid( false );
                                }
                            }

                            // Now that we've moved all the points, and tet tets have been squished,
                            // use their change in area to scale their height
                            for( int i = 0; i < squishTets.size(); i++ ){
                                Tet tet = (Tet)squishTets.get( i );
                                tet.calcArea();
                                tet.scaleHeights( tet.oldArea / tet.area );
                            }
                        }
                    }
                    else {
                        // Exert small force due to collision, even though non-intercontinental
                        Vector3d vector3d = new Vector3d( tecpoint2.getPos() );
                        vector3d.sub( tecpoint.getPos() );
                        vector3d.scale( 0.2 * collisionForce * naturalLength / length );  // Smaller force due to non-intercontinental collision
                        //tecpoint2.getPlate().force(tecpoint2.getPos(), vector3d);
                        //tecpoint.collisionForce.scaleAdd(0.1,vector3d, tecpoint.collisionForce);
                        vector3d.scale( -1.0 );
                        //tecpoint.getPlate().force(tecpoint.getPos(), vector3d);
                        //tecpoint.collisionForce.scaleAdd(0.1,vector3d, tecpoint.collisionForce);
                        if( (!tecpoint.isContinental() || !tecpoint2.isContinental()) && length < naturalLength * 0.6 ){
                            // Yay! Link is squashed enough to subduct the oceanic crust!
                            System.out.println( "Subducting point!" );
                            subductionLinks.add( linkpair );
                        }
                    }
                }
            }
        }

        // Find all the edge points
        HashSet edgePoints = new HashSet( m_points.size() );
        Iterator it = m_linkSystem.getIterator();
        while( it.hasNext() ){
            LinkPair linkpair = (LinkPair)it.next();
            if( linkpair.getCount() > 0 && linkpair.getA().getPlate() != linkpair.getB().getPlate() ){
                // Link crosses two plates!
                edgePoints.add( linkpair.getA() );
                edgePoints.add( linkpair.getB() );
            }
        }

        // Do the actual subduction
        for( int i = 0; i < subductionLinks.size(); i++ ){
            LinkPair linkpair = (LinkPair)subductionLinks.get( i );
            TecPoint tecpoint = linkpair.getA();
            TecPoint tecpoint2 = linkpair.getB();
            TecPoint op, cp;
            if( tecpoint.getSurfaceHeight() < tecpoint2.getSurfaceHeight() ){
                op = tecpoint;
                cp = tecpoint2;
            }
            else {
                op = tecpoint2;
                cp = tecpoint;
            }
            TecPlate tecplate = op.getPlate();  //  Oceanic plate
            TecPlate tecplate2 = cp.getPlate(); // Continental plate   (not necessarily true, but reflects which one gets subducted and which one overrides)
            // Before we kill the point (removing all its linking info, etc), we need to find out which direction it subducted in!
            Vector3d subDir = new Vector3d();
            ArrayList linkPoints = m_linkSystem.getPointLinks( op );
            // Add the vector of the link from each point (on the same plate) to the subducting point. This will give the vague direction of subduction (away from the edge of the plate)
            for( int j = 0; j < linkPoints.size(); j++ ){
                TecPoint tp = (TecPoint)linkPoints.get( j );
                if( tp.getPlate() == op.getPlate() ){
                    subDir.sub( tp.getPos() );
                    subDir.add( op.getPos() );
                }
            }
            // Scale the force
            if( subDir.length() > 0 ){
                subDir.normalize();
                subDir.scale( 100 );
            }
            // _Now_ kill it
            killPoint( op );

            // Vulcanism in the overriding (continental) plate
            // Spread the layer over nearby (400km) points
            double thickness = 1.2; //(op.getDepth() * op.getSize() * op.getSize() / 500000.0);   // How thick a layer of rock to add
            System.out.println( "Adding rock layer of thickness " + thickness + "km" );
            for( int i_169_ = 0; i_169_ < tecplate2.getPoints().size(); i_169_++ ){
                TecPoint temppoint = (TecPoint)tecplate2.getPoints().get( i_169_ );
                double dist = cp.getPos().distance( temppoint.getPos() );
                if( dist < 400.0 ){
                    temppoint.addLayer( thickness * (1.0 - dist / 400.0), 2.7 );  // Volcanic rocks are lighter than oceanic basalt
                }
            }

            // Cause pull-down on the subducted plate
            if( subDir.length() > 0 ){
                for( int i_172_ = 0; i_172_ < tecplate.getPoints().size(); i_172_++ ){
                    TecPoint temppoint = (TecPoint)tecplate.getPoints().get( i_172_ );
                    double dist = op.getPos().distance( temppoint.getPos() );
                    if( dist < 200.0 ){
                        temppoint.addBaseDepthOffset( -2.0 * (1.0 - dist / 200.0) );
                        // Also add slab-pull, a force which drags nearby points (on the subducting plate) down in the same direction as the subducting point
                        temppoint.collisionForce.add( subDir );
                    }
                }
                // Cause even wider pull-down along the boundary of the plate
                for( int i_172_ = 0; i_172_ < tecplate.getPoints().size(); i_172_++ ){
                    TecPoint temppoint = (TecPoint)tecplate.getPoints().get( i_172_ );
                    if( edgePoints.contains( temppoint ) ){
                        double dist = op.getPos().distance( temppoint.getPos() );
                        if( dist < 1500.0 ){
                            temppoint.addBaseDepthOffset( -1.0 * (1.0 - dist / 1500.0) );
                        }
                    }
                }
            }

            // And push-up on the overthrusting plate
            for( int i_175_ = 0; i_175_ < tecplate2.getPoints().size(); i_175_++ ){
                TecPoint temppoint = (TecPoint)tecplate2.getPoints().get( i_175_ );
                double dist = cp.getPos().distance( temppoint.getPos() );
                if( dist < 400.0 )
                    temppoint.addBaseDepthOffset( 0.2 * (1.0 - dist / 400.0) );
            }
            // Cause even wider push-up along the boundary of the plate
            for( int i_172_ = 0; i_172_ < tecplate2.getPoints().size(); i_172_++ ){
                TecPoint temppoint = (TecPoint)tecplate2.getPoints().get( i_172_ );
                if( edgePoints.contains( temppoint ) ){
                    double dist = cp.getPos().distance( temppoint.getPos() );
                    if( dist < 1500.0 ){
                        temppoint.addBaseDepthOffset( 0.1 * (1.0 - dist / 1500.0) );
                    }
                }
            }
        }

        // Fade the baseDepthOffset on all the points
        System.out.println( "Fading baseDepthOffset" );
        for( int i = 0; i < m_points.size(); i++ )
            getPoint( i ).scaleBaseDepthOffset( 0.99 );

        // Check for inverted triangles
        int invCount = 0;
        for( int i_178_ = 0; i_178_ < tris.size(); i_178_++ ){
            Triangle triangle = (Triangle)tris.get( i_178_ );
            if( triangle.facingUp != triangle.isFacingUp() )
                invCount++;
        }
        System.out.println( invCount + " triangles inverted" );

        //pointsCheck();

        // Check gaps between plates
        checkPlateGaps();

        //pointsCheck();

        redelaunay(); // Includes calculateAreas() at the end...

        // The FEA needs doing _after_ the reDelaunay - it relies on a correct LinkSystem to find linkWidths
        ///////////////////
        // New FEA system
        System.out.print( "Doing FEA..." );
        double breakForce = 0.04;//40000.0/100.0;  // Tensile strength of links (*320 to compensate for link length)
        // Clear the old FEA data
        ArrayList links = new ArrayList( m_linkSystem.getCollection() );
        for( int i = 0; i < links.size(); i++ ){
            LinkPair link = (LinkPair)links.get( i );
            link.pushForce = 0;
        }
        for( int r = 0; r < m_points.size(); r++ ){
            getPoint( r ).FEAforce.set( 0, 0, 0 );  // Clear the old FEA forces
            getPoint( r ).collisionForce.scale( 0.7 );  // Fade the old collision forces
        }
        // Make sure that only plate-crossing links are "broken"
        for( int i = 0; i < links.size(); i++ ){
            LinkPair link = (LinkPair)links.get( i );
            link.broken = false;
            link.plateCrosser = (link.getA().getPlate() != link.getB().getPlate());
        }

        // Make a vector of the links inside each plate
        ArrayList[] plVecs = new ArrayList[m_plates.size()];
        for( int j = 0; j < m_plates.size(); j++ )
            plVecs[j] = new ArrayList( links.size() );
        LinkPair lp;
        for( int j = 0; j < links.size(); j++ ){
            lp = (LinkPair)links.get( j );
            if( lp.getA().getPlate() == lp.getB().getPlate() && getPlateNum( lp.getA().getPlate() ) != -1 ){
                plVecs[getPlateNum( lp.getA().getPlate() )].add( lp );
                lp.linkWidth = m_linkSystem.linkWidth( lp );
            }
        }

        // FEA one plate at a time
        ArrayList newPlates = new ArrayList();
        for( int i = 0; i < m_plates.size(); i++ ){
            if( Math.random() < 1.2 ){
                TecPlate plate = getPlate( i );
                double plateArea = plate.getArea();
                double plateExp = (0.1 + 0.9 * Math.exp( -Math.pow( Math.min( 0, plateArea - 5000000 ) / 35000000, 2 ) ));
                //System.out.println("Plate size exponent="+plateExp);
                // Collect the links of this plate
                ArrayList plateLinks = plVecs[i];
                /*LinkPair lp;
                for (int j=0; j<links.size(); j++) {
                lp=(LinkPair)links.get(j);
                if (lp.getA().getPlate()==plate && lp.getB().getPlate()==plate) plateLinks.add(lp);
                }*/
                // Recalculate the force distribution, breaking any overstressed links
                boolean brokeMoreLinks = false, brokeAnyLinks = false, splitPlate = false;
                double currentBreakForce = breakForce;
                do {
                    brokeMoreLinks = false;
                    for( int r = 0; r < plateLinks.size() * 10; r++ ){
                        lp = (LinkPair)plateLinks.get( ((int)(Math.random() * plateLinks.size())) );
                        lp.sortLink();
                    }
                    // Check for (and break) overstressed links
                    double aveForce = 0;
                    int lc = 0;
                    for( int r = 0; r < plateLinks.size(); r++ ){
                        lp = (LinkPair)plateLinks.get( r );
                        // Estimate the _width_ of the link. Use the sqrt of the average area of the 2 points
                        //double linkWidth=Math.pow((lp.getA().getArea()+lp.getB().getArea())/2,0.5);
                        //if (lp.pushForce<0) {aveForce+=lp.pushForce/lp.linkWidth; lc++;}
                        if( !lp.broken && lp.pushForce < -currentBreakForce * lp.linkWidth * plateExp ){    // This test needs making better. Needs to vary with rock type+thickness.
                            lp.broken = true;
                            lp.removeFromFEA();
                            brokeMoreLinks = true;
                            brokeAnyLinks = true;
//System.out.println("Broke a link");
                        }
                        //aveForce=aveForce/(lc);
                        //System.out.println("Average pushforce/linkwidth="+aveForce);
                    }

                    /////////////////
                    // Now check if the current amount of broken links is sufficient to break the plate in two.
                    // If so, we don't need to go any further
                    /////////////////
                    // If _any_ links were overstressed, check if the plate split into parts
                    if( brokeMoreLinks ){
                        // Expand the broken areas
                        for( int l = 0; l < plateLinks.size(); l++ ){
                            lp = (LinkPair)plateLinks.get( l );
                            if( lp.broken ){
                                ArrayList linksFromA = m_linkSystem.getPointLinks( lp.getA() );
                                for( int r = 0; r < linksFromA.size(); r++ )
                                    ((TecPoint)linksFromA.get( r )).broken = true;
                                ArrayList linksFromB = m_linkSystem.getPointLinks( lp.getB() );
                                for( int r = 0; r < linksFromB.size(); r++ )
                                    ((TecPoint)linksFromB.get( r )).broken = true;
                            }
                        }
                        // Find an unbroken TecPoint to start from...
                        ArrayList platePoints = plate.getPoints();
                        TecPoint start = null;
                        for( int r = 0; r < platePoints.size() && start == null; r++ ){
                            if( !plate.getPoint( r ).broken )
                                start = plate.getPoint( r );
                        }
                        if( start != null ){
                            // OK, we're got a start point. Set up to spread out from this point
                            ArrayList movedPoints = new ArrayList();
                            ArrayList unmovedPoints = new ArrayList( platePoints );
                            ArrayList brokenPoints = new ArrayList();
                            // Move the start point
                            movedPoints.add( start );
                            unmovedPoints.remove( start );
                            // Remove any broken points from the unmoved pile
                            for( int r = 0; r < unmovedPoints.size(); r++ ){
                                if( ((TecPoint)unmovedPoints.get( r )).broken ){
                                    brokenPoints.add( unmovedPoints.get( r ) );
                                    unmovedPoints.remove( r );
                                    r--;
                                }
                            }
                            // Loop around, moving connected unbroken points to the "moved" pile
                            for( int r = 0; r < movedPoints.size() && unmovedPoints.size() > 0; r++ ){
                                TecPoint p1 = (TecPoint)movedPoints.get( r );
                                ArrayList linkedPoints = m_linkSystem.getPointLinks( p1 );
                                for( int p = 0; p < linkedPoints.size(); p++ ){
                                    TecPoint linkedPoint = (TecPoint)linkedPoints.get( p );
                                    if( linkedPoint != p1 && !linkedPoint.broken && unmovedPoints.contains( linkedPoint ) ){
                                        movedPoints.add( linkedPoint );
                                        unmovedPoints.remove( linkedPoint );
                                    }
                                }
                            }
                            // Now see if there are any points left in the unmoved pile...
                            if( unmovedPoints.size() > 0 ){
                                // The plate was split into bits! Make a new plate...
                                System.out.println( "FEA split a plate!" );
                                TecPlate newPlate = new TecPlate( 0, 0, 0 );
                                newPlates.add( newPlate );
                                for( int j = 0; j < movedPoints.size(); j++ )
                                    ((TecPoint)movedPoints.get( j )).setPlate( newPlate );
                                newPlate.center();
                                plate.center();
                                // Just need to work out which side the broken points should go on.
                                int oldBrokenPointsSize = -1;
                                while( brokenPoints.size() > 0 ){
                                    // Check if we removed any points last time round
                                    if( brokenPoints.size() == oldBrokenPointsSize ){
                                        // Uh-oh. WTF? Why didn't we remove any points?
                                        System.out.println( "Uh-oh. WTF? Why didn't we remove any points?\nDoing it the slow way" );
                                        // Make sure all the broken points are really borken
                                        for( int j = 0; j < brokenPoints.size(); j++ )
                                            ((TecPoint)brokenPoints.get( j )).broken = true;
                                        // And assign them the slow way
                                        // Get the linkedPoints for each brokenPoint, and count how many are in movedPoints and how many are in unmovedPoints
                                        while( brokenPoints.size() > 0 ){
                                            TecPoint bp = (TecPoint)brokenPoints.get( 0 );
                                            ArrayList linkedPoints = m_linkSystem.getPointLinks( bp );
                                            int countMoved = 0, countUnmoved = 0;
                                            for( int k = 0; k < linkedPoints.size(); k++ ){
                                                if( movedPoints.contains( linkedPoints.get( k ) ) )
                                                    countMoved++;
                                                else if( unmovedPoints.contains( linkedPoints.get( k ) ) )
                                                    countUnmoved++;
                                                else
                                                    System.out.println( "!!!!!One of the brokenPoints wasn't in Moved _or_ Unmoved!!!!!" );
                                            }
                                            // Assign the brokenPoint to whichever it links to more.
                                            if( countMoved > countUnmoved ){
                                                // Assign to new plate
                                                bp.setPlate( newPlate );
                                                bp.broken = false;
                                                brokenPoints.remove( 0 );
                                            }
                                            else {
                                                // Assign to old plate
                                                bp.setPlate( plate );
                                                bp.broken = false;
                                                brokenPoints.remove( 0 );
                                            }
                                            // Repeat until no more brokenPoints
                                        }
                                    }
                                    oldBrokenPointsSize = brokenPoints.size();
                                    // Find broken points which are _directly_ linked to the new plate, and remember them
                                    ArrayList remember = new ArrayList();
                                    for( int j = 0; j < plateLinks.size(); j++ ){
                                        lp = (LinkPair)plateLinks.get( j );
                                        if( lp.getA().broken && !lp.getB().broken && lp.getB().getPlate() == newPlate && !remember.contains( lp.getA() ) )
                                            remember.add( lp.getA() );
                                        else if( lp.getB().broken && !lp.getA().broken && lp.getA().getPlate() == newPlate && !remember.contains( lp.getB() ) )
                                            remember.add( lp.getB() );
                                    }
                                    // Move all those points to the new plate (and remove them from the "broken" pile)
                                    System.out.println( "Moving " + remember.size() + " of " + brokenPoints.size() + " points to the new plate" );
                                    while( remember.size() > 0 ){
                                        ((TecPoint)remember.get( 0 )).setPlate( newPlate );
                                        ((TecPoint)remember.get( 0 )).broken = false;
                                        brokenPoints.remove( remember.get( 0 ) );
                                        remember.remove( 0 );
                                    }

                                    // Now do the same for the _old_ plate, then repeat until we've allocated all the broken points to one side or the other...
                                    // Find broken points which are _directly_ linked to the old plate, and remember them
                                    remember.clear();
                                    for( int j = 0; j < plateLinks.size(); j++ ){
                                        lp = (LinkPair)plateLinks.get( j );
                                        if( lp.getA().broken && !lp.getB().broken && lp.getB().getPlate() == plate && !remember.contains( lp.getA() ) )
                                            remember.add( lp.getA() );
                                        else if( lp.getB().broken && !lp.getA().broken && lp.getA().getPlate() == plate && !remember.contains( lp.getB() ) )
                                            remember.add( lp.getB() );
                                    }
                                    // Move all those points to the old plate (and remove them from the "broken" pile)
                                    System.out.println( "Moving " + remember.size() + " of " + brokenPoints.size() + " points to the old plate" );
                                    while( remember.size() > 0 ){
                                        ((TecPoint)remember.get( 0 )).setPlate( plate );
                                        ((TecPoint)remember.get( 0 )).broken = false;
                                        brokenPoints.remove( remember.get( 0 ) );
                                        remember.remove( 0 );
                                    }

                                }
                                // Tidy up which links are plate-crossing
                                for( int l = 0; l < links.size(); l++ ){
                                    LinkPair link = (LinkPair)links.get( l );
                                    link.plateCrosser = (link.getA().getPlate() != link.getB().getPlate());
                                }
                                System.out.println( "done splitting plate." );
                                splitPlate = true;
                            }
                        }
                        else {
                            System.out.println( "\nCouldn't find unbroken point to start from :(\n" );
                            // This means the plate was completely overstressed, and should definitely break, but we need to be clever about it
                            // Hmm.
                            //splitPlate=true;    // We haven't really split the plate, but the plate is smashed, so we don't need to "sortLinks" any more, just quit

                            // OK, clear all the data so far: we're going to try again with a bigger breakForce

                            // Clear the old FEA data
                            for( int i2 = 0; i2 < links.size(); i2++ ){
                                LinkPair link = (LinkPair)links.get( i2 );
                                link.pushForce = 0;
                            }
                            for( int r2 = 0; r2 < m_points.size(); r2++ ){
                                getPoint( r2 ).FEAforce.set( 0, 0, 0 );  // Clear the old FEA forces
                            }
                            // Make sure that only plate-crossing links are "broken"
                            for( int i2 = 0; i2 < links.size(); i2++ ){
                                LinkPair link = (LinkPair)links.get( i2 );
                                link.broken = false;
                            }
                            // Put the links back
                            plateLinks = plVecs[i];
                            // Choose a new breakForce to try
                            currentBreakForce += 0.005;
                        }
                    }

                } while( brokeMoreLinks && !splitPlate );
            }
        }
        for( int i = 0; i < newPlates.size(); i++ )
            addPlate( (TecPlate)newPlates.get( i ) );
        System.out.println( "done FEA" );
        //////////////////


        // Check if any plates are entirely captured within another plate
        if( m_plates.size() > 2 ){
            System.out.println( "Checking for captured plates..." );
            HashSet capturedPlates = new HashSet();
            HashMap linkedPlate = new HashMap();
            for( int i = 0; i < m_plates.size(); i++ )
                capturedPlates.add( getPlate( i ) );
            Iterator iter = m_linkSystem.getIterator();
            while( iter.hasNext() ){
                lp = (LinkPair)iter.next();
                TecPlate p1 = lp.getA().getPlate();
                TecPlate p2 = lp.getB().getPlate();
                if( lp.getA().getPlate() != lp.getB().getPlate() ){
                    // This link crosses a plate boundary
                    TecPlate lp1 = (TecPlate)linkedPlate.get( p1 );
                    TecPlate lp2 = (TecPlate)linkedPlate.get( p2 );
                    if( lp1 == null ){
                        linkedPlate.put( p1, p2 ); // We haven't seen p1 linked to another plate before
                    }
                    else {  // We _have_ seen p1 linked to another plate before. Check whether this plate is the same one
                        if( p2 != lp1 ){  // It's not the same plate, so p1 can't be captured
                            capturedPlates.remove( p1 );
                        }
                    }
                    if( lp2 == null ){
                        linkedPlate.put( p2, p1 ); // We haven't seen p2 linked to another plate before
                    }
                    else {  // We _have_ seen p2 linked to another plate before. Check whether this plate is the same one
                        if( p1 != lp2 ){  // It's not the same plate, so p2 can't be captured
                            capturedPlates.remove( p2 );
                        }
                    }
                }
            }
            // Accrete all the capturedPlates onto their LinkedPlate
            if( capturedPlates.size() > 0 )
                System.out.println( "Found " + capturedPlates.size() + " captured plates" );
            iter = capturedPlates.iterator();
            while( iter.hasNext() ){
                TecPlate innerPlate = (TecPlate)iter.next();
                TecPlate outerPlate = (TecPlate)linkedPlate.get( innerPlate );
                while( innerPlate.getPoints().size() > 0 && outerPlate != null ){
                    TecPoint tecPoint = innerPlate.getPoint( 0 );
                    tecPoint.setPlate( outerPlate );
                    ArrayList linkedPoints = m_linkSystem.getPointLinks( tecPoint );
                    for( int j = 0; j < linkedPoints.size(); j++ ){
                        m_linkSystem.getLinkPair( tecPoint, (TecPoint)linkedPoints.get( j ) ).plateCrosser = false;
                    }
                }
                m_plates.remove( innerPlate );
            }
        }

        // Accrete small plates onto their most highly-linked neighbour
        System.out.println( "Checking for tiny plates..." );
        double plateSizeLimit = 100000;  // Anything less than 100,000 km ^2 is tiny :)
        ArrayList accPlates = new ArrayList();
        for( int i = 0; i < m_plates.size(); i++ ){
            //System.out.println("Plate size="+getPlate(i).getArea());
            if( getPlate( i ).getArea() < plateSizeLimit || getPlate( i ).getPoints().size() < 3 )
                accPlates.add( getPlate( i ) );
        }

        for( int i = 0; i < accPlates.size(); i++ ){
            TecPlate plate = (TecPlate)accPlates.get( i );
            // Find which plate this plate should accrete onto
            HashMap linkWidths = new HashMap();
            TecPlate otherPlate;
            Iterator iter = m_linkSystem.getIterator();
            while( iter.hasNext() ){
                lp = (LinkPair)iter.next();
                otherPlate = null;
                if( lp.getA().getPlate() == plate && lp.getB().getPlate() != plate ){
                    otherPlate = lp.getB().getPlate();
                }
                else if( lp.getA().getPlate() != plate && lp.getB().getPlate() == plate ){
                    otherPlate = lp.getA().getPlate();
                }
                if( otherPlate != null ){
                    if( linkWidths.get( otherPlate ) == null ){
                        linkWidths.put( otherPlate, new Double( 0 ) );
                    }
                    double w = ((Double)linkWidths.get( otherPlate )).doubleValue();
                    linkWidths.put( otherPlate, new Double( w + m_linkSystem.linkWidth( lp ) ) );
                }
            }
            // OK, now find the plate with which we have the longest border with
            double longest = 0;
            TecPlate bestNeighbour = null;
            iter = linkWidths.keySet().iterator();
            while( iter.hasNext() ){
                otherPlate = (TecPlate)iter.next();
                double w = ((Double)linkWidths.get( otherPlate )).doubleValue();
                if( w > longest ){
                    longest = w;
                    bestNeighbour = otherPlate;
                }
            }
            // And finally do the actual accretion...
            if( bestNeighbour != null ){
                System.out.println( "Removing tiny plate!" );
                while( plate.getPoints().size() > 0 ){
                    plate.getPoint( 0 ).setPlate( bestNeighbour );
                }
                m_plates.remove( plate );
            }
        }

        // Break any stupidly large plates "by force", ie not realistically, just split them!
        System.out.println( "Checking for huge plates..." );
        double plateSizeUpperLimit = 100000000;  // Anything bigger than 100,000,000 km ^2 is huge :)
        for( int i = m_plates.size() - 1; i >= 0; i-- ){
            //System.out.println("Plate size="+getPlate(i).getArea());
            if( getPlate( i ).getArea() > plateSizeUpperLimit ){
                System.out.println( "Splitting huge plate because it's too damn big!" );
                // Choose where to split it: pick a plane which passes through the middle of the plate
                Vector3d middle = new Vector3d();
                for( int j = 0; j < getPlate( i ).getPoints().size(); j++ )
                    middle.add( getPlate( i ).getPoint( j ).getPos() );
                Vector3d plane = new Vector3d();  // The vector which defines the plane
                plane.cross( middle, getRandomVector() );
                // OK, got the plane.
                // Now move all the points on one side of it into a new plate
                ArrayList newPlatePoints = new ArrayList();
                for( int j = 0; j < getPlate( i ).getPoints().size(); j++ )
                    if( plane.dot( new Vector3d( getPlate( i ).getPoint( j ).getPos() ) ) > 0 )
                        newPlatePoints.add( getPlate( i ).getPoint( j ) );
                if( newPlatePoints.size() > 5 && getPlate( i ).getPoints().size() - newPlatePoints.size() > 5 ){  // Check there are points on both sides!
                    TecPlate newPlate = new TecPlate( 0, 0, 1 );  // Random position, 'cos we'll recenter it after we've added all the points
                    for( int j = 0; j < newPlatePoints.size(); j++ )
                        ((TecPoint)newPlatePoints.get( j )).setPlate( newPlate );
                    newPlate.center();
                    addPlate( newPlate );
                }
            }
        }
        calculateEdgeLinkPairs();

        pourOnWater();


        //////////////////////////////////////////////////
        // Now do the "surface" stuff: erosion, soil, rivers, etc //   (but mostly not, in version 5)
        //////////////////////////////////////////////////

        // Construct the watershed data
        System.out.print( "Calculating volCaps..." );
        double gradientLimitOnLand = 1.0 / 40.0;  // 2.5% gradient on land
        double gradientLimitInSea = 1.0 / 16.0;  // 6% gradient in the sea
        double moveLimit = 50;    // Can't add or remove more than this (vertical km of rock) in one turn
        for( int i = 0; i < m_points.size(); i++ ){
            TecPoint p = getPoint( i );
            p.volCap = 10e10;
            p.volCap2 = 10e10;
        }
        it = m_linkSystem.getIterator();
        while( it.hasNext() ){
            LinkPair linkpair = (LinkPair)it.next();
            TecPoint tecpoint;
            TecPoint tecpoint2;
            // Ensure that tecpoint is lower than tecpoint2
            if( linkpair.getA().getSurfaceHeight() > linkpair.getB().getSurfaceHeight() ){
                tecpoint2 = linkpair.getA();
                tecpoint = linkpair.getB();
            }
            else {
                tecpoint = linkpair.getA();
                tecpoint2 = linkpair.getB();
            }
            double diff = (tecpoint2.getSurfaceHeight() - tecpoint.getSurfaceHeight());
            double dist = linkpair.getLength();
            double gradient = diff / dist;
            // Calculate limit for tecpoint
            double gradLimit = gradientLimitOnLand;
            if( tecpoint.heightAboveSeaLevel() < 0 )
                gradLimit = gradientLimitInSea;
            tecpoint.volCap = Math.min( tecpoint.volCap, tecpoint.getArea() * (diff + gradLimit * dist) );
            tecpoint.volCap = Math.min( tecpoint.volCap, tecpoint.getArea() * diff );    // Limit due to not wanting tp to grow higher than tp2
            tecpoint.volCap2 = Math.min( tecpoint.volCap2, tecpoint.getArea() * (diff + gradLimit * dist) );
            // Calculate limit for tecpoint2
            gradLimit = gradientLimitOnLand;
            if( tecpoint2.heightAboveSeaLevel() < 0 )
                gradLimit = gradientLimitInSea;
            tecpoint2.volCap = Math.min( tecpoint2.volCap, tecpoint2.getArea() * (gradLimit * dist - diff) );
            tecpoint2.volCap = Math.min( tecpoint2.volCap, 0 );  // Can't add any more without getting higher than surrounding points - we're ALREADY higher than them!
            tecpoint2.volCap2 = Math.min( tecpoint2.volCap2, tecpoint2.getArea() * (gradLimit * dist - diff) );
        }
        System.out.println( "done" );

        // Now erode point with negative volCap (gradients which break the gradient limits)
        System.out.print( "Eroding..." );
        for( int i = 0; i < m_points.size(); i++ ){
            TecPoint p = getPoint( i );
            if( p.volCap < 0 ){
                // Slice off the excess rock, and make a note of it
                double liftedVol = Math.min( -p.volCap, moveLimit * p.getArea() );
                liftedVol = Math.min( liftedVol, Math.max( 0, (p.getDepth() - 4) * p.getArea() ) ); // Don't try to erode the plate to be thinner than 4km
                double liftedDens = p.getDensity();
                //System.out.println("\nLifting "+liftedVol+"km^3 of rock");
                p.remove( liftedVol / p.getArea() );  // Remove by height, not volume
                // Redo the volCap of this point, and all the points linked from it
                calcVolCap( p, gradientLimitOnLand, gradientLimitInSea );
                ArrayList linkedPoints = m_linkSystem.getPointLinks( p );
                for( int j = 0; j < linkedPoints.size(); j++ ){
                    calcVolCap( (TecPoint)linkedPoints.get( j ), gradientLimitOnLand, gradientLimitInSea );
                }
                // Find where to dump the rock
                int count = 0;
                while( liftedVol > 1 && count < 1000 ){
                    // Find the lowest point from here
                    TecPoint lowest = p, tempPoint;
                    double lh = p.getSurfaceHeight();
                    for( int j = 0; j < linkedPoints.size(); j++ ){
                        tempPoint = (TecPoint)linkedPoints.get( j );
                        //System.out.println("  linkedPoint height="+tempPoint.getSurfaceHeight()+", volCap="+tempPoint.volCap);
            /*if (tempPoint.getSurfaceHeight()<p.getSurfaceHeight() && tempPoint.volCap>0) {
                        // Dump rock here, because we can
                        /*double moveVol=Math.min(tempPoint.volCap,liftedVol);
                        tempPoint.add(moveVol/tempPoint.getArea(),liftedDens);
                        liftedVol-=moveVol;
                        // Redo the volCap of the lowest point, and all the points linked from it
                        calcVolCap(tempPoint, gradientLimitOnLand,gradientLimitInSea);
                        Vector linkedPoints2=linkSystem.getPointLinks(tempPoint);
                        for (int k=0; k<linkedPoints2.size(); k++) {calcVolCap((TecPoint)linkedPoints2.get(k), gradientLimitOnLand,gradientLimitInSea);}
                        }*/
                        if( tempPoint.getSurfaceHeight() < lh ){
                            lowest = tempPoint;
                            lh = lowest.getSurfaceHeight();
                        }  // Found a new lowest point
                    }
                    //System.out.println("My height="+p.getSurfaceHeight()+", lh="+lh+", lowest.volCap="+(int)lowest.volCap);
                    // Deal with the lowest point
                    if( lowest.volCap > 100 ){
                        // Dump as much rock as poss on the lowest point (without making it higher than surrounding points)
                        double moveVol = Math.max( 0, Math.min( lowest.volCap, liftedVol ) );
                        lowest.add( moveVol / lowest.getArea(), liftedDens );
                        liftedVol -= moveVol;
                        // Redo the volCap of the lowest point, and all the points linked from it
                        calcVolCap( lowest, gradientLimitOnLand, gradientLimitInSea );
                        ArrayList linkedPoints2 = m_linkSystem.getPointLinks( lowest );
                        for( int k = 0; k < linkedPoints2.size(); k++ ){
                            calcVolCap( (TecPoint)linkedPoints2.get( k ), gradientLimitOnLand, gradientLimitInSea );
                        }
                        //System.out.println("Dumped "+moveVol+"km^3 of rock\n"+liftedVol+"km^3 remaining");
                    }
                    else if( Math.abs( lowest.getSurfaceHeight() - p.getSurfaceHeight() ) < 0.01 ){
                        // We're in the bottom of a hole. Drop a pile here, then repeat.
                        double moveVol = Math.min( Math.max( 0.01 * lowest.getArea(), lowest.volCap2 ), liftedVol ); // Add a 10m thick layer
                        lowest.add( moveVol / lowest.getArea(), liftedDens );
                        liftedVol -= moveVol;
                        // Redo the volCap of the lowest point, and all the points linked from it
                        calcVolCap( lowest, gradientLimitOnLand, gradientLimitInSea );
                        ArrayList linkedPoints2 = m_linkSystem.getPointLinks( lowest );
                        for( int k = 0; k < linkedPoints2.size(); k++ ){
                            calcVolCap( (TecPoint)linkedPoints2.get( k ), gradientLimitOnLand, gradientLimitInSea );
                        }
                        //System.out.println("Dropped "+moveVol+"km^3 in a hole\n"+liftedVol+"km^3 remaining");
                    }/*else {
                    // Dammit, we have to scrape rock off here, too!
                    lowest.remove(lowest.volCap/lowest.getArea());
                    liftedDens=(liftedVol*liftedDens+lowest.volCap*lowest.getDensity())/(liftedVol+lowest.volCap);
                    liftedVol+=lowest.volCap;
                    // Redo the volCap of the lowest point, and all the points linked from it
                    calcVolCap(lowest, gradientLimitOnLand,gradientLimitInSea);
                    Vector linkedPoints2=linkSystem.getPointLinks(lowest);
                    for (int k=0; k<linkedPoints2.size(); k++) {calcVolCap((TecPoint)linkedPoints2.get(k), gradientLimitOnLand,gradientLimitInSea);}
                    }*/
                    // Move pointer to lowest point, and repeat
                    p = lowest;
                    linkedPoints = m_linkSystem.getPointLinks( p );
                    count++;
                }
                if( count >= 1000 ){
                    System.out.println( "*****\nBAD EROSION! Lost " + liftedVol + "km^3 of rock.\n*****" );
                    // Problem. Dump this rock over the whole planet :)
                    for( int j = 0; j < m_points.size(); j++ ){
                        getPoint( j ).add( liftedVol / m_planetSurfaceArea, liftedDens );
                    }
                }
            }
        }
        System.out.println( "done" );

        // Very crude smoothing
  	/*System.out.print("Smoothing..");
        double seaLevel = TecPoint.seaLevel;
        double gradientLimitOnLand=1.0/40.0;  // 2.5% gradient on land
        double gradientLimitInSea=1.0/16.0;  // 6% gradient in the sea
        double moveLimit=50;    // Can't add or remove more than this (vertical km of rock) in one turn
        int smoothed=0;
        do {
        System.out.print(".");
        smoothed=0;
        Iterator iterator_193_ = linkSystem.getIterator();
        while (iterator_193_.hasNext()) {
        LinkPair linkpair;
        do {
        linkpair = (LinkPair) iterator_193_.next();
        } while (iterator_193_.hasNext()  && (Math.random() < 0.6 || linkpair.getCount() < 2));
        TecPoint tecpoint;
        TecPoint tecpoint2;
        if (linkpair.getA().getSurfaceHeight() > linkpair.getB().getSurfaceHeight()) {
        tecpoint2 = linkpair.getA();
        tecpoint = linkpair.getB();
        } else {
        tecpoint = linkpair.getA();
        tecpoint2 = linkpair.getB();
        }
        // Move rock from p1 to p2
        double diff = (tecpoint2.getSurfaceHeight() - tecpoint.getSurfaceHeight());
        double dist= linkpair.getLength();
        double gradient=diff/dist;
        if (tecpoint2.heightAboveSeaLevel()>0 && gradient>=gradientLimitOnLand) {
        // It's above the gradient limit (on land)
        double depthToRemove=-(gradientLimitOnLand*dist+tecpoint.getSurfaceHeight()-tecpoint2.getSurfaceHeight())/(1+tecpoint2.getArea()/tecpoint.getArea());
        if (depthToRemove<0.001) depthToRemove=0.001;
        double depthToAdd=depthToRemove*tecpoint2.getArea()/tecpoint.getArea();
        if (depthToRemove<0 || depthToAdd<0) {System.out.println("depthToRemove="+depthToRemove+",  depthToAdd="+depthToAdd+", highArea="+tecpoint2.getArea()+", lowArea="+tecpoint.getArea());}
        else if (depthToRemove<moveLimit && depthToAdd<moveLimit) {
        tecpoint2.remove(depthToRemove);
        tecpoint.add(depthToAdd, tecpoint2.getDensity()*0.95 + 2.7*0.05);
        smoothed++;
        }
        } else if (tecpoint2.heightAboveSeaLevel()<=0 && gradient>=gradientLimitInSea) {
        // It's above the gradient limit (in sea)
        double depthToRemove=-(gradientLimitInSea*dist+tecpoint.getSurfaceHeight()-tecpoint2.getSurfaceHeight())/(1+tecpoint2.getArea()/tecpoint.getArea());
        if (depthToRemove<0.001) depthToRemove=0.001;
        double depthToAdd=depthToRemove*tecpoint2.getArea()/tecpoint.getArea();
        if (depthToRemove<0 || depthToAdd<0) {System.out.println("depthToRemove="+depthToRemove+",  depthToAdd="+depthToAdd+", highArea="+tecpoint2.getArea()+", lowArea="+tecpoint.getArea());}
        else if (depthToRemove<moveLimit && depthToAdd<moveLimit) {
        tecpoint2.remove(depthToRemove);
        tecpoint.add(depthToAdd, tecpoint2.getDensity()*0.95 + 2.7*0.05);
        smoothed++;
        }
        }
        }
        } while (smoothed>linkSystem.size()*0.6*0.0001);*/

        // Maybe do coral reef growth?
        // Bring anything within 30d of the equator and 50m of the ocean surface to the surface.
  	/*System.out.print("Growing coral reefs...");
        for (int i=0; i<points.size(); i++) {
        TecPoint p=getPoint(i);
        if (Math.abs(p.getLat())<Math.toRadians(30) && p.heightAboveSeaLevel()<0 && p.heightAboveSeaLevel()>-0.050) {
        p.add(-p.heightAboveSeaLevel()*Math.random(), 2); // Assume limestone density 2
        }
        }
        System.out.println("done");*/

        System.out.print( "Smoothing..." );
        double iceAgeSeaLevel = -0.140 * Math.random(); // Ice ages will reduce sea levels by up to 140m
        for( int rep = 0; rep < 1; rep++ ){
            Iterator iterator_193_ = m_linkSystem.getIterator();
            while( iterator_193_.hasNext() ){
                LinkPair linkpair;
                do {
                    linkpair = (LinkPair)iterator_193_.next();
                } while( iterator_193_.hasNext() && (Math.random() < 0.5 || linkpair.getCount() < 2) );
                TecPoint tecpoint;
                TecPoint tecpoint2;
                if( linkpair.getA().getSurfaceHeight() > linkpair.getB().getSurfaceHeight() ){
                    tecpoint2 = linkpair.getA();
                    tecpoint = linkpair.getB();
                }
                else {
                    tecpoint = linkpair.getA();
                    tecpoint2 = linkpair.getB();
                }
                // Move rock from p1 to p2
                double diff = (tecpoint2.getSurfaceHeight() - tecpoint.getSurfaceHeight());
                double dist = linkpair.getLength();
                double gradient = diff / dist;
                if( Math.random() < 0.0001 )
                    System.out.println( "             Gradient=" + gradient );
                if( tecpoint2.heightAboveSeaLevel() > 1400 ){  // Where "mountains" start - high erosion above here
                    if( Math.random() < 1.5 ){
                        double depthToRemove = Math.min( moveLimit, diff / (1 + tecpoint2.getArea() / tecpoint.getArea()) );
                        depthToRemove = Math.max( 0, Math.min( depthToRemove, tecpoint2.getDepth() - 4 ) ); // Don't make the column shorter than 4km
                        double depthToAdd = depthToRemove * tecpoint2.getArea() / tecpoint.getArea();
                        if( depthToRemove < 0 || depthToAdd < 0 ){
                            System.out.println( "depthToRemove=" + depthToRemove + ",  depthToAdd=" + depthToAdd + ", highArea=" + tecpoint2.getArea() + ", lowArea=" + tecpoint.getArea() );
                            System.exit( 1 );
                        }
                        tecpoint2.remove( depthToRemove );
                        tecpoint.add( depthToAdd, tecpoint2.getDensity() * 0.95 + 2.3 * 0.05 ); // Slowly make the rock closer to 2.3 in density (due to now being sedimentary)
                    }
                }
                else if( tecpoint2.heightAboveSeaLevel() > iceAgeSeaLevel ){
                    if( Math.random() < 0.001 ){
                        // It's "legal", but randomly smooth it anyway, to provide gradual erosion over the aeons...
                        double depthToRemove = Math.min( moveLimit, diff / (1 + tecpoint2.getArea() / tecpoint.getArea()) );
                        depthToRemove = Math.max( 0, Math.min( depthToRemove, tecpoint2.getDepth() - 4 ) ); // Don't make the column shorter than 4km
                        double depthToAdd = depthToRemove * tecpoint2.getArea() / tecpoint.getArea();
                        if( depthToRemove < 0 || depthToAdd < 0 ){
                            System.out.println( "depthToRemove=" + depthToRemove + ",  depthToAdd=" + depthToAdd + ", highArea=" + tecpoint2.getArea() + ", lowArea=" + tecpoint.getArea() );
                            System.exit( 1 );
                        }
                        tecpoint2.remove( depthToRemove );
                        tecpoint.add( depthToAdd, tecpoint2.getDensity() * 0.95 + 2.3 * 0.05 ); // Slowly make the rock closer to 2.3 in density (due to now being sedimentary)
                    }
                }
                else if( Math.random() < 0.0005 ){  // Set to zero to cancel underwater smoothing
                    // It's "legal", but randomly smooth it anyway, to provide EVEN MORE gradual erosion over the aeons UNDERWATER...
                    double depthToRemove = Math.min( moveLimit, diff / (1 + tecpoint2.getArea() / tecpoint.getArea()) );
                    depthToRemove = Math.max( 0, Math.min( depthToRemove, tecpoint2.getDepth() - 4 ) ); // Don't make the column shorter than 4km
                    double depthToAdd = depthToRemove * tecpoint2.getArea() / tecpoint.getArea();
                    if( depthToRemove < 0 || depthToAdd < 0 ){
                        System.out.println( "depthToRemove=" + depthToRemove + ",  depthToAdd=" + depthToAdd + ", highArea=" + tecpoint2.getArea() + ", lowArea=" + tecpoint.getArea() );
                        System.exit( 1 );
                    }
                    tecpoint2.remove( depthToRemove );
                    tecpoint.add( depthToAdd, tecpoint2.getDensity() * 0.95 + 2.3 * 0.05 ); // Slowly make the rock closer to 2.3 in density (due to now being sedimentary)
                }
            }
        }
        System.out.println( "done" );

        // Melt the base off ridiculously high mountains
        int meltcount = 0;
        for( int i = 0; i < getNumPoints(); i++ )
            if( getPoint( i ).getBaseDepth() < -60 ){
                // Melt that sucka!
                double melt = Math.min( 100, -getPoint( i ).getBaseDepth() - 58 );
                //System.out.println("Melting "+(float)melt+"km off the base of a mountain (its baseDepth was "+getPoint(i).getBaseDepth()+"km)");
                getPoint( i ).remove( melt );
                meltcount++;
            }
        if( meltcount > 0 )
            System.out.println( "Melted the base off " + meltcount + " points due to mountains being too high" );

        // Now we've eroded things, redo the sealevel:
        pourOnWater();

        // Set Colours
        for( int i_39_ = 0; i_39_ < getNumPoints(); i_39_++ )
            getPoint( i_39_ ).setColor( m_colorMap.map( getPoint( i_39_ ).getSurfaceHeight() - TecPoint.seaLevel ) );
        // store interpolated colours in the links
        Color c1, c2;
        ArrayList tempVec = new ArrayList( getLinkSystem().getCollection() );
        for( int i = 0; i < tempVec.size(); i++ ){
            lp = (LinkPair)tempVec.get( i );
            c1 = lp.getA().getColor();
            c2 = lp.getB().getColor();
            lp.col = new Color( (c1.getRed() + c2.getRed()) / 2, (c1.getGreen() + c2.getGreen()) / 2, (c1.getBlue() + c2.getBlue()) / 2 );
        }
        m_epoch++;
    }

    public void calcVolCap( TecPoint p, double gradientLimitOnLand, double gradientLimitInSea ){
        p.volCap = 10e10;
        p.volCap2 = 10e10;
        ArrayList linkedPoints = m_linkSystem.getPointLinks( p );
        for( int i = 0; i < linkedPoints.size(); i++ ){
            TecPoint p2 = (TecPoint)linkedPoints.get( i );
            TecPoint tecpoint;
            TecPoint tecpoint2;
            if( p.getSurfaceHeight() > p2.getSurfaceHeight() ){
                tecpoint2 = p;
                tecpoint = p2;
            }
            else {
                tecpoint = p;
                tecpoint2 = p2;
            }
            double diff = (tecpoint2.getSurfaceHeight() - tecpoint.getSurfaceHeight());
            double dist = tecpoint.getPos().distance( tecpoint2.getPos() );
            double gradient = diff / dist;
            // Calculate limit for tecpoint
            double gradLimit = gradientLimitOnLand;
            if( tecpoint.heightAboveSeaLevel() < 0 )
                gradLimit = gradientLimitInSea;
            tecpoint.volCap = Math.min( tecpoint.volCap, tecpoint.getArea() * (diff + gradLimit * dist) );
            tecpoint.volCap = Math.min( tecpoint.volCap, tecpoint.getArea() * diff );    // Limit due to not wanting tp to grow higher than tp2
            tecpoint.volCap2 = Math.min( tecpoint.volCap2, tecpoint.getArea() * (diff + gradLimit * dist) );
            // Calculate limit for tecpoint2
            gradLimit = gradientLimitOnLand;
            if( tecpoint2.heightAboveSeaLevel() < 0 )
                gradLimit = gradientLimitInSea;
            tecpoint2.volCap = Math.min( tecpoint2.volCap, tecpoint2.getArea() * (gradLimit * dist - diff) );
            tecpoint2.volCap2 = Math.min( tecpoint2.volCap2, tecpoint2.getArea() * (gradLimit * dist - diff) );
        }
    }

    // reDelaunay the whole planet to keep it up to date
    public void redelaunay(){
        System.out.println( "Redelaunaying..." );
        long time = System.currentTimeMillis();

        // Empty all the gridBoxes
        for( int i = 0; i < m_gridSize; i++ ){
            for( int j = 0; j < m_gridSize; j++ ){
                for( int k = 0; k < m_gridSize; k++ ){
                    if( m_gridBox[i][j][k] != null )
                        m_gridBox[i][j][k].clear();
                }
            }
        }

        // Put all the points in the gridBoxes
        for( int i = 0; i < m_points.size(); i++ )
            gridBoxAdd( (TecPoint)m_points.get( i ) );

        // The points have probably all moved, so let's recalc all the tets
        for( int i = 0; i < m_tets.size(); i++ )
            ((Tet)m_tets.get( i )).calc();

        // Expand the zone of invalidation
        System.out.println( "Expanding zone of invalidation..." );
        ArrayList invalidTets = new ArrayList( m_tets.size() );
        /*for (int i = 0; i <= 1; i++) {
        for (int i_198_ = 0; i_198_ < tets.size(); i_198_++) {
        Tet tet = (Tet) tets.get(i_198_);
        if (!tet.b.isValid() || !tet.c.isValid() || !tet.d.isValid())
        invalidTets.add(tet);
        }
        for (int i_199_ = 0; i_199_ < invalidTets.size(); i_199_++) {
        Tet tet = (Tet) invalidTets.get(i_199_);
        tet.b.setValid(false);
        tet.c.setValid(false);
        tet.d.setValid(false);
        }
        invalidTets.removeAllElements();
        }*/

        // Check the existing possibly invalid tets for actual validity
        System.out.print( "Checking validity..." );
        long validityTime = System.currentTimeMillis();
        /*  // Old validity checking method
        for (int i = 0; i < tets.size(); i++) {
        Tet tet = (Tet) tets.get(i);
        if (!tet.b.isValid() || !tet.c.isValid() || !tet.d.isValid()) {
        HashSet nearby = pointsSurrounding(tet.b.getPos(), tet.c.getPos(), tet.d.getPos());
        // Now check against all nearby points
        boolean ok = true;
        int check = 0;
        tet.calc();
        Iterator iter=nearby.iterator();
        while (ok && iter.hasNext()) {
        //for (; ok && check < nearby.size(); check++) {
        TecPoint checkpoint = (TecPoint) iter.next();//nearby.get(check);
        if (!tet.uses(checkpoint) && tet.contains(checkpoint.getPos()))
        ok = false;
        }
        if (!ok)  invalidTets.add(tet);
        }
        }*/
        // New validity checking method
        for( int i = 0; i < m_tets.size(); i++ ){
            Tet tet = (Tet)m_tets.get( i );
            //if (!tet.b.isValid() || !tet.c.isValid() || !tet.d.isValid()) {
            if( !checkIfValid( tet ) )
                invalidTets.add( tet );
            //}
        }
        System.out.println( "Done in " + ((System.currentTimeMillis() - validityTime) / 1000.0) + " seconds" );

        System.out.println( "Removing " + invalidTets.size() + " tets" );
        if( invalidTets.size() % 2 == 1 ){
            boolean bool = true;
        }
        else {
            boolean bool = false;
        }
        for( int i = 0; i < invalidTets.size(); i++ ){
            Tet tet = (Tet)invalidTets.get( i );
            // Bad tet! Remove it, and tidy the linksystem
            m_tets.remove( tet );
            m_linkSystem.removeLink( tet.b, tet.c );
            m_linkSystem.removeLink( tet.b, tet.d );
            m_linkSystem.removeLink( tet.c, tet.d );
        }

        // Build list of active tets
        ArrayList activeTets = new ArrayList( m_tets.size() );
        for( int i = 0; i < m_tets.size(); i++ ){
            Tet tet = (Tet)m_tets.get( i );
            if( m_linkSystem.getCount( tet.b, tet.c ) < 2
                    || m_linkSystem.getCount( tet.b, tet.d ) < 2
                    || m_linkSystem.getCount( tet.c, tet.d ) < 2 )
                activeTets.add( tet );
        }

        // If we removed an odd number of tets, correct the problem here
        for( int i = 0; i < activeTets.size(); i++ ){
            Tet tet = (Tet)activeTets.get( i );
            // Bad tet! Remove it, and tidy the linksystem
            m_tets.remove( tet );
            m_linkSystem.removeLink( tet.b, tet.c );
            m_linkSystem.removeLink( tet.b, tet.d );
            m_linkSystem.removeLink( tet.c, tet.d );
        }

        // Build list of active tets
        activeTets = new ArrayList( m_tets.size() );
        for( int i = 0; i < m_tets.size(); i++ ){
            Tet tet = (Tet)m_tets.get( i );
            if( m_linkSystem.getCount( tet.b, tet.c ) < 2
                    || m_linkSystem.getCount( tet.b, tet.d ) < 2
                    || m_linkSystem.getCount( tet.c, tet.d ) < 2 )
                activeTets.add( tet );
        }

        System.out.print( "Doing Delaunay tets..." );
        Tet t1 = null;
        Tet t2 = null;
        Tet t3 = null;

        boolean failedToExpand = false;
        Tet tet = null;
        int tetNum = 0;
        while( activeTets.size() > 0 && !failedToExpand && tetNum < activeTets.size() ){
            if( !failedToExpand )
                tet = (Tet)activeTets.get( tetNum );    //((Tet)activeTets.get((int) (Math.random() * (double) activeTets.size())));
            if( Math.random() < 0.01 )
                System.out.print( "." );

            t1 = null;
            t2 = null;
            t3 = null;

            Iterator iter, iter2;
            // Take the centre point and two of the surface points as the
            // start of a new tet

            // Can we use link tet.b<->tet.c?
            if( m_linkSystem.getCount( tet.b, tet.c ) <= 1 ){
                HashSet nearby = pointsSurrounding( tet.b.getPos(), tet.c.getPos() );
                if( failedToExpand ){
                    nearby = new HashSet( m_points );
                    failedToExpand = false;
                }
                iter = nearby.iterator();
                while( t1 == null && iter.hasNext() ){
                    //for (int i = 0; t1 == null && i < nearby.size(); i++) {
                    TecPoint tecpoint = (TecPoint)iter.next();//nearby.get(i);
                    if( !tecpoint.equals( tet.a ) && !tecpoint.equals( tet.b )
                            && !tecpoint.equals( tet.c ) && !tecpoint.equals( tet.d )
                            && m_linkSystem.getCount( tet.b, tecpoint ) <= 1
                            && m_linkSystem.getCount( tet.c, tecpoint ) <= 1 ){
                        Tet tet_209_ = new Tet( m_planetCenter, tet.b, tet.c, tecpoint );
                        tet_209_.calc();
                        // Now check against all nearby points
                        boolean ok = true;
                        iter2 = nearby.iterator();
                        while( ok && iter2.hasNext() ){
                            //for (int check = 0; ok && check < nearby.size(); check++) {
                            TecPoint checkPoint = (TecPoint)iter2.next();//nearby.get(check);
                            if( tecpoint != checkPoint && checkPoint != tet.b
                                    && checkPoint != tet.c
                                    && tet_209_.contains( checkPoint.getPos() ) )
                                ok = false;
                        }
                        if( ok ){
//System.out.println("Added new tet");
                            t1 = tet_209_;
                            m_tets.add( t1 );
                            activeTets.add( t1 );
                            m_linkSystem.addLink( t1.b, t1.c );
                            m_linkSystem.addLink( t1.b, t1.d );
                            m_linkSystem.addLink( t1.c, t1.d );
                        }
                    }
                }
            }

            // Can we use link tet.c<->tet.d?
            if( m_linkSystem.getCount( tet.c, tet.d ) <= 1 ){
                HashSet nearby = pointsSurrounding( tet.c.getPos(), tet.d.getPos() );
                if( failedToExpand ){
                    nearby = new HashSet( m_points );
                    failedToExpand = false;
                }
                iter = nearby.iterator();
                while( t2 == null && iter.hasNext() ){
                    //for (int i = 0; t2 == null && i < nearby.size(); i++) {
                    TecPoint tecpoint = (TecPoint)iter.next();//nearby.get(i);
                    if( !tecpoint.equals( tet.a ) && !tecpoint.equals( tet.b )
                            && !tecpoint.equals( tet.c ) && !tecpoint.equals( tet.d )
                            && m_linkSystem.getCount( tet.c, tecpoint ) <= 1
                            && m_linkSystem.getCount( tet.d, tecpoint ) <= 1 ){
                        Tet tempTet = new Tet( m_planetCenter, tet.c, tet.d, tecpoint );
                        tempTet.calc();
                        // Now check against all nearby points
                        boolean ok = true;
                        iter2 = nearby.iterator();
                        while( ok && iter2.hasNext() ){
                            //for (int i_216_ = 0; ok && i_216_ < nearby.size(); i_216_++) {
                            TecPoint checkPoint = (TecPoint)iter2.next();//nearby.get(i_216_);
                            if( tecpoint != checkPoint && checkPoint != tet.c
                                    && checkPoint != tet.d
                                    && tempTet.contains( checkPoint.getPos() ) )
                                ok = false;
                        }
                        if( ok ){
//System.out.println("Added new tet");
                            t2 = tempTet;
                            m_tets.add( t2 );
                            activeTets.add( t2 );
                            m_linkSystem.addLink( t2.b, t2.c );
                            m_linkSystem.addLink( t2.b, t2.d );
                            m_linkSystem.addLink( t2.c, t2.d );
                        }
                    }
                }
            }

            // Can we use link tet.b<->tet.d?
            if( m_linkSystem.getCount( tet.b, tet.d ) <= 1 ){
                HashSet nearby = pointsSurrounding( tet.b.getPos(), tet.d.getPos() );
                if( failedToExpand ){
                    nearby = new HashSet( m_points );
                    failedToExpand = false;
                }
                iter = nearby.iterator();
                while( t3 == null && iter.hasNext() ){
                    //for (int i = 0; t3 == null && i < nearby.size(); i++) {
                    TecPoint tecpoint = (TecPoint)iter.next();//nearby.get(i);
                    if( !tecpoint.equals( tet.a ) && !tecpoint.equals( tet.b )
                            && !tecpoint.equals( tet.c ) && !tecpoint.equals( tet.d )
                            && m_linkSystem.getCount( tet.b, tecpoint ) <= 1
                            && m_linkSystem.getCount( tet.d, tecpoint ) <= 1 ){
                        Tet tempTet = new Tet( m_planetCenter, tet.b, tet.d, tecpoint );
                        tempTet.calc();
                        // Now check against all nearby points
                        boolean ok = true;
                        iter2 = nearby.iterator();
                        while( ok && iter2.hasNext() ){
                            //for (int check = 0; ok && check < nearby.size(); check++) {
                            TecPoint checkPoint = (TecPoint)iter2.next();//nearby.get(check);
                            if( tecpoint != checkPoint && checkPoint != tet.b
                                    && checkPoint != tet.d
                                    && tempTet.contains( checkPoint.getPos() ) )
                                ok = false;
                        }
                        if( ok ){
//System.out.println("Added new tet");
                            t3 = tempTet;
                            m_tets.add( t3 );
                            activeTets.add( t3 );
                            m_linkSystem.addLink( t3.b, t3.c );
                            m_linkSystem.addLink( t3.b, t3.d );
                            m_linkSystem.addLink( t3.c, t3.d );
                        }
                    }
                }
            }
            if( m_linkSystem.getCount( tet.b, tet.c ) >= 2
                    && m_linkSystem.getCount( tet.b, tet.d ) >= 2
                    && m_linkSystem.getCount( tet.c, tet.d ) >= 2 ){
                tetNum++;//activeTets.remove(remTetNum);
            }
            else {
                System.out.println( "Completely failed to expand from tet.\nLinkcounts are " + m_linkSystem.getCount( tet.b, tet.c ) + "," + m_linkSystem.getCount( tet.c, tet.d ) + "," + m_linkSystem.getCount( tet.b, tet.d ) );
                // Detailed error report/debugging info
                if( m_tets.contains( tet ) ){
                    System.out.println( "Tet is #" + m_tets.indexOf( tet ) + " of " + m_tets.size() + " tets" );
                }
                else
                    System.out.println( "Tet isn't in tets" );
                if( activeTets.contains( tet ) ){
                    System.out.println( "and #" + activeTets.indexOf( tet ) + " of " + activeTets.size() + " activeTets" );
                }
                else
                    System.out.println( "Tet isn't in activeTets" );
                System.out.print( "tet.a=(" + tet.a.getX() + "," + tet.a.getY() + "," + tet.a.getZ() + ")" );
                if( m_points.contains( tet.a ) )
                    System.out.println( " and is point #" + m_points.indexOf( tet.a ) );
                else
                    System.out.println( " and isn't in points!" );
                System.out.print( "tet.b=(" + tet.b.getX() + "," + tet.b.getY() + "," + tet.b.getZ() + ")" );
                if( m_points.contains( tet.b ) )
                    System.out.println( " and is point #" + m_points.indexOf( tet.b ) );
                else
                    System.out.println( " and isn't in points!" );
                System.out.print( "tet.c=(" + tet.c.getX() + "," + tet.c.getY() + "," + tet.c.getZ() + ")" );
                if( m_points.contains( tet.c ) )
                    System.out.println( " and is point #" + m_points.indexOf( tet.c ) );
                else
                    System.out.println( " and isn't in points!" );
                System.out.print( "tet.d=(" + tet.d.getX() + "," + tet.d.getY() + "," + tet.d.getZ() + ")" );
                if( m_points.contains( tet.d ) )
                    System.out.println( " and is point #" + m_points.indexOf( tet.d ) );
                else
                    System.out.println( " and isn't in points!" );
                if( t1 == null ){
                    System.out.println( "t1 is null" );
                    if( m_linkSystem.getCount( tet.b, tet.c ) <= 1 ){
                        System.out.println( "We should have expanded from b-c, so why didn't we?" );
                        HashSet nearby = pointsSurrounding( tet.b.getPos(), tet.c.getPos() );
                        //int i = 0;
                        TecPoint bestPoint = null;
                        double bestDist = 0.0, closestDist = Double.MAX_VALUE;
                        iter = nearby.iterator();
                        while( t1 == null && iter.hasNext() ){
                            //for (/**/; t1 == null && i < nearby.size(); i++) {
                            TecPoint pk = (TecPoint)iter.next();//nearby.get(i);
                            if( !pk.equals( tet.a )
                                    && !pk.equals( tet.b )
                                    && !pk.equals( tet.c )
                                    && !pk.equals( tet.d )
                                    && (m_linkSystem.getCount( tet.b, pk ) <= 1)
                                    && (m_linkSystem.getCount( tet.c, pk ) <= 1) ){
                                Tet tempTet = new Tet( m_planetCenter, tet.b, tet.c, pk );
                                tempTet.calc();
                                // Now check against all nearby points
                                boolean ok = true;
                                int check = 0;
                                TecPoint checkPoint = null, closestPoint = null;
                                closestDist = Double.MAX_VALUE;
                                iter2 = nearby.iterator();
                                while( ok && iter2.hasNext() ){
                                    //for (/**/; ok && check < nearby.size(); check++) {
                                    checkPoint = (TecPoint)iter2.next();//nearby.get(check);
                                    double distRatio = ((tempTet.center.distance( checkPoint.getPos() )) / Math.sqrt( tempTet.radiussq ));
                                    if( pk != checkPoint && checkPoint != tet.b
                                            && checkPoint != tet.c
                                            && distRatio < closestDist ){
                                        // This checkPoint is the closest so far, record it
                                        TecPoint tecpoint_233_ = checkPoint;
                                        closestDist = distRatio;
                                    }
                                }
                            }
                            // If that pk has the best results so far, record it
                            if( closestDist > bestDist ){
                                bestPoint = pk;
                                bestDist = closestDist;
                            }
                        }
                        Tet temp = new Tet( m_planetCenter, tet.b, tet.c, bestPoint );
                        temp.calc();
                        System.out.print( "The best possible tet for this side involved a point at (" + bestPoint.getX() + "," + bestPoint.getY() + "," + bestPoint.getZ() + ") " );
                        System.out.println( "whose closest stopper had a distanceRatio of " + bestDist + " away." );
                        System.out.println( "The circumradius was inner=" + Math.sqrt( temp.innersq ) + ", outer=" + Math.sqrt( temp.outersq ) );
                    }
                }
                else
                    System.out.println( "t1 is NOT null" );
                if( t2 == null ){
                    System.out.println( "t2 is null" );
                    if( m_linkSystem.getCount( tet.c, tet.d ) <= 1 ){
                        System.out.println( "We should have expanded from c-d, so why didn't we?" );
                        HashSet nearby = pointsSurrounding( tet.c.getPos(), tet.d.getPos() );
                        int i = 0;
                        TecPoint bestPoint = null;
                        double bestDist = 0.0;
                        double closestDist = Double.MAX_VALUE;
                        iter = nearby.iterator();
                        while( t1 == null && iter.hasNext() ){
                            //for (/**/; t1 == null && i < nearby.size(); i++) {
                            TecPoint pk = (TecPoint)iter.next();//nearby.get(i);
                            if( !pk.equals( tet.a )
                                    && !pk.equals( tet.b )
                                    && !pk.equals( tet.c )
                                    && !pk.equals( tet.d )
                                    && (m_linkSystem.getCount( tet.c, pk ) <= 1)
                                    && (m_linkSystem.getCount( tet.d, pk ) <= 1) ){
                                Tet tempTet = new Tet( m_planetCenter, tet.c, tet.d, pk );
                                tempTet.calc();
                                // Now check against all nearby points
                                boolean ok = true;
                                int check = 0;
                                closestDist = Double.MAX_VALUE;
                                iter2 = nearby.iterator();
                                while( ok && iter2.hasNext() ){
                                    //for (/**/; ok && check < nearby.size(); check++) {
                                    TecPoint checkPoint = (TecPoint)iter2.next();//nearby.get(check);
                                    double d_244_ = ((tempTet.center.distance( checkPoint.getPos() )) / Math.sqrt( tempTet.radiussq ));
                                    if( checkPoint != pk && checkPoint != tet.c && checkPoint != tet.d && d_244_ < closestDist ){
                                        // This checkPoint is the closest so far, record it
                                        TecPoint closestPoint = checkPoint;
                                        closestDist = d_244_;
                                    }
                                }
                            }
                            // If that pk has the best results so far, record it
                            if( closestDist > bestDist ){
                                bestPoint = pk;
                                bestDist = closestDist;
                            }
                        }
                        Tet tempTet = new Tet( m_planetCenter, tet.c, tet.d, bestPoint );
                        tempTet.calc();
                        System.out.print( "The best possible tet for this side involved a point at (" + bestPoint.getX() + "," + bestPoint.getY() + "," + bestPoint.getZ() + ") " );
                        System.out.println( "whose closest stopper had a distanceRatio of " + bestDist + " away." );
                        System.out.println( "The circumradius was inner=" + Math.sqrt( tempTet.innersq ) + ", outer=" + Math.sqrt( tempTet.outersq ) );
                    }
                }
                else
                    System.out.println( "t2 is NOT null" );
                if( t3 == null ){
                    System.out.println( "t3 is null" );
                    if( m_linkSystem.getCount( tet.b, tet.d ) <= 1 ){
                        System.out.println( "We should have expanded from b-d, so why didn't we?" );
                        HashSet nearby = pointsSurrounding( tet.b.getPos(), tet.d.getPos() );
                        int i = 0;
                        TecPoint bestPoint = null;
                        double bestDist = 0.0;
                        double closestDist = Double.MAX_VALUE;
                        iter = nearby.iterator();
                        while( t1 == null && iter.hasNext() ){
                            //for (/**/; t1 == null && i < nearby.size(); i++) {
                            TecPoint pk = (TecPoint)iter.next();//nearby.get(i);
                            if( !pk.equals( tet.a ) && !pk.equals( tet.b )
                                    && !pk.equals( tet.c ) && !pk.equals( tet.d )
                                    && (m_linkSystem.getCount( tet.b, pk ) <= 1)
                                    && (m_linkSystem.getCount( tet.d, pk ) <= 1) ){
                                Tet temp = new Tet( m_planetCenter, tet.b, tet.d, pk );
                                temp.calc();
                                // Now check against all nearby points
                                boolean ok = true;
                                int check = 0;
                                Object object_253_ = null;
                                Object object_254_ = null;
                                closestDist = Double.MAX_VALUE;
                                iter2 = nearby.iterator();
                                while( ok && iter2.hasNext() ){
                                    //for (/**/; ok && check < nearby.size(); check++) {
                                    TecPoint checkPoint = (TecPoint)iter2.next();//nearby.get(check);
                                    double d_256_ = ((temp.center.distance( checkPoint.getPos() )) / Math.sqrt( temp.radiussq ));
                                    if( checkPoint != pk && checkPoint != tet.b
                                            && checkPoint != tet.d
                                            && d_256_ < closestDist ){
                                        // This checkPoint is the closest so far, record it
                                        TecPoint tecpoint_257_ = checkPoint;
                                        closestDist = d_256_;
                                    }
                                }
                            }
                            // If that pk has the best results so far, record it
                            if( closestDist > bestDist ){
                                bestPoint = pk;
                                bestDist = closestDist;
                            }
                        }
                        Tet temp = new Tet( m_planetCenter, tet.b, tet.d, bestPoint );
                        temp.calc();
                        System.out.print( "The best possible tet for this side involved a point at (" + bestPoint.getX() + "," + bestPoint.getY() + "," + bestPoint.getZ() + ") " );
                        System.out.println( "whose closest stopper had a distanceRatio of " + bestDist + " away." );
                        System.out.println( "The circumradius was inner=" + Math.sqrt( temp.innersq ) + ", outer=" + Math.sqrt( temp.outersq ) );
                    }
                }
                else
                    System.out.println( "t3 is NOT null" );
                failedToExpand = true;
            }
        }
        if( failedToExpand ){
            // OK, we got stuck trying to reDelaunay
            System.out.println( "=====================\nARG! :(  Redelaunay failed\nTry to rescue it by doing a first-time delaunay\n=====================" );
            // Try to rescue it by doing a first-time Delaunay
            delaunay();
        }

        resetTetGridSystem(); // Hopefully this fixes the bug in saveJPG which was introduced in version 5
        calculateAreas();
        pourOnWater();
        for( int i = 0; i < m_points.size(); i++ )
            getPoint( i ).setValid( true );
        System.out.println( "done!" );
        System.out.println( "Whole planet reDelaunayed in " + ((float)(System.currentTimeMillis() - time) / 60000.0F) + " minutes! (" + ((float)(System.currentTimeMillis() - time) / 1000.0F) + " seconds)" );
        System.out.println( m_tets.size() + " tets" );
    }

    private void calculateAreas(){
        System.out.print( "Calculating polygon areas..." );
        for( int i = 0; i < m_points.size(); i++ )
            getPoint( i ).setArea( 0.0 );
        double d = 0.0;
        for( int i = 0; i < m_tets.size(); i++ ){
            Tet tet = (Tet)m_tets.get( i );
            double d_259_ = tet.area / 3.0;
            tet.b.addArea( d_259_ );
            tet.c.addArea( d_259_ );
            tet.d.addArea( d_259_ );
            d += tet.area;
        }
        System.out.println( "done. Planetary surface area=" + d + " km^2" );
        m_planetSurfaceArea = d;
    }

    private void pourOnWater(){
        // Pour water onto points, calc ocean depths
        double waterVolume = 1.36E9;	// Total water on planet, in km^3
        double addedVolume = 0.0;	// The volume already poured onto the planet
        ArrayList coveredPoints = new ArrayList();
        PointsByHeight pointsbyheight = new PointsByHeight();
        for( int i_182_ = 0; i_182_ < m_points.size(); i_182_++ )
            pointsbyheight.add( getPoint( i_182_ ) );
        // Move the lowest point from the treeSet to coveredPoints, until the total required depth of water doesn't also cover the next point in the treeSet
        double coveredArea = 0.0;
        double baseHeight = ((TecPoint)pointsbyheight.first().get( 0 )).getSurfaceHeight();
        System.out.println( "Lowest point on planet is " + baseHeight + " km" );
        System.out.println( "Highest point on planet is " + (((TecPoint)pointsbyheight.last().get( 0 )).getSurfaceHeight()) + " km" );
        double currentHeight = baseHeight;
        boolean done = false;
        // Add first points
        ArrayList nextPoints = pointsbyheight.first();
        double nextHeight = ((TecPoint)nextPoints.get( 0 )).getSurfaceHeight();
        for( int i_189_ = 0; i_189_ < nextPoints.size(); i_189_++ ){
            coveredPoints.add( nextPoints.get( i_189_ ) );
            coveredArea += ((TecPoint)nextPoints.get( i_189_ )).getArea();
        }
        pointsbyheight.removeFirst();
        double requiredDepth;
        do {
            nextPoints = pointsbyheight.first();
            currentHeight = nextHeight;
            nextHeight = ((TecPoint)nextPoints.get( 0 )).getSurfaceHeight();
            // Now check if we can fit the water in the current area
            requiredDepth = (waterVolume - addedVolume) / coveredArea;	// Required additional depth using the current area
            if( currentHeight + requiredDepth > nextHeight ){
                // Water will overflow onto next lowest point
                // Pour on enough to fill the already-being-used points
                addedVolume += coveredArea * (nextHeight - currentHeight);
                // Add more points
                nextPoints = pointsbyheight.first();
                nextHeight = ((TecPoint)nextPoints.get( 0 )).getSurfaceHeight();
                for( int i_191_ = 0; i_191_ < nextPoints.size(); i_191_++ ){
                    coveredPoints.add( nextPoints.get( i_191_ ) );
                    coveredArea += ((TecPoint)nextPoints.get( i_191_ )).getArea();
                }
                pointsbyheight.removeFirst();
            }
            else
                done = true;
        } while( !done && pointsbyheight.size() > 0 );
        TecPoint.seaLevel = currentHeight + requiredDepth;
        if( pointsbyheight.size() == 0 )
            System.out.println( "Whole surface covered by water!!!!" );
        System.out.println( "Area covered by water is " + coveredArea + " km^2" );
        System.out.println( "Sea level is " + (currentHeight + requiredDepth) + " km above datum" );
    }
    /////////////////////////
    // JPG saving bits
    /////////////////////////

    private void saveJPG( BufferedImage image, File file, float quality ) throws IOException{
        try {
            Iterator iter = ImageIO.getImageWritersByFormatName( "jpeg" );
            //Then, choose the first image writer available (unless you want to choose a specific writer) and create an ImageWriter instance:
            ImageWriter writer = (ImageWriter)iter.next();
            // instantiate an ImageWriteParam object with default compression options
            ImageWriteParam iwp = writer.getDefaultWriteParam();

            //Now, we can set the compression quality:
            iwp.setCompressionMode( ImageWriteParam.MODE_EXPLICIT );
            iwp.setCompressionQuality( 1 );   // a float between 0 and 1
            // 1 specifies minimum compression and maximum quality

            //Output the file:
            FileImageOutputStream output = new FileImageOutputStream( file );
            writer.setOutput( output );
            writer.write( null, new IIOImage( image, null, null ), iwp );
        }
        catch( Exception exception ){
            System.out.println( "Error saving JPG file - " + exception );
            exception.printStackTrace( System.out );
        }
    }

    public void saveJPG(){
        long time = System.currentTimeMillis();
        // Render to JPG
        int i = m_imageSettings.singleSize();
        int imageWidth = i;
        if( !m_imageSettings.singleSquare() )
            imageWidth = i * 2;
        m_imageBuffer = new BufferedImage( imageWidth, i, 1 );
        renderImage( true, m_imageBuffer, imageWidth, i, m_imageSettings.singleFaults() );

        // Now save it
        try {
            int num;
            if( !new File( "TectonicPlanet/Images" ).exists() )
                new File( "TectonicPlanet/Images" ).mkdirs();
            for( num = 1; new File( "TectonicPlanet/Images/Image" + numberCode( num ) + ".jpg" ).exists(); num++ ){
                /* empty */
            }
            File file = new File( "TectonicPlanet/Images/Image" + numberCode( num ) + ".jpg" );
            //ImageIO.write(image, "jpg", file);
            saveJPG( m_imageBuffer, file, 1 );
        }
        catch( Exception exception ){
            System.out.println( "Exception! - " + exception );
            exception.printStackTrace( System.out );
        }
        System.out.println( "Images saved in " + (System.currentTimeMillis() - time) / 60000.0f + " minutes! (" + (System.currentTimeMillis() - time) / 1000.0f + " seconds)" );
    }

    public void saveJPGsequence(){
        if( m_imageSequenceNumber == -1 ){
            m_imageSequenceNumber = 1;
            while( new File( "TectonicPlanet/Images/Image" + m_imageSequenceNumber + "_1.jpg" ).exists()
                    || new File( "TectonicPlanet/Images/Image" + m_imageSequenceNumber + "_" + numberCode( 1 ) + ".jpg" ).exists()
                    || new File( "TectonicPlanet/Images/Image" + numberCode( m_imageSequenceNumber ) + "_" + numberCode( 1 ) + ".jpg" ).exists() ){
                m_imageSequenceNumber++;
            }
        }
        long time = System.currentTimeMillis();
        // Render to JPG
        int i = m_imageSettings.seqSize();
        int imageWidth = i;
        if( !m_imageSettings.seqSquare() )
            imageWidth = i * 2;
        m_imageBuffer = new BufferedImage( imageWidth, i, 1 );
        renderImage( false, m_imageBuffer, imageWidth, i, m_imageSettings.seqFaults() );
        // Now save it
        try {
            int num;
            if( !new File( "TectonicPlanet/Images" ).exists() )
                new File( "TectonicPlanet/Images" ).mkdirs();
            for( num = 1; new File( "TectonicPlanet/Images/Image" + numberCode( m_imageSequenceNumber ) + "_" + numberCode( num ) + ".jpg" ).exists(); num++ ){
                /* empty */
            }
            File file = new File( "TectonicPlanet/Images/Image" + numberCode( m_imageSequenceNumber ) + "_" + numberCode( num ) + ".jpg" );
            //ImageIO.write(image, "jpg", file);
            saveJPG( m_imageBuffer, file, 1 );
        }
        catch( Exception exception ){
            System.out.println( "Exception! - " + exception );
            exception.printStackTrace( System.out );
        }
        System.out.println( "Images saved in " + (System.currentTimeMillis() - time) / 60000.0f + " minutes! (" + (System.currentTimeMillis() - time) / 1000.0f + " seconds)" );
    }

    public String numberCode( int num ){
        String out = "" + num;
        while( out.length() < 4 ){
            out = "0" + out;
        }
        return out;
    }

    public String numberCode( int num, int len ){
        String out = "" + num;
        while( out.length() < len ){
            out = "0" + out;
        }
        return out;
    }

    private void renderImage( boolean single, BufferedImage image, int width, int height, boolean showFaults ){
        if( single ){
            if( m_imageSettings.singleCylindrical() )
                renderCylindrical( image, width, height, showFaults, m_imageSettings.singleAgeDots() );
            if( m_imageSettings.singleEllipse() )
                renderElliptical( image, width, height, showFaults, m_imageSettings.singleAgeDots() );
        }
        else {
            if( m_imageSettings.seqCylindrical() )
                renderCylindrical( image, width, height, showFaults, m_imageSettings.seqAgeDots() );
            if( m_imageSettings.seqEllipse() )
                renderElliptical( image, width, height, showFaults, m_imageSettings.seqAgeDots() );
        }
    }

    private void renderCylindrical( BufferedImage image, int width, int height, boolean showFaults, boolean showAgeDots ){
        Graphics g = image.getGraphics();
        g.setColor( Color.white );
        g.fillRect( 0, 0, width, height );
        Point3d pos = new Point3d();
        double lon, lat, dist1, dist2, dist3, temp, a, b, c, area1, area2, area3;
        TecPoint closest1 = null, closest2 = null, closest3 = null;
        Tet tet = null;
        //Vector gridBox;
        for( int x = 0; x < width; x++ ){
            if( x % 20 == 0 )
                System.out.println( (x * 100 / width) + "%" );
            lon = (double)x / width * 2 * Math.PI;
            for( int y = 0; y < height; y++ ){
                lat = -Math.PI / 2 + (double)y / (height - 1) * Math.PI;
                pos.x = Math.sin( lon ) * Math.cos( lat ) * m_planetRadius;
                pos.y = Math.sin( lat ) * m_planetRadius;
                pos.z = Math.cos( lon ) * Math.cos( lat ) * m_planetRadius;

                /*dist1=planetRadius*planetRadius;
                dist2=planetRadius*planetRadius;
                dist3=planetRadius*planetRadius;
                closest1=closest2=null;
                gridBox=pointsSurrounding(getGridBoxX(pos.x),getGridBoxY(pos.y),getGridBoxZ(pos.z));
                if (gridBox!=null) {
                for (int i=0; i<gridBox.size(); i++) {
                temp=((TecPoint)gridBox.get(i)).getPos().distanceSquared(pos);
                if (temp<dist1) {
                dist3=dist2;
                closest3=closest2;
                dist2=dist1;
                closest2=closest1;
                closest1=(TecPoint)gridBox.get(i);
                dist1=temp;
                } else if (temp<dist2) {
                dist3=dist2;
                closest3=closest2;
                closest2=(TecPoint)gridBox.get(i);
                dist2=temp;
                } else if (temp<dist3) {
                closest3=(TecPoint)gridBox.get(i);
                dist3=temp;
                }
                }
                dist1=Math.sqrt(dist1);
                dist2=Math.sqrt(dist2);*/

                tet = getTet( pos );
                if( tet != null ){
                    closest1 = tet.b;
                    closest2 = tet.c;
                    closest3 = tet.d;
                    dist1 = pos.distance( closest1.getPos() );
                    dist2 = pos.distance( closest2.getPos() );
                    dist3 = pos.distance( closest3.getPos() );

                    // Make sure they're actually in order of size, or the faultlines break
                    // This can be removed for speedup once there's a better way of drawing on the faultlines
                    if( dist1 > dist2 ){
                        temp = dist2;
                        dist2 = dist1;
                        dist1 = temp;
                        TecPoint tempP = closest2;
                        closest2 = closest1;
                        closest1 = tempP;
                    }
                    if( dist2 > dist3 ){
                        temp = dist2;
                        dist2 = dist3;
                        dist3 = temp;
                        TecPoint tempP = closest2;
                        closest2 = closest3;
                        closest3 = tempP;
                        if( dist1 > dist2 ){
                            temp = dist2;
                            dist2 = dist1;
                            dist1 = temp;
                            tempP = closest2;
                            closest2 = closest1;
                            closest1 = tempP;
                        }
                    }

                    if( showFaults && (closest1.getPlate() != closest2.getPlate())
                            && Math.min( dist1, dist2 ) / Math.max( dist1, dist2 ) > 0.7 ){
                        image.setRGB( x, height - y - 1, Color.red.getRGB() );
                    }
                    else {
                        //dist3=Math.sqrt(dist3);
                        area1 = triangleArea( dist2, dist3, closest2.getPos().distance( closest3.getPos() ) );
                        area2 = triangleArea( dist1, dist3, closest1.getPos().distance( closest3.getPos() ) );
                        area3 = triangleArea( dist1, dist2, closest1.getPos().distance( closest2.getPos() ) );
                        area1 = Math.pow( area1, 1.5 );
                        area2 = Math.pow( area2, 1.5 );  // This powering is beautiful :)  Lovely, soft, cubic curves on the image! :D
                        area3 = Math.pow( area3, 1.5 );  // Causes slight "bobbling" in low-res areas, but smooths the jagged edges of coastlines.
                        temp = 1.0 / (area1 + area2 + area3);
                        a = area1 * temp;
                        b = area2 * temp;
                        c = area3 * temp;
                        //image.setRGB(x,height-y-1,colorMap.map((closest1.getSurfaceHeight() - TecPoint.seaLevel)*a+(closest2.getSurfaceHeight() - TecPoint.seaLevel)*b+(closest3.getSurfaceHeight() - TecPoint.seaLevel)*c).getRGB());
                        double hasl = closest1.heightAboveSeaLevel() * a + closest2.heightAboveSeaLevel() * b + closest3.heightAboveSeaLevel() * c;
                        image.setRGB( x, height - y - 1, m_colorMap.mapToRGB( hasl ) );
                        if( hasl > m_colorMap.maxValue() ){
                            //System.out.println(hasl+" -> "+tcol);
                            image.setRGB( x, height - y - 1, m_colorMap.topRGB() );
                        }
                        //image.setRGB(x,height-y-1,colorMap.mapToRGB(closest1.heightAboveSeaLevel()*a+closest2.heightAboveSeaLevel()*b+closest3.heightAboveSeaLevel()*c).getRGB());
                    }
                }
                //Thread.yield();
            }
        }
        if( showAgeDots ){
            TecPoint p;
            for( int i = 0; i < m_points.size(); i++ ){
                p = getPoint( i );
                if( p.getCreationDate() > 0 ){
                    lat = p.getLat();
                    lon = p.getLon();
                    if( lon < 0 )
                        lon += 2 * Math.PI;
                    int x = (int)(lon * width / (2 * Math.PI));
                    int y = (int)((lat + Math.PI / 2) * (height - 1) / Math.PI);
                    int age = (p.getCreationDate() * 1) % 256;
                    try {
                        image.setRGB( x, height - y - 1, new Color( age, age, age ).getRGB() );
                    }
                    catch( Exception e ){
                        System.out.println( "Couldn't print the agedot at " + x + "," + y );
                        System.out.println( e );
                    }
                }
            }
        }
    }

    private void renderElliptical( BufferedImage image, int width, int height, boolean showFaults, boolean showAgeDots ){
        Graphics g = image.getGraphics();
        Point3d pos = new Point3d();
        double lon, lat, dist1, dist2, dist3, temp, a, b, c, area1, area2, area3;
        TecPoint closest1 = null, closest2 = null, closest3 = null;
        HashSet gridBoxSet;
        for( int y1 = 0; y1 < height; y1++ ){
            int y = (int)((Math.asin( 1 - 2 * (double)y1 / (height - 1) ) + Math.PI / 2) / Math.PI * height);
            lat = -Math.PI / 2 + (double)y / (height - 1) * Math.PI;
            if( y % 20 == 0 )
                System.out.println( (100 - y * 100 / height) + "%" );
            int scanWidth = (int)(width * Math.cos( lat ));
            for( int x = (width - scanWidth) / 2; x < (width + scanWidth) / 2; x++ ){
                lon = (double)(x - (width - scanWidth) / 2) / scanWidth * 2 * Math.PI;
                pos.x = Math.sin( lon ) * Math.cos( lat ) * m_planetRadius;
                pos.y = Math.sin( lat ) * m_planetRadius;
                pos.z = Math.cos( lon ) * Math.cos( lat ) * m_planetRadius;
                dist1 = m_planetRadius * m_planetRadius;
                dist2 = m_planetRadius * m_planetRadius;
                dist3 = m_planetRadius * m_planetRadius;
                closest1 = closest2 = null;
                gridBoxSet = pointsSurrounding( getGridBoxX( pos.x ), getGridBoxY( pos.y ), getGridBoxZ( pos.z ) );
                if( gridBoxSet != null ){
                    Iterator iter = gridBoxSet.iterator();
                    while( iter.hasNext() ){
                        TecPoint tp = (TecPoint)iter.next();
                        temp = tp.getPos().distanceSquared( pos );
                        if( temp < dist1 ){
                            dist3 = dist2;
                            closest3 = closest2;
                            dist2 = dist1;
                            closest2 = closest1;
                            closest1 = tp;
                            dist1 = temp;
                        }
                        else if( temp < dist2 ){
                            dist3 = dist2;
                            closest3 = closest2;
                            closest2 = tp;
                            dist2 = temp;
                        }
                        else if( temp < dist3 ){
                            closest3 = tp;
                            dist3 = temp;
                        }
                    }
                    dist1 = Math.sqrt( dist1 );
                    dist2 = Math.sqrt( dist2 );
                    if( showFaults && (closest1.getPlate() != closest2.getPlate())
                            && Math.min( dist1, dist2 ) / Math.max( dist1, dist2 ) > 0.7 ){
                        image.setRGB( x, y1, Color.red.getRGB() );
                    }
                    else {
                        dist3 = Math.sqrt( dist3 );
                        area1 = triangleArea( dist2, dist3, closest2.getPos().distance( closest3.getPos() ) );
                        area2 = triangleArea( dist1, dist3, closest1.getPos().distance( closest3.getPos() ) );
                        area3 = triangleArea( dist1, dist2, closest1.getPos().distance( closest2.getPos() ) );
                        temp = 1.0 / (area1 + area2 + area3);
                        a = area1 * temp;
                        b = area2 * temp;
                        c = area3 * temp;
                        image.setRGB( x, y1, m_colorMap.map( closest1.getSurfaceHeight() * a + closest2.getSurfaceHeight() * b + closest3.getSurfaceHeight() * c - TecPoint.seaLevel ).getRGB() );
                    }
                }
            }
        }
    }

    public double triangleArea( double a, double b, double c ){
        double s = (a + b + c) / 2;
        return Math.sqrt( s * (s - a) * (s - b) * (s - c) );
    }

    // Methods for saving data in a format suitable for POVray and dsTile
    public void saveSatelliteData( double lat, double lon, double rotation, double imageSize, int res, String name ){
        // Save a square of data, as viewed from a satellite (orthogonal, not perspective)
        // Lat and lon give the position of the top-left corner in degrees.
        // Rotation gives the rotation about that point in radians
        // imageSize gives the length of the side of the square in km
        // res gives the length of the side of the square in pixels
        // name gives the name under which the data is to be saved

        System.out.println( "Saving satellite data..." );

        try {
            if( !new File( "TectonicPlanet/SatelliteData" ).exists() )
                new File( "TectonicPlanet/SatelliteData" ).mkdir(); // Make sure the parent directory exists
        }
        catch( Exception exception ){
            System.out.println( "Exception while making sure the SatelliteData directory exists - " + exception );
            exception.printStackTrace( System.out );
        }

        System.out.println( "Saving .tfw..." );
        double A, B, C, D, E, F;
        double pixelSize = imageSize * 1000 / (double)res;    // size of a pixel, in metres
        // First calculate and save the .tfw file
        try {
            A = pixelSize * Math.cos( rotation );
            B = pixelSize * Math.sin( rotation );
            D = -pixelSize * Math.sin( rotation );
            E = -pixelSize * Math.cos( rotation );
            C = lon / 360 * m_planetRadius * 2 * Math.PI * 1000; // Easting (in metres)
            F = lat / 360 * m_planetRadius * 2 * Math.PI * 1000; //Northing (in metres)
            PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( "TectonicPlanet/SatelliteData/" + name + ".tfw" ) ) );
            out.println( A );
            out.println( D );
            out.println( B );
            out.println( E );
            out.println( C );
            out.println( F );
            out.close();
        }
        catch( Exception exception ){
            System.out.println( "Exception while saving .tfw - " + exception );
            exception.printStackTrace( System.out );
        }

        System.out.println( "Calculating heightmap..." );
        Point3d pos = new Point3d();  // Holds the position of the top left corner of the square
        Point3d pos2 = new Point3d(); // Holds the position of the pixel we're working on
        // Locate this pixel (i,j) on the surface of the planet
        pos.x = Math.sin( lon ) * Math.cos( lat ) * m_planetRadius;
        pos.y = Math.sin( lat ) * m_planetRadius;
        pos.z = Math.cos( lon ) * Math.cos( lat ) * m_planetRadius;
        Vector3d east = new Vector3d( Math.cos( Math.toRadians( lon ) ), 0, -Math.sin( Math.toRadians( lon ) ) );  // Vector which points to local east for this square
        Vector3d north = new Vector3d();
        north.cross( east, new Vector3d( pos ) );  // Local north is at right angles to local east and local vertical
        north.normalize();
        double[][] heightmap = new double[res][res];
        Tet tet;
        double minHeight = 100, maxHeight = -100;
        ArrayList previousTets = new ArrayList();
        for( int i = 0; i < res; i++ )
            for( int j = 0; j < res; j++ ){
                pos2.set( pos );
                pos2.scaleAdd( i * pixelSize / 1000, east, pos2 );
                pos2.scaleAdd( -j * pixelSize / 1000, north, pos2 );  // -j because we actually want to go south
                pos2.scale( m_planetRadius / (new Vector3d( pos2 ).length()) );
                tet = getTet( pos2, previousTets );
                if( !previousTets.contains( tet ) )
                    previousTets.add( tet );
                TecPoint tp1 = tet.b;
                TecPoint tp2 = tet.c;
                TecPoint tp3 = tet.d;
                double dist1 = pos2.distance( tp1.getPos() );
                double dist2 = pos2.distance( tp2.getPos() );
                double dist3 = pos2.distance( tp3.getPos() );
                double area1 = triangleArea( dist2, dist3, (tp2.getPos().distance( tp3.getPos() )) );
                double area2 = triangleArea( dist1, dist3, (tp1.getPos().distance( tp3.getPos() )) );
                double area3 = triangleArea( dist1, dist2, (tp1.getPos().distance( tp2.getPos() )) );
                area1 = Math.pow( area1, 1.5 );
                area2 = Math.pow( area2, 1.5 );  // This powering is beautiful :)  Lovely, soft, cubic curves on the image! :D
                area3 = Math.pow( area3, 1.5 );  // Causes slight "bobbling" in low-res areas, but smooths the jagged edges of coastlines.
                double areascale = 1.0 / (area1 + area2 + area3);
                double cont1 = area1 * areascale;
                double cont2 = area2 * areascale;
                double cont3 = area3 * areascale;
                heightmap[i][j] = tp1.getSurfaceHeight() * cont1 + tp2.getSurfaceHeight() * cont2 + tp3.getSurfaceHeight() * cont3;
            }
        for( int i = 0; i < res; i++ )
            for( int j = 0; j < res; j++ ){
                minHeight = Math.min( minHeight, heightmap[i][j] );
                maxHeight = Math.max( maxHeight, heightmap[i][j] );
            }
        System.out.println( "minHeight=" + minHeight + ",  maxHeight=" + maxHeight );
        // Got the heightmap
        // Now make the scaled .png image
        System.out.println( "Saving heightmap..." );
        try {
            WritableRaster raster = Raster.createWritableRaster( new BandedSampleModel( DataBuffer.TYPE_USHORT, res, res, 1 ), null );
            double hscale = 1.0 / (maxHeight - minHeight);
            double temp;
            for( int i = 0; i < res; i++ )
                for( int j = 0; j < res; j++ ){
                    temp = 65535 * (heightmap[i][j] - minHeight) * hscale;
                    if( temp > 32768 )
                        temp -= 65535;
                    raster.setSample( i, j, 0, temp );
                }
            BufferedImage img = new BufferedImage( res, res, BufferedImage.TYPE_USHORT_GRAY );  // 16-bit grayscale image
            img.setData( raster );
            // Save as PNG
            File file = new File( "TectonicPlanet/SatelliteData/" + name + ".png" );
            ImageIO.write( img, "png", file );
        }
        catch( Exception exception ){
            System.out.println( "Exception while saving .png - " + exception );
            exception.printStackTrace( System.out );
        }
        System.out.println( "done!" );
    }

    // Methods for calculating and saving an elevation histogram of the planet
    public void saveElevationHistogram(){
        System.out.println( "Saving elevation histogram..." );
        int width = 800, height = 600;

        System.out.println( "Calculating histogram..." );
        int numBins = (int)(height / 2);
        double[] histBins = new double[numBins];
        double lowest = -15, highest = 20;
        double minHeight = 100, maxHeight = -100;
        // First calculate and save the .tfw file
        for( int i = 0; i < m_points.size(); i++ ){
            TecPoint tp = getPoint( i );
            double h = tp.heightAboveSeaLevel();
            minHeight = Math.min( minHeight, h );
            maxHeight = Math.max( maxHeight, h );
            for( int b = 0; b < numBins; b++ )
                if( h > lowest + (double)b / (double)numBins * (highest - lowest) ){
                    histBins[b] += tp.getArea();
                }
                else
                    b = numBins;
        }

        System.out.println( "Drawing histogram..." );
        try {
            BufferedImage img = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
            Graphics g = img.getGraphics();
            g.setColor( Color.white );
            g.fillRect( 0, 0, width, height );
            // Vertical percentage lines
            g.setColor( Color.black );
            for( double p = 0.1; p < 1; p += 0.1 )
                g.drawLine( (int)(width * p), 0, (int)(width * p), height );
            g.setColor( Color.gray );
            for( int b = 0; b < numBins; b++ ){
                g.fillRect( 0, height - (int)(height * b / (double)numBins), (int)(width * histBins[b] / histBins[0]), (int)(height / (double)numBins) );
            }
            g.setColor( Color.blue );
            g.drawLine( 0, height - (int)(height * -lowest / (highest - lowest)), width, height - (int)(height * -lowest / (highest - lowest)) );
            // Highest and lowest points
            g.setColor( Color.black );
            g.drawLine( 0, height - (int)(height * (minHeight - lowest) / (highest - lowest)), width, height - (int)(height * (minHeight - lowest) / (highest - lowest)) );
            g.drawLine( 0, height - (int)(height * (maxHeight - lowest) / (highest - lowest)), width, height - (int)(height * (maxHeight - lowest) / (highest - lowest)) );
            g.drawString( "Lowest point at " + (int)(minHeight * 1000) + "metres", 0, height - (int)(height * (minHeight - lowest) / (highest - lowest)) );
            g.drawString( "Highest point at " + (int)(maxHeight * 1000) + "metres", 0, height - (int)(height * (maxHeight - lowest) / (highest - lowest)) );
            g.dispose();
            // Save as jpg
            File file = new File( "TectonicPlanet/ElevationHistogram.jpg" );
            ImageIO.write( img, "jpg", file );
        }
        catch( Exception exception ){
            System.out.println( "Exception while saving elevation histogram jpg - " + exception );
            exception.printStackTrace( System.out );
        }
        System.out.println( "done!" );
    }

    // Methods for saving images and data in a format suitable for NASA WorldWind
    public void saveQuadsets(){
        long l = System.currentTimeMillis();
        saveQuadsets( 8, 4, 256 );
        System.out.println( "Quadsets done in " + ((float)(System.currentTimeMillis() - l) / 60000.0F + " minutes! (") + ((float)(System.currentTimeMillis() - l) / 1000.0F) + " seconds)" );
    }

    public void saveQuadsets( int num, int levels, int size ){
        // Save sets of square images
        System.out.println( "Saving quadset of " + num + "*" + (num / 2) + " images, with " + levels + " levels" );
        System.out.println( 2 * Math.PI * m_planetRadius / (Math.pow( 2.0, (double)(levels - 1) ) * (double)num * (double)size) + "km/pixel at the highest resolution" );
        m_imageBuffer = new BufferedImage( size, size, 1 );
        int[][] is = new int[size][size];
        Point3d point3d = new Point3d();
        double therm = 0.0;
        Object object = null;
        Object object_324_ = null;
        Object object_325_ = null;
        Tet tet = null;
        for( int lev = levels - 1; lev < levels; lev++ ){
            for( int row = 0; ( (double)row < (double)num * Math.pow( 2.0, (double)lev ) / 2.0 ); row++ ){
                for( int col = 0; ( (double)col < (double)num * Math.pow( 2.0, (double)lev ) ); col++ ){
                    System.out.println( "Generating tile(" + row + "," + col + ") of level " + lev );
                    ArrayList vector = new ArrayList();
                    for( int x = 0; x < size; x++ ){
                        double lon = (6.283185307179586 * ((double)col + (double)x / (double)size) / ((double)num * Math.pow( 2.0, (double)lev )));
                        lon = (lon + 3.141592653589793) % 6.283185307179586;
                        for( int y = 0; y < size; y++ ){
                            double lat = (-1.5707963267948966 + (3.141592653589793 * ((double)row + (double)y / (double)size) / ((double)num * Math.pow( 2.0, (double)lev ) / 2.0)));
                            point3d.x = (Math.sin( lon ) * Math.cos( lat ) * m_planetRadius);
                            point3d.y = Math.sin( lat ) * m_planetRadius;
                            point3d.z = (Math.cos( lon ) * Math.cos( lat ) * m_planetRadius);
                            if( tet == null || !tet.strictlyContains( point3d ) )
                                tet = getTet( point3d, vector );
                            if( tet != null ){
                                if( !vector.contains( tet ) )
                                    vector.add( tet );
                                TecPoint tecpoint = tet.b;
                                TecPoint tecpoint_333_ = tet.c;
                                TecPoint tecpoint_334_ = tet.d;
                                double dist1 = point3d.distance( tecpoint.getPos() );
                                double dist2 = point3d.distance( tecpoint_333_.getPos() );
                                double dist3 = point3d.distance( tecpoint_334_.getPos() );
                                double area1 = triangleArea( dist2, dist3, (tecpoint_333_.getPos().distance( tecpoint_334_.getPos() )) );
                                double area2 = triangleArea( dist1, dist3, (tecpoint.getPos().distance( tecpoint_334_.getPos() )) );
                                double area3 = triangleArea( dist1, dist2, (tecpoint.getPos().distance( tecpoint_333_.getPos() )) );
                                double d_341_ = 1.0 / (area1 + area2 + area3);
                                double d_342_ = area1 * d_341_;
                                double d_343_ = area2 * d_341_;
                                double d_344_ = area3 * d_341_;
                                double d_345_ = (tecpoint.getSurfaceHeight() * d_342_ + (tecpoint_333_.getSurfaceHeight() * d_343_) + (tecpoint_334_.getSurfaceHeight() * d_344_));
                                is[size - y - 1][x] = (int)(d_345_ * 1000.0);
                                m_imageBuffer.setRGB( x, size - y - 1, m_colorMap.map( d_345_ - TecPoint.seaLevel ).getRGB() );
                            }
                        }
                    }
                    try {
                        File file = new File( "TectonicPlanet/Quads/visible/" + lev + "/" + numberCode( row ) + "/" + numberCode( row ) + "_" + numberCode( col ) + ".jpg" );
                        file.getParentFile().mkdirs();
                        ImageIO.write( m_imageBuffer, "jpg", file );
                        /*for (int i_349_ = 0; i_349_ < 10; i_349_++) {
                        File file_350_
                        = new File(new StringBuilder().append
                        ("TectonicPlanet/Quads/thermal")
                        .append
                        (i_349_).append
                        ("/").append
                        (lev).append
                        ("/").append
                        (numberCode(row)).append
                        ("/").append
                        (numberCode(row)).append
                        ("_").append
                        (numberCode(col)).append
                        (".jpg").toString());
                        file_350_.getParentFile().mkdirs();
                        ImageIO.write(bufferedimages[i_349_], "jpg",
                        file_350_);
                        }*/
                    }
                    catch( IOException ioexception ){
                        System.out.println( "I/O exception! - " + ioexception.toString() );
                        ioexception.printStackTrace( System.out );
                    }
                }
            }
        }

        // MIP map the rest of the levels
        System.out.println( "MIPmapping the rest of the levels..." );
        recurseTiles( num, levels, size, "TectonicPlanet/Quads/visible/" );
        /*for (int lev = levels - 2; lev >= 0; lev--) {
        System.out.println(new StringBuilder().append
        ("Generating level ").append
        (lev).toString());
        for (int i_354_ = 0;
        ((double) i_354_
        < (double) num * Math.pow(2.0, (double) lev) / 2.0);
        i_354_++) {
        for (int i_355_ = 0;
        ((double) i_355_
        < (double) num * Math.pow(2.0, (double) lev));
        i_355_++) {
        System.out.println(new StringBuilder().append
        ("Generating tile(").append
        (i_354_).append
        (",").append
        (i_355_).append
        (") of level ").append
        (lev).toString());
        int i_356_ = i_354_ * 2;
        int i_357_ = i_355_ * 2;
        combineImages(image,
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/visible/")
        .append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_)).append
        ("/").append
        (numberCode(i_356_)).append
        ("_").append
        (numberCode(i_357_)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/visible/")
        .append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_)).append
        ("/").append
        (numberCode(i_356_)).append
        ("_").append
        (numberCode(i_357_ + 1)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/visible/")
        .append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("_").append
        (numberCode(i_357_)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/visible/")
        .append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("_").append
        (numberCode(i_357_ + 1)).append
        (".jpg").toString()));
        for (int i_358_ = 0; i_358_ < 10; i_358_++)
        combineImages
        (bufferedimages[i_358_],
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/thermal").append
        (i_358_).append
        ("/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_)).append
        ("/").append
        (numberCode(i_356_)).append
        ("_").append
        (numberCode(i_357_)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/thermal").append
        (i_358_).append
        ("/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_)).append
        ("/").append
        (numberCode(i_356_)).append
        ("_").append
        (numberCode(i_357_ + 1)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/thermal").append
        (i_358_).append
        ("/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("_").append
        (numberCode(i_357_)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/thermal").append
        (i_358_).append
        ("/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("_").append
        (numberCode(i_357_ + 1)).append
        (".jpg").toString()));
        combineImages(bufferedimage,
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/biome/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_)).append
        ("/").append
        (numberCode(i_356_)).append
        ("_").append
        (numberCode(i_357_)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/biome/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_)).append
        ("/").append
        (numberCode(i_356_)).append
        ("_").append
        (numberCode(i_357_ + 1)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/biome/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("_").append
        (numberCode(i_357_)).append
        (".jpg").toString()),
        new File(new StringBuilder().append
        ("TectonicPlanet/Quads/biome/").append
        (lev + 1).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("/").append
        (numberCode(i_356_ + 1)).append
        ("_").append
        (numberCode(i_357_ + 1)).append
        (".jpg").toString()));
        try {
        File file
        = new File(new StringBuilder().append
        ("TectonicPlanet/Quads/visible/").append
        (lev).append
        ("/").append
        (numberCode(i_354_)).append
        ("/").append
        (numberCode(i_354_)).append
        ("_").append
        (numberCode(i_355_)).append
        (".jpg").toString());
        file.getParentFile().mkdirs();
        ImageIO.write(image, "jpg", file);
        for (int i_359_ = 0; i_359_ < 10; i_359_++) {
        File file_360_
        = new File(new StringBuilder().append
        ("TectonicPlanet/Quads/thermal")
        .append
        (i_359_).append
        ("/").append
        (lev).append
        ("/").append
        (numberCode(i_354_)).append
        ("/").append
        (numberCode(i_354_)).append
        ("_").append
        (numberCode(i_355_)).append
        (".jpg").toString());
        file_360_.getParentFile().mkdirs();
        ImageIO.write(bufferedimages[i_359_], "jpg",
        file_360_);
        }
        file = new File(new StringBuilder().append
        ("TectonicPlanet/Quads/biome/").append
        (lev).append
        ("/").append
        (numberCode(i_354_)).append
        ("/").append
        (numberCode(i_354_)).append
        ("_").append
        (numberCode(i_355_)).append
        (".jpg").toString());
        file.getParentFile().mkdirs();
        ImageIO.write(bufferedimage, "jpg", file);
        } catch (IOException ioexception) {
        System.out.println(new StringBuilder().append
        ("I/O exception! - ").append
        (ioexception).toString());
        ioexception.printStackTrace();
        }
        }
        }
        }
        int i_361_ = 1024;
        int i_362_ = i_361_ / 2;
        image = new BufferedImage(i_361_, i_362_, 2);
        Graphics2D graphics2d = (Graphics2D) image.getGraphics();
        Composite composite = graphics2d.getComposite();
        graphics2d.setComposite(AlphaComposite.getInstance(1, 0.0F));
        Rectangle2D.Double var_double
        = new Rectangle2D.Double(0.0, 0.0, (double) i_361_,
        (double) i_362_);
        graphics2d.fill(var_double);
        graphics2d.setComposite(composite);
        graphics2d.setColor(Color.red);
        for (int i_363_ = 0; i_363_ < points.size(); i_363_ += 16) {
        TecPoint tecpoint = getPoint(i_363_);
        if (tecpoint.heightAboveSeaLevel() < 0.0) {
        double d_364_ = tecpoint.getLat();
        double d_365_ = tecpoint.getLon();
        if (d_365_ < 0.0)
        d_365_ += 6.283185307179586;
        int i_366_
        = (int) (d_365_ * (double) i_361_ / 6.283185307179586);
        int i_367_
        = (int) ((d_364_ + 1.5707963267948966)
         * (double) (i_362_ - 1) / 3.141592653589793);
        TecPoint tecpoint_368_
        = new TecPoint(tecpoint.getX() + tecpoint.flow.x * 50.0,
        tecpoint.getY() + tecpoint.flow.y * 50.0,
        tecpoint.getZ() + tecpoint.flow.z * 50.0,
        0);
        double d_369_ = tecpoint_368_.getLat();
        double d_370_ = tecpoint_368_.getLon();
        if (d_370_ < 0.0)
        d_370_ += 6.283185307179586;
        int i_371_
        = (int) (d_370_ * (double) i_361_ / 6.283185307179586);
        int i_372_
        = (int) ((d_369_ + 1.5707963267948966)
         * (double) (i_362_ - 1) / 3.141592653589793);
        double d_373_
        = Math.sqrt((double) ((i_366_ - i_371_) * (i_366_ - i_371_)
        + (i_367_ - i_372_) * (i_367_
        - i_372_)));
        if (d_373_ < 100.0) {
        try {
        graphics2d.drawLine((i_366_ + i_361_ / 2) % i_361_,
        i_362_ - i_367_ - 1,
        (i_371_ + i_361_ / 2) % i_361_,
        i_362_ - i_372_ - 1);
        graphics2d.fillOval((i_366_ - 1 + i_361_ / 2) % i_361_,
        i_362_ - i_367_ - 2, 3, 3);
        } catch (Exception exception) {
        System.out.println
        (new StringBuilder().append
        ("Couldn't draw the flowline at ").append
        (i_366_).append
        (",").append
        (i_367_).toString());
        System.out.println(exception);
        }
        }
        }
        }
        try {
        File file = new File("TectonicPlanet/OceanCurrentOverlay.png");
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        } catch (Exception exception) {
        System.out.println("Error saving OceanCurrentOverlay.png");
        System.out.println(exception);
        }
        for (int i_374_ = 0; i_374_ < 10; i_374_++) {
        graphics2d.setComposite(AlphaComposite.getInstance(1, 0.0F));
        graphics2d.fill(var_double);
        graphics2d.setComposite(composite);
        graphics2d.setColor(Color.yellow);
        for (int i_375_ = 0; i_375_ < points.size(); i_375_ += 4) {
        TecPoint tecpoint = getPoint(i_375_);
        Vector3d vector3d
        = predictedWindVector(tecpoint.getPos(), i_374_);
        double d_376_ = tecpoint.getLat();
        double d_377_ = tecpoint.getLon();
        if (d_377_ < 0.0)
        d_377_ += 6.283185307179586;
        int i_378_
        = (int) (d_377_ * (double) i_361_ / 6.283185307179586);
        int i_379_
        = (int) ((d_376_ + 1.5707963267948966)
         * (double) (i_362_ - 1) / 3.141592653589793);
        TecPoint tecpoint_380_
        = new TecPoint(tecpoint.getX() + vector3d.x * 30.0,
        tecpoint.getY() + vector3d.y * 30.0,
        tecpoint.getZ() + vector3d.z * 30.0, 0);
        double d_381_ = tecpoint_380_.getLat();
        double d_382_ = tecpoint_380_.getLon();
        if (d_382_ < 0.0)
        d_382_ += 6.283185307179586;
        int i_383_
        = (int) (d_382_ * (double) i_361_ / 6.283185307179586);
        int i_384_
        = (int) ((d_381_ + 1.5707963267948966)
         * (double) (i_362_ - 1) / 3.141592653589793);
        double d_385_
        = Math.sqrt((double) ((i_378_ - i_383_) * (i_378_ - i_383_)
        + (i_379_ - i_384_) * (i_379_
        - i_384_)));
        if (d_385_ < 100.0) {
        try {
        graphics2d.drawLine((i_378_ + i_361_ / 2) % i_361_,
        i_362_ - i_379_ - 1,
        (i_383_ + i_361_ / 2) % i_361_,
        i_362_ - i_384_ - 1);
        graphics2d.fillOval((i_378_ - 1 + i_361_ / 2) % i_361_,
        i_362_ - i_379_ - 2, 3, 3);
        } catch (Exception exception) {
        System.out.println
        (new StringBuilder().append
        ("Couldn't draw the flowline at ").append
        (i_378_).append
        (",").append
        (i_379_).toString());
        System.out.println(exception);
        }
        }
        }
        try {
        File file = new File(new StringBuilder().append
        ("TectonicPlanet/WindOverlay").append
        (i_374_).append
        (".png").toString());
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        } catch (Exception exception) {
        System.out.println(new StringBuilder().append
        ("Error saving WindOverlay").append
        (i_374_).append
        (".png").toString());
        System.out.println(exception);
        }
        }
        graphics2d.setComposite(AlphaComposite.getInstance(1, 0.0F));
        graphics2d.fill(var_double);
        graphics2d.setComposite(composite);
        for (int i_386_ = 0; i_386_ < points.size(); i_386_++) {
        TecPoint tecpoint = getPoint(i_386_);
        if (tecpoint.getCreationDate() > 0) {
        double d_387_ = tecpoint.getLat();
        double d_388_ = tecpoint.getLon();
        if (d_388_ < 0.0)
        d_388_ += 6.283185307179586;
        int i_389_
        = (int) (d_388_ * (double) i_361_ / 6.283185307179586);
        int i_390_
        = (int) ((d_387_ + 1.5707963267948966)
         * (double) (i_362_ - 1) / 3.141592653589793);
        int i_391_ = tecpoint.getCreationDate() % 256;
        int i_392_ = (int) ((double) tecpoint.getCreationDate() * 0.5
        % 256.0);
        int i_393_ = (int) ((double) tecpoint.getCreationDate() * 0.1
        % 256.0);
        graphics2d.setColor(new Color(i_391_, i_392_, i_393_));
        graphics2d.fillOval((i_389_ - 1 + i_361_ / 2) % i_361_,
        i_362_ - i_390_ - 2, 3, 3);
        }
        }
        System.out.println("Starting on ageOverlay...");
        for (int i_394_ = 0; i_394_ < i_361_; i_394_++) {
        double d_395_
        = 6.283185307179586 * (double) i_394_ / (double) i_361_;
        for (int i_396_ = 0; i_396_ < i_362_; i_396_++) {
        double d_397_ = (-1.5707963267948966
        + (3.141592653589793 * (double) i_396_
        / (double) i_362_));
        point3d.x = Math.sin(d_395_) * Math.cos(d_397_) * planetRadius;
        point3d.y = Math.sin(d_397_) * planetRadius;
        point3d.z = Math.cos(d_395_) * Math.cos(d_397_) * planetRadius;
        if (tet == null || !tet.strictlyContains(point3d))
        tet = getTet(point3d);
        if (tet != null) {
        TecPoint tecpoint = tet.b;
        TecPoint tecpoint_398_ = tet.c;
        TecPoint tecpoint_399_ = tet.d;
        double d_400_ = point3d.distance(tecpoint.getPos());
        double d_401_ = point3d.distance(tecpoint_398_.getPos());
        double d_402_ = point3d.distance(tecpoint_399_.getPos());
        double d_403_
        = triangleArea(d_401_, d_402_,
        tecpoint_398_.getPos()
        .distance(tecpoint_399_.getPos()));
        double d_404_
        = triangleArea(d_400_, d_402_,
        tecpoint.getPos()
        .distance(tecpoint_399_.getPos()));
        double d_405_
        = triangleArea(d_400_, d_401_,
        tecpoint.getPos()
        .distance(tecpoint_398_.getPos()));
        double d_406_ = 1.0 / (d_403_ + d_404_ + d_405_);
        double d_407_ = d_403_ * d_406_;
        double d_408_ = d_404_ * d_406_;
        double d_409_ = d_405_ * d_406_;
        double d_410_
        = (double) ((int) ((((double) tecpoint
        .getCreationDate()
         * d_407_)
        + ((double) tecpoint_398_
        .getCreationDate()
         * d_408_)
        + ((double) tecpoint_399_
        .getCreationDate()
         * d_409_))
         * 0.5)
        % 256);
        image.setRGB((i_394_ + i_361_ / 2) % i_361_,
        i_362_ - i_396_ - 1,
        new Color
        ((int) d_410_, (int) d_410_, (int) d_410_)
        .getRGB());
        }
        }
        }
        try {
        File file = new File("TectonicPlanet/AgeOverlay.png");
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        } catch (Exception exception) {
        System.out.println("Error saving AgeOverlay.png");
        System.out.println(exception);
        }
        System.out.println("Finished saving quadsets");
        System.out.println("Saving boundaries...");
        Vector vector = new Vector();
        Vector[] vectors = new Vector[plates.size()];
        for (int i_411_ = 0; i_411_ < plates.size(); i_411_++)
        vectors[i_411_] = new Vector();
        int i_412_ = -1;
        int i_413_ = -1;
        int i_414_ = -1;
        for (int i_415_ = 0; i_415_ < tets.size(); i_415_++) {
        tet = (Tet) tets.get(i_415_);
        if (tet.b.getPlate() != tet.c.getPlate()
        || tet.b.getPlate() != tet.d.getPlate()) {
        TecPoint tecpoint = tet.b;
        TecPoint tecpoint_416_ = tet.c;
        TecPoint tecpoint_417_ = tet.d;
        i_412_ = -1;
        i_413_ = -1;
        i_414_ = -1;
        for (int i_418_ = 0; i_418_ < plates.size(); i_418_++) {
        if (getPlate(i_418_) == tecpoint.getPlate())
        i_412_ = i_418_;
        if (getPlate(i_418_) == tecpoint_416_.getPlate())
        i_413_ = i_418_;
        if (getPlate(i_418_) == tecpoint_417_.getPlate())
        i_414_ = i_418_;
        }
        if (i_412_ != -1 && !vectors[i_412_].contains(tet))
        vectors[i_412_].add(tet);
        if (i_413_ != -1 && !vectors[i_413_].contains(tet))
        vectors[i_413_].add(tet);
        if (i_414_ != -1 && !vectors[i_414_].contains(tet))
        vectors[i_414_].add(tet);
        }
        }
        for (int i_419_ = 0; i_419_ < plates.size(); i_419_++) {
        while (vectors[i_419_].size() > 0) {
        Vector vector_420_ = new Vector();
        tet = (Tet) vectors[i_419_].get(0);
        vector_420_.add(tet);
        vectors[i_419_].remove(0);
        Object object_421_ = null;
        Tet tet_422_;
        do {
        int i_423_ = 0;
        for (tet_422_ = null;
        tet_422_ == null && i_423_ < vectors[i_419_].size();
        i_423_++) {
        if (sharePlateCrossingLink((Tet) vectors[i_419_]
        .get(i_423_),
        tet))
        tet_422_ = (Tet) vectors[i_419_].get(i_423_);
        }
        if (tet_422_ != null) {
        vector_420_.add(tet_422_);
        vectors[i_419_].remove(tet_422_);
        tet = tet_422_;
        }
        } while (tet_422_ != null);
        vector.add(vector_420_);
        System.out.println(new StringBuilder().append
        ("Found loop of length ").append
        (vector_420_.size()).toString());
        }
        }
        ByteBuffer bytebuffer = ByteBuffer.allocate(1000000);
        bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer bytebuffer_424_ = ByteBuffer.allocate(1000000);
        bytebuffer_424_.order(ByteOrder.LITTLE_ENDIAN);
        try {
        FileChannel filechannel
        = new FileOutputStream(new File("TectonicPlanet/pathlist.pkg"))
        .getChannel();
        FileChannel filechannel_425_
        = new FileOutputStream(new File("TectonicPlanet/pathlist.idx"))
        .getChannel();
        bytebuffer_424_.putInt(vector.size());
        for (int i_426_ = 0; i_426_ < vector.size(); i_426_++) {
        Vector vector_427_ = (Vector) vector.get(i_426_);
        String string = new StringBuilder().append("Plate ").append
        (i_426_).toString();
        bytebuffer_424_.put((byte) string.length());
        bytebuffer_424_.put(string.getBytes());
        bytebuffer_424_.putDouble(-180.0);
        bytebuffer_424_.putDouble(-90.0);
        bytebuffer_424_.putDouble(180.0);
        bytebuffer_424_.putDouble(90.0);
        bytebuffer_424_.putLong((long) bytebuffer.position());
        System.out.println(new StringBuilder().append("Pos=").append
        (bytebuffer.position()).append
        (" limit=").append
        (bytebuffer.limit()).toString());
        bytebuffer.putInt(vector_427_.size() + 1);
        bytebuffer.put((byte) 3);
        for (int i_428_ = 0; i_428_ <= vector_427_.size(); i_428_++) {
        tet = (Tet) vector_427_.get(i_428_ % vector_427_.size());
        point3d.x = (tet.b.getPos().x + tet.c.getPos().x
        + tet.d.getPos().x) / 3.0;
        point3d.y = (tet.b.getPos().y + tet.c.getPos().y
        + tet.d.getPos().y) / 3.0;
        point3d.z = (tet.b.getPos().z + tet.c.getPos().z
        + tet.d.getPos().z) / 3.0;
        bytebuffer
        .putDouble(Math.toDegrees(TecPoint.getLat(point3d)));
        bytebuffer
        .putDouble(Math.toDegrees(TecPoint.getLon(point3d)));
        bytebuffer.putShort((short) 0);
        }
        }
        bytebuffer_424_.flip();
        filechannel_425_.write(bytebuffer_424_);
        filechannel_425_.close();
        bytebuffer.flip();
        filechannel.write(bytebuffer);
        filechannel.close();
        } catch (Exception exception) {
        System.out.println(exception);
        exception.printStackTrace();
        }
        graphics2d.dispose();*/
    }

    public void recurseTiles( int num, int levels, int size, String filename ){
        // allocate a big cube of tiles for the pyramid to sit in
        BufferedImage[][][] tile = new BufferedImage[levels][(int)(num * Math.pow( 2, levels ) / 2)][(int)(num * Math.pow( 2, levels ))]; // [levels][rows][cols]
        for( int i = 0; i < levels; i++ )
            for( int j = 0; j < num * Math.pow( 2, levels ) / 2; j++ )
                for( int k = 0; k < num * Math.pow( 2, levels ); k++ )
                    tile[i][j][k] = null;

        // Start each of the level zero tiles
        for( int row = 0; row < num / 2; row++ )
            for( int col = 0; col < num; col++ )
                recurseTiles( tile, 0, row, col, levels, size, filename );
    }

    private void recurseTiles( BufferedImage[][][] tile, int lev, int row, int col, int levels, int size, String filename ){
        // Check if this tile is on disk
        if( lev == levels - 1 ){
            // Load the tile off disk
            try {
                tile[lev][row][col] = ImageIO.read( new File( filename + lev + "/" + numberCode( row ) + "/" + numberCode( row ) + "_" + numberCode( col ) + ".jpg" ) );
            }
            catch( IOException e ){
                System.out.println( "Error loading image:" );
                e.printStackTrace( System.out );
            }
        }
        else {
            System.out.println( "Doing tile(" + row + "," + col + ") of level " + lev );
            // To build this tile, we need the 4 child tiles to be built
            for( int i = 0; i <= 1; i++ )
                for( int j = 0; j <= 1; j++ )
                    recurseTiles( tile, lev + 1, row * 2 + i, col * 2 + j, levels, size, filename );
            // OK, children built. Now build this tile
            tile[lev][row][col] = new BufferedImage( size, size, BufferedImage.TYPE_INT_RGB );
            Graphics2D g = (Graphics2D)tile[lev][row][col].getGraphics();
            for( int i = 0; i <= 1; i++ )
                for( int j = 0; j <= 1; j++ )
                    g.drawImage( tile[lev + 1][row * 2 + i][col * 2 + j], j * size / 2, (1 - i) * size / 2, size / 2, size / 2, null );
            g.dispose();
            // Dispose of the children
            for( int i = 0; i <= 1; i++ )
                for( int j = 0; j <= 1; j++ )
                    tile[lev + 1][row * 2 + i][col * 2 + j] = null;
            // Save to disk
            try {
                File file = new File( filename + lev + "/" + numberCode( row ) + "/" + numberCode( row ) + "_" + numberCode( col ) + ".jpg" );
                file.getParentFile().mkdirs(); // Make sure the parent directory exists
                ImageIO.write( tile[lev][row][col], "jpg", file );
            }
            catch( IOException e ){
                System.out.println( "Error saving image:" );
                e.printStackTrace( System.out );
            }
        }
    }

    // Load 4 images and combine them into one tile
    public void combineImages( BufferedImage bufferedimage, File file, File file_429_, File file_430_, File file_431_ ){
        try {
            BufferedImage bufferedimage_432_ = ImageIO.read( file );
            BufferedImage bufferedimage_433_ = ImageIO.read( file_429_ );
            BufferedImage bufferedimage_434_ = ImageIO.read( file_430_ );
            BufferedImage bufferedimage_435_ = ImageIO.read( file_431_ );
            int i = bufferedimage.getWidth();
            int i_436_ = bufferedimage.getHeight();
            bufferedimage.getGraphics().drawImage( bufferedimage_432_, 0,
                                                   i_436_ / 2, i / 2,
                                                   i_436_ / 2, m_imageSettings );
            bufferedimage.getGraphics().drawImage( bufferedimage_433_, i / 2,
                                                   i_436_ / 2, i / 2,
                                                   i_436_ / 2, m_imageSettings );
            bufferedimage.getGraphics().drawImage( bufferedimage_434_, 0, 0,
                                                   i / 2, i_436_ / 2,
                                                   m_imageSettings );
            bufferedimage.getGraphics().drawImage( bufferedimage_435_, i / 2, 0,
                                                   i / 2, i_436_ / 2,
                                                   m_imageSettings );
        }
        catch( Exception exception ){
            System.out.println( exception );
            exception.printStackTrace( System.out );
        }
    }

    // Do the two tets share a link which crosses a plate?
    public boolean sharePlateCrossingLink( Tet tet, Tet tet_437_ ){
        TecPoint tecpoint = null;
        TecPoint tecpoint_438_ = null;
        TecPoint tecpoint_439_ = null;
        int i = 0;
        if( tet_437_.uses( tet.b ) ){
            tecpoint = tet.b;
            i++;
        }
        if( tet_437_.uses( tet.c ) ){
            tecpoint_438_ = tet.c;
            i++;
        }
        if( tet_437_.uses( tet.d ) ){
            tecpoint_439_ = tet.d;
            i++;
        }
        if( i < 2 )
            return false;
        if( tecpoint != null ){
            if( tecpoint_438_ != null && tecpoint.getPlate() != tecpoint_438_.getPlate() )
                return true;
            if( tecpoint_439_ != null && tecpoint.getPlate() != tecpoint_439_.getPlate() )
                return true;
        }
        if( tecpoint_438_ != null && tecpoint_439_ != null && tecpoint_438_.getPlate() != tecpoint_439_.getPlate() )
            return true;
        return false;
    }

    /////////////////////////////
    // Plate splitting methods //
    /////////////////////////////
    public double plateSplitScore( TecPlate tecplate ){
        double d = 0.0;
        double n1 = 0.0;
        double n2 = 0.0;
        Vector3d vector3d = new Vector3d( 0.0, 0.0, 0.0 );
        Vector3d total2 = new Vector3d( 0.0, 0.0, 0.0 );
        ArrayList vector = tecplate.getPoints();
        ArrayList points1 = new ArrayList( vector.size() );
        ArrayList points2 = new ArrayList( vector.size() );
        for( int i = 0; i < vector.size(); i++ ){
            TecPoint tecpoint = (TecPoint)vector.get( i );
            if( tecplate.splitVector.dot( new Vector3d( tecpoint.getPos() ) ) > 0.0 ){
                vector3d.add( tecpoint.mantleFlow );
                points1.add( tecpoint );
                n1++;
            }
            else {
                total2.add( tecpoint.mantleFlow );
                points2.add( tecpoint );
                n2++;
            }
        }
        if( n1 == 0.0 || n2 == 0.0 )
            return -1.0;
        vector3d.scale( 1.0 / n1 );
        total2.scale( 1.0 / n2 );

        vector3d.sub( total2 );
        boolean bool = true;
        if( vector3d.dot( tecplate.splitVector ) < 0.0 ){
            int i = -1;
        }
        // Somehow check the pointyness of the new (smaller) plate.
        // If it's too pointy, reduce its score
        ArrayList checkPoints;
        if( points1.size() < points2.size() )
            checkPoints = points1;
        else
            checkPoints = points2;
        Point3d point3d = new Point3d();
        for( int i = 0; i < checkPoints.size(); i++ )
            point3d.add( ((TecPoint)checkPoints.get( i )).getPos() );
        point3d.scale( 1.0 / (double)checkPoints.size() );
        double max = 0.0;
        for( int i = 0; i < checkPoints.size(); i++ )
            max = Math.max( max, point3d.distance( ((TecPoint)checkPoints.get( i )).getPos() ) );

        if( max * max * Math.PI > ((double)(5 * checkPoints.size()) * Math.PI * (double)m_pointSpacing * (double)m_pointSpacing / 4.0) )
            return -3.0;

        // The sqrt is (was!) because we're ignoring the cross-sectional length for now...
        //return Math.sqrt(total1.length()*n1*n2)*rev;

        double crossSection = plateLinkCrossers( tecplate );
        if( crossSection == 0.0 )
            return -2.0;

        // Try total1.dot(plate.splitVector)*... instead of total1.length()*rev*...
        return (vector3d.dot( tecplate.splitVector ) * Math.pow( n1 * n2 * Math.pow( (double)m_pointSpacing / 200.0, 4.0 ), 2.0 ) / crossSection);
    }

    // Calculate the cross-sectional length of the plate along the current split
    public double plateLinkCrossers( TecPlate tecplate ){
        double d = 0.0;

        // Find the edges
        ArrayList vector = new ArrayList();
        for( int i = 0; i < tecplate.edgeLinkPairs.size(); i++ ){
            LinkPair linkpair = (LinkPair)tecplate.edgeLinkPairs.get( i );
            if( linkpair.getA().getPlate() == tecplate && linkpair.getB().getPlate() == tecplate
                    && (new Vector3d( linkpair.getA().getPos() ).dot( tecplate.splitVector ) * new Vector3d( linkpair.getB().getPos() ).dot( tecplate.splitVector )) < 0.0 )
                vector.add( linkpair );
        }

        if( vector.isEmpty() )
            return 0.0;

        Vector3d vector3d = new Vector3d( tecplate.splitVector.x, tecplate.splitVector.z, -tecplate.splitVector.y );
        Vector3d vector3d_448_ = new Vector3d( tecplate.splitVector.z, tecplate.splitVector.y, -tecplate.splitVector.x );

        // Now find the planar coords of those points
        double d_449_ = -3.141592653589793;
        double d_450_ = 3.141592653589793;
        double[] ds = new double[vector.size()];
        for( int i = 0; i < vector.size(); i++ ){
            LinkPair linkpair = (LinkPair)vector.get( i );
            double d_451_ = Math.abs( new Vector3d( linkpair.getA().getPos() ).dot( tecplate.splitVector ) );
            double d_452_ = Math.abs( new Vector3d( linkpair.getB().getPos() ).dot( tecplate.splitVector ) );
            Point3d point3d = new Point3d();
            point3d.interpolate( linkpair.getA().getPos(), linkpair.getB().getPos(), d_451_ / (d_451_ + d_452_) );
            Point2d point2d = simulSolve( vector3d, vector3d_448_, point3d );
            double d_453_ = Math.atan2( point2d.x, point2d.y );
            ds[i] = d_453_;
        }
        for( int i = 0; i < vector.size(); i++ ){
            for( int i_454_ = 0; i_454_ < vector.size(); i_454_++ ){
                double d_455_ = Math.abs( ds[i] - ds[i_454_] );
                if( d_455_ > 3.141592653589793 )
                    d_455_ = 6.283185307179586 - d;
                d = Math.max( d, d_455_ );
            }
        }
        return d * m_planetRadius;
    }

    public void splitPlate( TecPlate tecplate ){
        double d = plateLinkCrossers( tecplate );
        ArrayList vector = new ArrayList();
        for( int i = 0; i < tecplate.getPoints().size(); i++ ){
            TecPoint tecpoint = tecplate.getPoint( i );
            if( tecplate.splitVector.dot( new Vector3d( tecpoint.getPos() ) ) > 0.0 )
                vector.add( tecpoint );
        }
        int i = 10; // plate size limit
        if( vector.size() <= i || vector.size() >= tecplate.getPoints().size() - i )
            // Can't split the plate along that line! There's not enough points on one side of it!
            System.out.println( "Bad split attempted" );
        else {
            TecPlate newPlate = new TecPlate( tecplate.getPos().x, tecplate.getPos().y, tecplate.getPos().z );
            newPlate.densityTweak = Math.random() * 0.1;
            addPlate( newPlate );
            for( int i_457_ = 0; i_457_ < vector.size(); i_457_++ ){
                TecPoint tecpoint = (TecPoint)vector.get( i_457_ );
                tecpoint.setPlate( newPlate );
            }
            // Display the different vectors of the plates, for diagnostics
            double d_458_ = 0.0;
            double d_459_ = 0.0;
            Vector3d vector3d = new Vector3d( 0.0, 0.0, 0.0 );
            Vector3d vector3d_460_ = new Vector3d( 0.0, 0.0, 0.0 );
            for( int i_461_ = 0; i_461_ < newPlate.getPoints().size(); i_461_++ ){
                TecPoint tecpoint = (TecPoint)newPlate.getPoints().get( i_461_ );
                vector3d.add( tecpoint.mantleFlow );
                d_458_++;
            }
            for( int i_462_ = 0; i_462_ < tecplate.getPoints().size(); i_462_++ ){
                TecPoint tecpoint = (TecPoint)tecplate.getPoints().get( i_462_ );
                vector3d_460_.add( tecpoint.mantleFlow );
                d_459_++;
            }
            if( d_458_ != 0.0 && d_459_ != 0.0 ){
                vector3d.scale( 1.0 / d_458_ );
                vector3d_460_.scale( 1.0 / d_459_ );
                System.out.println( "New plate has vector " + new Vector3f( vector3d ) + ",\n with " + d_458_ + " points" );
                System.out.println( "Angle from splitVector=" + Math.toDegrees( vector3d.angle( tecplate.splitVector ) ) );
                System.out.println( "Dot with splitVector=" + vector3d.dot( tecplate.splitVector ) );
                System.out.println( "Old plate has vector " + new Vector3f( vector3d_460_ ) + ",\n with " + d_459_ + " points" );
                System.out.println( "Angle from splitVector=" + Math.toDegrees( vector3d_460_.angle( tecplate.splitVector ) ) );
                System.out.println( "Dot with splitVector=" + vector3d_460_.dot( tecplate.splitVector ) );
                System.out.println( "Angle between v1 and v2=" + Math.toDegrees( vector3d.angle( vector3d_460_ ) ) );
                System.out.println( "Dot between v1 and v2=" + vector3d.dot( vector3d_460_ ) );
                System.out.println( "Split length=" + d );
            }
            newPlate.center();
            tecplate.center();
            calculateEdgeLinkPairs();
        }
    }

    public void calculateEdgeLinkPairs(){
        // Go through all the tets, and give each plate a list of its edge linkPairs.

        // First clear the existing lists
        for( int i = 0; i < m_plates.size(); i++ )
            ((TecPlate)m_plates.get( i )).edgeLinkPairs.clear();
        for( int i = 0; i < m_tets.size(); i++ ){
            Tet tet = (Tet)m_tets.get( i );
            Triangle triangle = tet.getTopTriangle();
            if( triangle.bridgesPlates() ){
                if( triangle.p1.getPlate() == triangle.p2.getPlate() )
                    triangle.p1.getPlate().edgeLinkPairs.add( new LinkPair( triangle.p1, triangle.p2 ) );
                if( triangle.p1.getPlate() == triangle.p3.getPlate() )
                    triangle.p1.getPlate().edgeLinkPairs.add( new LinkPair( triangle.p1, triangle.p3 ) );
                if( triangle.p3.getPlate() == triangle.p2.getPlate() )
                    triangle.p3.getPlate().edgeLinkPairs.add( new LinkPair( triangle.p3, triangle.p2 ) );
            }
        }
    }

    public void checkPlateGaps(){
        Iterator iterator = m_linkSystem.getIterator();
        while( iterator.hasNext() ){
            LinkPair linkpair = (LinkPair)iterator.next();
            if( linkpair.getCount() > 0 && linkpair.getA().getPlate() != linkpair.getB().getPlate() ){
                // Link crosses two plates!
                TecPoint tecpoint = linkpair.getA();
                TecPoint tecpoint2 = linkpair.getB();
                double d = tecpoint.getSize() + tecpoint2.getSize();  // NaturalLength
                double length = tecpoint.getPos().distance( tecpoint2.getPos() );
                if( length >= d * 2.2 ){
                    // Break the link!!

                    ///////////////////////////////////////////////////////////////
                    // If the break is _not_ on the ocean floor, add magma until it reaches the level of surrounding rocks //
                    ///////////////////////////////////////////////////////////////
                    System.out.println( "New Ocean floor!" );
                    TecPoint np1 = new TecPoint( (tecpoint.getPos().x * 2.0 / 3.0 + tecpoint2.getPos().x / 3.0),
                                                 (tecpoint.getPos().y * 2.0 / 3.0 + tecpoint2.getPos().y / 3.0),
                                                 (tecpoint.getPos().z * 2.0 / 3.0 + tecpoint2.getPos().z / 3.0), tecpoint.getPlate(), m_epoch );
                    TecPoint np2 = (new TecPoint( (tecpoint.getPos().x / 3.0 + tecpoint2.getPos().x * 2.0 / 3.0),
                                                  (tecpoint.getPos().y / 3.0 + tecpoint2.getPos().y * 2.0 / 3.0),
                                                  (tecpoint.getPos().z / 3.0 + tecpoint2.getPos().z * 2.0 / 3.0), tecpoint2.getPlate(), m_epoch ));
                    np1.setHeight( m_planetRadius );
                    np2.setHeight( m_planetRadius );
                    np1.setSize( (double)(m_pointSpacing / 2) );
                    np2.setSize( (double)(m_pointSpacing / 2) );
                    np1.makeNewOceanFloor();
                    np2.makeNewOceanFloor();
                    np1.setValid( false );
                    np2.setValid( false );
                    addPoint( np1 );
                    addPoint( np2 );
                    calcMantleForceForTecPoint( np1 );
                    calcMantleForceForTecPoint( np2 );
                }
            }
        }
    }

    public void obduct( ArrayList p1, ArrayList p2 ){
        System.out.println( "\t\t************" );
        System.out.println( "\t\tOBDUCTION!!!" );
        System.out.println( "\t\t************" );
        // Move the points from p1 onto the plate of p2
        TecPlate tecplate = ((TecPoint)p2.get( 0 )).getPlate();
        for( int i = 0; i < p1.size(); i++ )
            ((TecPoint)p1.get( i )).setPlate( tecplate );
    }

    private void pointsCheck(){
        for( int i = 0; i < m_points.size(); i++ ){
            Point3d p = getPoint( i ).getPos();
            Double var_double = new Double( p.x );
            Double var_double_468_ = new Double( p.y );
            Double var_double_469_ = new Double( p.z );
            if( var_double.isNaN() || var_double_468_.isNaN() || var_double_469_.isNaN() || var_double.isInfinite() || var_double_468_.isInfinite() || var_double_469_.isInfinite() ){
                System.out.println( "Point has bad position (" + p.x + "," + p.y + "," + p.z + ")" );
                new Exception().printStackTrace( System.out );
                System.exit( 1 );
            }
        }
    }

    public Point2d simulSolve( Vector3d u, Vector3d v, Point3d point3d ){
        Point2d point2d = new Point2d();
        if( v.x != 0.0 && v.y != 0.0 )
            point2d.x = ((point3d.y / v.y - point3d.x / v.x) / (u.y / v.y - u.x / v.x));
        else if( v.x != 0.0 && v.z != 0.0 )
            point2d.x = ((point3d.z / v.z - point3d.x / v.x) / (u.z / v.z - u.x / v.x));
        else if( v.z != 0.0 && v.y != 0.0 )
            point2d.x = ((point3d.y / v.y - point3d.z / v.z) / (u.y / v.y - u.z / v.z));
        else {
            System.out.println( "Error trying to solve simultaneous equations: Vector v is zero!" );
            System.exit( 1 );
        }
        if( v.x != 0.0 )
            point2d.y = (point3d.x - point2d.x * u.x) / v.x;
        else if( v.y != 0.0 )
            point2d.y = (point3d.y - point2d.x * u.y) / v.y;
        else if( v.z != 0.0 )
            point2d.y = (point3d.z - point2d.x * u.z) / v.z;
        return point2d;
    }

    public void killPoint( TecPoint tecpoint ){
        m_points.remove( tecpoint );
        if( tecpoint.getPlate() != null ){
            tecpoint.getPlate().removePoint( tecpoint );
            if( tecpoint.getPlate().getPoints().isEmpty() )
                m_plates.remove( tecpoint.getPlate() );
        }
        for( int i = 0; i < m_tets.size(); i++ ){
            if( ((Tet)m_tets.get( i )).uses( tecpoint ) ){
                Tet tet = (Tet)m_tets.get( i );
                m_tets.remove( tet );
                m_linkSystem.removeLink( tet.b, tet.c );
                m_linkSystem.removeLink( tet.b, tet.d );
                m_linkSystem.removeLink( tet.c, tet.d );
                tet.b.setValid( false );
                tet.c.setValid( false );
                tet.d.setValid( false );
                i--;
            }
        }
    }

    public ColorMap getColorMap(){
        return m_colorMap;
    }

    public ImageSettings getImageSettings(){
        return m_imageSettings;
    }

    public Tet getTet( Point3d point3d ){
        for( int i = 0; i < m_tetGridSize; i++ ){
            HashSet vector = tetsSurrounding( point3d, i );
            Iterator iter = vector.iterator();
            while( iter.hasNext() ){
                //for (int i_471_ = 0; i_471_ < vector.size(); i_471_++) {
                Tet t = (Tet)iter.next();
                if( t.strictlyContains( point3d ) )
                    return t;
            }
        }
        for( int i = 0; i < m_tets.size(); i++ ){
            if( ((Tet)m_tets.get( i )).strictlyContains( point3d ) )
                return (Tet)m_tets.get( i );
        }
        return null;
    }

    public Tet getTet( Point3d point3d, ArrayList vector ){
        if( vector != null ){
            for( int i = 0; i < vector.size(); i++ ){
                if( ((Tet)vector.get( i )).strictlyContains( point3d ) )
                    return (Tet)vector.get( i );
            }
        }
        for( int i = 0; i < m_tetGridSize; i++ ){
            HashSet vector_472_ = tetsSurrounding( point3d, i );
            Iterator iter = vector.iterator();
            while( iter.hasNext() ){
                //for (int i_473_ = 0; i_473_ < vector_472_.size(); i_473_++) {
                Tet t = (Tet)iter.next();
                if( t.strictlyContains( point3d ) )
                    return t;
            }
        }
        for( int i = 0; i < m_tets.size(); i++ ){
            if( ((Tet)m_tets.get( i )).strictlyContains( point3d ) )
                return (Tet)m_tets.get( i );
        }
        return null;
    }

    public double getSeaLevel(){
        return TecPoint.seaLevel;
    }

    private void resetTetGridSystem(){
        for( int i = 0; i < m_tetGridSize; i++ ){
            for( int i_548_ = 0; i_548_ < m_tetGridSize; i_548_++ ){
                for( int i_549_ = 0; i_549_ < m_tetGridSize; i_549_++ )
                    m_tetGridBox[i][i_548_][i_549_] = null;
            }
        }
        for( int i = 0; i < m_tets.size(); i++ )
            gridBoxAdd( (Tet)m_tets.get( i ) );
    }

    private int getTetGridBoxX( double d ){
        return (int)Math.rint( (d + m_planetRadius) * (double)(m_tetGridSize - 1) / (m_planetRadius * 2.0) );
    }

    private int getTetGridBoxY( double d ){
        return (int)Math.rint( (d + m_planetRadius) * (double)(m_tetGridSize - 1) / (m_planetRadius * 2.0) );
    }

    private int getTetGridBoxZ( double d ){
        return (int)Math.rint( (d + m_planetRadius) * (double)(m_tetGridSize - 1) / (m_planetRadius * 2.0) );
    }

    public void gridBoxAdd( Tet tet ){
        int i = getTetGridBoxX( tet.b.getPos().x );
        int i_550_ = getTetGridBoxY( tet.b.getPos().y );
        int i_551_ = getTetGridBoxZ( tet.b.getPos().z );
        try {
            if( m_tetGridBox[i][i_550_][i_551_] == null )
                m_tetGridBox[i][i_550_][i_551_] = new HashSet( 100 );
            m_tetGridBox[i][i_550_][i_551_].add( tet );
        }
        catch( Exception exception ){
            System.out.println( "Problem adding to tetGridBox[" + i + "][" + i_550_ + "][" + i_551_ + "] :" + exception );
            System.out.println( "Point was " + tet.b.getPos().x + "," + tet.b.getPos().y + "," + tet.b.getPos().z );
            System.out.println( "PlanetRadius=" + m_planetRadius + ", tetGridSize=" + m_tetGridSize );
            exception.printStackTrace( System.out );
            System.exit( 1 );
        }
    }

    private HashSet getTetGridBox( Point3d point3d ){
        int i = getTetGridBoxX( point3d.x );
        int i_552_ = getTetGridBoxY( point3d.y );
        int i_553_ = getTetGridBoxZ( point3d.z );
        return m_tetGridBox[i][i_552_][i_553_];
    }

    public HashSet tetsSurrounding( Point3d point3d, int i ){
        HashSet out = new HashSet( 100 );
        int x = getTetGridBoxX( point3d.x );
        int y = getTetGridBoxY( point3d.y );
        int z = getTetGridBoxZ( point3d.z );
        for( int dx = -i; dx <= i; dx++ ){
            for( int dy = -i; dy <= i; dy++ ){
                for( int dz = -i; dz <= i; dz++ ){
                    if( x + dx >= 0 && x + dx < m_tetGridSize && y + dy >= 0 && y + dy < m_tetGridSize && z + dz >= 0 && z + dz < m_tetGridSize && (m_tetGridBox[x + dx][y + dy][z + dz]) != null && (dx <= -i || dx >= i || dy <= -i || dy >= i || dz <= -i || dz >= i) )
                        out.addAll( m_tetGridBox[x + dx][y + dy][z + dz] );
                }
            }
        }
        return out;
    }

    public void removeTet( Tet tet ){
        m_tets.remove( tet );
        getTetGridBox( tet.b.getPos() ).remove( tet );
        m_linkSystem.removeLink( tet.b, tet.c );
        m_linkSystem.removeLink( tet.d, tet.c );
        m_linkSystem.removeLink( tet.b, tet.d );
    }

    public Vector3d getRandomVector(){
        // Returns a random 3D vector of length 1
        Vector3d out = new Vector3d( 1, 0, 0 );
        Transform3D trans = new Transform3D();
        trans.rotY( Math.random() * 2 * Math.PI );
        trans.transform( out );
        trans.rotX( Math.random() * 2 * Math.PI );
        trans.transform( out );
        return out;
    }

    /**
     * Calculates all the points along a given latitude and assigns them to the
     * closest point.
     *
     * @param latCircum The circumference of the latitude line.
     * @param latAngle  The angle from the equator in degrees.
     */
    private void _calcLatPoints( double latCircum, double latAngle ){
        // Calculate the number of points that will fit on this lat line.
        int maxLatPoints = (int)(latCircum / (double)m_pointSpacing);

        // Generate all the points on the line.
        for( int i = 0; i <= maxLatPoints; i++ ){
            // Calculate the longitude of the point and add some randomness.
            double lon = (double)i / (double)(maxLatPoints + 1) * 360.0;
            double adjustedAngle = latAngle + Math.random();
            double adjustedLon   = lon + Math.random();

            TecPoint tecPoint = new TecPoint(
                adjustedAngle,
                adjustedLon,
                m_planetRadius,
                false,
                m_epoch
            );

            // Find closest plate to this point. Use the distance to plate 0 as
            // a starting point.
            tecPoint.setPlate( _findClosestPlate( tecPoint ) );
            tecPoint.setSize( (double)(m_pointSpacing) / 2.0 );

            // This bit _would_ make a circular continent in the middle of each
            // plate, if uncommented.
            //if (cdist>1500) p.makeNewOceanFloor();
            //else p.addLayer(new RockLayer(15,2.6));	// Make it all continental crust

            // Add the point!
            addPoint( tecPoint );
        }
    }

    /**
     * Finds the plate that is closest to the point provided.
     *
     * @param tecPoint The point to find a plate for.
     *
     * @return The closest TecPlate.
     */
    private TecPlate _findClosestPlate( TecPoint tecPoint ){
        TecPlate closestPlate    = null;
        double   closestDistance = m_planetRadius * 3; // Impossibly far away.
        for( int i = 0; i < m_plates.size(); ++i ){
            // Calculate this plates distance from the point.
            TecPlate plate         = getPlate( i );
            double   plateDistance = _calcPlateDistance( plate, tecPoint );

            // If this one is closer then use it!
            if( plateDistance < closestDistance ){
                closestPlate    = plate;
                closestDistance = plateDistance;
            }
        }
        return closestPlate;
    }

    /**
     * Calculates the distance between the given point and the plate.
     *
     * @param plate     The plate to calculate the distance from.
     * @param tecPoint  The point to calculate the distance to.
     *
     * @return The distance between the plate center and the point.
     */
    private double _calcPlateDistance( TecPlate plate, TecPoint tecPoint ){
        Point3d plateCenter = plate.getPos();
        return tecPoint.getPos().distance( plateCenter );
    }

    /**
     * Sets the plate as either ocean floor or continent crust.
     *
     * @param plate   The plate to set the type of.
     * @param isOcean Flag indicating if this plate should be ocean floor.
     */
    private void _setPlateType( TecPlate plate, boolean isOcean ){
        // Loop through each of the points on the plate and either create new
        // ocean floor or generate a random crust thickness.
        ArrayList points = plate.getPoints();
        for( int k = 0; k < points.size(); ++k ){
            TecPoint point = (TecPoint)points.get( k );
            if( isOcean ){
                point.makeNewOceanFloor();
            }
            else {
                float thickness = 20.8f * (0.9f + 0.1f * (float)Math.random());
                point.addLayer( thickness, 2.6f );
            }
        }
    }

    /**
     * Reads the data out of the named file and loads it into this world.
     * @throws FileNotFoundException
     * @throws IOException
     *
     * @param filename The name of the file to read.
     *
     * @return The number of plates specified in the file.
     */
    private int _readFile( String filename )
            throws FileNotFoundException, IOException {
        DataInputStream data = new DataInputStream( new FileInputStream( filename ) );
        // Load the epoch, planet radius, and point spacing first.
        try {
            m_epoch        = data.readInt();
            m_planetRadius = data.readDouble();
            m_pointSpacing = data.readInt();
        }
        catch( Exception exception ){
            System.out.println( "Error reading file - " + exception.toString() );
            exception.printStackTrace( System.out );
        }

        // Load mantle points and flow strength
        m_numMantlePoints    = data.readInt();
        m_mantleFlowStrength = new double[m_numMantlePoints];
        m_mantlePoint        = new Point3d[m_numMantlePoints];
        for( int i = 0; i < m_numMantlePoints; ++i ){
            m_mantleFlowStrength[i] = data.readDouble();
            Point3d point = new Point3d();
            point.x = data.readDouble();
            point.y = data.readDouble();
            point.z = data.readDouble();
            m_mantlePoint[i] = point;
        }

        // Load the ColorMap
        m_colorMap      = new ColorMap();
        int numColorPoints = data.readInt();
        for( int i = 0; i < numColorPoints; ++i ){
            double value = data.readDouble();
            int    rgb   = data.readInt();
            m_colorMap.add( value, new Color( rgb ) );
        }
        initColors(); // Reset the colours to the built-in ones

        // Load the number of plates and create that many default plates.
        m_plates      = new ArrayList();
        int numPlates = data.readInt();
        for( int i = 0; i < numPlates; ++i ){
            m_plates.add( new TecPlate( 0.0, 0.0, 0.0 ) );
        }

        // Load the point data
        m_points = new ArrayList();
        int numPoints = data.readInt();
        for( int i = 0; i < numPoints; ++i ){
            TecPoint tecpoint = new TecPoint(
                this,               // Owning world
                data.readDouble(),  // X
                data.readDouble(),  // Y
                data.readDouble(),  // Z
                data.readInt()      // Depth
            );

            // Next comes its size and plateId.
            tecpoint.setSize(  data.readDouble() );
            tecpoint.setPlate( getPlate( data.readInt() ) );

            // After that is the rock column data.
            tecpoint.setMagmaDensity(    data.readDouble() );
            tecpoint.setBaseDepthOffset( data.readDouble() );
            tecpoint.setRockThickness(   data.readDouble() );
            tecpoint.setDensity(         data.readDouble() );

            // Finally, add the point.
            addPoint( tecpoint );
        }
        data.close();
        return numPlates;
    }

    /**
     * The opposite of _readFile, this writes the world to the current saveFile.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void _writeFile() throws FileNotFoundException, IOException {
        DataOutputStream data = new DataOutputStream( new FileOutputStream( m_saveFile ) );

        // Write the epoch, planet radius, and point spacing
        data.writeInt( m_epoch );
        data.writeDouble( m_planetRadius );
        data.writeInt( m_pointSpacing );

        // Next write the mantle points
        data.writeInt( m_numMantlePoints );
        for( int i = 0; i < m_numMantlePoints; ++i ){
            data.writeDouble( m_mantleFlowStrength[i] );
            data.writeDouble( m_mantlePoint[i].x );
            data.writeDouble( m_mantlePoint[i].y );
            data.writeDouble( m_mantlePoint[i].z );
        }

        // Then the ColorMap
        data.writeInt( m_colorMap.size() );
        for( int i = 0; i < m_colorMap.size(); ++i ){
            data.writeDouble( m_colorMap.getValue( i ) );
            data.writeInt( m_colorMap.getColor( i ).getRGB() );
        }

        // The plates and points go last.
        data.writeInt( m_plates.size() );
        data.writeInt( m_points.size() );
        for( int i = 0; i < m_points.size(); ++i ){
            // Save the point's position
            TecPoint point = getPoint( i );
            Point3d  pos   = point.getPos();
            data.writeDouble( pos.x );
            data.writeDouble( pos.y );
            data.writeDouble( pos.z );

            // And creation date, size, and plateID
            data.writeInt( point.getCreationDate() );
            data.writeDouble( point.getSize() );
            for( int k = 0; k < m_plates.size(); ++k ){
                if( getPlate( k ).equals( point.getPlate() ) ){
                    data.writeInt( k );
                    break;
                }
            }

            // And last its RockColumn
            data.writeDouble( point.getMagmaDensity() );
            data.writeDouble( point.getBaseDepthOffset() );
            data.writeDouble( point.getRockThickness() );
            data.writeDouble( point.getDensity() );
        }
        m_altered = false;
        data.close();
    }

    /**
     * Creates a new save file that doesn't exist on disc yet.
     */
    private void _openNewSaveFile(){
        // Find somewhere suitable to put this world
        try {
            // If the worlds folder doesn't exist, make it.
            File worldsFolder = new File( "TectonicPlanet/Worlds/" );
            if( !worldsFolder.exists() ){
                worldsFolder.mkdirs();
            }

            // Next find a world file name that doesn't already exist.
            String worldsPath = worldsFolder.getAbsolutePath();
            int i = 0;
            do {
                m_saveFile = new File( worldsPath + "World_" + (i++) + ".world" );
            } while( m_saveFile.exists() );
        }
        catch( Exception e ){
            System.out.println( "Error finding suitable file to save to - " + e );
            e.printStackTrace( System.out );
        }
    }
}
