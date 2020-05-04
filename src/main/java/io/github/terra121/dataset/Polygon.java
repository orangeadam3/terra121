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

    private int _minX = Integer.MAX_VALUE;
    public int minX() {
        if (_minX != Integer.MAX_VALUE) return _minX;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lon < _minX)
                _minX = (int)g.lon;
        }
        return _minX;
    }
    private int _minZ = Integer.MAX_VALUE;
    public int minZ() {
        if (_minZ != Integer.MAX_VALUE) return _minZ;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lat < _minZ)
                _minZ = (int)g.lat;
        }
        return _minZ;
    }
    private int _maxX = Integer.MIN_VALUE;
    public int maxX() {
        if (_maxX != Integer.MIN_VALUE) return _maxX;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lon > _maxX)
                _maxX = (int)g.lon;
        }
        return _maxX;
    }
    private int _maxZ = Integer.MIN_VALUE;
    public int maxZ() {
        if (_maxZ != Integer.MIN_VALUE) return _maxZ;
        for (OpenStreetMaps.Geometry g : vertices) {
            if (g.lat > _maxZ)
                _maxZ = (int)g.lat;
        }
        return _maxZ;
    }
}
