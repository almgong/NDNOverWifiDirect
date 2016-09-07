package ag.ndn.ndnoverwifidirect.videosharing.model;

/**
 * Represents a Video resource, either local or remote.
 *
 * Created by allengong on 9/6/16.
 */
public class VideoResource {

    private int id;
    private String videoName;

    public VideoResource(int id, String videoName) {
        this.videoName = videoName;
        this.id = id;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoResource that = (VideoResource) o;

        if (id != that.id) return false;
        return videoName != null ? videoName.equals(that.videoName) : that.videoName == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (videoName != null ? videoName.hashCode() : 0);
        return result;
    }
}
