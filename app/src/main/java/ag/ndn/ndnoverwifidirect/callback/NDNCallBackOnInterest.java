package ag.ndn.ndnoverwifidirect.callback;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;

/**
 * Interface specification of a general callback
 * that takes in all input from a regular NDN OnInterest
 * callback, but fulfills a certain job.
 *
 * Created by allengong on 8/11/16.
 */
public interface NDNCallBackOnInterest {

    /**
     * Do something particular with available information onInterest.
     * @param prefix NDN Name
     * @param interest NDN Interest
     * @param face NDN Face
     * @param interestFilterId the interest filter ID
     * @param filter the InterestFilter
     */
    public void doJob(Name prefix, Interest interest, Face face,
                      long interestFilterId, InterestFilter filter);
}
