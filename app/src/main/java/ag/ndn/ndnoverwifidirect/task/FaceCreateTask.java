package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Name;

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

    public FaceCreateTask(String peerIp, String[] prefixes) {
        this.peerIp = peerIp;
        this.prefixesToRegister = prefixes;
    }

    @Override
    protected Integer doInBackground(String... faceUris) {
        int faceId = -1;

        try {
            System.out.println("-------- Inside face create task --------");

            faceId = mController.faceCreate(faceUris[0]);

            // register desired forwarding prefixes, e.g. the "/ndn/wifid/register" registration prefix
            for (String prefix : prefixesToRegister) {
                mController.ribRegisterPrefix(new Name(prefix), faceId, 0, true, false);
            }


            System.out.println("Successfully registered registration prefix");

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "!!!Created face with face id: " + faceId);
        if (faceId != -1) {
            // if face creation successful, log it
            mController.logPeerToFaceId(peerIp, faceId);
        }

        System.out.println("---------- END face create task -----------");

        return faceId;
    }
}
