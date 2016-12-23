package ag.ndn.ndnoverwifidirect.callback;

/**
 * Generic callback interface used for representing
 * anonymous functions.
 *
 * Created by allengong on 11/13/16.
 */

public interface GenericCallback {

    /**
     * No-arg function that all implementing classes must
     * define.
     */
    public void doJob();
}
