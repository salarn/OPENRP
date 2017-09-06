package com.example.salar.openrp;

import android.provider.Settings.Secure;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.example.salar.openrp.ui.DeviceListFragment;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

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
    public static final String GIVE_ME_RECOMMENDATION_REQUEST = "give_me_recommendation_request";
    public static final String MY_RECOMMENDATION_REQUEST = "my_recommendation_request";
    public static final String OBJECT_TASK = "here_should_be_an_object";

    public SalutDataReceiver dataReceiver;
    public SalutServiceData serviceData;
    public Salut network = null;
    public MainActivity mainActivity;
    public String androidId;
    public Map<Pair<String, Long>, String> stateDevices;
    public Pair<String,Long> pair;
    public Vector<Timer> timerList;

    public Network(MainActivity activity){
        /*Create a data receiver object that will bind the callback
        with some instantiated object from our app. */
        this.mainActivity = activity;
        stateDevices = new HashMap<Pair<String,Long>,String>();
        timerList = new Vector<Timer>();

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
        //discoverServices();
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

    public void discoverServices()
    {
        if(!network.isRunningAsHost && !network.isDiscovering)
        {
            network.discoverWithTimeout(new SalutCallback()
            {
                @Override
                public void call() {
                    Toast.makeText(mainActivity.getApplicationContext(), network.foundDevices.size() + " Devices found.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, network.foundDevices.size() + " Devices found.");
                    final DeviceListFragment fragment = (DeviceListFragment) mainActivity.getFragmentManager().
                            findFragmentById(R.id.frag_list);
                    fragment.onPeersAvailable(network.foundDevices);
                    //connectingToDevice(network.foundDevices.get(0));
                }
            }, new SalutCallback() {
                @Override
                public void call() {
                    Toast.makeText(mainActivity.getApplicationContext(), "We didn't find any server.", Toast.LENGTH_SHORT).show();
                    final DeviceListFragment fragment = (DeviceListFragment) mainActivity.getFragmentManager().
                            findFragmentById(R.id.frag_list);
                    fragment.onPeersAvailable(network.foundDevices);
                    Log.d(TAG, "We didn't find any server.");
                }
            }, 2000);

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
                giveMeRecommendations(hostDevice,
                        mainActivity.databaseHandler.getLastRecommendationTime(hostDevice.readableName).getLastRecomTime());
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
            Log.d(TAG , "Parse Message start time: "+ messageReceived.requestStartTime);
            pair = Pair.create(messageReceived.requestPeerId,messageReceived.requestStartTime);
            switch (messageReceived.requestTitle){
                case SHAKE_REQUEST: // As Host
                    answerToShake(messageReceived);
                    break;
                case ACC_SHAKE_REQUEST: // As Client
                    if(stateDevices.containsKey(pair) &&
                            stateDevices.get(pair).equals(SHAKE_REQUEST)) {
                        changeState(pair,TASK_REQUEST);
                        sendingTaskRequest(messageReceived);
                    }
                    break;
                case REJ_SHAKE_REQUEST: // As Client
                    if(stateDevices.containsKey(pair)  &&
                            stateDevices.get(pair).equals(SHAKE_REQUEST)) {
                        changeState(pair,FINISH_REQUEST);
                        evaluateReputationRejectShakeRequest(messageReceived);
                    }
                    break;
                case TASK_REQUEST: // As Host
                    if(stateDevices.containsKey(pair) &&
                            stateDevices.get(pair).equals(ACC_SHAKE_REQUEST)) {
                        changeState(pair,FINISH_REQUEST);
                        processTaskRequest(messageReceived);
                    }
                    break;
                case ANS_REQUEST: // As Client
                    if(stateDevices.containsKey(pair) &&
                            stateDevices.get(pair).equals(TASK_REQUEST)) {
                        changeState(pair,FINISH_REQUEST);
                        measureAnswerRequest(messageReceived);
                    }
                    break;
                case GIVE_ME_RECOMMENDATION_REQUEST: // As Host
                    sendRecommendation(messageReceived);
                    break;
                case MY_RECOMMENDATION_REQUEST: // As Client
                    processRecommendation(messageReceived);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Toast.makeText(mainActivity.getApplicationContext(), "Message Recieved: "+ data, Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "Message Received: "+ data);

    }

    public void changeState(Pair<String,Long> key, String to){
        if(stateDevices.containsKey(key))
            stateDevices.remove(key);
        stateDevices.put(key,to);
    }

    public void measureAnswerRequest(Request answerRequest){
        // send response to application and receive feedback
        try {
            JSONObject jsonFromHost = new JSONObject(answerRequest.requestDetail);
            int answerFromHost = (int) jsonFromHost.get("output");
            if(answerFromHost == OBJECT_TASK.length())
                evaluateReputationWithCorrectAnswer(answerRequest);
            else
                evaluateReputationWithWrongAnswer(answerRequest);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
    public void evaluateReputationWithCorrectAnswer(Request answerRequest){
        DatabaseHandler databaseHandler = this.mainActivity.databaseHandler;

        databaseHandler.addCacheRequest(new CacheRequest(answerRequest.requestPeerId,
                answerRequest.requestStartTime, System.currentTimeMillis(), (float)0.9 ));
        //Finish Requests with correct answer.
        Log.d(TAG,"Finish Requests with correct answer.");
        Toast.makeText(mainActivity.getApplicationContext(), "Finish Requests with correct answer.", Toast.LENGTH_SHORT).show();
    }
    public void evaluateReputationWithWrongAnswer(Request answerRequest){
        DatabaseHandler databaseHandler = this.mainActivity.databaseHandler;

        databaseHandler.addCacheRequest(new CacheRequest(answerRequest.requestPeerId,
                answerRequest.requestStartTime, System.currentTimeMillis(), (float)0.3 ));
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
        answerRequest.requestStartTime = taskRequest.requestStartTime;

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
                rejectRequest.requestStartTime, System.currentTimeMillis(), (float)0.1 ));
        //Finish Requests with reject shaking.
        Log.d(TAG,"Finish Requests with reject shaking.");
        Toast.makeText(mainActivity.getApplicationContext(), "Finish Requests with reject shaking.", Toast.LENGTH_SHORT).show();

    }

    public void sendingTaskRequest(Request acceptRequest){
        Request taskRequest = new Request(); // our task is counting object's(String) length.
        taskRequest.requestTitle = TASK_REQUEST;
        taskRequest.requestPeerId = this.androidId;
        taskRequest.requestStartTime = acceptRequest.requestStartTime;
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

        long currentTime = System.currentTimeMillis();
        pair = Pair.create(hostDevice.readableName,currentTime);
        changeState(pair,SHAKE_REQUEST);
        long expectedDurationTime = 10000;
        startDeadlineTimer(expectedDurationTime,currentTime,hostDevice.readableName);

        Request shakeRequest = new Request();
        shakeRequest.requestTitle = SHAKE_REQUEST;
        shakeRequest.requestPeerId = this.androidId;
        shakeRequest.requestStartTime = currentTime;

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

    public void startDeadlineTimer(long expectedDurationTime,final long currentTime,final String hostId){

        Timer timer = new Timer();
        timerList.add(timer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // this code will be executed after 3 seconds
                if(checkFindingDataWithStartTime(currentTime,hostId)){
                    Log.d(TAG,"Task data was stored successfully, And was checked at deadline moment.");
                }
                else{
                    Log.d(TAG,"Task not finished yet, And deadline time ended.");
                    pair = Pair.create(hostId,currentTime);
                    changeState(pair,FINISH_REQUEST);
                }
            }
        }, expectedDurationTime);

    }

    public boolean checkFindingDataWithStartTime(long startTime, String hostId){
        CacheRequest cacheRequest = this.mainActivity.databaseHandler.getCacheRequestWithStartTime(startTime);
        if(cacheRequest == null)
            return false;
        if(cacheRequest.getPeer_id().equals(hostId))
            return true;
        return false;
    }

    //Decide to accept or reject shake request here
    public void answerToShake(Request shakeRequest){
        pair = Pair.create(shakeRequest.requestPeerId,shakeRequest.requestStartTime);
        if(true) {
            changeState(pair,ACC_SHAKE_REQUEST);
            sendAcceptShakeRequest(shakeRequest);
        }
        else {
            changeState(pair,REJ_SHAKE_REQUEST);
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
        acceptShakeRequest.requestStartTime = shakeRequest.requestStartTime;

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
        rejectShakeRequest.requestStartTime = shakeRequest.requestStartTime;

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
    // Ask host to give us his recommendations
    public void giveMeRecommendations(SalutDevice hostDevice, long fromTime){
        Request giveRecommendation = new Request();
        giveRecommendation.requestTitle = GIVE_ME_RECOMMENDATION_REQUEST;
        giveRecommendation.requestPeerId = this.androidId;
        giveRecommendation.requestStartTime = fromTime;
        giveRecommendation.requestDetail = "";

        network.sendToHost(giveRecommendation, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Applying for recommend data request failed to send.");
            }
        });
    }

    public void processRecommendation(Request recommendationRequest){
        try {
            JSONArray recommendationJson = new JSONArray(recommendationRequest.requestDetail);
            for(int i = 0;i < recommendationJson.length(); i++){
                CacheRecommendation cacheRecommendation = new CacheRecommendation();
                cacheRecommendation.setFrom_peer_id(recommendationRequest.requestPeerId);
                cacheRecommendation.setPeer_id((String) ((JSONObject)recommendationJson.get(i)).get(
                        DatabaseHandler.KEY_PEER_ID
                ));
                cacheRecommendation.setStartTime(Long.valueOf((String)(((JSONObject)recommendationJson.get(i)).get(
                        DatabaseHandler.KEY_START_TIME
                ))));
                cacheRecommendation.setFinishTime(Long.valueOf((String)(((JSONObject)recommendationJson.get(i)).get(
                        DatabaseHandler.KEY_FINISH_TIME
                ))));
                cacheRecommendation.setValue(Float.valueOf((String)( ((JSONObject)recommendationJson.get(i)).get(
                        DatabaseHandler.KEY_VALUE
                ))));
                this.mainActivity.databaseHandler.addCacheRecommendation(cacheRecommendation);
            }
        }
        catch (JSONException e){
            e.printStackTrace();
        }
        changeLastRecommendationTime(recommendationRequest.requestPeerId,recommendationRequest.requestStartTime);
    }

    public void changeLastRecommendationTime(String deviceId, long to){
        RecommendationTime rt = new RecommendationTime(deviceId,to);
        mainActivity.databaseHandler.addRecommendationTime(rt);
    }

    // Sending our Recommendation to client

    public void sendRecommendation(Request recommendationRequest){

        SalutDevice clientDevice = findDeviceWithId(recommendationRequest.requestPeerId);
        if(clientDevice == null){
            Log.e(TAG,"Can't find Client, And then can't send our recommendation data.");
            return;
        }

        Request ourRecommendation = new Request();
        ourRecommendation.requestTitle = MY_RECOMMENDATION_REQUEST;
        ourRecommendation.requestPeerId = this.androidId;
        ourRecommendation.requestStartTime = System.currentTimeMillis();
        ourRecommendation.requestDetail = this.mainActivity.databaseHandler.convertDataTableToJson(
                recommendationRequest.requestStartTime);

        network.sendToDevice(clientDevice, ourRecommendation, new SalutCallback() {
            @Override
            public void call() {
                Log.e(TAG, "Oh no! Recommendation data failed to send.");
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

    public void cancelAllTimers(){
        for(int i = 0;i < timerList.size(); i++)
            timerList.get(i).cancel();
    }

    public void shutdownNetwork(){
        Log.d("END","STOP Host or Client.");
        stateDevices.clear();
        cancelAllTimers();
        timerList.clear();
        if(network.isRunningAsHost)
            network.stopNetworkService(false);
        if(network.isDiscovering)
            network.stopServiceDiscovery(false);
    }
}
