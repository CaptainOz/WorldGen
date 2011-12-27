package TectonicPlanet;

// Standard Java imports
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
			ArrayList v=(ArrayList)tree.get(height);
			v.add(tp);
		} else {
			// First point witht that height, make a new vector entry
			ArrayList n=new ArrayList();
			n.add(tp);
			tree.put(height,n);
		}
	}
	public ArrayList get(double h) {
	  return (ArrayList)tree.get(new Double(h));
	}
	public ArrayList first() {
	  return (ArrayList)tree.get(tree.firstKey());
	}
	public ArrayList last() {
	  return (ArrayList)tree.get(tree.lastKey());
	}
	public void removeFirst() {
	  tree.remove(tree.firstKey());
	}
	public int size() {return tree.size();}
}