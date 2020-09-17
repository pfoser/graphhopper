package com.graphhopper.routing.weighting;

import com.carrotsearch.hppc.IntFloatHashMap;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters.Routing;
import java.util.List;

/**
 * Calculates the fastest route with the specified vehicle (VehicleEncoder). Calculates the weight
 * in seconds. Updates the weight for MyImport_Sigma.java
 * <p>
 *
 * @author Tunaggina
 */


public class FastestDynamicWeighting extends AbstractWeighting {

    protected final static double SPEED_CONV = 3.6;
    private final double headingPenalty;
    private final long headingPenaltyMillis;
    private final double maxSpeed;
    private double dynamicEdgePenaltyFactor = 50.0 ; //0.01

    // need to correspond "myWeighedEdges" to "count10" in "MyImport_Sigma"
    // list of the edges that have been traversed.
    private List<Integer> myWeighedEdges = new ArrayList();

    // private List<Integer> getCountLog(){
    //     return null;
    // }

    // I am trying to get the values directly..however, as 'Weighting' class does not have any parameter for that,
    // it will not take this value. To do this, I think we need to add another parameter "customWeight" along with FlagEncoder
    // and PMap. If that parameter is empty= it won't impact anything. if the parameter has the arraylist of edges= then it consider.
    public FastestDynamicWeighting(FlagEncoder encoder, PMap map) {
        super(encoder);
        headingPenalty = map.getDouble(Routing.HEADING_PENALTY, Routing.DEFAULT_HEADING_PENALTY);
        headingPenaltyMillis = Math.round(headingPenalty * 1000);
        maxSpeed = encoder.getMaxSpeed() / SPEED_CONV;
        // this.myWeighedEdges = getCountLog();
        this.myWeighedEdges = getMyWeighedEdges();
    }

    public FastestDynamicWeighting(FlagEncoder encoder) {
        this(encoder, new HintsMap(0));
    }

    private List<Integer> getMyWeighedEdges(){
        return myWeighedEdges;
    }

    public void setMyWeighedEdges(List<Integer> myWeighedEdges) {
        this.myWeighedEdges = myWeighedEdges;
    }

    @Override
    public double getMinWeight(double distance) {
        return distance / maxSpeed;
    }

    @Override
    public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId) {
        double speed = reverse ? edge.getReverse(avSpeedEnc) : edge.get(avSpeedEnc);
        if (speed == 0)
            return Double.POSITIVE_INFINITY;

        double time = edge.getDistance() / speed * SPEED_CONV;

        // add direction penalties at start/stop/via points
        boolean unfavoredEdge = edge.get(EdgeIteratorState.UNFAVORED_EDGE);
        if (unfavoredEdge)
            time += headingPenalty;

        // get the value of the edge
        if (myWeighedEdges.contains(edge.getEdge()))
            time=  time + time * dynamicEdgePenaltyFactor ;

        return time ;
    }


    @Override
    public long calcMillis(EdgeIteratorState edgeState, boolean reverse, int prevOrNextEdgeId) {
        // TODO move this to AbstractWeighting? see #485
        long time = 0;
        boolean unfavoredEdge = edgeState.get(EdgeIteratorState.UNFAVORED_EDGE);
        if (unfavoredEdge)
            time += headingPenaltyMillis;

        return time + super.calcMillis(edgeState, reverse, prevOrNextEdgeId);
    }

    @Override
    public String getName() {
        return "fastestDynamic";
    }

}

    /*
    // i also tried to make it public (like AvoiEdgesWeighting class) but the weighting class does not allow

    public void setMyWeighedEdges(List<Integer> avoidedEdges) {   //just the edge_ids that needed to be avoided
        this.myWeighedEdges = myWeighedEdges;
    }
     */
