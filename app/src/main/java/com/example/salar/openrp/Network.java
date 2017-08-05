package com.example.salar.openrp;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by salar on 8/5/17.
 */

public class Network implements SalutDataCallback{

    public static final String TAG = "NetworkLog";
    public SalutDataReceiver dataReceiver;
    public SalutServiceData serviceData;
    public Salut network;
    public Activity mainActivity;

    public Network(Activity activity){
        /*Create a data receiver object that will bind the callback
        with some instantiated object from our app. */
        this.mainActivity = activity;

       // Toast.makeText(mainActivity.getApplicationContext(),"HELLOOO",Toast.LENGTH_SHORT).show();
        dataReceiver = new SalutDataReceiver(activity, this);

        /*Populate the details for our awesome service. */
        serviceData = new SalutServiceData("OPENRPNetworkService", 60606,
                "Salar");
        /*Create an instance of the Salut class, with all of the necessary data from before.
        * We'll also provide a callback just in case a device doesn't support WiFi Direct, which
        * Salut will tell us about before we start trying to use methods.*/

        network = new Salut(dataReceiver, serviceData, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Sorry, but this device does not support WiFi Direct.");
            }
        });
    }
    public void startAsHost(){
        //Host
        setupNetwork();
    }
    public void startAsClient(){
        //Client
        discoverServices();
    }

    private void setupNetwork()
    {
        if(!network.isRunningAsHost)
        {
            network.startNetworkService(new SalutDeviceCallback() {
                @Override
                public void call(SalutDevice salutDevice) {
                    Log.d(TAG, "Device: " + salutDevice.instanceName + " connected.");
                    Toast.makeText(mainActivity.getApplicationContext(), "Device: " + salutDevice.instanceName + " connected.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else
        {
            network.stopNetworkService(false);
        }
    }

    private void discoverServices()
    {
        if(!network.isRunningAsHost && !network.isDiscovering)
        {
            network.discoverNetworkServices(new SalutCallback()
            {
                @Override
                public void call() {
                    Toast.makeText(mainActivity.getApplicationContext(), "Device: " + network.foundDevices.get(0).instanceName + " found.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Device: " + network.foundDevices.get(0).instanceName + " found.");
                    connectingToDevice(network.foundDevices.get(0));

                }
            }, true);

        }
        else
        {
            network.stopServiceDiscovery(true);
        }
    }

    private void connectingToDevice(final SalutDevice hostDevice){
        network.registerWithHost(hostDevice, new SalutCallback() {
            @Override
            public void call() {
                Toast.makeText(mainActivity.getApplicationContext(), "We're now registered.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "We're now registered.");
                sendSomethingToHost(hostDevice);
            }
        }, new SalutCallback() {
            @Override
            public void call() {
                Toast.makeText(mainActivity.getApplicationContext(), "We failed to register.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "We failed to register.");
            }
        });
    }

    public void sendSomethingToHost(SalutDevice hostDevice){
        Request myMessage = new Request();
        myMessage.description = "See you on the other side!";
        JSONObject a = new JSONObject();
        try {
            a.put("Hi","Salam");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        network.sendToHost(myMessage, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! The data failed to send.");
            }
        });
    }

    /*Create a callback where we will actually process the data.*/
    @Override
    public void onDataReceived(Object data) {
        //Data Is Received
        Log.d(TAG, "Received network data.");

        //Message newMessage = LoganSquare.parse((InputStream) data,Message.class);
        Toast.makeText(mainActivity.getApplicationContext(), "Message Recieved: "+ data, Toast.LENGTH_SHORT).show();
        try {
            JSONObject help = new JSONObject((String) data);
            Log.d(TAG, "Message Detail is: "+ help.get("description"));
        } catch (Throwable t){
            Log.e(TAG ,"Can not creat Json file from data Object.");
        }
        Log.d(TAG, "Message Recieved: "+ data.getClass());  //See you on the other side!
        //Do other stuff with data.
    }
    public void shutdownNetwork(){
        Log.d("END","STOP Host or Client.");
        if(network.isRunningAsHost)
            network.stopNetworkService(false);
        if(network.isDiscovering)
            network.stopServiceDiscovery(false);
    }
}
