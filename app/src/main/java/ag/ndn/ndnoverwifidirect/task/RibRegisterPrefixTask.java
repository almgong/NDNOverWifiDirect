package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.Nfdc;

import net.named_data.jndn.Name;

import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.utils.NfdcHelper;

/**
 * Same as RegisterPrefixTask, but for a specifc face id, rather than a Face instance.
 * Created by allengong on 8/22/16.
 */
public class RibRegisterPrefixTask extends AsyncTask<String, Void, Integer> {

    private final String TAG = "RibRegisterTask";

    private String prefixToRegister;
    private int faceId;
    private int cost;
    private boolean childInherit;
    private boolean capture;

    public RibRegisterPrefixTask(String prefixToRegister, int faceId, int cost,
                                 boolean childInherit, boolean capture) {
        this.prefixToRegister = prefixToRegister;
        this.capture = capture;
        this.childInherit = childInherit;
        this.cost = cost;
        this.faceId = faceId;
    }

    public Integer doInBackground(String... nothing) {

        try {

            NDNController.getInstance().getNfdcHelper().ribRegisterPrefix(new Name(prefixToRegister),
                    faceId, cost, childInherit, capture);
            Log.d(TAG, "registered rib prefix: " + prefixToRegister);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return 0;
    }
}
