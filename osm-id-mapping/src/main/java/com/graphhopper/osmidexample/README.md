# How to create sets of shortest paths
The MyImportXXX files in this folder gives examples on how 
* to take an
OD input file consisting of a set of origin - destination coordinates and 
* calculate shortest paths for each pair and
* write them to a file.

For example the **MyImport_TK_threeways** uses ./data/od_coords_3ways.csv as the OD input file. 
The format of the file is:
    
    ht,wt,homeX,homeY,workX,workY,home_S000,CarTotal_from_MetroFare,CarTotal_withtaxi_fromACS
    11001000100,11001000100,-77.0597078,38.90556743,-77.0597078,38.90556743,89,89,31.35292229

Field 8 (f8 used as a file reference - it is the 9th value, but the index starts with 0) - 
is **CarTotal_withtaxi_fromACS**. The count is 31.35...
What we do is to generate a shortest path from the origin coords to the 
destination and assign the count of 31.35 to each of the edges of this path.

Also a note on the **coordinates** - the following code reads in the coords from the input file. 
Observe the ordering!

    Float.parseFloat(data[3]), 
    Float.parseFloat(data[2]),
    Float.parseFloat(data[5]),
    Float.parseFloat(data[4])).


The output file ./data/od_tk_3ways_f8_routes.csv will look as follows.

    
    i_id,osm_id,count
    2,v,917757,673665486,6.341040462
    2, ,382491,46899646,6.341040462

i_id is the SP and we record for each osm_id the count which comes from the OD file.

__