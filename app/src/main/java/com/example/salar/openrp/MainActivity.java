package com.example.salar.openrp;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.salar.openrp.ui.DeviceListFragment;
import com.peak.salut.Callbacks.SalutCallback;

import java.util.List;

public class MainActivity extends AppCompatActivity{

    public Network network;
    public boolean isHost = true;
    public DatabaseHandler databaseHandler;
    public Button button;
    public DeviceListFragment fragmentList;
    public long lastServerSearchTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.startNetwork();

        /////////// UI

        setThisDeviceInfo();

        button = (Button) findViewById(R.id.btn_switch);
        setButtonText();
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                network.shutdownNetwork();
                changeIsHost();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startNetwork();
                        setButtonText();
                        setThisDeviceInfo();
                        updateFragmentList();
                    }
                }, 500);
            }
        });

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

        Log.d("Reading: ", "Reading all RecommendationTime..");
        List<RecommendationTime> recommendationTimes = databaseHandler.getAllRecommendationTimes();

        for (RecommendationTime cn : recommendationTimes) {
            String log = "Peer ID: "+cn.getPeer_id()+" ,LastRecom Time: " + cn.getLastRecomTime() ;
            // Writing CacheRequest to log
            Log.d("OthersData", log);
        }
        ///////////////// DATABASE
    }

    public void startNetwork(){
        network = new Network(this);
        if(isHost)
            network.startAsHost();
        else
            network.startAsClient();
    }
    public void changeIsHost(){
        isHost = !isHost;
    }
    public void setButtonText(){
        button = (Button) findViewById(R.id.btn_switch);
        button.setText(isHost?"Change to Client":"Change to Server");
    }
    public void setThisDeviceInfo(){
        fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        if(fragmentList != null && network.network != null)
            fragmentList.updateThisDevice(network.network.thisDevice,isHost);
    }
    public void updateFragmentList(){
        fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        if(fragmentList != null && network.network != null)
            fragmentList.clearPeers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }
    /**
     * Peer discover button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_discover:
                discoverServers();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public boolean discoverServers(){
        if (!network.network.isWiFiEnabled(getApplicationContext())) {
            // If p2p not enabled try to connect as a legacy device
            Toast.makeText(getApplicationContext(), R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (isHost){
            Toast.makeText(getApplicationContext(),"Your device is Server, change it to Client.", Toast.LENGTH_SHORT).show();
            return true;
        }
        network.network.unregisterClient(new SalutCallback() {
            @Override
            public void call() {
                Log.d("UNREG","YES");
                lastServerSearchTime = System.currentTimeMillis();
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(
                        R.id.frag_list);
                fragment.onInitiateDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery Initiated", Toast.LENGTH_SHORT).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        network.discoverServices();
                    }
                }, 2000);
            }
        } , new SalutCallback() {
            @Override
            public void call() {
                Log.d("UNREG","NO");
            }
        }, false);
        return true;
    }
    @Override
    public void onRestart(){
        super.onRestart();
        this.startNetwork();
    }
    @Override
    public void onStop() {
        super.onStop();
        network.shutdownNetwork();
    }
}
