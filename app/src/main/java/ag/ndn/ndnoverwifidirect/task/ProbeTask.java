package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;

/**
 * Periodically probes registered peers for available
 * data prefixes.
 *
 * Created by allengong on 11/5/16.
 */

public class ProbeTask extends AsyncTask<Void, Void, Void> {

    private static final int REPEAT_TIMER_MS = 5000;
    private boolean loop = true;

    public void stop() {
        loop = true;
    }

    @Override
    protected Void doInBackground(Void... params) {
        // TODO probe probe probe...
        while (loop) {
            try {

                // enumerate FIB entries

                    // look only for the ones related to /localhop/wifidirect/xxx
                    // exclude localhost bound faces

                // for each face (or can create a face using ip in FIB name)
                    // send out probe interest towards this face with /localhop/wifidirect/xxx/yourIp/probe?mustBeFresh=1


                        // for each response, should then note the new data prefix(es)

                Thread.sleep(REPEAT_TIMER_MS);  // wait a little before doing again

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
