package TectonicPlanet;

// Standard Java imports
import java.math.*;
import java.util.*;



public class PointsByHeight {
  private TreeMap tree;
	public PointsByHeight() {
	  tree=new TreeMap();
	}
	public void add(TecPoint tp) {
	  Double height=new Double(tp.getSurfaceHeight());
		if (tree.containsKey(height)) {
		  // Already have a point with that height, add it to the set
			Vector v=(Vector)tree.get(height);
			v.add(tp);
		} else {
			// First point witht that height, make a new vector entry
			Vector n=new Vector();
			n.add(tp);
			tree.put(height,n);
		}
	}
	public Vector get(double h) {
	  return (Vector)tree.get(new Double(h));
	}
	public Vector first() {
	  return (Vector)tree.get(tree.firstKey());
	}
	public Vector last() {
	  return (Vector)tree.get(tree.lastKey());
	}
	public void removeFirst() {
	  tree.remove(tree.firstKey());
	}
	public int size() {return tree.size();}
}