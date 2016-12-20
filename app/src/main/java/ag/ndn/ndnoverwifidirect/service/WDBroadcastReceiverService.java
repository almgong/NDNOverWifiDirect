package ag.ndn.ndnoverwifidirect.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.WDBroadcastReceiver;

/**
 * Service that continues listening to WiFi Direct
 * broadcasted intents.
 *
 * Created by allengong on 11/28/16.
 */

public class WDBroadcastReceiverService extends Service {

    private final static String TAG = "WDBRService";

    private WDBroadcastReceiver mReceiver = null;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "initWifiP2p() service");
        initWifiP2p();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "registerReceiver()");
        registerReceiver(mReceiver, mIntentFilter);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if (mReceiver != null) {
            Log.d(TAG, "unregisterReceiver()");
            unregisterReceiver(mReceiver);

            // clean up any accumulated, static state
            WDBroadcastReceiver.cleanUp();
        }

        super.onDestroy();
    }

    /* initialize manager and receiver for activity */
    private void initWifiP2p() {
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WDBroadcastReceiver(mManager, mChannel);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        NDNController.getInstance().recordWifiP2pResources(mManager, mChannel);
    }


}

