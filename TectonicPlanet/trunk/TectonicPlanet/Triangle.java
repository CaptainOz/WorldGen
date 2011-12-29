package TectonicPlanet;

import javax.vecmath.*;

/**
 * A Triangle is a single part of a tectonic plate. It is the smallest unit used
 * to calculate movements and topology.
 *
 * @author Tom Groves
 * @author Nate Lillich
 */
class Triangle {
    public TecPoint p1, p2, p3;
    public boolean facingUp;
    private double m_areaSquared;
    private Point3d m_center;

    public Triangle(){
    }

    public Triangle( TecPoint a, TecPoint b, TecPoint c ){
        p1 = a;
        p2 = b;
        p3 = c;
    }

    /**
     * Detects if this Triangle is equal to the other one.
     *
     * @bug If either triangle has overlapping points it could lead to false
     *      positives.
     *
     * @param t The triangle to compare this to.
     * @return True if the triangles have 3 matching points.
     */
    public boolean equals( Triangle t ){
        return (p1.equals( t.p1 ) || p1.equals( t.p2 ) || p1.equals( t.p3 )) &&
               (p2.equals( t.p1 ) || p2.equals( t.p2 ) || p2.equals( t.p3 )) &&
               (p3.equals( t.p1 ) || p3.equals( t.p2 ) || p3.equals( t.p3 ));
    }

    /**
     * Calculates the center of the triangle.
     *
     * @return The point at the center of the triangle.
     */
    public Point3d getCenter(){
        Point3d center = new Point3d();
        center.x = (p1.getPos().x + p2.getPos().x + p3.getPos().x) / 3;
        center.y = (p1.getPos().y + p2.getPos().y + p3.getPos().y) / 3;
        center.z = (p1.getPos().z + p2.getPos().z + p3.getPos().z) / 3;
        return center;
    }

    /**
     * Estimates the obtuseness of the triangle by comparing the length of the
     * hypotenuse to the legs.
     *
     * @return The ratio of hypotenuse length to combined leg length.
     */
    public double sharpness(){
        // Get the lengths of each side.
        double length1, length2, length3, temp;
        length1 = p1.getPos().distance( p2.getPos() );
        length2 = p1.getPos().distance( p3.getPos() );
        length3 = p2.getPos().distance( p3.getPos() );

        // Swap the lengths such that length3 is the longest.
        if( length1 > length2 ){
            temp    = length1;
            length1 = length2;
            length2 = temp;
        }
        if( length2 > length3 ){
            temp    = length2;
            length2 = length3;
            length3 = temp;
        }
        if( length1 > length2 ){
            temp    = length1;
            length1 = length2;
            length2 = temp;
        }
        if( length2 > length3 ){
            temp    = length2;
            length2 = length3;
            length3 = temp;
        }

        // Return the aspect ratio of the hypotenuse to the legs.
        return length3 / (length2 + length1);
    }

    /**
     * Detects if this Triangle is on the edge of a larger polyhedron.
     *
     * @return True if any of the edges of this triangle have no neighbor.
     */
    public boolean isEdge(){
        return (p1.edge && p2.edge) ||
               (p2.edge && p3.edge) ||
               (p3.edge && p1.edge);
    }

    /**
     * Detects if this triangle is facing upward and caches the result.
     */
    public void cacheIsFacingUp(){
        facingUp = isFacingUp();
    }

    /**
     * Calculates and caches the square of the area of the triangle.
     */
    public void cacheArea(){
        m_areaSquared = getAreaSquared();
    }

    /**
     * Calculates and caches the center of the triangle.
     */
    public void cacheCenter(){
        m_center = getCenter();
    }

    /**
     * Returns a cached value for the center of the Triangle.
     *
     * @return The cached point at the center of the triangle.
     */
    public Point3d getOldCenter(){
        return m_center;
    }

    /**
     * Returns the cached area of the triangle.
     *
     * @return The cached area.
     */
    public double getOldArea(){
        return Math.sqrt( m_areaSquared );
    }

    /**
     * Returns the cached squared area of the triangle.
     *
     * @return The cached area, squared.
     */
    public double getOldAreaSquared(){
        return m_areaSquared;
    }

    /**
     * Detects if the triangle is facing up (away from the planet center).
     *
     * @return True if the triangle faces away from the planet center.
     */
    public boolean isFacingUp(){
        // From center of planet to the plate
        Vector3d planetRadian = new Vector3d( p1.getPos() );

        // Perpendicular to the plate
        Vector3d faceNormal = new Vector3d();
        Vector3d side1 = new Vector3d( p1.getPos().x - p2.getPos().x,
                                       p1.getPos().y - p2.getPos().y,
                                       p1.getPos().z - p2.getPos().z );
        Vector3d side2 = new Vector3d( p1.getPos().x - p3.getPos().x,
                                       p1.getPos().y - p3.getPos().y,
                                       p1.getPos().z - p3.getPos().z );
        faceNormal.cross( side1, side2 );
        return planetRadian.dot( faceNormal ) >= 0;
    }

    /**
     * Calculates the edge lengths in squares and returns the largest.
     *
     * @return The square of the length of the longest leg.
     */
    public double getLongestEdgeSquared(){
        // Calculate the length of each leg.
        double edge1Length = p1.getPos().distanceSquared( p2.getPos() );
        double edge2Length = p2.getPos().distanceSquared( p3.getPos() );
        double edge3Length = p3.getPos().distanceSquared( p1.getPos() );

        // Return the largest of the three.
        return Math.max( edge1Length, Math.max( edge2Length, edge3Length ) );
    }

    /**
     * Detects if this triangle spans the gap between two tectonic plates.
     *
     * @return True if any edge is not on the same plate as any other.
     */
    public boolean bridgesPlates(){
        return p1.getPlate() != p2.getPlate() ||
               p1.getPlate() != p3.getPlate() ||
               p2.getPlate() != p3.getPlate();
    }

    /**
     * Calculates the square of the triangle's area.
     *
     * @return The area of the triangle, squared.
     */
    public double getAreaSquared(){
        double a,b,c;
        a = p1.getPos().distanceSquared( p2.getPos() );
        b = p1.getPos().distanceSquared( p3.getPos() );
        c = p2.getPos().distanceSquared( p3.getPos() );
        return (2*b*c + 2*c*a + 2*a*b - a*a - b*b - c*c) / 16;
    }

    /**
     * Calculates the area of the triangle.
     *
     * @return The area of the triangle.
     */
    public double getArea(){
        return Math.sqrt( getAreaSquared() );
    }
}
