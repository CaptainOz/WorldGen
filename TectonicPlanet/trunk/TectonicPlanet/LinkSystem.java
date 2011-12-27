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

/**
 * keeps records of which TecPoints are linked to which
 * @author Tom Groves
 */
public class LinkSystem {
  private HashMap tree;
	private HashMap pointLinks;
	
  public LinkSystem() {
    tree=new HashMap();
    pointLinks=new HashMap();
  }
  public void addLink(TecPoint a, TecPoint b) {
		if (!hashOK(a,b)) {
			System.out.println("!!!!!!HASH NOT OK!!!!!!!!");
			System.exit(1);
		}
		if (a==null) {
			System.out.println("Can't 'addlink' with a null TecPoint!!");
			System.exit(1);
		}
		if (b==null) {
			System.out.println("Can't 'addlink' with a null TecPoint!!");
			System.exit(1);
		}
		if (a==b) {
			System.out.println("Can't 'addlink' with A and B as the same point!!");
			System.exit(1);
		}
    LinkPair nl=new LinkPair(a,b);
    if (getLinkPair(new Long(getHash(a,b)))!=null) {
      // Increment the count of that link
      getLinkPair(new Long(getHash(a,b))).increment();
    } else {
      // Add new link
      tree.put(new Long(getHash(a,b)), nl);
    }
		if (!pointLinks.containsKey(a)) pointLinks.put(a,new ArrayList());
		if (!pointLinks.containsKey(b)) pointLinks.put(b,new ArrayList());
	  ArrayList v1=getPointLinks(a);
	  ArrayList v2=getPointLinks(b);
		if (!v1.contains(b)) v1.add(b);
		if (!v2.contains(a)) v2.add(a);
  }
  public void removeLink(TecPoint a, TecPoint b) {
    LinkPair nl=new LinkPair(a,b);
    if (getLinkPair(new Long(getHash(a,b)))!=null) {
      // Decrement the count of that link
      getLinkPair(new Long(getHash(a,b))).decrement();
      if (getLinkPair(new Long(getHash(a,b))).getCount()==0)
        tree.remove(new Long(getHash(a,b)));
    } else {
      // WTF?!
      System.out.println("Can't remove that link - it isn't there!");
    }
		if (pointLinks.containsKey(a)) {
  	  ArrayList v1=getPointLinks(a);
		  if (v1!=null) v1.remove(b);
		}
		if (pointLinks.containsKey(b)) {
			ArrayList v2=getPointLinks(b);
			if (v2!=null) v2.remove(a);
		}
  }
  public LinkPair getLinkPair(Long i) {
    return (LinkPair)tree.get(i);
  }
  public LinkPair getLinkPair(TecPoint a, TecPoint b) {
    return (LinkPair)tree.get(getHash(a,b));
  }
  public int getCount(TecPoint a, TecPoint b) {
    LinkPair lp=(LinkPair)tree.get(new Long(getHash(a,b)));
    if (lp==null) return 0;
    return lp.getCount();
  }
  public int size() {return tree.size();}
  public Iterator getIterator() {return tree.values().iterator();}
  public Collection getCollection() {return tree.values();}
  public long getHash(TecPoint a, TecPoint b) {
    long v1=Math.min(a.hash,b.hash);
    long v2=Math.max(a.hash,b.hash);
    return (v1*(v1+2*v2+1) + (v2+1)*(v2+2))/2+1;
  }
	public boolean hashOK(TecPoint a, TecPoint b) {
	  return getHash(a,b)==getHash(b,a);
	}
  public void empty() {tree=new HashMap();pointLinks=new HashMap();}
	public int getPointLinksSize(TecPoint p) {
	  if (!pointLinks.containsKey(p)) return -1;
		ArrayList v=(ArrayList)pointLinks.get(p);
		return v.size();
	}
	public ArrayList getPointLinks(TecPoint p) {
	  if (!pointLinks.containsKey(p))
			return null;
		return (ArrayList)pointLinks.get(p);
	}
	public void removePoint(TecPoint p) {
		if (pointLinks.containsKey(p)) {
			ArrayList linkedPoints=new ArrayList(getPointLinks(p));
			for (int i=0; i<linkedPoints.size(); i++)
				removeLink(p,(TecPoint)linkedPoints.get(i));
		}
    ArrayList linkPairVec=new ArrayList(tree.values());
		for (int i=0; i<linkPairVec.size(); i++) {
			LinkPair lp=(LinkPair)linkPairVec.get(i);
			if (lp.getA()==p || lp.getB()==p) removeLink(lp.getA(),lp.getB());
		}
	}
	public ArrayList getLinkedPoints(TecPoint p, ArrayList points) {
	  ArrayList out=new ArrayList();
	  TecPoint tempPoint;
	  for (int i=0; i<points.size(); i++) {
		  tempPoint=(TecPoint)points.get(i);
			if (getCount(p,tempPoint)>0 && p!=tempPoint) out.add(tempPoint);
		}
		return out;
	}
	public HashSet getLinkedPoints(TecPoint p, HashSet points) {
	  HashSet out=new HashSet();
	  TecPoint tempPoint;
    Iterator iter=points.iterator();
    while (iter.hasNext()) {
	  //for (int i=0; i<points.size(); i++) {
		  tempPoint=(TecPoint)iter.next();//get(i);
			if (getCount(p,tempPoint)>0 && p!=tempPoint) out.add(tempPoint);
		}
		return out;
	}
  public double linkWidth(LinkPair lp) {
    // return the width of this link
    // ie 1/3rd of the distance between the two points which complete the 2 tets this link is part of
    TecPoint a=lp.getA();
    TecPoint b=lp.getB();
    // Find the two points
    HashSet points1=new HashSet(getPointLinks(a));
    HashSet points2=new HashSet(getPointLinks(b));
    points1.remove(b);
    points2.remove(a);
    TecPoint p1=null, p2=null, tempPoint;
    Iterator iter=points2.iterator();
    while (iter.hasNext() && p2==null) {
      tempPoint=(TecPoint)iter.next();
      if (points1.contains(tempPoint)) {
        if (p1==null) p1=tempPoint; else p2=tempPoint;
      }
    }
    if (p2!=null) return p1.getPos().distance(p2.getPos())/3;
    System.out.println("Couldn't find width of link");
    iter=points1.iterator();
    System.out.println("points1 contains:");
    while (iter.hasNext()) {
      tempPoint=(TecPoint)iter.next();
      System.out.println(tempPoint);
    }
    iter=points2.iterator();
    System.out.println("points2 contains:");
    while (iter.hasNext()) {
      tempPoint=(TecPoint)iter.next();
      System.out.println(tempPoint);
    }
    return -1;
  }
}

class LinkPair implements Comparable {
  private TecPoint a,b;
  private int count=1;
  private long hash;
  public double pushForce;  // The outwards force exerted by this link
  public double linkWidth;  // The width of this link
  public boolean broken=false;  // Is this link broken, for the purposes of the FEA?
  public boolean plateCrosser=false;  // Does this link connect two different plates?
  public Color col;   // Just for speedup

  public LinkPair(TecPoint a, TecPoint b) {
    this.a=a; this.b=b;
    long v1=Math.min(a.hash,b.hash);
    long v2=Math.max(a.hash,b.hash);
    hash= (v1*(v1+2*v2+1) + (v2+1)*(v2+2))/2+1;
  }
/*  public boolean equals(Object ob) {
    try {
      LinkPair other=(LinkPair)ob;
      return ((other.a==a && other.b==b) || (other.a==b && other.b==a));
    } catch(Exception e) {
      System.out.println("Trying to 'equals' a LinkPair with another class of object");
      return false;
    }
  }
  public int hashCode() {
    return 194357107+hash;
  }*/
  public int getCount() {return count;}
  public void increment() {
    if (++count>2) {
      System.out.println("Incremented LinkPair count above 2!");
      //System.exit(1);
    }
  }
  public void decrement() {
    if (--count<0) {
      System.out.println("Decremented LinkPair count below 0!");
      //System.exit(1);
    }
  }
  public int compareTo(Object ob) {
    // Compares this object with the specified object for order.
    try {
      LinkPair other=(LinkPair)ob;
      if ((other.a==a && other.b==b) || (other.a==b && other.b==a)) return 0;
    } catch(Exception e) {}

    return hashCode()-ob.hashCode();
  }
  public TecPoint getA() {return a;}
  public TecPoint getB() {return b;}
	public double getLength() {return a.getPos().distance(b.getPos());}
	public double getLengthSq() {return a.getPos().distanceSquared(b.getPos());}
  // The FEA bit
	public void sortLink() {
    if (!broken) {
  	  //double dx=cell2.x-cell1.x;
  	  //double dy=cell2.y-cell1.y;
  		double invlen=1.0/a.getPos().distance(b.getPos());
  		//double nx=dx*invlen;
  		//double ny=dy*invlen;
      Vector3d linkVec=new Vector3d(b.getPos());
      linkVec.sub(a.getPos());
      linkVec.normalize();  // LinkVec is the normal vector pointing from a to b
      
  		// Remove effects of link
      a.FEAforce.add(new Vector3d(linkVec.x*pushForce,linkVec.y*pushForce,linkVec.z*pushForce));
      b.FEAforce.sub(new Vector3d(linkVec.x*pushForce,linkVec.y*pushForce,linkVec.z*pushForce));
      
      // Work out what the pushForce _should_ be (to minimise the difference between the two tecPoints)
  		double f1=a.getForce().dot(linkVec);  // The force on the tecPoint in the direction of the link
  		double f2=b.getForce().dot(linkVec);  // The force on the tecPoint in the direction of the link
  		double diff=f1-f2;
  		
  		double change=0;
  		pushForce=pushForce*change+ (1-change)*diff/2;
  		if (broken) pushForce=0;
  		
  		// Put effects back
      a.FEAforce.sub(new Vector3d(linkVec.x*pushForce,linkVec.y*pushForce,linkVec.z*pushForce));
      b.FEAforce.add(new Vector3d(linkVec.x*pushForce,linkVec.y*pushForce,linkVec.z*pushForce));
    }
  }
  public void removeFromFEA() {
    double invlen=1.0/a.getPos().distance(b.getPos());
    Vector3d linkVec=new Vector3d(b.getPos());
    linkVec.sub(a.getPos());
    linkVec.normalize();  // LinkVec is the normal vector pointing from a to b
    // Remove effects of link
    a.FEAforce.add(new Vector3d(linkVec.x*pushForce,linkVec.y*pushForce,linkVec.z*pushForce));
    b.FEAforce.sub(new Vector3d(linkVec.x*pushForce,linkVec.y*pushForce,linkVec.z*pushForce));
  }
}












