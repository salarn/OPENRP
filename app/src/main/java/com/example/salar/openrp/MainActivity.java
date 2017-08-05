package com.example.salar.openrp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

public class MainActivity extends AppCompatActivity{

    public Network network;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        network = new Network(this);
        //network.startAsHost();
        network.startAsClient();



        /*
        /////////// DATABASE
        DatabaseHandler db = new DatabaseHandler(this);

        // Inserting CacheRequest

        Log.d("Insert: ", "Inserting ..");

        //Timestamp a = new Timestamp(2);

        db.addCacheRequest(new CacheRequest(12,new Timestamp(System.currentTimeMillis()), (float) 0.53));
        db.addCacheRequest(new CacheRequest(26,new Timestamp(System.currentTimeMillis()+2000), (float) 0.94));
        db.addCacheRequest(new CacheRequest(45,new Timestamp(System.currentTimeMillis()+6000), (float) 0.08));

        // Reading all CacheRequests
        Log.d("Reading: ", "Reading all CacheRequest..");
        List<CacheRequest> cacheRequests = db.getAllCacheRequests();

        for (CacheRequest cn : cacheRequests) {
            String log = "Peer_ID: "+cn.getPeer_id()+" ,Time: " + cn.getTime() + " ,Value: " + cn.getValue();
            // Writing CacheRequest to log
            Log.d("Data: ", log);
        }
        ///////////////// DATABASE
        */
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_base, menu);
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        network.shutdownNetwork();
    }
}
