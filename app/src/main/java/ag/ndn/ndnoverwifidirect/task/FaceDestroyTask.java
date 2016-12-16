package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import net.named_data.jndn_xx.util.FaceUri;

import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Attempts to destroy a given Face, denoted by its Face id.
 * Created by allengong on 12/15/16.
 */
public class FaceDestroyTask extends AsyncTask<Integer, Void, Void> {

    private static final String TAG = "FaceDestroyTask";

    @Override
    protected Void doInBackground(Integer... params) {

        try {
            Log.d(TAG, "-------- Inside face destroy task --------");
            // attempt to destroy Face Id, specified as the first and only parameter
            Nfdc.destroyFace(NDNController.getInstance().getLocalHostFace(), params[0]);
            Log.d(TAG, "Successfully destroyed Face with Face id: " + params[0]);
        } catch (ManagementException me) {
            Log.e(TAG, me.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        Log.d(TAG, "---------- END face destroy task -----------");

        return null;
    }
}
