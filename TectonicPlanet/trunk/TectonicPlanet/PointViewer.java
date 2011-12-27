package TectonicPlanet;

// Standard Java imports
import java.io.*;
import java.awt.*;
import java.math.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.vecmath.*;
import javax.media.j3d.*;
import java.util.*;
import javax.imageio.*;
import com.sun.image.codec.jpeg.*;



/** 
 * PointViewer displays a given {@link TecPoint}.
 * @author Tom Groves
 */
 
public class PointViewer extends JPanel {
  private GeneralFrame frame=null;
  private JPanel topPanel;
	private JLabel latlon,surfaceHeight,thickness, baseDepth;
	
	private TecPoint point;
  
  public PointViewer(TecPoint p) {
	  point=p;
		init();
		updateDetails();
	}
  public void pop() {
    // And pop us in a GeneralFrame
		if (frame==null) {
      frame=new GeneralFrame(topPanel);
      frame.setTitle("Point Viewer");
      frame.setSizeRel(0.4,0.4);
      frame.centre();
		}
    frame.setVisible(true);
    //frame.pack();
	}
	private void init() {
	  topPanel=new JPanel();
	  latlon=new JLabel();
		topPanel.add(latlon);
	  surfaceHeight=new JLabel();
		topPanel.add(surfaceHeight);
	  thickness=new JLabel();
    topPanel.add(thickness);
	  baseDepth=new JLabel();
    topPanel.add(baseDepth);
	}
	public void updateDetails() {
	  double lat=point.getLat();
	  double lon=point.getLon();
	  String temp=new String("Latitude "+(float)Math.toDegrees(Math.abs(lat))+":");
		if (lat<0) temp+="S"; else temp+="N";
		temp+=", longitude "+(float)Math.toDegrees(Math.abs(lon))+":";
		if (lon>0) temp+="E"; else temp+="W";
	  latlon.setText(temp);
		
		surfaceHeight.setText("Height above sea level="+(int)(point.heightAboveSeaLevel()*1000)+"m");
		thickness.setText("Total thickness of crust="+(int)(point.getDepth()*1000)+"m");
		baseDepth.setText("\"Sinkage\" of base of crust="+(int)(point.getBaseDepth()*1000)+"m");
	}
	public void setPoint(TecPoint p) {
	  point=p;
		updateDetails();
	}
}
 