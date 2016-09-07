package ag.ndn.ndnoverwifidirect.videosharing.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a list of either: available video resources found
 * in the default android resource location, OR video resources
 * that are available remotely from a peer.
 *
 * Internally, the list is represented as a Set, but is returned
 * as a List. This automatically guards against duplicate inserts.
 *
 * Created by allengong on 9/2/16.
 */
public class VideoResourceList {

    private Set<VideoResource> videoSet;
    private List<VideoResource> videoList;   // for use in list fragments

    public VideoResourceList() {
        videoList = new ArrayList<>();
        videoSet = new HashSet<>();
    }

    public List<VideoResource> getList() {
        return videoList;
    }

    public void addToList(VideoResource videoResource) {
        if (!videoSet.contains(videoResource)) {
            videoSet.add(videoResource);
            videoList.add(videoResource);
        }
    }

    public void removeFromList(VideoResource videoResource) {
        if (videoSet.contains(videoResource)) {
            videoSet.remove(videoResource);
            videoList.remove(videoResource);
        }
    }
}
