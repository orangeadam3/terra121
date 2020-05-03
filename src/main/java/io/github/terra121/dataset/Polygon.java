package io.github.terra121.dataset;

import io.github.terra121.projection.GeographicProjection;

public class Polygon {
    public OpenStreetMaps.Geometry[] vertices;

    public Polygon(OpenStreetMaps.Geometry[] vertices) {
        this.vertices = vertices;
        if (vertices.length < 3)
            throw new IllegalArgumentException("Polygons cannot have fewer than 3 vertices.");
    }

    public boolean isInside(int x, int y) {
        int j = vertices.length - 1;
        boolean c = false;
        for (int i = 0; i < vertices.length; i++) {
            if (((vertices[i].lat > y) != (vertices[j].lat > y)) &&
            (x < vertices[i].lon + (vertices[j].lon - vertices[i].lon) * (y - vertices[i].lat) /
                    (vertices[j].lat - vertices[i].lat))) {
                c = !c;
            }
            j = i;
        }
        return c;
    }

    public Polygon projectFromGeo(GeographicProjection projection) {
        if (projection == null)
            throw new IllegalArgumentException("Projection cannot be null!");
        for (OpenStreetMaps.Geometry geom : vertices) {
            double[] proj = projection.fromGeo(geom.lon, geom.lat);
            geom.lon = proj[0];
            geom.lat = proj[1];
        }
        return this;
    }

    public int minX() {
        int min = Integer.MAX_VALUE;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lon < min)
                min = (int)g.lon;
        }
        return min;
    }
    public int minZ() {
        int min = Integer.MAX_VALUE;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lat < min)
                min = (int)g.lat;
        }
        return min;
    }
    public int maxX() {
        int max = Integer.MIN_VALUE;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lon > max)
                max = (int)g.lon;
        }
        return max;
    }
    public int maxZ() {
        int max = Integer.MIN_VALUE;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lat > max)
                max = (int)g.lat;
        }
        return max;
    }
}
