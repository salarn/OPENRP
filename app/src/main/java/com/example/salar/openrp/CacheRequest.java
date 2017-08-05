package com.example.salar.openrp;

import java.sql.Timestamp;

/**
 * Created by salar on 8/4/17.
 */

public class CacheRequest {
    private int peer_id;
    private Timestamp time;
    private float value;

    public CacheRequest(){}

    public CacheRequest(int peer_id, Timestamp time, float value){
        this.peer_id = peer_id;
        this.time = time;
        this.value = value;
    }

    public void setPeer_id(int peer_id) {
        this.peer_id = peer_id;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getPeer_id() {

        return peer_id;
    }

    public Timestamp getTime() {
        return time;
    }

    public float getValue() {
        return value;
    }

}
