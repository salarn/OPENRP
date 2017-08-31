package com.example.salar.openrp;

/**
 * Created by salar on 8/30/17.
 */

public class RecommendationTime {
    private String peer_id;
    private long lastRecomTime;

    public RecommendationTime(){}

    public RecommendationTime(String peer_id , long lastRecomTime){
        this.peer_id = peer_id;
        this.lastRecomTime = lastRecomTime;
    }

    public void setPeer_id(String peer_id) {
        this.peer_id = peer_id;
    }

    public void setLastRecomTime(long lastRecomTime) {
        this.lastRecomTime = lastRecomTime;
    }

    public String getPeer_id() {

        return peer_id;
    }

    public long getLastRecomTime() {
        return lastRecomTime;
    }
}
