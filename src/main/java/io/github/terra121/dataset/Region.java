package io.github.terra121.dataset;

import java.util.HashSet;
import java.util.Set;

public class Region {
    public boolean failedDownload;
    public final OpenStreetMaps.Coord coord;
    public final Water water;
    public LandLine southLine;
    public LandLine[] lines;
    public final double south;
    public final double west;

    public short[][] indexes;
    public byte[][] states;

    public Region(OpenStreetMaps.Coord coord, Water water) {
        this.coord = coord;
        this.water = water;

        this.lines = new LandLine[water.hres];
        for (int i = 0; i < this.lines.length; i++) {
            this.lines[i] = new LandLine();
        }

        this.southLine = new LandLine();

        this.south = coord.y * OpenStreetMaps.TILE_SIZE;
        this.west = coord.x * OpenStreetMaps.TILE_SIZE;
    }

    public void addWaterEdge(double slon, double slat, double elon, double elat, long type) {

        slat -= this.south;
        elat -= this.south;
        slon -= this.west;
        elon -= this.west;

        slon /= OpenStreetMaps.TILE_SIZE / this.water.hres;
        elon /= OpenStreetMaps.TILE_SIZE / this.water.hres;

        slat /= OpenStreetMaps.TILE_SIZE / this.water.hres;
        elat /= OpenStreetMaps.TILE_SIZE / this.water.hres;

        if (slat <= 0 || elat <= 0 && (slat >= 0 || elat >= 0)) {
            if (slat == 0) {
                slat = 0.00000001;
            }
            if (elat == 0) {
                elat = 0.00000001;
            }

            if (elat != slat) {
                double islope = (elon - slon) / (elat - slat);
                this.southLine.add(elon - islope * elat, type);
            }
        }

        if (slon != elon) {
            double slope = (elat - slat) / (elon - slon);

            int beg = (int) Math.ceil(Math.min(slon, elon));
            int end = (int) Math.floor(Math.max(slon, elon));

            if (beg < 0) {
                beg = 0;
            }
            if (end >= this.water.hres) {
                end = this.water.hres - 1;
            }

            for (int x = beg; x <= end; x++) {
                this.lines[x].add(slope * x + (elat - slope * elon), type);
            }
        }
    }

    private void addComp(Object[] line, int x) {
        this.indexes[x] = (short[]) line[0];
        this.states[x] = (byte[]) line[1];
    }

    public void renderWater(Set<Long> ground) {
        double size = OpenStreetMaps.TILE_SIZE;
        double ressize = OpenStreetMaps.TILE_SIZE / this.water.hres;

        this.indexes = new short[this.water.hres][];
        this.states = new byte[this.water.hres][];

        this.southLine.run(this.water.hres, ground, (status, x) -> this.addComp(this.lines[x].compileBreaks(new HashSet<>(status), this.water.hres), x));

        //we are done with these resources, now that they are compiled
        this.lines = null;
        this.southLine = null;

		/*for(int y=0;y<test.length;y++) {
			for(int x=0;x<test[0].length;x++) {
				char c = test[test.length-y-1][x]>0?' ':'#';
				System.out.print(c+""+c);
			}
			System.out.println();
		}*/
    }

    //another fliping binary search, why can't java have a decent fuzzy one built in
    public int getStateIdx(short x, short y) {
        short[] index = this.indexes[x];

        int min = 0;
        int max = index.length;

        while (min < max - 1) {
            int mid = min + (max - min) / 2;
            if (index[mid] < y) {
                min = mid;
            } else if (index[mid] > y) {
                max = mid;
            } else {
                return mid;
            }
        }
        return min;
    }

    public int hashCode() {
        return this.coord.hashCode();
    }

    public boolean equals(Object other) {
        return (other instanceof Region) && this.coord.equals(((Region) other).coord);
    }

    public enum BoundaryType {
        water
    }
}
