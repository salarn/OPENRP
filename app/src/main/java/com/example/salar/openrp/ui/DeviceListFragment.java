package com.example.salar.openrp.ui;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.salar.openrp.MainActivity;
import com.example.salar.openrp.R;
import com.peak.salut.SalutDevice;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by salar on 9/3/17.
 */

public class DeviceListFragment extends ListFragment {

    public static final String TAG = "DeviceList";
    View mContentView = null;
    SalutDevice device;
    private List<SalutDevice> peers = new ArrayList<SalutDevice>();
    ProgressDialog progressDialog = null;

    /**
     * Once the activity is created make sure that an adapter is fit to the fragment to update on finding new peers
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));
    }

    /**
     * Inflate the devices list view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    public void updateThisDevice(SalutDevice device, boolean isHost) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText("My Device name: "+device.readableName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(isHost?"Server":"Client");
    }

    // Show all founded device detail Here.

    private class WiFiPeerListAdapter extends ArrayAdapter<SalutDevice> {

        private List<SalutDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId, List<SalutDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            final SalutDevice device = items.get(position);
            if (device != null) {
                //// Render Devices Info
                TextView name = (TextView) v.findViewById(R.id.device_name);
                TextView status = (TextView) v.findViewById(R.id.device_status);
                TextView ownReputation = (TextView) v.findViewById(R.id.device_own_reputation);
                TextView recommendReputation = (TextView) v.findViewById(R.id.device_recommend_reputation);
                TextView numberOfInteractions = (TextView) v.findViewById(R.id.device_number_of_interactions);
                TextView lastTime = (TextView) v.findViewById(R.id.device_last_time_interaction);
                TextView averageTime = (TextView) v.findViewById(R.id.device_average_time);
                if (name != null) {
                    name.setText("Name: "+device.readableName);
                }
                if (status != null) {
                    status.setText("Status: "+getDeviceStatus(device));
                }
                if(ownReputation != null){
                    ownReputation.setText("Own Reputation: "+getDeviceOwnReputation(device));
                }
                if(recommendReputation != null){
                    recommendReputation.setText("Recommend Reputation: "+getDeviceRecommendReputation(device));
                }
                if(numberOfInteractions != null){
                    numberOfInteractions.setText("Number of past interactions: "+getNumberOfInteractions(device));
                }
                if(lastTime != null){
                    lastTime.setText("Last interaction time: "+getLastTimeInteraction(device));
                }
                if(averageTime != null){
                    averageTime.setText("Average interaction time: "+getAverageTime(device));
                }
                //// Render Devices Button

                Button regButton = (Button) v.findViewById(R.id.btn_register);
                regButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).network.
                                connectingToDevice(device);
                    }
                });
                Button unregButton = (Button) v.findViewById(R.id.btn_unregister);
                unregButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ((MainActivity)getActivity()).discoverServers();
                    }
                });
            }
            return v;
        }
        public String getDeviceStatus(SalutDevice device){
            if(device.isRegistered == true)
                return "Connected";
            return  "Available";
        }
        public String getDeviceOwnReputation(SalutDevice device){
            long lastRefreshTime = ((MainActivity)getActivity()).lastServerSearchTime;
            float reputation = ((MainActivity)getActivity()).databaseHandler.
                    getOwnReputation(device.readableName,lastRefreshTime);
            if(reputation == (float)0.0)
                return "No interaction yet";
            return String.valueOf(reputation);
        }
        public String getDeviceRecommendReputation(SalutDevice device){
            long lastRefreshTime = ((MainActivity)getActivity()).lastServerSearchTime;
            float reputation = ((MainActivity)getActivity()).databaseHandler.
                    getDeviceRecommendReputation(device.readableName,lastRefreshTime);
            if(reputation == (float)0.0)
                return "No recommend data yet";
            return String.valueOf(reputation);
        }
        public String getNumberOfInteractions(SalutDevice device){
            int number = ((MainActivity)getActivity()).databaseHandler.
                    getNumberOfInteractions(device.readableName);
            return String.valueOf(number);
        }
        public String getLastTimeInteraction(SalutDevice device){
            long lastTime = ((MainActivity)getActivity()).databaseHandler.
                    getLastTimeInteraction(device.readableName);
            if( lastTime == (float)0.0)
                return "No interaction yet";
            Date date = new Date(lastTime);
            return date.toString();
        }
        public String getAverageTime(SalutDevice device){
            float avvTime = ((MainActivity)getActivity()).databaseHandler.
                    getAverageTime(device.readableName);
            if( avvTime == (float)0.0)
                return "No interaction yet";
            return String.valueOf(avvTime);
        }
    }

    public void onPeersAvailable(ArrayList<SalutDevice> peerList) {
        dismissProgressDialog();
        peers.clear();
        peers.addAll(peerList);
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(TAG, "No devices found");
            return;
        }

    }
    public void dismissProgressDialog(){
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Remove the peers
     */
    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    public void updateDiscoverList(){
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Callback to bring up searching modal
     */
    public void onInitiateDiscovery() {
        dismissProgressDialog();
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding servers", true, true,
                new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }
}
