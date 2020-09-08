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
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;


/**
 * Start with the following command line settings:
 *
 * config=config.properties osmreader.osm=area.pbf
 *
 * @author Peter Karich
 */
public class MyImport_dataUpdater {
//    private DataUpdaterSigmaTest updater;

    public static void main(String[] args) {
        //just to log all the traversed edges
        List<Integer> usedEdges = new ArrayList<Integer>();
        //list of edges that need to block : testing
        List<Integer> avoidEdges = new ArrayList<Integer>(Arrays.asList(917757, 382491, 401432, 472388, 920976, 785218,
                369751,369749, 472384, 775624,762019,125371,129355,775627,474159,474437));

        //trying to use 'feed' method to change the speed of the edges.
        DataUpdaterSigmaTest updater = null;
        updater.feedEntry((RoadEntrySigmaTest) avoidEdges);

        MyGraphHopper graphHopper = new MyGraphHopper();
        graphHopper.setDataReaderFile("./data/dc-baltimore_maryland.osm.pbf");
        // where to store graphhopper files?
        graphHopper.setGraphHopperLocation("./data/dc-baltimore_maryland.osm-gh");
        graphHopper.setEncodingManager(EncodingManager.create("car_exp"));
        graphHopper.setCHEnabled(false);
        graphHopper.importOrLoad();

        Logger logger = LoggerFactory.getLogger(MyImport_dataUpdater.class);
        logger.info("avoidEdges" + avoidEdges);

        //read OD file, contains two coordinate pairs
//        try (BufferedReader csvReader = new BufferedReader(new FileReader("./data/od_coords_3ways.csv"))) {
        try (BufferedReader csvReader = new BufferedReader(new FileReader("./data/test1.csv"))) {

            File route_file = new File("./data/test_result2.csv"); // output file for routes
            try {
                // create CSVWriter object filewriter object as parameter
                CSVWriter route_writer = new CSVWriter(new FileWriter(route_file));

                // adding header to csv
                String[] out_header = { "i_id", "osm_id", "count" };
                route_writer.writeNext(out_header);

                String header = csvReader.readLine();
                String row = null;
                int count = 0; //shortest path count
                while ((row = csvReader.readLine()) != null) {
                    count++; System.out.println(count);
                    if (count%10000 == 0) System.out.println(count); // route count -> include also in output as first attribute

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
                            int edgeId = edge.getEdge();
                            String vInfo = "";
                            if (edge instanceof VirtualEdgeIteratorState) {
                                // first, via and last edges can be virtual
                                VirtualEdgeIteratorState vEdge = (VirtualEdgeIteratorState) edge;
                                edgeId = vEdge.getOriginalEdgeKey() / 2;
                                vInfo = "v";
                            }

                            usedEdges.add(edgeId);
//                            logger.info("edgeid: "+ edgeId);

                            try {
                                long osm_way = graphHopper.getOSMWay(edgeId);
                                if (old_osmway != osm_way) {
                                    old_osmway = osm_way; // output edgeId only if it is not the same as previous
//                                    String[] segment = {String.valueOf(count), vInfo, String.valueOf(edgeId), String.valueOf(osm_way), data[8]};
                                    String[] segment = {String.valueOf(edgeId),String.valueOf(osm_way), data[8]};

                                    route_writer.writeNext(segment);
                                }

                            } catch (Exception e) {
//                                String[] segment = {String.valueOf(count), vInfo, String.valueOf(edgeId)};
                                String[] segment = {""};

                                route_writer.writeNext(segment);
                            }
                        }
                    }
                    // do something with the data
                }
                logger.info(String.valueOf(usedEdges));
                // closing writer connection
                route_writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
