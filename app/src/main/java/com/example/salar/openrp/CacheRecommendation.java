package com.example.salar.openrp;

/**
 * Created by salar on 8/9/17.
 */

public class CacheRecommendation {
    private String from_peer_id;
    private String peer_id;
    private long startTime;
    private long finishTime;
    private float value;

    public CacheRecommendation(){}

    public CacheRecommendation(String from_peer_id, String peer_id,long startTime, long finishTime, float value){
        this.from_peer_id = from_peer_id;
        this.peer_id = peer_id;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.value = value;
    }

    public void setFrom_peer_id(String from_peer_id){
        this.from_peer_id = from_peer_id;
    }

    public void setPeer_id(String peer_id) {
        this.peer_id = peer_id;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getFrom_peer_id(){
        return this.from_peer_id;
    }

    public String getPeer_id() {
        return peer_id;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime(){
        return finishTime;
    }

    public float getValue() {
        return value;
    }
}
