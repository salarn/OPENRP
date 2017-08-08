package com.example.salar.openrp;

/**
 * Created by salar on 8/4/17.
 */

public class CacheRequest {
    private String peer_id;
    private long startTime;
    private long finishTime;
    private float value;

    public CacheRequest(){}

    public CacheRequest(String peer_id,long startTime, long finishTime, float value){
        this.peer_id = peer_id;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.value = value;
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
