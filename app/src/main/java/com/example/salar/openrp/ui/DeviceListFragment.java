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
import android.widget.TextView;

import com.example.salar.openrp.R;
import com.peak.salut.SalutDevice;

import java.util.ArrayList;
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
        view.setText(device.readableName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(isHost?"Host":"Client");
    }

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
            SalutDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.readableName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device));
                }
            }
            return v;
        }
        public String getDeviceStatus(SalutDevice salutDevice){
            if(salutDevice.isRegistered == true)
                return "Connected";
            return  "Available";
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
