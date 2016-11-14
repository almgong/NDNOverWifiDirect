package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Name;

import ag.ndn.ndnoverwifidirect.callback.GenericCallback;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * Created by allengong on 7/29/16.
 */
// task to create a network face without using main thread
public class FaceCreateTask extends AsyncTask<String, Void, Integer> {

    private final String TAG = "FaceCreateTask";
    private String peerIp;
    private String[] prefixesToRegister;
    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();
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
            faceId = NDNController.getInstance().getNfdcHelper().faceCreate(faceUris[0]);

            // register desired forwarding prefixes, e.g. the "/ndn/wifid/register" registration prefix
            for (String prefix : prefixesToRegister) {
                Log.d(TAG, "registering prefix: " + prefix);
                mController.ribRegisterPrefix(new Name(prefix), faceId, 0, true, false);
            }

            System.out.println("Successfully registered " + prefixesToRegister.length + " prefixes");

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "!!!Created face with face id: " + faceId);
        if (faceId != -1) {
            // if face creation successful, log it
            //mController.logPeerToFaceId(peerIp, faceId);

            NDNController.getInstance().logPeer(peerIp, faceId);

            // invoke callback, if any
            if (callback != null) {
                callback.doJob();
            }
        }

        System.out.println("---------- END face create task -----------");

        return faceId;
    }
}
