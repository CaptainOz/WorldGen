package TectonicPlanet;

// Standard Java imports
import java.awt.*;

// Deal with Colormaps
public class ColorMap {
  private Color[] color;
  private double[] value;
  private boolean changed=false;
  private int[] red,green,blue,rgb;
  private Color[] preColor;

  public ColorMap() {
    color=new Color[0];
    value=new double[0];
  	red=new int[10000];
  	green=new int[red.length];
  	blue=new int[red.length];
  	preColor=new Color[red.length];
  	rgb=new int[red.length];
  }

  public void add(double v, Color c) {
    if (color.length>0 && alreadyGot(v)) {
      for (int i=0; i<color.length; i++)
        if (value[i]==v) color[i]=c;
    } else {
      int position=0;
      while (position<color.length && v>value[position]) {position++;};
      Color[] newColor=new Color[color.length+1];
      double[] newValue=new double[color.length+1];
      for (int i=0; i<color.length; i++) {
        newColor[i]=color[i];
        newValue[i]=value[i];
      }
      newColor[position]=c;
      newValue[position]=v;
      for (int i=position+1; i<newColor.length; i++) {
        newColor[i]=color[i-1];
        newValue[i]=value[i-1];
      }
      color=newColor;
      value=newValue;
    }
	  changed=true;
  }

  private boolean alreadyGot(double v) {
    for (int i=0; i<color.length; i++)
      if (value[i]==v) return true;
    return false;
  }

  public Color map(double in) {
    // Check if it's off one of the ends
    if (in<=value[0]) return color[0];
    if (in>=value[color.length-1]) return color[color.length-1];
  	// otherwise, check if we've precalculated the colours
  	if (changed) precalc();
    return preColor[getIndex(in)];
  }
  public int mapToRGB(double in) {
    // Check if it's off one of the ends
    if (in<=value[0]) return rgb[0];
    if (in>=value[color.length-1]) return rgb[color.length-1];
  	// otherwise, check if we've precalculated the colours
  	if (changed) precalc();
    return rgb[getIndex(in)];
  }
  private Color calculate(double in) {
    int position=0;
    while (position<color.length && in>value[position]) {position++;};

    if (position==0) return color[0];
    if (position==color.length) return color[color.length-1];

    double fade=(in-value[position-1])/(value[position]-value[position-1]);

    float r=(float)(color[position-1].getRed()*(1-fade)+color[position].getRed()*fade)/255;
    float g=(float)(color[position-1].getGreen()*(1-fade)+color[position].getGreen()*fade)/255;
    float b=(float)(color[position-1].getBlue()*(1-fade)+color[position].getBlue()*fade)/255;

    return new Color(r,g,b);
  }
	public int size() {return color.length;}
	public double getValue(int i) {return value[i];}
	public Color getColor(int i) {return color[i];}
	private void precalc() {
	  for (int i=0; i<preColor.length; i++) {
	    preColor[i]=calculate(value[0]+(double)i*(value[color.length-1]-value[0])/(double)(preColor.length-1));
	    red[i]=preColor[i].getRed();
	    green[i]=preColor[i].getGreen();
	    blue[i]=preColor[i].getBlue();
      rgb[i]=preColor[i].getRGB();
	  }
	  changed=false;
	}
	private int getIndex(double in) {
    if (in<=value[0]) return 0;
    if (in>=value[color.length-1]) return color.length-1;
	  return (int)(red.length*(in-value[0])/(value[color.length-1]-value[0]));
	}
  public String toString() {
    String out="Value\tColor";
    for (int i=0; i<color.length; i++) {
      out+="\n"+value[i]+"\t"+color[i];
    }
    return out;
  }
  public double maxValue() {return value[color.length-1];}
  public int topRGB() {return rgb[rgb.length-1];}
}



