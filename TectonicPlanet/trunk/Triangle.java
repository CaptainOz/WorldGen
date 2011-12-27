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




class Triangle {
  public TecPoint p1,p2, p3;
  public boolean facingUp;
  private double areaSquared;
  private Point3d center;

  public Triangle() {}
  public Triangle(TecPoint a,TecPoint b,TecPoint c) {
    p1=a;
    p2=b;
    p3=c;
  }

  public boolean sameAs(Triangle t) {
    return (p1.equals(t.p1) || p1.equals(t.p2) || p1.equals(t.p3)) && 
           (p2.equals(t.p1) || p2.equals(t.p2) || p2.equals(t.p3)) && 
           (p3.equals(t.p1) || p3.equals(t.p2) || p3.equals(t.p3));
  }

  public Point3d getCenter() {
    return new Point3d((p1.getPos().x+p2.getPos().x+p3.getPos().x)/3,
                       (p1.getPos().y+p2.getPos().y+p3.getPos().y)/3,
                       (p1.getPos().z+p2.getPos().z+p3.getPos().z)/3);
  }

  public double sharpness() {
    double l1,l2,l3,temp;
    l1=p1.getPos().distance(p2.getPos());
    l2=p1.getPos().distance(p3.getPos());
    l3=p2.getPos().distance(p3.getPos());
    if (l1>l2) {temp=l1;l1=l2;l2=temp;}
    if (l2>l3) {temp=l2;l2=l3;l3=temp;}
    if (l1>l2) {temp=l1;l1=l2;l2=temp;}
    if (l2>l3) {temp=l2;l2=l3;l3=temp;}
    return l3/(l2+l1);
  }

  public boolean isEdge() {
    return (p1.edge&&p2.edge)||(p2.edge&&p3.edge)||(p3.edge&&p1.edge);
  }
  public void checkWayUp() {
    facingUp=wayUp();
  }
  public void logArea() {
    areaSquared=getAreaSquared();
  }
  public void logCenter() {
    center=getCenter();
  }
  public Point3d getOldCenter() {
    return center;
  }
  public double getOldArea() {
    return Math.sqrt(areaSquared);
  }
  public double getOldAreaSquared() {
    return areaSquared;
  }
  public boolean wayUp() {
    Vector3d v1=new Vector3d(p1.getPos());	// From center of planet to the plate
    Vector3d v2=new Vector3d();			// Perp to the plate
    v2.cross(new Vector3d(p1.getPos().x-p2.getPos().x,
                          p1.getPos().y-p2.getPos().y,
                          p1.getPos().z-p2.getPos().z),
             new Vector3d(p1.getPos().x-p3.getPos().x,
                          p1.getPos().y-p3.getPos().y,
                          p1.getPos().z-p3.getPos().z));
    return (v1.dot(v2)>=0);
  }
  public double longestEdgeSq() {
    return Math.max(p1.getPos().distanceSquared(p2.getPos()),Math.max(p1.getPos().distanceSquared(p3.getPos()),p2.getPos().distanceSquared(p3.getPos())));
  }
  public boolean bridgesPlates() {
    return p1.getPlate()!=p2.getPlate() || p1.getPlate()!=p3.getPlate() || p2.getPlate()!=p3.getPlate();
  }
  public double getAreaSquared() {
    double a,b,c;
    a=p1.getPos().distanceSquared(p2.getPos());
    b=p1.getPos().distanceSquared(p3.getPos());
    c=p2.getPos().distanceSquared(p3.getPos());
    return (2*b*c+2*c*a+2*a*b-a*a-b*b-c*c)/16;
  }
  public double getArea() {
    return Math.sqrt(getAreaSquared());
  }
}


