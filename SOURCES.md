# Attribution

This project uses a set of online datasets, some of which require attribution. I am not a legal expert so some of these may be done wrong. This file also provides more insight into the nature of the datasources we use.

## [OpenStreetMap](https://www.openstreetmap.org/)

OpenSteetMap is a public map database of that can be contributed to by anyone.
It is requested that they are credited with the standard:

© OpenStreetMap contributors

We use [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API) to download data when the mod is running. Different Overpass API instances have are hosted by different companies and have different use policies. The [default instance](https://overpass.kumi.systems/) used by this mod, hosted by [Kumi Systems](https://kumi.systems/), has no restrictions on usage. There is an option to change the instance, but make sure you follow their usage policies. Change at your own risk.

Disclaimer: Please do not edit OpenStreetMap data to change in game locations or any purpose other than improving the accuracy or relevancy of the data. This is unethical and only hurts the hundreds of contributors to the non-profit organization, that remains a modern beacon of free public information.

## Climate (monthly average tempature and percipitaion)

The [tempature and percipitaion datasets](http://climate.geog.udel.edu/~climate/html_pages/README.lw.html) were download from [Willmott, Matsuura and Collaborators' Global Climate Resource Pages](http://climate.geog.udel.edu/~climate/) under the [University of Delaware](https://www.udel.edu/). They have a precision of 0.5° (30 arcminutes) and were created by interpolating the readings of many weather staions using a neural network.

These sources are provided as a service to the public and they do not guarantee that the information is correct or up to date. [disclaimer](http://climate.geog.udel.edu/~climate/html_pages/disclaimer.html)

A filtered copy of these sources (some of the unnecessary data is filtered out) is stored inside the mods assets and does not need to be downloaded.

## [Global Soil Suborder Map](https://www.nrcs.usda.gov/wps/portal/nrcs/detail/soils/use/?cid=nrcs142p2_054013)

This is a rasterized image with an accuracy of 2 arcminutes (2/60°) and is used along with climate data to estimate the biome of a region.

It is provided by the [Natural Resources Conservation Service](https://www.nrcs.usda.gov/) under the United States Department of Agriculture.

A compressed copy of this dataset is kept within the mod so it does not need to be downloaded in realtime.

## [Tree Cover 2000](https://data.globalforestwatch.org/datasets/tree-cover-2000)

This dataset shows tree canopy data from around the year 2000 (A little out of date but we are working on updating). It also has other components like forest cover loss but that is not used.

Citation:

Source: Hansen/UMD/Google/USGS/NASA

Hansen, M. C., P. V. Potapov, R. Moore, M. Hancher, S. A. Turubanova, A. Tyukavina, D. Thau, S. V. Stehman, S. J. Goetz, T. R. Loveland, A. Kommareddy, A. Egorov, L. Chini, C. O. Justice, and J. R. G. Townshend. 2013. “High-Resolution Global Maps of 21st-Century Forest Cover Change.” Science 342 (15 November): 850–53. Data available on-line from: http://earthenginepartners.appspot.com/science-2013-global-forest.

The [default host](https://gis-treecover.wri.org/arcgis/rest/services) is the [World Resources Institute](https://www.wri.org/) on their [ArcGis REST](https://developers.arcgis.com/rest/) [server](https://gis-treecover.wri.org/arcgis/rest/services). There are other hosts but, this is the leagally shakiest dataset so edit at your own risk.

This may be moved to a github based file host in the future.

Their ImageServer is used to produce tiff files of the forest cover of a region which is then used to guide the procedural placement the placement of trees.

## Mapzen Terrain Tiles ([Joerd](https://github.com/tilezen/joerd/))

The [Amazon Web Services Terrain Tiles](https://registry.opendata.aws/terrain-tiles/) is used to download this data as the game runs. Their original source is Joerd made for [Mapzen](https://www.mapzen.com/terms/) which is currently closed to new users.

These tiles are a conglomeration of several datasources, some of which must be [attributed](https://github.com/tilezen/joerd/blob/master/docs/attribution.md#the-fine-print). Since the entire globe is used, none can be left out.

3DEP, STRM, and GMTED2010 data courtesy of the U.S. Geological Survey

DOC/NOAA/NESDIS/NCEI > National Centers for Environmental Information, NESDIS, NOAA, U.S. Department of Commerce

Land Information New Zeland Data: Copyright 2011 Crown copyright (c) Land Information New Zealand and the New Zealand Government. All rights reserved

data.gov.uk LIDAR Composite Digital Terrain Model: © Environment Agency copyright and/or database right 2015. All rights reserved.

data.gv.at Digitales Geländemodell (DGM) Österreich: © offene Daten Österreichs – Digitales Geländemodell (DGM) Österreich.

data.kartverket.no Digital terrengmodell: © Kartverket

Arctic Digital Elevation Model (ArcticDEM): DEM(s) were created from DigitalGlobe, Inc., imagery and funded under National Science Foundation awards 1043681, 1559691, and 1542736.

Digital Terrain Model over Europe (EU-DEM): Produced using Copernicus data and information funded by the European Union - EU-DEM layers.

Canadian Digital Elevation Model (CDEM): Contains information licensed under the Open Government Licence – Canada.

National Institute of Statistics and Geography (INEGI): Source: INEGI, Continental relief, 2016

Digital Elevation Model (DEM) of Australia derived from LiDAR 5 Metre Grid: © Commonwealth of Australia (Geoscience Australia) 2017.
