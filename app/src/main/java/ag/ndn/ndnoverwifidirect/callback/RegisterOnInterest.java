package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.IOException;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * For when one receives a registration interest
 * Created by allengong on 8/11/16.
 */
public class RegisterOnInterest implements NDNCallBackOnInterest {

    private final String TAG = "RegisterOnInterest";

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
            <LIST OF PEER IPs...>
         */
        String regRes = "Hi! Got your registration interest - new.\n";
        regRes+= (NDNOverWifiDirect.getInstance().enumerateLoggedFaces().size()+"\n");
        for (String ip : NDNOverWifiDirect.getInstance().enumerateLoggedFaces()) {
            regRes += (ip + "\n");
        }
        Log.d(TAG, "Response: "+ regRes);
        Blob payload = new Blob(regRes);
        response.setContent(payload);

        // associate peer to face info
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
