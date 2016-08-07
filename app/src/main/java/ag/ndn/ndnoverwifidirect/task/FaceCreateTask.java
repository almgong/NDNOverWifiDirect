package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * Created by allengong on 7/29/16.
 */
// task to create a network face without using main thread
public class FaceCreateTask extends AsyncTask<String, Void, Integer> {
    private final String TAG = "FaceCreateTask";
    NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();

    @Override
    protected Integer doInBackground(String... faceUris) {
        int faceId = -1;

        try {
            System.out.println("--------Inside face create task--------");
            faceId = mController.faceCreate(faceUris[0]);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "!!!Created face with face id: " + faceId);
        return faceId;
    }

//        @Override
//        protected Integer onPostExecute(Integer faceId) {
//            return faceId;
//        }

}
