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





class Tet {
  public TecPoint a,b,c,d;
  public Point3d center=null;
  public double radiussq, da, innersq, outersq,v1,v2,v3,area,oldArea;
  public static double biggestError=0;
  public static double planetRadius;
	public static boolean goneBad=false;
	public Vector3d n1,n2,n3;

  public Tet() {}
  public Tet(TecPoint ta, TecPoint tb, TecPoint tc, TecPoint td) {
    a=ta;
    b=tb;
    c=tc;
    d=td;
  }
  public double getDelaunayA() {
    return new Matrix4d(a.getPos().x,a.getPos().y,a.getPos().z,1,
                        b.getPos().x,b.getPos().y,b.getPos().z,1,
                        c.getPos().x,c.getPos().y,c.getPos().z,1,
                        d.getPos().x,d.getPos().y,d.getPos().z,1).determinant();
  }
  public double getDelaunayBx() {
    return new Matrix4d(a.getPos().x*a.getPos().x+a.getPos().y*a.getPos().y+a.getPos().z*a.getPos().z, a.getPos().y,a.getPos().z,1,
                        b.getPos().x*b.getPos().x+b.getPos().y*b.getPos().y+b.getPos().z*b.getPos().z, b.getPos().y,b.getPos().z,1,
                        c.getPos().x*c.getPos().x+c.getPos().y*c.getPos().y+c.getPos().z*c.getPos().z, c.getPos().y,c.getPos().z,1,
                        d.getPos().x*d.getPos().x+d.getPos().y*d.getPos().y+d.getPos().z*d.getPos().z, d.getPos().y,d.getPos().z,1).determinant();
  }
  public double getDelaunayBy() {
    return new Matrix4d(a.getPos().x*a.getPos().x+a.getPos().y*a.getPos().y+a.getPos().z*a.getPos().z, a.getPos().x,a.getPos().z,1,
                        b.getPos().x*b.getPos().x+b.getPos().y*b.getPos().y+b.getPos().z*b.getPos().z, b.getPos().x,b.getPos().z,1,
                        c.getPos().x*c.getPos().x+c.getPos().y*c.getPos().y+c.getPos().z*c.getPos().z, c.getPos().x,c.getPos().z,1,
                        d.getPos().x*d.getPos().x+d.getPos().y*d.getPos().y+d.getPos().z*d.getPos().z, d.getPos().x,d.getPos().z,1).determinant()*-1;
  }
  public double getDelaunayBz() {
    return new Matrix4d(a.getPos().x*a.getPos().x+a.getPos().y*a.getPos().y+a.getPos().z*a.getPos().z, a.getPos().x,a.getPos().y,1,
                        b.getPos().x*b.getPos().x+b.getPos().y*b.getPos().y+b.getPos().z*b.getPos().z, b.getPos().x,b.getPos().y,1,
                        c.getPos().x*c.getPos().x+c.getPos().y*c.getPos().y+c.getPos().z*c.getPos().z, c.getPos().x,c.getPos().y,1,
                        d.getPos().x*d.getPos().x+d.getPos().y*d.getPos().y+d.getPos().z*d.getPos().z, d.getPos().x,d.getPos().y,1).determinant();
  }
  public Tet calc() {
    n1=new Vector3d();	// a,b,c
    n1.cross(new Vector3d(a.getPos().x-b.getPos().x,a.getPos().y-b.getPos().y,a.getPos().z-b.getPos().z),
             new Vector3d(a.getPos().x-c.getPos().x,a.getPos().y-c.getPos().y,a.getPos().z-c.getPos().z));
    n2=new Vector3d();	// a,c,d
    n2.cross(new Vector3d(a.getPos().x-c.getPos().x,a.getPos().y-c.getPos().y,a.getPos().z-c.getPos().z),
             new Vector3d(a.getPos().x-d.getPos().x,a.getPos().y-d.getPos().y,a.getPos().z-d.getPos().z));
    n3=new Vector3d();	// a,d,b
    n3.cross(new Vector3d(a.getPos().x-d.getPos().x,a.getPos().y-d.getPos().y,a.getPos().z-d.getPos().z),
             new Vector3d(a.getPos().x-b.getPos().x,a.getPos().y-b.getPos().y,a.getPos().z-b.getPos().z));
    /*da=getDelaunayA();
    center=new Point3d(getDelaunayBx()/(2*da),
                       getDelaunayBy()/(2*da),
                       getDelaunayBz()/(2*da));
    radiussq=center.distanceSquared(a.getPos());
    innersq=Math.min(radiussq,Math.min(center.distanceSquared(b.getPos()),Math.min(center.distanceSquared(c.getPos()),center.distanceSquared(d.getPos()))));
    outersq=Math.max(radiussq,Math.max(center.distanceSquared(b.getPos()),Math.max(center.distanceSquared(c.getPos()),center.distanceSquared(d.getPos()))));*/
    Vector3d normal=new Vector3d();
    normal.cross(new Vector3d(b.getPos().x-c.getPos().x,b.getPos().y-c.getPos().y,b.getPos().z-c.getPos().z),
                 new Vector3d(b.getPos().x-d.getPos().x,b.getPos().y-d.getPos().y,b.getPos().z-d.getPos().z));
    normal.scale(normal.dot(new Vector3d(b.getPos())));
    normal.normalize();
    center=new Point3d(normal);
    center.scale(planetRadius);
    radiussq=center.distanceSquared(b.getPos());
    innersq=Math.min(center.distanceSquared(b.getPos()),Math.min(center.distanceSquared(c.getPos()),center.distanceSquared(d.getPos())));
    outersq=Math.max(center.distanceSquared(b.getPos()),Math.max(center.distanceSquared(c.getPos()),center.distanceSquared(d.getPos())));
/*double er=Math.sqrt(outersq)-Math.sqrt(innersq);
if (er>biggestError) {
  System.out.println("Center error="+er);
  biggestError=er;
}*/
		// Recalc the area of this tet
		calcArea();
    // Find the highest x pos of the points making up this tet (not the centre one, though!)
    return this;
  }
  public boolean contains(Point3d p) {
    double dist=p.distanceSquared(center);
    if (dist>outersq) return false;
    if (dist<innersq) return true;
    //if (!goneBad) System.out.print("x");
		//goneBad=true;
    if (p.x!=Math.max(b.getPos().x,Math.max(c.getPos().x, d.getPos().x)))
      return p.x>Math.max(b.getPos().x,Math.max(c.getPos().x, d.getPos().x));
    if (p.y!=Math.max(b.getPos().y,Math.max(c.getPos().y, d.getPos().y)))
      return p.y>Math.max(b.getPos().y,Math.max(c.getPos().y, d.getPos().y));
    if (p.z!=Math.max(b.getPos().z,Math.max(c.getPos().z, d.getPos().z)))
      return p.z>Math.max(b.getPos().z,Math.max(c.getPos().z, d.getPos().z));

	return true;
	
/*System.out.println("Points are the same!!!");

double er=Math.sqrt(outersq)-Math.sqrt(innersq);
System.out.println("Center error="+er);
biggestError=er;
System.out.println("radiussq="+radiussq+", distsq(p)="+dist);
System.exit(1);
    return dist<=outersq;*/
  }
  public boolean uses(TecPoint p) {
	  return  p.hash==b.hash || p.hash==c.hash || p.hash==d.hash || p.hash==a.hash;
    //return pointEquals(a.getPos(),p.getPos()) || pointEquals(b.getPos(),p.getPos()) || pointEquals(c.getPos(),p.getPos()) || pointEquals(d.getPos(),p.getPos());
  }

  public boolean pointEquals(Point3d p1, Point3d p2) {
    return p1.distanceSquared(p2)<1;	// If they're closer than 1 km, they're the same point!
  }

  public Triangle getTopTriangle() {
    Point3d centerOfEarth=new Point3d(0,0,0);
    if (pointEquals(a.getPos(),centerOfEarth))
      return new Triangle(b,c,d);
    if (pointEquals(b.getPos(),centerOfEarth))
      return new Triangle(a,c,d);
    if (pointEquals(c.getPos(),centerOfEarth))
      return new Triangle(a,b,d);
    if (pointEquals(d.getPos(),centerOfEarth))
      return new Triangle(a,b,c);
    return null;
  }
  /*public boolean strictlyContains(Point3d p) {
	  if (!contains(p)) return false;
	  v1=n1.x*p.x+n1.y*p.y+n1.z*p.z;
	  v2=n2.x*p.x+n2.y*p.y+n2.z*p.z;
		if (v1<0 && v2>0) return false;
		if (v1>0 && v2<0) return false;
	  v3=n3.x*p.x+n3.y*p.y+n3.z*p.z;
		if ((v1<=0 && v2<=0 && v3<=0) || (v1>=0 && v2>=0 && v3>=0)) return true;
		return false;
	}*/
  public boolean strictlyContains(Point3d p) {
	  //if (!contains(p)) return false;
	  v1=n1.x*p.x+n1.y*p.y+n1.z*p.z;
	  v2=n2.x*p.x+n2.y*p.y+n2.z*p.z;
		if (v1<0 && v2>0) return false;
		if (v1>0 && v2<0) return false;
	  v3=n3.x*p.x+n3.y*p.y+n3.z*p.z;
		if ((v1<=0 && v2<=0 && v3<=0) || (v1>=0 && v2>=0 && v3>=0)) return true;
		return false;
	}
	public boolean allOcean() {
	  return b.isOcean() && c.isOcean() && d.isOcean();
	}
	public void calcArea() {
	  // Heron's formula
		double l1=b.getPos().distance(c.getPos());
		double l2=c.getPos().distance(d.getPos());
		double l3=d.getPos().distance(b.getPos());
		double s=(l1+l2+l3)*0.5;
	  area=Math.sqrt(s*(s-l1)*(s-l2)*(s-l3));
	}
  public void scaleHeights(double s) {
    // Change the rock thickness of all the points used by this tet, according to ratio 'd'
    b.scale(s);
    c.scale(s);
    d.scale(s);
  }
  public TecPlate getPlate() {
    if (b.getPlate()==c.getPlate() && b.getPlate()==d.getPlate()) return b.getPlate();
    return null;
  }
}




