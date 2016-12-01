package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import net.named_data.jndn_xx.util.FaceUri;

import ag.ndn.ndnoverwifidirect.callback.GenericCallback;
import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
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
            System.out.println("-------- Inside face create task --------");

            //faceId = mController.faceCreate(faceUris[0]);
            //faceId = mController.getNfdcHelper().faceCreate(faceUris[0]);
            faceId = Nfdc.createFace(mController.getLocalHostFace(),
                    new FaceUri(faceUris[0]).canonize().toString());

            // piggyback registering desired prefixes -- deprecated, supply a callback instead
            //mController.ribRegisterPrefix(faceId, prefixesToRegister);

            System.out.println("Successfully registered " + prefixesToRegister.length + " prefixes");

            Log.d(TAG, "!!!Created face with face id: " + faceId);
            if (faceId != -1) {
                // if face creation successful, log it

                mController.logPeer(peerIp, faceId);

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


        System.out.println("---------- END face create task -----------");

        return faceId;
    }
}
