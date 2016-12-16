package ag.ndn.ndnoverwifidirect.model;

import android.location.Address;

/**
 * Represents a WifiDirect Peer.
 *
 * Created by allengong on 7/24/16.
 */
public class Peer {

    // members
    private String deviceAddress;   // device address
    private String name;            // user-friendly device name
    private int faceId;
    private int numProbeTimeouts = 0;   // number of timeouts while probing prefixes from this peer

    public Peer() {}

    public Peer(String deviceAddress, String name, int faceId, int numProbeTimeouts) {
        this.deviceAddress = deviceAddress;
        this.name = name;
        this.faceId = faceId;
        this.numProbeTimeouts = numProbeTimeouts;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFaceId() {
        return faceId;
    }

    public void setFaceId(int faceId) {
        this.faceId = faceId;
    }

    public int getNumProbeTimeouts() {
        return numProbeTimeouts;
    }

    public void setNumProbeTimeouts(int numProbeTimeouts) {
        this.numProbeTimeouts = numProbeTimeouts;
    }

    @Override
    public String toString() {
        return "Peer{" +
                ", deviceAddress='" + deviceAddress + '\'' +
                ", name='" + name + '\'' +
                ", faceId=" + faceId +
                ", numProbeTimeouts=" + numProbeTimeouts +
                '}';
    }
}
