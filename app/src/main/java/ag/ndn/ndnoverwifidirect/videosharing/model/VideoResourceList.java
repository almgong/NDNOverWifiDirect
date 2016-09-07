package ag.ndn.ndnoverwifidirect.videosharing.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of either: available video resources found
 * in the default android resource location, OR video resources
 * that are available remotely from a peer.
 *
 * Currently, a list does not allow duplicates. Sets are not used
 * because these lists can serve as input to ListFragments, which
 * expect lists.
 *
 * Created by allengong on 9/2/16.
 */
public class VideoResourceList {

    private List<VideoResource> videoList;   // for use in list fragments

    public VideoResourceList() {
        videoList = new ArrayList<>();
    }

    public List<VideoResource> getList() {
        return videoList;
    }

    public boolean addToList(VideoResource videoResource) {
        if (!videoList.contains(videoResource)) {
            videoList.add(videoResource);
            return true;
        }

        return false;
    }

    public boolean removeFromList(VideoResource videoResource) {
        return videoList.remove(videoResource);
    }
}
