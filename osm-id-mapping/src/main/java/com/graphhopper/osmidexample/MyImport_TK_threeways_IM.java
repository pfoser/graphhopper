/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.osmidexample;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.VirtualEdgeIteratorState;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.routing.util.CarExpFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

/**
 * Start with the following command line settings:
 *
 * config=config.properties osmreader.osm=area.pbf
 *
 * @author Dieter Pfoser
 */
public class MyImport_TK_threeways_IM {

    public static void main(String[] args) {
        MyGraphHopper graphHopper = new MyGraphHopper();
//        graphHopper.init(CmdArgs.read(args));
        graphHopper.setDataReaderFile("./data/dc-baltimore_maryland.osm.pbf");
// where to store graphhopper files?
        graphHopper.setGraphHopperLocation("./data/dc-baltimore_maryland.osm-gh");
        graphHopper.setEncodingManager(EncodingManager.create("car_exp")); //Dieter: car_exp - create this one... allows for blocked edges
        graphHopper.setCHEnabled(false);

        graphHopper.importOrLoad();

        FlagEncoder encoder;
        DecimalEncodedValue avSpeedEnc;

        HashMap<Integer, Integer> edge_occ = new HashMap<>();

        // Logger logger = LoggerFactory.getLogger(MyImport3.class);
        // logger.info("edge 30 -> " + graphHopper.getOSMWay(30) + ", " + graphHopper.getGraphHopperStorage().getEdgeIteratorState(30, Integer.MIN_VALUE).fetchWayGeometry(2));

        //read OD file, contains two coordinate pairs
        try (BufferedReader csvReader = new BufferedReader(new FileReader("./data/od_coords_3ways.csv"))) {

            File route_file = new File("./data/od_tk_3ways_f8_routes.csv"); // output file for routes
            try {
                // create CSVWriter object filewriter object as parameter
                CSVWriter route_writer = new CSVWriter(new FileWriter(route_file));

                // adding header to csv
                String[] out_header = { "i_id", "osm_id", "count" };
                route_writer.writeNext(out_header);

                String header = csvReader.readLine();
                String row = null;
                int count = 0; //shortest path count

                //get the car flagEncoder for this gh instance. FE capture the weights of the graph!
                encoder = graphHopper.getEncodingManager().getEncoder("car");
                avSpeedEnc = encoder.getAverageSpeedEnc();

                while ((row = csvReader.readLine()) != null) {
                    count++;
                    if (count%10000 == 0) System.out.println(count); // route count -> include also in output as first attribute
                    //place to update weight database...

                    long old_osmway = -1;

                    String[] data = row.split(",");

                    GHResponse rsp = new GHResponse();
                    List<Path> paths = graphHopper.calcPaths(new GHRequest(
                            Float.parseFloat(data[3]),
                            Float.parseFloat(data[2]),
                            Float.parseFloat(data[5]),
                            Float.parseFloat(data[4])).
                            setWeighting("fastest").setVehicle("car").setPathDetails(Arrays.asList("average_speed", "edge_id", "time")), rsp);
    //                List<Path> paths = graphHopper.calcPaths(new GHRequest(38.836515, -77.310709, 38.872872, -77.229750).setWeighting("fastest").setVehicle("car").setPathDetails(Arrays.asList("average_speed", "edge_id", "time")), rsp);

                    if (!paths.isEmpty()) {
                        Path path0 = paths.get(0);
                        for (EdgeIteratorState edge : path0.calcEdges()) {

                            if (encoder instanceof CarExpFlagEncoder){
                                double speed = ((CarExpFlagEncoder) encoder).getSpeed(edge.getFlags()); //made this method again public in CarExpFlagEncoder
                                //edge.getFlags returns a reference to flags and not a number anymore... thus no need for writing the flags back to datastore.
                                // although double value eventually converted to int...
                                double new_speed = speed-5; //difference has to be at least 5km/h!
                                ((CarExpFlagEncoder) encoder).setSpeed(edge.getFlags(),new_speed);
                                //edge.set(avSpeedEnc, new_speed).setReverse(avSpeedEnc, new_speed/2); //not working
                                // mew method -> https://github.com/graphhopper/graphhopper/pull/1447
                                double mod_speed = ((CarExpFlagEncoder) encoder).getSpeed(edge.getFlags());
                                double mod_speed2 = edge.get(avSpeedEnc);

                           }
                            int edgeId = edge.getEdge();
                            String vInfo = "";
                            //here we get the edge ids from a path -> create an array to
                            if (edge instanceof VirtualEdgeIteratorState) {
                                // first, via and last edges can be virtual
                                VirtualEdgeIteratorState vEdge = (VirtualEdgeIteratorState) edge;
                                edgeId = vEdge.getOriginalEdgeKey() / 2;
                                vInfo = "v";
                            }

                            int ew = edge_occ.get(edgeId) == null ? 0 : edge_occ.get(edgeId);
                            edge_occ.put(edgeId, ew + 1); //increment value https://stackoverflow.com/questions/4157972/how-to-update-a-value-given-a-key-in-a-hashmap

                            try {
                                long osm_way = graphHopper.getOSMWay(edgeId);
                                if (old_osmway != osm_way) {
                                    old_osmway = osm_way; // output edgeId only if it is not the same as previous
                                    String[] segment = {String.valueOf(count), vInfo, String.valueOf(edgeId), String.valueOf(osm_way), data[8]};
                                    route_writer.writeNext(segment);
                                }

                            } catch (Exception e) {
                                String[] segment = {String.valueOf(count), vInfo, String.valueOf(edgeId)};
                                route_writer.writeNext(segment);
                            }
                        }
                    }
                        // do something with the data
                }

                // closing writer connection
                route_writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        csvReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
