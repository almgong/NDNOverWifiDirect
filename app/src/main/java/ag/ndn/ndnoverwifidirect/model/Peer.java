package ag.ndn.ndnoverwifidirect.model;

/**
 * Represents a WifiDirect Peer.
 *
 * Created by allengong on 7/24/16.
 */
public class Peer {

    private String id;

    public Peer() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // for display
    @Override
    public String toString() {
        return id;
    }
}
