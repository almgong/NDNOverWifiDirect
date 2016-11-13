package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Simply discovers peers on a non-UI bound thread.
 *
 * Created by allengong on 11/5/16.
 */

public class DiscoverPeersTask extends AsyncTask<Void, Void, Void> {

    private static final int REPEAT_TIMER_MS = 5000;   // when to check for peers again
    private boolean loop = true;

    public void stop() {
        loop = false;
    }
    @Override
    protected Void doInBackground(Void... params) {

        try {
            while (loop) {
                System.err.println("Discover peers....");
                NDNController.getInstance().discoverPeers();
                Thread.sleep(REPEAT_TIMER_MS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
