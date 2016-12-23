package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import net.named_data.jndn_xx.util.FaceUri;

import ag.ndn.ndnoverwifidirect.callback.GenericCallback;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Convenience class that creates a Face with the forwarder. A callback
 * is accpeted via the public setCallback(...) method, and will be called
 * if and only if face creation succeeds.
 * Created by allengong on 7/29/16.
 */
// task to create a network face without using main thread
public class FaceCreateTask extends AsyncTask<String, Void, Integer> {

    private final String TAG = "FaceCreateTask";
    private String peerIp;
    private String[] prefixesToRegister;
    private NDNController mController = NDNController.getInstance();
    private GenericCallback callback = null;

    public FaceCreateTask(String peerIp, String[] prefixes) {
        this.peerIp = peerIp;
        this.prefixesToRegister = prefixes;
    }

    public void setCallback(GenericCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Integer doInBackground(String... faceUris) {
        int faceId = -1;

        try {
            Log.d(TAG, "-------- Inside face create task --------");

            faceId = Nfdc.createFace(mController.getLocalHostFace(),
                    new FaceUri(faceUris[0]).canonize().toString());


            Log.d(TAG, "Successfully registered " + prefixesToRegister.length + " prefixes");
            Log.d(TAG, "Created Face with Face id: " + faceId);
            if (faceId != -1) {

                // if face creation successful, log the new peer
                Peer peer = new Peer();
                peer.setFaceId(faceId);
                mController.logPeer(peerIp, peer);

                // invoke callback, if any
                if (callback != null) {
                    callback.doJob();
                }
            }

        } catch (ManagementException me) {
            Log.e(TAG, me.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        Log.d(TAG, "---------- END face create task -----------");

        return faceId;
    }
}
