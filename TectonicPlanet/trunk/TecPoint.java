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




class TecPoint implements Comparable {
  private World world=null;
  private Point3d pos, oldpos, rotPos;
  private TecPlate plate=null;
  public int count=0;
  public long hash;
  public boolean edge=false;
  private double rockThickness=0, density=3, baseDepthOffset=0; // New, simplified "rockColumn" system
  private static long hashCount=0;
  private double size;
  private boolean valid=true;
  public Vector3d mantleFlow=null, mantleForce=null, originalNorth=null;
  public int mean=0;
  public int lms=-1;
  private Color color;
	private int creationDate;
	private double area;
	public static double seaLevel, magmaDensity=3.3;  // I've heard rumours that it's actually 3.3
  public double volCap;   // The volume of rock which can be added to this point without upsetting the gradients or getting higher than surrounding points
  public double volCap2;   // The volume of rock which can be added to this point without upsetting the gradients
  // FEA bits
  public Vector3d FEAforce, collisionForce;
  public boolean broken;  // = attached to at least one link which is "broken" for the purposes of FEA

  public TecPoint(double x, double y, double z, int d) {
	  creationDate=d;
    init();
    pos.x=oldpos.x=x;
    pos.y=oldpos.y=y;
    pos.z=oldpos.z=z;
    setOriginalNorth();
  }
  public TecPoint(double x, double y, double z, TecPlate p, int d) {
	  creationDate=d;
    init();
    pos.x=oldpos.x=x;
    pos.y=oldpos.y=y;
    pos.z=oldpos.z=z;
    setPlate(p);
    setOriginalNorth();
  }
  public TecPoint(double lat, double lon, double height, boolean radians, int d) {
	  creationDate=d;
    init();
    if (radians) {
      pos.x=oldpos.x=Math.sin(lon)*Math.cos(lat)*height;
      pos.y=oldpos.y=Math.sin(lat)*height;
      pos.z=oldpos.z=Math.cos(lon)*Math.cos(lat)*height;
    } else {
      pos.x=oldpos.x=Math.sin(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*height;
      pos.y=oldpos.y=Math.sin(Math.toRadians(lat))*height;
      pos.z=oldpos.z=Math.cos(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*height;
    }
    setOriginalNorth();
  }
  public TecPoint(World w, double x, double y, double z, int d) {
	  world=w;
	  creationDate=d;
    init();
    pos.x=oldpos.x=x;
    pos.y=oldpos.y=y;
    pos.z=oldpos.z=z;
    setOriginalNorth();
  }
  public TecPoint(World w, double x, double y, double z, TecPlate p, int d) {
	  world=w;
	  creationDate=d;
    init();
    pos.x=oldpos.x=x;
    pos.y=oldpos.y=y;
    pos.z=oldpos.z=z;
    setPlate(p);
    setOriginalNorth();
  }
  public TecPoint(World w, double lat, double lon, double height, boolean radians, int d) {
	  world=w;
	  creationDate=d;
    init();
    if (radians) {
      pos.x=oldpos.x=Math.sin(lon)*Math.cos(lat)*height;
      pos.y=oldpos.y=Math.sin(lat)*height;
      pos.z=oldpos.z=Math.cos(lon)*Math.cos(lat)*height;
    } else {
      pos.x=oldpos.x=Math.sin(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*height;
      pos.y=oldpos.y=Math.sin(Math.toRadians(lat))*height;
      pos.z=oldpos.z=Math.cos(Math.toRadians(lon))*Math.cos(Math.toRadians(lat))*height;
    }
    setOriginalNorth();
  }
  private void init() {
    pos=new Point3d();
    oldpos=new Point3d();
    hash=hashCount++;
		
    FEAforce=new Vector3d();
    collisionForce=new Vector3d();
  }
  public void makeNewOceanFloor() {
    rockThickness=7;
    density=3+plate.densityTweak;// 7km thick basalt, ~3tonnes/cubic meter
  }
  public void setPlate(TecPlate p) {
    if (plate!=null) plate.removePoint(this);
    plate=p;
    plate.addPoint(this);
  }
  public void setOriginalNorth() {
    // Make a vector which points north (along the surface).
    // This will get turned as the point moves.
    originalNorth=getNorth(pos);
  }
  //public RockColumn getRocks() {return rocks;}
  public TecPlate getPlate() {return plate;}
  public Point3d getPos() {
    /*try {
      if (Double.isNaN(pos.x) || Double.isInfinite(pos.x)) throw(new Exception("pos.x is bad"));
      if (Double.isNaN(pos.y) || Double.isInfinite(pos.y)) throw(new Exception("pos.y is bad"));
      if (Double.isNaN(pos.z) || Double.isInfinite(pos.z)) throw(new Exception("pos.z is bad"));
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
      System.exit(1);
    }*/
    return pos;
  }
  public Point3d getRotPos() {return rotPos;}
  public Point3d getOldpos() {return oldpos;}
  public void copyOldpos() {
    oldpos.x=pos.x;
    oldpos.y=pos.y;
    oldpos.z=pos.z;
  }
  public double getSize() {return size;}
  public void setSize(double s) {size=s;}
  public boolean isValid() {return valid;}
  public void setValid(boolean v) {valid=v;}
  public double getX() {return pos.x;}
  public double getY() {return pos.y;}
  public double getZ() {return pos.z;}
  public double getLat() {return Math.asin(pos.y/getHeight());}
  public double getLon() {
    double x=pos.x;
    double z=pos.z;
    double len=Math.sqrt(x*x+z*z);
    if (len==0) return 0;
    x=x/len;
    z=z/len;
    double lon=Math.asin(z);
    if (x<0) lon=lon-Math.PI/2;
    if (x>=0) lon=Math.PI/2-lon;
    return lon;
  }
  public static double getLat(Point3d pos) {return Math.asin(pos.y/(pos.distance(new Point3d(0,0,0))));}
  public static double getLon(Point3d pos) {
    double x=pos.x;
    double z=pos.z;
    double len=Math.sqrt(x*x+z*z);
    if (len==0) return 0;
    x=x/len;
    z=z/len;
    double lon=Math.asin(z);
    if (x<0) lon=lon-Math.PI/2;
    if (x>=0) lon=Math.PI/2-lon;
    return lon;
  }
	public static Vector3d getNorth(Point3d pos) {
	  // Returns a unit vector pointing north along the surface from this point
		Vector3d up=new Vector3d(pos);
		Vector3d east=new Vector3d();
		Vector3d north=new Vector3d();
		up.normalize();
		Vector3d temp=new Vector3d(0,1,0);
		east.cross(temp,up);
		east.normalize();
		north.cross(up,east);
		north.normalize();
		return north;
	}
	public static Vector3d getEast(Point3d pos) {
	  // Returns a unit vector pointing east along the surface from this point
		Vector3d up=new Vector3d(pos);
		Vector3d east=new Vector3d();
		Vector3d north=new Vector3d();
		up.normalize();
		Vector3d temp=new Vector3d(0,1,0);
		east.cross(temp,up);
		east.normalize();
		return east;
	}
  public double getHeight() {return pos.distance(new Point3d(0,0,0));}
  public void setHeight(double h) {
    Vector3d vec=new Vector3d(pos);
    vec.normalize();
    vec.scale(h);
    pos.x=vec.x;
    pos.y=vec.y;
    pos.z=vec.z;
  }
  public double getSurfaceHeight() {
    return baseDepthOffset+rockThickness*(1-density/magmaDensity);
  }
  public double getBaseDepth() {
    return baseDepthOffset-rockThickness*density/magmaDensity;
  }
  public void scale(double s) {
	  rockThickness*=s;
  }
  public double getDepth() {
	  return rockThickness;
	}
  public void add(double thick, double dens) {
    try {
      if (thick<0) throw(new Exception("Trying to add negative rock"));
    } catch (Exception e) {
      System.out.println("Trying to add negative rock");
      e.printStackTrace();
      System.exit(1);
    }
	  density=(rockThickness*density+thick*dens)/(rockThickness+thick);
    rockThickness+=thick;
  }
  public void addLayer(double thick, double dens) {add(thick, dens);}
  public void remove(double thick) {
    try {
      if (thick>rockThickness) throw(new Exception("Trying to remove too much rock"));
    } catch (Exception e) {
      System.out.println("Trying to remove more rock than there is");
      e.printStackTrace();
      System.exit(1);
    }
    try {
      if (thick<0) throw(new Exception("Trying to remove negative rock"));
    } catch (Exception e) {
      System.out.println("Trying to remove negative rock");
      e.printStackTrace();
      System.exit(1);
    }
	  rockThickness-=thick;
  }
  public void setMagmaDensity(double d) {magmaDensity=d;}
  public double getMagmaDensity() {return magmaDensity;}
  public void setDensity(double d) {density=d;}
  public double getDensity() {return density;}
  public void setRockThickness(double t) {rockThickness=t;}
  public double getRockThickness() {return rockThickness;}
  public void retreat(double r) {
    // if r==1, it moves back to oldpos. If r==0, it stays where it is.
    pos.interpolate(oldpos,r);
  }
  public void move(Tuple3d m) {
    pos.add(m);
  }
  public void rotate(Transform3D rTrans1, Transform3D rTrans2) {
    rotPos=new Point3d(pos);
    rTrans1.transform(rotPos);
    rTrans2.transform(rotPos);
  }
  public Point3d rotPos() {return rotPos;}
  public boolean isContinental() {return rockThickness>18;}
  public boolean isOcean() {return !isContinental();}
  public void setColor(Color c) {
    color=c;
  }
  public Color getColor() {return color;}
	public int getCreationDate() {return creationDate;}
  public double getBaseDepthOffset() {return baseDepthOffset;}
  public void setBaseDepthOffset(double d) {baseDepthOffset=d;}
	public void addBaseDepthOffset(double d) {baseDepthOffset+=d;}
	public void subBaseDepthOffset(double d) {baseDepthOffset-=d;}
	public void scaleBaseDepthOffset(double d) {baseDepthOffset*=d;}
	
	public double heightAboveSeaLevel() {
	  return getSurfaceHeight()-seaLevel;
	}
	
	public int compareTo(Object o) {
    // Compares this object with the specified object for order.
		return this.hashCode()-o.hashCode();
	}
	
	public void setArea(double d) {area=d;}
	public void addArea(double d) {area+=d;}
	public double getArea() {return area;}
  // FEA methods
  public Vector3d getForce() {
    Vector3d out=new Vector3d(FEAforce);
    out.scaleAdd(0.004,mantleForce,out);  // Add the force due to mantle flow
    //out.scale(0.5);   // Scale down the mantle-plame forces, since we have slab-pull forces now.
    // Add on the force due to the mantle flow
    out.scaleAdd(0.2,collisionForce,out);  // And the forces due to collisions with other plates (suitably scaled down)
    return out;
  }
}

