package TectonicPlanet;

// Standard Java imports
import java.awt.*;
import javax.vecmath.*;
import java.util.*;



public class TecPlate {
  private Point3d pos=null;		// Notional plate centre
  private ArrayList points=null;	// List of points of this plate
  //private Vector tets=null;	// List of tetrahedrons for this plate
  private Color col=null;
  private Vector3d force=null;
  private double rotation=0;
  public double densityTweak=0;
  public Vector3d splitVector=null;
  public ArrayList edgeLinkPairs=null;
  public Point3d meanPos1=null,meanPos2=null;
  public Vector3d meanVec1=null,meanVec2=null;
	public boolean linkRemoved=false;

  public TecPlate(double x, double y, double z) {
    init();
    pos.x=x;
    pos.y=y;
    pos.z=z;
  }
  private void init() {
    pos=new Point3d();
    points=new ArrayList();
    col=new Color((float)(Math.random()),(float)(Math.random()),(float)(Math.random()));
    force=new Vector3d();
    splitVector=new Vector3d();
    edgeLinkPairs=new ArrayList();
		resetForces();
  }
  public void addPoint(TecPoint p) {
    points.add(p);
  }
  public void removePoint(TecPoint p) {
      points.remove( p );
  }
  public TecPoint getPoint(int i) {
    return (TecPoint)points.get(i);
  }
  public ArrayList getPoints() {return points;}
  public Point3d getPos() {return pos;}
  public Color getCol() {return col;}
  public void setCol(Color c) {col=c;}
  //public ArrayList getTets() {return tets;}
	
	public ArrayList getLinks(LinkSystem linkSystem) {
		ArrayList out=new ArrayList();
		TecPoint p;
		for (int i=0; i<points.size(); i++) {
			p=getPoint(i);
		  for (int j=i; j<points.size(); j++)
			  if (linkSystem.getCount(p,getPoint(j))>0) out.add(new LinkPair(p,getPoint(j)));
		}
		return out;
	}

  public void center() {
    double x=0, y=0, z=0, height=0;
    for (int i=0; i<points.size(); i++) {
      x+=getPoint(i).getX();
      y+=getPoint(i).getY();
      z+=getPoint(i).getZ();
      height+=getPoint(i).getHeight();
    }
    x/=points.size();
    y/=points.size();
    z/=points.size();
    height/=points.size();
    Vector3d vec=new Vector3d();
    vec.x=x;
    vec.y=y;
    vec.z=z;
    vec.normalize();
    vec.scale(height);
    pos.x=vec.x;
    pos.y=vec.y;
    pos.z=vec.z;
  }

  public void resetForces() {
    force.x=force.y=force.z=0;
    rotation=0;
  }
  public void force(Point3d p, Vector3d vec) {
    if (vec.lengthSquared()>0) {
      // This force (vec) acts through the point p
      // Find unit vector perpendicular to force vector and axis through center of planet
      //check(p);
      //check(vec);
      Vector3d perp=new Vector3d();
      //check(pos);
      perp.cross(vec,new Vector3d(pos));
      perp.normalize();
      if (perp.length()>0 && !Double.isInfinite(perp.x) && !Double.isInfinite(perp.y) && !Double.isInfinite(perp.z)) {

        double d1=new Vector3d(pos).dot(perp);
        //check(d1);
        double d2=new Vector3d(p).dot(perp);
        //check(d2);
        double perpDist=d1-d2;

        // Torque is force*perpDist
        rotation-=perpDist*vec.length();
        force.add(vec);
      }
    }
  }
  public void move() {

    // Scale it all down a bit!
    double plateArea=getArea();//*10000;
    double scale=1.0/plateArea; //0.001/points.size(); // Should this be removed?
    double MoI=plateArea*plateArea/(2*Math.PI);  // Rough guess at moment of inertia
    rotation=rotation/MoI;
    force.scale(scale);

    // Check for badness before we do the move
    /*if (plateArea==0) {System.out.println("Plate area=0 during plate move.");System.exit(1);}
    if (Double.isNaN(plateArea) || Double.isInfinite(plateArea)) {System.out.println("plateArea is bad during plate move.");System.exit(1);}
    if (MoI==0) {System.out.println("MoI=0 during plate move.");System.exit(1);}
    if (Double.isNaN(MoI) || Double.isInfinite(MoI)) {System.out.println("MoI is bad during plate move.");System.exit(1);}
    if (Double.isNaN(scale) || Double.isInfinite(scale)) {System.out.println("scale is bad during plate move.");System.exit(1);}
    if (Double.isNaN(rotation) || Double.isInfinite(rotation)) {System.out.println("rotation is bad during plate move.");System.exit(1);}*/
    
    //System.out.println("Plate area="+plateArea+", speed="+force.length());
		if (force.length()>0) {
			
	    TecPoint tp;

	    // First rotation is "rotation": turning about axis from center of planet to center of plate
	    Vector3d axis=new Vector3d(pos);
	    axis.normalize();
	    double u=axis.x;
	    double v=axis.y;
	    double w=axis.z;

      // Check for badness
      /*if (Double.isNaN(u) || Double.isInfinite(u)) {System.out.println("u is bad during plate move.");System.exit(1);}
      if (Double.isNaN(v) || Double.isInfinite(v)) {System.out.println("v is bad during plate move.");System.exit(1);}
      if (Double.isNaN(w) || Double.isInfinite(w)) {System.out.println("w is bad during plate move.");System.exit(1);}*/
      
	    //double cos=Math.cos(rotation*0.001);  // And this? Surely this scaling should be done elsewhere? Probably where the forces are calculated in the first place?
	    //double sin=Math.sin(rotation*0.001);
	    double cos=Math.cos(rotation);
	    double sin=Math.sin(rotation);

	    Matrix4d m1=new Matrix4d(u*u+(v*v+w*w)*cos, u*v*(1-cos)-w*sin, u*w*(1-cos)+v*sin, 0,
	                             u*v*(1-cos)+w*sin, v*v+(u*u+w*w)*cos, v*w*(1-cos)-u*sin, 0,
	                             u*w*(1-cos)-v*sin, v*w*(1-cos)+u*sin, w*w+(u*u+v*v)*cos, 0,
	                             0,                 0,                 0,                 1);
	                             

	    // Second rotation is "movement": turning about an axis through center of planet,
	    // but perpendicular to the direction of movement
	    axis.cross(force,axis);
	    axis.normalize();
	    u=axis.x;
	    v=axis.y;
	    w=axis.z;
      
      // Check for badness
      /*if (Double.isNaN(u) || Double.isInfinite(u)) {System.out.println("u is bad during plate move.(2)");System.exit(1);}
      if (Double.isNaN(v) || Double.isInfinite(v)) {System.out.println("v is bad during plate move.(2)");System.exit(1);}
      if (Double.isNaN(w) || Double.isInfinite(w)) {System.out.println("w is bad during plate move.(2)");System.exit(1);}*/

	    cos=Math.cos(-force.length());
	    sin=Math.sin(-force.length());

	    Matrix4d m2=new Matrix4d(u*u+(v*v+w*w)*cos, u*v*(1-cos)-w*sin, u*w*(1-cos)+v*sin, 0,
	                             u*v*(1-cos)+w*sin, v*v+(u*u+w*w)*cos, v*w*(1-cos)-u*sin, 0,
	                             u*w*(1-cos)-v*sin, v*w*(1-cos)+u*sin, w*w+(u*u+v*v)*cos, 0,
	                             0,                 0,                 0,                 1);
	                             


	    double height;
	    for (int i=0; i<points.size(); i++) {
	      tp=(TecPoint)points.get(i);
	      height=tp.getHeight();
	      tp.copyOldpos();
	      m1.transform(tp.getPos());
	      m2.transform(tp.getPos());
        m1.transform(tp.originalNorth); // Remember to rotate the "originalNorth" vector
        m2.transform(tp.originalNorth); // so as to keep track of distortions
	      tp.setHeight(height);
	    }
	    center();
		}
  }
  public double getArea() {
    double out=0;
	  for (int i=0; i<points.size(); i++)
      out+=((TecPoint)points.get(i)).getArea();
    return out;
  }
  private void check(Double d) {
    try {
      if (Double.isNaN(d)) throw(new Exception("double is NaN"));
      if (Double.isInfinite(d)) throw(new Exception("double is infinite"));
    } catch (Exception e) {
      e.printStackTrace( System.out );
      System.exit(1);
    }
  }
  private void check(Tuple3d t) {
    try {
      if (Double.isNaN(t.x)) throw(new Exception("t.x is NaN"));
      if (Double.isInfinite(t.x)) throw(new Exception("t.x is infinite"));
      if (Double.isNaN(t.y)) throw(new Exception("t.y is NaN"));
      if (Double.isInfinite(t.y)) throw(new Exception("t.y is infinite"));
      if (Double.isNaN(t.z)) throw(new Exception("t.z is NaN"));
      if (Double.isInfinite(t.z)) throw(new Exception("t.z is infinite"));
    } catch (Exception e) {
      e.printStackTrace( System.out );
      System.exit(1);
    }
  }
}






