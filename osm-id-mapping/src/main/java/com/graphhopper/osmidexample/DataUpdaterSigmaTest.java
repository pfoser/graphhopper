package com.graphhopper.osmidexample;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tunaggina
 */

public class DataUpdaterSigmaTest {

    private MyGraphHopper hopper;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Lock writeLock;
    private RoadEntrySigmaTest currentRoads = new RoadEntrySigmaTest();

//    private ArrayList<RoadEntrySigma> currentRoads = new RoadEntrySigma();
//    private ArrayList<Integer> myEdgeIds = new ArrayList<Integer>();  //the list of our edge-ids need to be avoided.
//    IntSet myEdgeIds = new GHIntHashSet();
//    public ArrayList<Integer> getMyEdgeIds(){
//        return myEdgeIds;
//    }
//    public void setMyEdgeIds(ArrayList<Integer> myEdgeIds) {
//        this.myEdgeIds = myEdgeIds;}

    public DataUpdaterSigmaTest(Lock writeLock) {
        this.writeLock = writeLock;
    }
    // when using only RoadEntry
    public void feedEntry(RoadEntrySigmaTest data) {
        writeLock.lock();
        try {
            lockedFeedEntry((List<Integer>) data);
        } finally {
            writeLock.unlock();
        }
    }

    public void lockedFeedEntry(List<Integer> data) {
        currentRoads.setMyAvoidedEdges(data);
        logger.info(String.format("currentroads: %s", currentRoads));
        Graph graph = hopper.getGraphHopperStorage();

        FlagEncoder carEncoder = hopper.getEncodingManager().getEncoder("car_exp");

        int errors = 0;
        int updates = 0;

        // change speed for each edge on the graph.
        List<Integer> myIds = currentRoads.getMyAvoidedEdges();
        for(int i=0 ; i< myIds.size(); ++i) {
            int edgeId = myIds.get(i);
            EdgeIteratorState edge = graph.getEdgeIteratorState(edgeId, Integer.MIN_VALUE);
            // FlagEncoder does not have 'getSpeed' and 'setSpeed' method anymore.. AbstractFlagEncoder has these methods.
            // probably need to use AbstractFlagEncoder
            double oldSpeed = carEncoder.getSpeed(edge.getFlags());
//            double oldSpeed = carEncoder.getMaxSpeed();
            double newValue = oldSpeed * 0.5;
            edge.setFlags(carEncoder.setSpeed(edge.getFlags(), newValue));
            logger.info(("Speed change at ") + edgeId + (" Old: ") + oldSpeed + (", new:"));
            }

        }
    }
