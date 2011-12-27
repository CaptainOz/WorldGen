package TectonicPlanet;

import javax.swing.*;
import java.awt.*;

public class HandyPanel extends JPanel {
  public JPanel slice;
  public HandyPanel() {
    setLayout(new GridLayout(0,1));
    newSlice();
  }
  public void newSlice() {
    slice=sliceOf(this);
  }
  public JPanel sliceOf(JPanel p) {
    JPanel n=new JPanel();
    p.add(n);
    return n;
  }
}
