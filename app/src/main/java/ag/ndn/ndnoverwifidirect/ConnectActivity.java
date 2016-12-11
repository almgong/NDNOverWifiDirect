package ag.ndn.ndnoverwifidirect;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import ag.ndn.ndnoverwifidirect.fragment.ConnectFragment;
import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Logic flow:
 *
 * init() wifidirect and register this activity with it, attempt to discover peers, and allow
 * user to select via a list fragment, a peer to connect to.
 */
public class ConnectActivity extends AppCompatActivity implements ConnectFragment.OnFragmentInteractionListener {

    private static final String TAG = "ConnectActivity";
    public static final int CONNECT_SUCCESS = 0;      // marks successfuly connection
    public static Handler mHandler;                   // android handler to trigger UI update

    private ConnectFragment mFragment;
    private String mFragmentTag = "connectFragmentTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NDNController.getInstance().setWifiDirectContext(this);
        mHandler = getHandler();

        // if there is saved state, don't recreate the fragment
        if (savedInstanceState == null) {
            Log.d(TAG, "There was no state to restore.");
            mFragment = ConnectFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.connectLayout, mFragment, mFragmentTag)
                    .commit();
        } else {
            Log.d(TAG, "There was STATE to restore!!");
            mFragment = (ConnectFragment) getSupportFragmentManager()
                    .findFragmentByTag(mFragmentTag);
        }
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        mHandler = getHandler();
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        mHandler = null;
    }

    // returns a handler for connection success
    private Handler getHandler() {
        return new Handler(Looper.getMainLooper()) {

            public void handleMessage(Message msg) {
                if (msg.what == CONNECT_SUCCESS) {
                    Toast.makeText(ConnectActivity.this, "Successfully connected to group.", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /* implement Fragment listener(s) to allow fragment communications */
    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, "Interaction with connectFragment");
    }
}