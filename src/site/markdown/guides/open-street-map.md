  By default, when building a transit data bundle, the builder process will automatically attempt to download OpenStreetMap data in the area covered by your transit network to power the street-network layer of the trip planner.  The OSM downloader breaks the area up into a grid and downloads the OSM data for each grid to avoid the problem of requesting more data than the API can handle.  That said, for some particularly dense areas of OSM data, it's hard to use the API without getting lots of 400 errors for requesting too much data.

  In these situations, one option is to download the OSM data you need directly and feed it into the transit data bundle.  While it's possible to download the [entire OSM planet file](http://wiki.openstreetmap.org/wiki/Planet.osm), it's more sensible to download a specific extract for your country or state.

  Even then, the resulting OSM extract will have more data than you want.  However, I've got a procedure for selecting a subset of data relevant to your transit network.  It's a two step process:

Generate a Bounding Polygon

  We want to generate a bounding polygon that captures the region covered by your transit network.  I wrote a utility program that will accept as input your GTFS feed and produce a polygon definition compatible with the OSM `osmosis` tool.

~~~
java -cp transit-data-federation.jar \
 org.onebusaway.transit_data_federation.utilities.GtfsComputePolylineBoundaryForStopsMain \
 -format osm \
 path/to/gtfs.zip \
 path/to/polygon_output
~~~

Extract OSM Data in the Bounding Polygon

  The [ Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis) tool can be used to apply transforms to OSM data.  Specifically, we will use its bounding polygon selection tool to produce an extract of an OSM file that has just the data that falls within the area of your transit network.

~~~
osmosis --read-xml file="planet-latest.osm" --bounding-polygon file="polygon.txt" --write-xml file="extract.osm"
~~~