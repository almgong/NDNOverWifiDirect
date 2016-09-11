package ag.ndn.ndnoverwifidirect.videosharing.model;

/**
 * Holds the lists that will be used in Fragments pertaining to
 * videosharing lists (i.e. resource/video lists).
 *
 * Created by allengong on 9/10/16.
 */
public class GlobalLists {

    private static VideoResourceList consumerVideoResourceList = new VideoResourceList();
    private static VideoResourceList producerVideoResourceList = new VideoResourceList();

    private GlobalLists() {}

    public static VideoResourceList getConsumerVideoResourceList() {
        return consumerVideoResourceList;
    }

    public static VideoResourceList getProducerVideoResourceList() {
        return producerVideoResourceList;
    }
}
