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




// Deal with Colormaps
public class ImageSettings extends HandyPanel implements ActionListener{
  private GeneralFrame frame=null;
	// Single Image settings
	private int singleSize=1024;
	private JLabel singleSizeLabel;
	private JButton singleSizeUp, singleSizeDown;
	private JRadioButton singleSquare,singleFaults,singleCylindrical,singleEllipse,singleAgeDots;
	
	
	// Sequence image settings
	private int seqSize=256;
	private JLabel seqSizeLabel;
	private JButton seqSizeUp, seqSizeDown;
	private JRadioButton seqSquare,seqFaults,seqCylindrical,seqEllipse,seqAgeDots;

  public ImageSettings() {
	  // Single Image Settings
	  slice.add(new JLabel("Single Image Settings:"));
		newSlice();
    ButtonGroup group = new ButtonGroup();
    singleCylindrical = new JRadioButton("Cylindrical projection");
    singleCylindrical.setSelected(true);
    group.add(singleCylindrical);
    slice.add(singleCylindrical);
		newSlice();

    singleEllipse = new JRadioButton("Elliptical projection");
    singleEllipse.setSelected(false);
    group.add(singleEllipse);
    slice.add(singleEllipse);
		newSlice();
		
		singleSizeLabel=new JLabel("Image height="+singleSize);
		slice.add(singleSizeLabel);
		singleSizeUp=new JButton("Up");
		singleSizeDown=new JButton("Down");
		singleSizeUp.addActionListener(this);
		singleSizeDown.addActionListener(this);
		slice.add(singleSizeUp);
		slice.add(singleSizeDown);
		newSlice();
		
		singleSquare=new JRadioButton("Square the image");
    singleSquare.setSelected(true);
    slice.add(singleSquare);
		newSlice();
		
		singleFaults=new JRadioButton("Show faultlines");
    singleFaults.setSelected(true);
    slice.add(singleFaults);
		newSlice();
		
		singleAgeDots=new JRadioButton("Show age dots");
    singleAgeDots.setSelected(true);
    slice.add(singleAgeDots);
		newSlice();
		
	  // Sequence Image Settings
	  slice.add(new JLabel("Sequence Image Settings:"));
		newSlice();
    ButtonGroup group2 = new ButtonGroup();
    seqCylindrical = new JRadioButton("Cylindrical projection");
    seqCylindrical.setSelected(true);
    group2.add(seqCylindrical);
    slice.add(seqCylindrical);
		newSlice();

    seqEllipse = new JRadioButton("Elliptical projection");
    seqEllipse.setSelected(false);
    group2.add(seqEllipse);
    slice.add(seqEllipse);
		newSlice();
		
		seqSizeLabel=new JLabel("Image height="+seqSize);
		slice.add(seqSizeLabel);
		seqSizeUp=new JButton("Up");
		seqSizeDown=new JButton("Down");
		seqSizeUp.addActionListener(this);
		seqSizeDown.addActionListener(this);
		slice.add(seqSizeUp);
		slice.add(seqSizeDown);
		newSlice();
		
		seqSquare=new JRadioButton("Square the images");
    seqSquare.setSelected(false);
    slice.add(seqSquare);
		newSlice();
		
		seqFaults=new JRadioButton("Show faultlines");
    seqFaults.setSelected(true);
    slice.add(seqFaults);
		newSlice();
		
		seqAgeDots=new JRadioButton("Show age dots");
    seqAgeDots.setSelected(true);
    slice.add(seqAgeDots);
		//newSlice();
		
  }
  public void pop() {
    // And pop us in a GeneralFrame
		if (frame==null) {
      frame=new GeneralFrame(this);
      frame.setTitle("Image Save Settings");
      frame.setSizeRel(0.7,0.9);
      frame.centre();
			frame.pack();
	  }
    frame.setVisible(true);
  }

	public boolean singleSquare() {return singleSquare.isSelected();}
	public boolean singleFaults() {return singleFaults.isSelected();}
	public int singleSize() {return singleSize;}
	public boolean singleCylindrical() {return singleCylindrical.isSelected();}
	public boolean singleEllipse() {return singleEllipse.isSelected();}
	public boolean singleAgeDots() {return singleAgeDots.isSelected();}
	
	public boolean seqSquare() {return seqSquare.isSelected();}
	public boolean seqFaults() {return seqFaults.isSelected();}
	public int seqSize() {return seqSize;}
	public boolean seqCylindrical() {return seqCylindrical.isSelected();}
	public boolean seqEllipse() {return seqEllipse.isSelected();}
	public boolean seqAgeDots() {return seqAgeDots.isSelected();}
	
  public void actionPerformed(ActionEvent e) {
    // Invoked when an action occurs.
		if (e.getSource().equals(singleSizeUp)) {
		  singleSize=singleSize*2;
		  singleSizeLabel.setText("Image height="+singleSize);
	  }
		if (e.getSource().equals(singleSizeDown)) {
		  singleSize=Math.max(64,singleSize/2);
		  singleSizeLabel.setText("Image height="+singleSize);
	  }
		if (e.getSource().equals(seqSizeUp)) {
		  seqSize=seqSize*2;
		  seqSizeLabel.setText("Image height="+seqSize);
	  }
		if (e.getSource().equals(seqSizeDown)) {
		  seqSize=Math.max(64,seqSize/2);
		  seqSizeLabel.setText("Image height="+seqSize);
	  }
	}
}



