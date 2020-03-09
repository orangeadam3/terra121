# These instructions are for older versions of Terra 1-to-1, to allow coordinate calculations!!!

## This may not work with different projection models or orientations that are usable in newer versions of the mod, only use this if you do not have a newer usable version.

### In the newer versions please utilize /tpll (lat) (long) to teleport


The block coordinates in Minecraft are calculated by (X, Y, Z). This mod will convert these values to coordinates on a world projection:

- X values are *(longitude × 10^5)*

- Y values is *the elevation in meters above the sea level*

- Z values are *(latitude × 10^5)*

**Remember that multiplying by 10^5 is the same as moving the decimal place 5 points to the right.**

#### Also remember that the longitude and latitude must be in decimal form (36.0660, -112.1172) and not degrees (36°03'57.6"N, 112°07'01.9"W)!!!

Example: **Yavapai Point, Grand Canyon, Arizona, USA** ([OpenStreetMap](https://www.openstreetmap.org/#map=16/36.0660/-112.1172), [Google Maps](https://www.google.com/maps/place/Yavapai+Point/@36.0660043,-112.1193887,17z))

![Yavapai Point](Pictures/YavapaiPointGrandCanyonUS1.png)

has decimal coordinates of (36.0660, -112.1172) with an elevation just under 2200 meters, multiplying the latitude and longitude by 10^5 and setting Y to 2200 (the meters from sea level) gives the (X,Y,Z) coordinates of:
**(3606600, 2200, -11211720) **

or in tp command form: 
```java
/tp 3606600 2200 -11211720
```
