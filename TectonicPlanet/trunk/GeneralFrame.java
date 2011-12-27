package TectonicPlanet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.net.*;


public class GeneralFrame extends JFrame {
  public JComponent panel;

  public GeneralFrame(JComponent p) {
    setTitle("General Frame");

    panel=p;

    getContentPane().add(panel,BorderLayout.CENTER);

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception exc) {
      System.out.println("Error loading L&F: " + exc);
      exc.printStackTrace();
    }

    // Make Frame size
    setSizeRel(0.5,0.5);

  }

  public void setSizeRel(float x, float y) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((int)((float)screenSize.width*x),
            (int)((float)screenSize.height*y));
  }
  public void setSizeRel(double x, double y) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((int)((double)screenSize.width*x),
            (int)((double)screenSize.height*y));
  }

  public void makeFinal() {
    addWindowListener(
      new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      }
    );
  }

  public void makeNotFinal() {
    WindowListener[] wls = (WindowListener[])(getListeners(WindowListener.class));
    if (wls.length>0) 
      for (int i=0; i<wls.length; i++)
        removeWindowListener(wls[i]);
  }

  public void centre() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(screenSize.width/2 -getWidth()/2,
                screenSize.height/2-getHeight()/2);
  }

  public void swapTo(JComponent p) {
    getContentPane().removeAll();
    panel=p;
    getContentPane().add(panel,BorderLayout.CENTER);
  }
}




