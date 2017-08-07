package com.example.salar.openrp;

import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by salar on 8/5/17.
 */

public class Network implements SalutDataCallback{

    public static final String TAG = "NetworkLog";
    public static final String SHAKE_REQUEST = "shake_request";
    public static final String ACC_SHAKE_REQUEST = "accept_shake_request";
    public static final String REJ_SHAKE_REQUEST = "reject_shake_request";
    public static final String TASK_REQUEST = "task_request";
    public static final String ANS_REQUEST = "answer_request";
    public static final String FINISH_REQUEST = "finish_request";
    public static final String OBJECT_TASK = "here_should_be_an_object";

    public SalutDataReceiver dataReceiver;
    public SalutServiceData serviceData;
    public Salut network;
    public MainActivity mainActivity;
    public String androidId;
    public Map<String , String> stateDevices;

    public Network(MainActivity activity){
        /*Create a data receiver object that will bind the callback
        with some instantiated object from our app. */
        this.mainActivity = activity;
        stateDevices = new HashMap<String,String>();

       // Toast.makeText(mainActivity.getApplicationContext(),"HELLOOO",Toast.LENGTH_SHORT).show();
        dataReceiver = new SalutDataReceiver(activity, this);

        androidId = Secure.getString(activity.getApplicationContext().getContentResolver(), Secure.ANDROID_ID).toString();

        Log.d("IDIDID", androidId);

        /*Use Android_id for servive name, can find devices */
        serviceData = new SalutServiceData("OPENRPNetworkService", 60606,
                androidId);

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
                    Log.d(TAG, "Device: " + salutDevice.readableName + " connected.");
                    Toast.makeText(mainActivity.getApplicationContext(), "Device: " + salutDevice.readableName + " connected.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(mainActivity.getApplicationContext(), "Device: " + network.foundDevices.get(0).readableName + " found.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Device: " + network.foundDevices.get(0).readableName + " found.");
                    connectingToDevice(network.foundDevices.get(0));
                }
            }, false);

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
                shakingRequestToHost(hostDevice);

            }
        }, new SalutCallback() {
            @Override
            public void call() {
                Toast.makeText(mainActivity.getApplicationContext(), "We failed to register.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "We failed to register.");
            }
        });
    }

    /*Each request receive this function call.*/
    @Override
    public void onDataReceived(Object data) {
        //Data Is Received
        Log.d(TAG, "A request received.");
        try {
            Request messageReceived = LoganSquare.parse((String) data,Request.class);
            Toast.makeText(mainActivity.getApplicationContext(), "Request Received: "+ messageReceived.requestTitle,
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG , "Parse Message title: "+ messageReceived.requestTitle );
            Log.d(TAG , "Parse Message peer id: "+ messageReceived.requestPeerId);
            Log.d(TAG , "Parse Message detail: "+ messageReceived.requestDetail);
            switch (messageReceived.requestTitle){
                case SHAKE_REQUEST: // As Host
                    answerToShake(messageReceived);
                    break;
                case ACC_SHAKE_REQUEST: // As Client
                    if(stateDevices.containsKey(messageReceived.requestPeerId) == true &&
                            stateDevices.get(messageReceived.requestPeerId).equals(SHAKE_REQUEST)) {
                        changeState(messageReceived.requestPeerId,TASK_REQUEST);
                        sendingTaskRequest(messageReceived);
                    }
                    break;
                case REJ_SHAKE_REQUEST: // As Client
                    if(stateDevices.containsKey(messageReceived.requestPeerId) == true &&
                            stateDevices.get(messageReceived.requestPeerId).equals(SHAKE_REQUEST)) {
                        changeState(messageReceived.requestPeerId,FINISH_REQUEST);
                        evaluateReputationRejectShakeRequest(messageReceived);
                    }
                    break;
                case TASK_REQUEST: // As Host
                    if(stateDevices.containsKey(messageReceived.requestPeerId) == true &&
                            stateDevices.get(messageReceived.requestPeerId).equals(ACC_SHAKE_REQUEST)) {
                        changeState(messageReceived.requestPeerId,FINISH_REQUEST);
                        processTaskRequest(messageReceived);
                    }
                    break;
                case ANS_REQUEST: // As Client
                    if(stateDevices.containsKey(messageReceived.requestPeerId) == true &&
                            stateDevices.get(messageReceived.requestPeerId).equals(TASK_REQUEST)) {
                        changeState(messageReceived.requestPeerId,FINISH_REQUEST);
                        measureAnswerRequest(messageReceived);
                    }
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Toast.makeText(mainActivity.getApplicationContext(), "Message Recieved: "+ data, Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "Message Received: "+ data);

    }

    public void changeState(String deviceId, String to){
        if(stateDevices.containsKey(deviceId))
            stateDevices.remove(deviceId);
        stateDevices.put(deviceId,to);
    }

    public void measureAnswerRequest(Request answerRequest){
        // send response to application and receive feedback
        try {
            JSONObject jsonFromHost = new JSONObject(answerRequest.requestDetail);
            int answerFromHost = (int) jsonFromHost.get("output");
            if(answerFromHost == OBJECT_TASK.length())
                evaluateReputationWithCorrectAnswer(answerRequest);
            else
                evaluteReputationWithWrongAnswer(answerRequest);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
    public void evaluateReputationWithCorrectAnswer(Request answerRequest){
        DatabaseHandler databaseHandler = this.mainActivity.databaseHandler;

        databaseHandler.addCacheRequest(new CacheRequest(answerRequest.requestPeerId,
                new Timestamp(System.currentTimeMillis()), (float)0.9 ));
        //Finish Requests with correct answer.
        Log.d(TAG,"Finish Requests with correct answer.");
        Toast.makeText(mainActivity.getApplicationContext(), "Finish Requests with correct answer.", Toast.LENGTH_SHORT).show();
    }
    public void evaluteReputationWithWrongAnswer(Request answerRequest){
        DatabaseHandler databaseHandler = this.mainActivity.databaseHandler;

        databaseHandler.addCacheRequest(new CacheRequest(answerRequest.requestPeerId,
                new Timestamp(System.currentTimeMillis()), (float)0.3 ));
        //Finish Requests with wrong answer.
        Log.d(TAG,"Finish Requests with wrong answer.");
        Toast.makeText(mainActivity.getApplicationContext(), "Finish Requests with wrong answer.", Toast.LENGTH_SHORT).show();
    }

    public void processTaskRequest(Request taskRequest){
        //Process task with sending it to Application and receiving answer.
        SalutDevice clientDevice = findDeviceWithId(taskRequest.requestPeerId);
        if(clientDevice == null){
            Log.e(TAG,"Can't find Client, And then can't send answer request for task.");
            return;
        }
        Request answerRequest = new Request();
        answerRequest.requestTitle = ANS_REQUEST;
        answerRequest.requestPeerId = this.androidId;

        JSONObject jsonDetail = new JSONObject();
        try {
            //Our process
            JSONObject jsonProcess = new JSONObject(taskRequest.requestDetail);
            int answer = (jsonProcess.get("object")).toString().length();
            //Make our answer request
            jsonDetail.put("nid",this.androidId);
            jsonDetail.put("tid","1");
            jsonDetail.put("aid","2");
            jsonDetail.put("output",answer); //Our answer
            answerRequest.requestDetail = jsonDetail.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        network.sendToDevice(clientDevice, answerRequest, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Answer request failed to send.");
            }
        });
    }

    public void evaluateReputationRejectShakeRequest(Request rejectRequest){
        //evaluate reputation with reason of reject
        DatabaseHandler databaseHandler = this.mainActivity.databaseHandler;

        databaseHandler.addCacheRequest(new CacheRequest(rejectRequest.requestPeerId,
                new Timestamp(System.currentTimeMillis()), (float)0.1 ));
        //Finish Requests with reject shaking.
        Log.d(TAG,"Finish Requests with reject shaking.");
        Toast.makeText(mainActivity.getApplicationContext(), "Finish Requests with reject shaking.", Toast.LENGTH_SHORT).show();

    }

    public void sendingTaskRequest(Request acceptRequest){
        Request taskRequest = new Request(); // our task is counting object's(String) length.
        taskRequest.requestTitle = TASK_REQUEST;
        taskRequest.requestPeerId = this.androidId;
        JSONObject jsonDetail = new JSONObject();
        try {
            jsonDetail.put("nid",this.androidId);
            jsonDetail.put("tid","1");
            jsonDetail.put("aid","2");
            jsonDetail.put("object",OBJECT_TASK);
            jsonDetail.put("input","Some input");
            taskRequest.requestDetail = jsonDetail.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        network.sendToHost(taskRequest, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Task request failed to send.");
            }
        });
    }

    public void shakingRequestToHost(SalutDevice hostDevice){

        changeState(hostDevice.readableName,SHAKE_REQUEST);

        Request shakeRequest = new Request();
        shakeRequest.requestTitle = SHAKE_REQUEST;
        shakeRequest.requestPeerId = this.androidId;

        JSONObject jsonDetail = new JSONObject();
        try {
            jsonDetail.put("nid",this.androidId);
            jsonDetail.put("tid","1");
            jsonDetail.put("aid","2");
            jsonDetail.put("weight","3");
            jsonDetail.put("deadline","4");
            jsonDetail.put("dur","5");
            jsonDetail.put("cpu","6");
            jsonDetail.put("input_size","7");
            jsonDetail.put("output_size","8");
            shakeRequest.requestDetail = jsonDetail.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        network.sendToHost(shakeRequest, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Shake request failed to send.");
            }

        });

    }

    //Decide to accept or reject shake request here
    public void answerToShake(Request shakeRequest){
        if(true) {
            changeState(shakeRequest.requestPeerId,ACC_SHAKE_REQUEST);
            sendAcceptShakeRequest(shakeRequest);
        }
        else {
            changeState(shakeRequest.requestPeerId,REJ_SHAKE_REQUEST);
            sendRejectShakeRequest(shakeRequest);
        }
    }


    public void sendAcceptShakeRequest(Request shakeRequest){
        //Log.d(TAG,"Testing: "+shakeRequest.requestPeerId +" "+ shakeRequest.requestPeerId.getClass());
        SalutDevice clientDevice = findDeviceWithId(shakeRequest.requestPeerId);
        if(clientDevice == null){
            Log.e(TAG,"Can't find Client, And then can't send accept request for shake.");
            return;
        }

        Request acceptShakeRequest = new Request();
        acceptShakeRequest.requestTitle = ACC_SHAKE_REQUEST;
        acceptShakeRequest.requestPeerId = this.androidId;

        JSONObject jsonDetail = new JSONObject();
        try {
            jsonDetail.put("nid",this.androidId);
            jsonDetail.put("tid","1");
            jsonDetail.put("aid","2");
            JSONObject ans = new JSONObject();
            ans.put("title","YES");
            jsonDetail.put("ans",ans);
            acceptShakeRequest.requestDetail = jsonDetail.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        network.sendToDevice(clientDevice, acceptShakeRequest, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Accept shake request failed to send.");
            }
        });
    }

    public void sendRejectShakeRequest(Request shakeRequest){
        SalutDevice clientDevice = findDeviceWithId(shakeRequest.requestPeerId);
        if(clientDevice == null){
            Log.e(TAG,"Can't find Client, And then can't send reject request for shake.");
            return;
        }

        Request rejectShakeRequest = new Request();
        rejectShakeRequest.requestTitle = REJ_SHAKE_REQUEST;
        rejectShakeRequest.requestPeerId = this.androidId;

        JSONObject jsonDetail = new JSONObject();
        try {
            jsonDetail.put("nid",this.androidId);
            jsonDetail.put("tid","1");
            jsonDetail.put("aid","2");
            JSONObject ans = new JSONObject();
            ans.put("title","NO");
            ans.put("reason","Here should be a reason");
            jsonDetail.put("ans",ans);
            rejectShakeRequest.requestDetail = jsonDetail.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        network.sendToDevice(clientDevice, rejectShakeRequest, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Reject shake request failed to send.");
            }
        });
    }
    public SalutDevice findDeviceWithId(String peerId){
        if(network.isRunningAsHost) {
            Iterator i$ = network.registeredClients.iterator();
            //Log.d(TAG,"Finding this Device: "+peerId);
            while(i$.hasNext()) {
                SalutDevice registered = (SalutDevice)i$.next();
                if(registered.readableName.equals(peerId))
                    return registered;
            }
        } else {
            Log.e(TAG ,"This device is not the host and therefore cannot search in client.");
        }
        return null;
    }
    public void shutdownNetwork(){
        Log.d("END","STOP Host or Client.");
        stateDevices.clear();
        if(network.isRunningAsHost)
            network.stopNetworkService(false);
        if(network.isDiscovering)
            network.stopServiceDiscovery(false);
    }
}
