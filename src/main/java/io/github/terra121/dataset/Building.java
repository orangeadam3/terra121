package io.github.terra121.dataset;

import io.github.terra121.projection.GeographicProjection;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Building {
    public Polygon[] outerPolygons;
    public Polygon[] innerPolygons;

    public boolean hasCalculatedHeights = false;
    public double heightOfLowestCorner;
    public double heightOfHighestCorner;

    public OpenStreetMaps.Element element;

    public int minHeight = 0;
    public int levels = 2;
    public int levelHeight = 4;
    public int height = levels * levelHeight;


    public Building(OpenStreetMaps.Element element, Map<Long, OpenStreetMaps.Element> ways) {
        this.element = element;
        String tagBuilding = element.tags.get("building");
        if (tagBuilding == null) throw new IllegalArgumentException("Building objects cannot be created from non-buildings. (No building tag)");

        if (element.type == OpenStreetMaps.EType.way) {
            Polygon polygon = new Polygon(element.geometry);
            this.outerPolygons = new Polygon[] { polygon };

            if (element.tags.containsKey("building:levels")) {
                try {
                    levels = Integer.parseInt(element.tags.get("building:levels"));
                    height = levels * levelHeight;
                } catch (NumberFormatException ignored) {}
            }
            if (element.tags.containsKey("building:height")) {
                try {
                    height = Integer.parseInt(element.tags.get("building:height"));
                    levelHeight = height / levels;
                } catch (NumberFormatException ignored) {}
            } else {
                height = levels * levelHeight;
            }
            if (element.tags.containsKey("building:min_height")) {
                try {
                    minHeight = Integer.parseInt(element.tags.get("building:min_height"));
                } catch (NumberFormatException ignored) {}
            }

        } else if (element.type == OpenStreetMaps.EType.relation) {
            List<Polygon> outerPolygons = new ArrayList<>(element.members.length);
            List<Polygon> innerPolygons = new ArrayList<>(element.members.length);

            for (OpenStreetMaps.Member member : element.members) {
                if (member.type != OpenStreetMaps.EType.way)
                    continue;
                if (!ways.containsKey(member.ref))
                    continue;
                OpenStreetMaps.Element way = ways.get(member.ref);
                Polygon polygon = new Polygon(way.geometry);
                String role = member.role;
                if (role == null)
                    role = "outer"; // If there's no role, then it's probably an outer edge.
                if (role.equals("outer")) {
                    outerPolygons.add(polygon);
                } else if (role.equals("inner")) {
                    innerPolygons.add(polygon);
                }
            }

            this.outerPolygons = outerPolygons.toArray(new Polygon[0]);
            this.innerPolygons = innerPolygons.toArray(new Polygon[0]);

            if (element.tags.containsKey("building:levels")) {
                try {
                    levels = Integer.parseInt(element.tags.get("building:levels"));
                } catch (NumberFormatException ignored) {}
            }
            if (element.tags.containsKey("building:height")) {
                try {
                    height = Integer.parseInt(element.tags.get("building:height"));
                    levelHeight = height / levels;
                } catch (NumberFormatException ignored) {}
            } else {
                height = levels * levelHeight;
            }
            if (element.tags.containsKey("building:min_height")) {
                try {
                    minHeight = Integer.parseInt(element.tags.get("building:min_height"));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            throw new IllegalArgumentException("Building Object constructor requires either a way or relation element");
        }


    }

    public boolean contains(int x, int z) {
        boolean inside = false;
        for (Polygon p : outerPolygons)
            if (p.isInside(x, z))
                inside = true;

        if (this.innerPolygons != null)
            for (Polygon p : innerPolygons)
                if (p.isInside(x, z))
                    inside = false;
        return inside;
    }

    public Building projectFromGeo(GeographicProjection projection) {
        if (projection == null)
            throw new IllegalArgumentException("Projection cannot be null!");
        for (Polygon p : outerPolygons)
            p.projectFromGeo(projection);
        if (this.innerPolygons != null)
        for (Polygon p : innerPolygons)
            p.projectFromGeo(projection);
        return this;
    }

    public int minX() {
        int min = Integer.MAX_VALUE;
        for (Polygon p : outerPolygons) {
            int minX = p.minX();
            if (minX < min)
                min = minX;
        }
        return min;
    }
    public int minZ() {
        int min = Integer.MAX_VALUE;
        for (Polygon p : outerPolygons) {
            int minZ = p.minZ();
            if (minZ < min)
                min = minZ;
        }
        return min;
    }
    public int maxX() {
        int max = Integer.MIN_VALUE;
        for (Polygon p : outerPolygons) {
            int maxX = p.maxX();
            if (maxX > max)
                max = maxX;
        }
        return max;
    }
    public int maxZ() {
        int max = Integer.MIN_VALUE;
        for (Polygon p : outerPolygons) {
            int maxZ = p.maxZ();
            if (maxZ > max)
                max = maxZ;
        }
        return max;
    }

    public void calculateHeights(Heights heights, GeographicProjection projection) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < outerPolygons.length; i++) {
            Polygon p = outerPolygons[i];
            for (OpenStreetMaps.Geometry g : p.vertices) {
                double[] geo = projection.toGeo(g.lon, g.lat);
                double height = heights.estimateLocal(geo[0], geo[1]);
                if (height > max)
                    max = height;
                if (height < min)
                    min = height;
            }
        }
        this.heightOfLowestCorner = min;
        this.heightOfHighestCorner = max;

        this.hasCalculatedHeights = true;
    }

    @Override
    public String toString() {
        if (!element.tags.containsKey("name"))
        return "Building{" +
                "heightOfLowestCorner=" + heightOfLowestCorner +
                ", heightOfHighestCorner=" + heightOfHighestCorner +
                '}';
        else
            return "Building{" +
                    "name = " + element.tags.get("name") +
                    ", heightOfLowestCorner=" + heightOfLowestCorner +
                    ", heightOfHighestCorner=" + heightOfHighestCorner +
                    '}';
    }
}
