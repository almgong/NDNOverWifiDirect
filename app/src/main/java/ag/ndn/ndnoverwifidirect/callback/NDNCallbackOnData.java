package ag.ndn.ndnoverwifidirect.callback;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * Interface specification of a generic callback
 * that takes in all input from a regular NDN OnData
 * callback, but fulfills a certain job.
 *
 * Created by allengong on 8/11/16.
 */
public interface NDNCallbackOnData {

    /**
     * Do something particular with the interest and data packets.
     * @param interest NDN Interest
     * @param data NDN Data corresponding to the Interest
     */
    public void doJob(Interest interest, Data data);
}
