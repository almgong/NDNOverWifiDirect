package ag.ndn.ndnoverwifidirect.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of Peers.
 * Created by allengong on 7/24/16.
 */
public class PeerList {

    private static List<Peer> peers = new ArrayList<>();

    // no instantiation
    private PeerList() {}

    public static List<Peer> getPeers() {
        return peers;
    }

    public static void setPeers(List<Peer> peers) {
        PeerList.peers = peers;
    }

    public static void addPeer(Peer p) {
        peers.add(p);
    }

    public static void removePeer(Peer p) {
        //TODO
    }

    public static void resetPeerList() {
        peers.clear();
    }

}
