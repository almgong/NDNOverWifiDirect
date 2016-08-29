package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.Set;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver;

/**
 * For when one receives a registration interest
 * Created by allengong on 8/11/16.
 */
public class RegisterOnInterest implements NDNCallBackOnInterest {

    private final String TAG = "RegisterOnInterest";
    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();

    @Override
    public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        Log.d(TAG, "Received interest with prefix " + interest.getName().toUri());

        String[] interestNameArr = interest.getName().toUri().split("/");
        String peerIp = interestNameArr[interestNameArr.length-1];

        // send an acknowledgement data packet
        Data response = new Data();
        response.setName(new Name(interest.getName().toUri()));

        // generate hardcoded registration response
        /*
            MESSAGE\n
            <NUMBER OF PEERS>
            <LIST OF PEER IPs\n...>
            <NUMBER OF PREFIXES YOU HANDLE>
            <LIST OF PREFIXES\n...>
         */
        String regRes = "Hi! Got your registration interest - 8/28/2016.\n";

        // num peers
        regRes += (mController.enumerateLoggedFaces().size()+"\n");  // never includes localhost

        // peer ips
        for (String ip : mController.enumerateLoggedFaces()) {
            if (!ip.equals("localhost")) {
                regRes += (ip + "\n");
            }
        }

        // num prefixes handled
        Set<String> prefixesHandled = mController.getPrefixesHandled();
        regRes += prefixesHandled.size() + "\n";

        for (String p : prefixesHandled) {
            regRes += (p + "\n");
        }

        Log.d(TAG, "Response: " + regRes);
        Blob payload = new Blob(regRes);
        response.setContent(payload);

        // if peer is not logged by this device, ask for prefixes handled
        if (NDNOverWifiDirect.getInstance().getFaceByUri(peerIp) == null) {

            Log.d(TAG, "New peer, sending interest to ask for prefixes handled...");

            // on data callback
            OnData onDataCallback = new OnData() {
                @Override
                public void onData(Interest interest, Data data) {
                    (new RegisterOnData()).doJob(interest, data);
                }
            };

            // send registration interest to group owner
            String[] prefixes = {"/ndn/wifid/register/" + peerIp};
            mController.createFace(peerIp, prefixes);
            mController.sendInterest(new Interest(new Name("/ndn/wifid/register/" +
                            peerIp + "/" + WiFiDirectBroadcastReceiver.myAddress)),
                    face, onDataCallback);
        } else {
            Log.d(TAG, "Peer already exists! Skip asking for prefixes handled.");
        }

        // associate peer to a direct face
        NDNOverWifiDirect.getInstance().logFace(peerIp, face);

        try {
            face.putData(response);
            Log.d(TAG, "responded to interest: " + prefix.toUri());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
