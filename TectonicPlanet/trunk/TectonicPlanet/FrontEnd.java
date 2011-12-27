package TectonicPlanet;

// Standard Java imports
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.vecmath.*;
import javax.media.j3d.*;
import java.util.*;



/** 
 * FrontEnd is the main class that implements a graphical front end for interacting with a {@link World} geological/tectonic simulation.
 * @author Tom Groves
 */
public class FrontEnd extends JPanel implements MouseListener, MouseMotionListener, ActionListener, ChangeListener, Runnable {
  private GeneralFrame frame;
  private JPanel topPanel;
  
	private World world=null;	// The World that we do all the work on
	
  // Mouse bits
  private Point mousePress, mouseDrag;
  private boolean dragging=false;
  // GUI bits
  private double rotation1=0, rotation2=0;
  private double zoom=6600;		// km^-1 scaling factor
  private JButton stepButton, multiStepButton, megaStepButton, uberStepButton, stepButton10000, runButton, stopButton;
  private boolean running=false, paintGuard=false, painting=false;
  private int steps=1;
	private PointViewer pointViewer=null;
	private ColorMap temperatureColorMap;
  // Menu bar
  private JMenuBar menuBar=null;
  private JMenu fileMenu, viewMenu, imageOutputMenu, optionsMenu;
  private JMenuItem saveMenuItem, openMenuItem, newMenuItem, saveImageMenuItem, imageSettingsMenuItem, saveQuadsetMenuItem;
  private JMenuItem randomiseUpwellingsItem, breakupItem, saveSatelliteData;
  private JCheckBoxMenuItem flowMenuItem, upwellingMenuItem, crustPointsMenuItem, plateGridMenuItem, planetaryGridMenuItem, exaggerateMenuItem, saveImageSequenceMenuItem, repeatedRandomiseMenuItem, repeatedBreakupMenuItem;
  private JRadioButtonMenuItem plateColorMenuItem, heightColorMenuItem, stressColorMenuItem;
  private JRadioButtonMenuItem cylindricalMenuItem, ellipseMenuItem;
  private JCheckBoxMenuItem rectangularMenuItem;
  private JLabel info;

  
  /**
   * The <code>main</code> method is the entry-point for calls from the command line.
   * Sets up a new FrontEnd and starts the GUI.
   * @param arg	the array of command line arguments
   */
  public static void main(String[] arg) {
    try {
      FrontEnd fe=new FrontEnd();
		  fe.initGUI();
    } catch(Exception e) {
      e.printStackTrace( System.out );
    }
  }
  
    @Override
  public void run() {
    running=true;
    runButton.setEnabled(false);
    stepButton.setEnabled(false);
    multiStepButton.setEnabled(false);
    megaStepButton.setEnabled(false);
    uberStepButton.setEnabled(false);
    stepButton10000.setEnabled(false);
    stopButton.setEnabled(true);
    System.out.print("Timestepping...");
    long time=System.currentTimeMillis();
    for (int i=0; i<steps || steps==-1; i++) {
		  paintGuard=true;
			while (painting) {}
      timeStep();
			paintGuard=false;
      repaint();
      Thread.yield();
      if (steps>0) {
        System.out.println("*** Finished step "+(i+1)+" of "+(steps)+" ***");
        double average=(System.currentTimeMillis()-time)/(1000.0f*(i+1)); 	// seconds
        double remaining=average*(steps-i-1);	// seconds
        System.out.println("*** Estimated time remaining: "+(remaining/60f)+" minutes ("+((int)remaining)+" seconds)\n");
      } else {
        System.out.println("*** Finished step "+(i+1)+" ***");
        System.out.println("*** Time so far is "+(int)((System.currentTimeMillis()-time)/(60000.0f))+" minutes ***\n");
      }
			if (Tet.goneBad) steps=1;
    }
    System.out.println("done, in "+(System.currentTimeMillis()-time)/60000.0f+" minutes! ("+(System.currentTimeMillis()-time)/1000.0f+" seconds)");
    runButton.setEnabled(true);
    stepButton.setEnabled(true);
    multiStepButton.setEnabled(true);
    megaStepButton.setEnabled(true);
    uberStepButton.setEnabled(true);
    stepButton10000.setEnabled(true);
    stopButton.setEnabled(false);
    running=false;
		Tet.goneBad=false;
  }
	
  private void initGUI() {

    topPanel=new JPanel(new BorderLayout());
    topPanel.add(this);

    JPanel bottomPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
    topPanel.add(bottomPanel,BorderLayout.SOUTH);
    
    runButton=new JButton("Run");
    runButton.addActionListener(this);
    bottomPanel.add(runButton);
    stepButton=new JButton("Step");
    stepButton.addActionListener(this);
    bottomPanel.add(stepButton);
    multiStepButton=new JButton("Step*10");
    multiStepButton.addActionListener(this);
    bottomPanel.add(multiStepButton);
    megaStepButton=new JButton("Step*200");
    megaStepButton.addActionListener(this);
    bottomPanel.add(megaStepButton);
    uberStepButton=new JButton("Step*2000");
    uberStepButton.addActionListener(this);
    bottomPanel.add(uberStepButton);
    stepButton10000=new JButton("Step*10000");
    stepButton10000.addActionListener(this);
    bottomPanel.add(stepButton10000);
    stopButton=new JButton("Stop");
    stopButton.addActionListener(this);
    stopButton.setEnabled(false);
    bottomPanel.add(stopButton);

    info=new JLabel(" ");
    bottomPanel.add(info);

    HandyPanel sidePanel=new HandyPanel();
    //topPanel.add(sidePanel,BorderLayout.WEST);

    temperatureColorMap=new ColorMap();
    temperatureColorMap.add(-30,new Color(0,0,100));
    temperatureColorMap.add(-5,Color.blue);
    temperatureColorMap.add(10,Color.green);
    temperatureColorMap.add(20,Color.yellow);
    temperatureColorMap.add(35,Color.red);
	 
    pop();
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  private void pop() {
    // And pop us in a GeneralFrame
    frame=new GeneralFrame(topPanel);
    frame.setJMenuBar(menuBar());
    frame.setTitle("Global Tectonics Simulator");
    frame.setSizeRel(0.7,0.9);
    frame.centre();
    frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    //frame.makeFinal();
    frame.addWindowListener(
      new WindowAdapter() {
            @Override
        public void windowClosing(WindowEvent e) {
          if (okToLoseUnsaved()) System.exit(0);
        }
      }
    );
  }
  private JMenuBar menuBar() {
    if (menuBar!=null) return menuBar;

    // Menu system
    menuBar=new JMenuBar();
    fileMenu=new JMenu("File");
    viewMenu=new JMenu("View");
    optionsMenu=new JMenu("Options");
    imageOutputMenu=new JMenu("View");
    menuBar.add(fileMenu);
    menuBar.add(viewMenu);
    menuBar.add(optionsMenu);

    openMenuItem=new JMenuItem("Open world");
    openMenuItem.addActionListener(this);
    fileMenu.add(openMenuItem);

    newMenuItem=new JMenuItem("New world");
    newMenuItem.addActionListener(this);
    fileMenu.add(newMenuItem);

    saveMenuItem=new JMenuItem("Save");
    saveMenuItem.addActionListener(this);
    fileMenu.add(saveMenuItem);

    fileMenu.add(imageOutputMenu);

    saveImageMenuItem=new JMenuItem("Save image");
    saveImageMenuItem.addActionListener(this);
    fileMenu.add(saveImageMenuItem);

    saveImageSequenceMenuItem=new JCheckBoxMenuItem("Save image sequence",false);
    saveImageSequenceMenuItem.addActionListener(this);
    fileMenu.add(saveImageSequenceMenuItem);

    imageSettingsMenuItem=new JMenuItem("Image Save Settings");
    imageSettingsMenuItem.addActionListener(this);
    fileMenu.add(imageSettingsMenuItem);

    saveQuadsetMenuItem=new JMenuItem("Save quadsets");
    saveQuadsetMenuItem.addActionListener(this);
    fileMenu.add(saveQuadsetMenuItem);

    saveSatelliteData=new JMenuItem("Save satellite data");
    saveSatelliteData.addActionListener(this);
    fileMenu.add(saveSatelliteData);

    flowMenuItem=new JCheckBoxMenuItem("Mantle flow",false);
    flowMenuItem.addActionListener(this);
    viewMenu.add(flowMenuItem);

    upwellingMenuItem=new JCheckBoxMenuItem("Mantle upwellings",true);
    upwellingMenuItem.addActionListener(this);
    viewMenu.add(upwellingMenuItem);

    crustPointsMenuItem=new JCheckBoxMenuItem("Crust points",false);
    crustPointsMenuItem.addActionListener(this);
    viewMenu.add(crustPointsMenuItem);

    plateGridMenuItem=new JCheckBoxMenuItem("Plate grids",true);
    plateGridMenuItem.addActionListener(this);
    viewMenu.add(plateGridMenuItem);

    planetaryGridMenuItem=new JCheckBoxMenuItem("Planetary grid",false);
    planetaryGridMenuItem.addActionListener(this);
    viewMenu.add(planetaryGridMenuItem);

    viewMenu.addSeparator();

    exaggerateMenuItem=new JCheckBoxMenuItem("Exaggerated heights",false);
    exaggerateMenuItem.addActionListener(this);
    viewMenu.add(exaggerateMenuItem);

    viewMenu.addSeparator();

    ButtonGroup group = new ButtonGroup();
    plateColorMenuItem = new JRadioButtonMenuItem("Colour by plate");
    plateColorMenuItem.setSelected(false);
    plateColorMenuItem.addActionListener(this);
    group.add(plateColorMenuItem);
    viewMenu.add(plateColorMenuItem);

    heightColorMenuItem = new JRadioButtonMenuItem("Colour by height");
    heightColorMenuItem.setSelected(true);
    heightColorMenuItem.addActionListener(this);
    group.add(heightColorMenuItem);
    viewMenu.add(heightColorMenuItem);
		
    stressColorMenuItem = new JRadioButtonMenuItem("Colour by stress");
    stressColorMenuItem.setSelected(false);
    stressColorMenuItem.addActionListener(this);
    group.add(stressColorMenuItem);
    viewMenu.add(stressColorMenuItem);
 
    ButtonGroup group2 = new ButtonGroup();
    cylindricalMenuItem = new JRadioButtonMenuItem("Cylindrical projection");
    cylindricalMenuItem.setSelected(true);
    cylindricalMenuItem.addActionListener(this);
    group2.add(cylindricalMenuItem);
    imageOutputMenu.add(cylindricalMenuItem);

    ellipseMenuItem = new JRadioButtonMenuItem("Elliptical projection");
    ellipseMenuItem.setSelected(false);
    ellipseMenuItem.addActionListener(this);
    group2.add(ellipseMenuItem);
    imageOutputMenu.add(ellipseMenuItem);

    rectangularMenuItem=new JCheckBoxMenuItem("Rectangular images",true);
    rectangularMenuItem.addActionListener(this);
    imageOutputMenu.add(rectangularMenuItem);

    randomiseUpwellingsItem=new JMenuItem("Randomise Upwellings");
    randomiseUpwellingsItem.addActionListener(this);
    optionsMenu.add(randomiseUpwellingsItem);
    
    repeatedRandomiseMenuItem=new JCheckBoxMenuItem("Randomise upwellings periodically",false);
    repeatedRandomiseMenuItem.addActionListener(this);
    optionsMenu.add(repeatedRandomiseMenuItem);
    
    repeatedBreakupMenuItem=new JCheckBoxMenuItem("Break up supercontinents periodically",true);
    repeatedBreakupMenuItem.addActionListener(this);
    optionsMenu.add(repeatedBreakupMenuItem);
    
    breakupItem=new JMenuItem("Break up continents");
    breakupItem.addActionListener(this);
    optionsMenu.add(breakupItem);
    
    return menuBar;
  }

	public double radiusSq(double x1, double y1, double x2, double y2) {
	  return Math.pow(x1-x2,2)+Math.pow(y1-y2,2);
	}
    @Override
  public void mouseClicked(MouseEvent e) {
    // Invoked when the mouse button has been clicked (pressed and released) on a component.
    mousePress=e.getPoint();
		if (e.getClickCount()==2 && world!=null) {
	    int width=getWidth();
	    int height=getHeight();
	    int size=Math.min(width,height);
		  Transform3D rTrans1=new Transform3D();
		  rTrans1.rotY(rotation1);
		  Transform3D rTrans2=new Transform3D();
		  rTrans2.rotX(rotation2);

		  Point3d pr=new Point3d();
		  double x=(mousePress.x-width/2+1)*2*zoom/size;
			double y=-(mousePress.y-height/2+1)*2*zoom/size;

			TecPoint p=null;
			double d,bestDist=100000000.0;
			TecPoint best=null;
			for (int i=0; i<world.getNumPoints(); i++) {
			  p=world.getPoint(i);
			  if (p.getRotPos().z>0) {
				  d=radiusSq(p.getRotPos().x,p.getRotPos().y,x,y);
					if (d<bestDist) {bestDist=d; best=p;}
				}
			}
			if (best!=null) {
			  if (pointViewer==null) {
				  pointViewer=new PointViewer(best);
				} else {
				  pointViewer.setPoint(best);
				}
				pointViewer.pop();
			}
		}
  }
    @Override
  public void mouseEntered(MouseEvent e) {
    // Invoked when the mouse enters a component.
  }
    @Override
  public void mouseExited(MouseEvent e) {
    // Invoked when the mouse exits a component.
  }
    @Override
  public void mousePressed(MouseEvent e) {
    // Invoked when a mouse button has been pressed on a component.
    mousePress=e.getPoint();
    mouseDrag=e.getPoint();
    dragging=true;
  }
    @Override
  public void mouseReleased(MouseEvent e) {
    // Invoked when a mouse button has been released on a component.
    dragging=false;
    repaint();
  }
    @Override
  public void mouseDragged(MouseEvent e) {
    // Invoked when a mouse button is pressed on a component and then dragged.
    Point newPos=e.getPoint();
    rotation1+=(newPos.x-mouseDrag.x)/1000.0;
    rotation2+=(newPos.y-mouseDrag.y)/1000.0;
    if (rotation2<-Math.PI/2) rotation2=-Math.PI/2;
    if (rotation2>Math.PI/2) rotation2=Math.PI/2;
    mouseDrag=e.getPoint();
    repaint();
  }
    @Override
  public void mouseMoved(MouseEvent e) {
    // Invoked when the mouse cursor has been moved onto a component but no buttons have been pushed.
  }

    @Override
  public void actionPerformed(ActionEvent e) {
    // Invoked when an action occurs (like button clicking).
    Object source=e.getSource();
    //repaint();
    if (source.equals(newMenuItem)) {
			boolean ok=true;
      if (world!=null && world.altered() && !okToLoseUnsaved()) ok=false;
			if (ok) {
				world=new World();
		    // Sort out the colors
        //for (int i=0; i<world.getNumPoints(); i++)
        //  world.getPoint(i).setColor(world.getColorMap().map(world.getPoint(i).getSurfaceHeight()-TecPoint.seaLevel));
				repaint();
			}
    }
    if (source.equals(runButton) && !running && world!=null) {
      steps=-1;
      new Thread(this).start();
    }
    if (source.equals(stepButton) && !running && world!=null) {
      steps=1;
      new Thread(this).start();
    }
    if (source.equals(multiStepButton) && !running && world!=null) {
      steps=10;
      new Thread(this).start();
    }
    if (source.equals(megaStepButton) && !running && world!=null) {
      steps=200;
      new Thread(this).start();
    }
    if (source.equals(uberStepButton) && !running && world!=null) {
      steps=2000;
      new Thread(this).start();
    }
    if (source.equals(stepButton10000) && !running && world!=null) {
      steps=10000;
      new Thread(this).start();
    }
    if (source.equals(stopButton) && running && world!=null) {
      steps=-2;
    }
    if (source.equals(saveImageMenuItem) && !running && world!=null) {
		  System.out.println("Save JPG");
      world.saveJPG();
    }
    if (source.equals(imageSettingsMenuItem) && !running && world!=null) {
      world.getImageSettings().pop();
    }
    if (source.equals(saveMenuItem) && !running && world!=null) {
      world.save();
    }
    if (source.equals(saveQuadsetMenuItem) && !running && world!=null) {
      world.saveQuadsets();
    }
    if (source.equals(saveSatelliteData) && !running && world!=null) {
      world.saveSatelliteData(Math.random()*360, Math.random()*180-90, 0, 4000, 256, "testData");
    }


    if (source.equals(openMenuItem) && !running && okToLoseUnsaved()) {
		  JFileChooser fc = new JFileChooser("TectonicPlanet/Worlds");
			int returnVal = fc.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
				try{
  				if (world==null) world=new World(file.getCanonicalPath());
	  			  else world.load(file.getCanonicalPath());
          if (world!=null) info.setText(world.getLinkSystem().size()+" links, "+world.getNumTets()+" tets, "+world.getNumPoints()+" points, "+world.getNumPlates()+" plates");
				} catch (Exception ex) {
				  System.out.println("Error converting from File to String!?!?: "+ex);
					ex.printStackTrace( System.out );
				}
			}
			repaint();
		}
    
    if (source.equals(randomiseUpwellingsItem) && !running && world!=null) {
      System.out.println("Randomising positions of mantle upwellings.");
      world.randomiseUpwellings();
			repaint();
    }
    if (source.equals(breakupItem) && !running && world!=null) {
      System.out.println("Moving mantle upwellings to break up continents.");
      world.supercontinentBreakup();
			repaint();
    }
    
    // Check if the user has changed a display option...
    if (source.equals(flowMenuItem) ||
        source.equals(upwellingMenuItem) ||
        source.equals(crustPointsMenuItem) ||
        source.equals(plateGridMenuItem) ||
        source.equals(planetaryGridMenuItem) ||
        source.equals(exaggerateMenuItem) ||
        source.equals(plateColorMenuItem) ||
        source.equals(heightColorMenuItem) ||
        source.equals(stressColorMenuItem)) repaint();
  }

    @Override
  public void stateChanged(ChangeEvent e) {
    // Invoked when the target of the listener has changed its state.
    Object source=e.getSource();
  }

    @Override
  public void paint(Graphics g) {
	  painting=true;
    int width=getWidth();
    int height=getHeight();
    int halfWidth=width/2;
    int halfHeight=height/2;
    int size=Math.min(width,height);
    g.setColor(Color.black);
    g.fillRect(0,0,width,height);

		if (world!=null) {
	    Transform3D rTrans1=new Transform3D();
	    rTrans1.rotY(rotation1);
	    Transform3D rTrans2=new Transform3D();
	    rTrans2.rotX(rotation2);

	    Point3d pr;
	    TecPoint p;
	    TecPlate pl;
	    pr=new Point3d();
	    int x,y;

	    // Draw the magma upwellings
	    g.setColor(Color.red);
	    if (upwellingMenuItem.getState())
	    for (int i=0; i<world.getNumMantlePoints(); i++) {
	      rTrans1.transform(world.getMantlePoint(i),pr);
	      rTrans2.transform(pr);
	      if (pr.z>0) {
	        x=(int)(pr.x/zoom*size/2);
	        y=(int)(pr.y/zoom*size/2);
	        g.fillOval(halfWidth+x-4,halfHeight-y-4,8,8);
	      }
	    }

	    // Draw magma flow arrows
	    Point3d arrowPoint1,arrowPoint2;
	    Vector3d vec;
	    int div=2;
	    if (flowMenuItem.getState())
	    for (int lat=-90; lat<=90; lat+=5/div) {
	      double circum=2*Math.PI*Math.cos(Math.toRadians(lat))*6000;
	      int numPoints=(int)(circum/(600/div));
	      for (int i=0; i<numPoints; i++) {
	        double lon=(double)i/(double)numPoints*360;
	        arrowPoint1=new Point3d(Math.sin(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*6000,
	                                Math.sin(Math.toRadians(lat))*6000,
	                                Math.cos(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*6000);
	        vec=world.getMantleFlow(arrowPoint1);
	        arrowPoint2=new Point3d(vec.x*150+Math.sin(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*6000,
	                                vec.y*150+Math.sin(Math.toRadians(lat))*6000,
	                                vec.z*150+Math.cos(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*6000);
	        rTrans1.transform(arrowPoint1);
	        rTrans2.transform(arrowPoint1);
	        rTrans1.transform(arrowPoint2);
	        rTrans2.transform(arrowPoint2);
	        if (arrowPoint1.z>0 && arrowPoint2.z>0) {
	          g.drawLine(halfWidth+(int)(arrowPoint1.x/zoom*size/2),
	                     halfHeight-(int)(arrowPoint1.y/zoom*size/2),
	                     halfWidth+(int)(arrowPoint2.x/zoom*size/2),
	                     halfHeight-(int)(arrowPoint2.y/zoom*size/2));
	        }
	      }
	    }

	    // Rotate all the points
	    for (int i=0; i<world.getNumPoints(); i++) {
	      world.getPoint(i).rotate(rTrans1, rTrans2);
	      if (exaggerateMenuItem.isSelected()) world.getPoint(i).rotPos().scale((world.getPlanetRadius()-200+world.getPoint(i).getSurfaceHeight()*100)/world.getPoint(i).getHeight());
	    }

	    // Draw the planetary delaunay grid
      double tempscale=size/(2*zoom);
	    Color c1,c2;
	    if (!paintGuard && !dragging && world.getNumTets()>0 && (plateGridMenuItem.getState() || planetaryGridMenuItem.getState())) {
	      Point3d p1,p2,p3,p4;
	      ArrayList tempVec=new ArrayList(world.getLinkSystem().getCollection());
	      for (int i=0; i<tempVec.size(); i++) {
	        LinkPair lp=(LinkPair)tempVec.get(i);
	        p1=lp.getA().rotPos();
	        p2=lp.getB().rotPos();
	        if (lp.getA().getPlate()!=lp.getB().getPlate()) {
	          if (planetaryGridMenuItem.getState()) {
	            if (lp.getCount()==2) g.setColor(Color.red);
	            //if (lp.getCount()==1) g.setColor(Color.blue);
	            //if (lp.getCount()>2) g.setColor(Color.green);
	            if (p1.z>0 || p2.z>0)
	              g.drawLine(halfWidth+(int)(p1.x*tempscale)-1,halfHeight-(int)(p1.y*tempscale)-1,
	                         halfWidth+(int)(p2.x*tempscale)-1,halfHeight-(int)(p2.y*tempscale)-1);
	          }
	        } else {
	          if (plateGridMenuItem.getState()) {
	            if (plateColorMenuItem.isSelected()) g.setColor(lp.getA().getPlate().getCol());
	            //if (lp.getA().mean==1 && lp.getB().mean==1 && Math.random()<0.3) g.setColor(Color.orange);
	            //if (lp.getA().mean==2 && lp.getB().mean==2 && Math.random()<0.3) g.setColor(Color.blue);
	            if (lp.getA().mean!=lp.getB().mean) g.setColor(Color.red);
	            if (heightColorMenuItem.isSelected()) g.setColor(lp.col);
	            if (stressColorMenuItem.isSelected()){
          		  int c=(int)(255*2*Math.atan(lp.pushForce*2)/Math.PI);
          			if (c>255) c=255;
          			if (c<-255) c=-255;
          			if (c>=0) g.setColor(new Color(255,255-c,255-c)); else g.setColor(new Color(255+c,255+c,255));
                if (lp.broken) g.setColor(new Color(0,100,0));
                //if (lp.pushForce<-4) g.setColor(Color.green);
	            }

	            if (p1.z>0 || p2.z>0)
	              g.drawLine(halfWidth+(int)(p1.x*tempscale)-1,halfHeight-(int)(p1.y*tempscale)-1,
	                         halfWidth+(int)(p2.x*tempscale)-1,halfHeight-(int)(p2.y*tempscale)-1);
	          } else if (planetaryGridMenuItem.getState()) {
	            if (lp.getCount()==2) g.setColor(Color.red);
	            //if (lp.getCount()==1) g.setColor(Color.blue);
	            //if (lp.getCount()>2) g.setColor(Color.green);
	            if (p1.z>0 || p2.z>0)
	              g.drawLine(halfWidth+(int)(p1.x*tempscale)-1,halfHeight-(int)(p1.y*tempscale)-1,
	                         halfWidth+(int)(p2.x*tempscale)-1,halfHeight-(int)(p2.y*tempscale)-1);
	          }
	        }
	      }
	    }

	    // Draw the tecpoints
	    if (paintGuard || crustPointsMenuItem.getState() || dragging)
	    for (int i=0; i<world.getNumPoints(); i++) {
	      p=world.getPoint(i);
	      Point3d p1=new Point3d(p.getPos());
	      if (exaggerateMenuItem.isSelected()) {
	        // Do an exaggerated height map
	        p1.scale((world.getPlanetRadius()-200+p.getSurfaceHeight()*100)/p.getHeight());
	      }
	      rTrans1.transform(p1,pr);
	      rTrans2.transform(pr);
	      if (pr.z>0) {
          if (crustPointsMenuItem.getState() || dragging) {
  	        if (plateColorMenuItem.isSelected()) g.setColor(p.getPlate().getCol());
            if (heightColorMenuItem.isSelected()) g.setColor(p.getColor());
  	        x=(int)(pr.x*tempscale);
  	        y=(int)(pr.y*tempscale);
    	      g.fillOval(halfWidth+x-1,halfHeight-y-1,2,2);
          }
					//g.setColor(Color.white);
				  //g.drawLine(width/2+(int)(pr.x/zoom*size/2)-1,height/2-(int)(pr.y/zoom*size/2)-1,
          //           width/2+(int)(p3.x/zoom*size/2)-1,height/2-(int)(p3.y/zoom*size/2)-1);
	      }
	    }
		}
	  painting=false;
  }

	private boolean okToLoseUnsaved() {
	  if (world!=null && world.altered()) {
			// The world isn't all saved up - check if they really want to lose it all
			Object[] options = {"Yes, throw it away","No, I want that!"};
			int n = JOptionPane.showOptionDialog(frame,"The World has been altered since the last save!\nAre you sure you want to lose the work?",
																					"Work not saved!",
																					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			return n==0;
		} else return true;
	}
	
	private void timeStep() {
	  world.timeStep();
		//world.oceanTimeStep();
    // Set Colours
    //for (int i=0; i<world.getNumPoints(); i++)
    //  world.getPoint(i).setColor(world.getColorMap().map(world.getPoint(i).getSurfaceHeight()-TecPoint.seaLevel));

    // Update info
    info.setText(world.getLinkSystem().size()+" links, "+world.getNumTets()+" tets, "+world.getNumPoints()+" points, "+world.getNumPlates()+" plates");
		if (pointViewer!=null) pointViewer.updateDetails();

    if (world.getEpoch()%20==0 && saveImageSequenceMenuItem.getState()) world.saveJPGsequence();
    if (world.getEpoch()%20==0) world.save();
    if (world.getEpoch()%2000==0 && repeatedRandomiseMenuItem.getState()) {
      System.out.println("Randomising positions of mantle upwellings.");
      world.randomiseUpwellings();
    }
    if (world.getEpoch()%2000==0 && repeatedBreakupMenuItem.getState()) {
      System.out.println("Randomising positions of mantle upwellings to breakup supercontinents.");
      world.supercontinentBreakup();
    }
    if (world.getEpoch()%20==0) world.saveElevationHistogram();
  }
}