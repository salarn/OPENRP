package com.example.salar.openrp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import java.util.List;

public class MainActivity extends AppCompatActivity{

    public Network network;
    public boolean isHost = false;
    public DatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        network = new Network(this);
        if(this.isHost)
            network.startAsHost();
        else
            network.startAsClient();

        /////////// DATABASE

        databaseHandler = new DatabaseHandler(this);
        /*
        // Inserting CacheRequest

        Log.d("Insert: ", "Inserting ..");

        //Timestamp a = new Timestamp(2);

        databaseHandler.addCacheRequest(new CacheRequest(12,new Timestamp(System.currentTimeMillis()), (float) 0.53));
        databaseHandler.addCacheRequest(new CacheRequest(26,new Timestamp(System.currentTimeMillis()+2000), (float) 0.94));
        databaseHandler.addCacheRequest(new CacheRequest(45,new Timestamp(System.currentTimeMillis()+6000), (float) 0.08));
        */
        // Reading all CacheRequests
        Log.d("Reading: ", "Reading all CacheRequest..");
        List<CacheRequest> cacheRequests = databaseHandler.getAllCacheRequests();

        for (CacheRequest cn : cacheRequests) {
            String log = "Peer ID: "+cn.getPeer_id()+" ,Start Time: " + cn.getStartTime() +
                    " ,Finish Time: "+ cn.getFinishTime() +" ,Value: " + cn.getValue();
            // Writing CacheRequest to log
            Log.d("OwnData", log);
        }

        Log.d("Reading: ", "Reading all CacheRecommendation..");
        List<CacheRecommendation> cacheRecommendations = databaseHandler.getAllCacheRecommendation();

        for (CacheRecommendation cn : cacheRecommendations) {
            String log = "From_Peer_ID: "+ cn.getFrom_peer_id() +" ,Peer ID: "+cn.getPeer_id()+" ,Start Time: " + cn.getStartTime() +
                    " ,Finish Time: "+ cn.getFinishTime() +" ,Value: " + cn.getValue();
            // Writing CacheRequest to log
            Log.d("OthersData", log);
        }
        ///////////////// DATABASE
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_base, menu);
        return true;
    }
    @Override
    public void onRestart(){
        super.onRestart();
        network = new Network(this);
        if(this.isHost)
            network.startAsHost();
        else
            network.startAsClient();
    }
    @Override
    public void onStop() {
        super.onStop();
        network.shutdownNetwork();
    }
}
