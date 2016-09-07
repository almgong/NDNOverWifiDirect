package ag.ndn.ndnoverwifidirect.model;

import android.location.Address;

/**
 * Represents a WifiDirect Peer.
 *
 * Created by allengong on 7/24/16.
 */
public class Peer {

    private String id;
    private String deviceAddress;
    private String name;

    public Peer() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "Peer{" +
                "id='" + id + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                '}';
    }
}
