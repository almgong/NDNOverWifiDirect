package ag.ndn.ndnoverwifidirect.runnable;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Initiates peer discovery.
 * Created by allengong on 12/9/16.
 */

public class DiscoverPeersRunnable implements Runnable {
    @Override
    public void run() {
        try {
            NDNController.getInstance().discoverPeers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
