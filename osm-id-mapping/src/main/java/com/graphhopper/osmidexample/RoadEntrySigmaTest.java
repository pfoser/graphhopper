package com.graphhopper.osmidexample;

import java.util.List;

/**
 *
 * @author Tunaggina
 */
public class RoadEntrySigmaTest {

    private List<Integer> myAvoidedEdges;

    public RoadEntrySigmaTest() {
    }

    public RoadEntrySigmaTest(List<Integer> myAvoidedEdges) {
        this.myAvoidedEdges = myAvoidedEdges;
    }

    public List<Integer> getMyAvoidedEdges() {
        return myAvoidedEdges;
    }

    public void setMyAvoidedEdges(List<Integer> myAvoidedEdges) {
        this.myAvoidedEdges = myAvoidedEdges;
    }


    @Override
    public String toString() {
        return "myAvoidedEdges:" + myAvoidedEdges;
    }
}
