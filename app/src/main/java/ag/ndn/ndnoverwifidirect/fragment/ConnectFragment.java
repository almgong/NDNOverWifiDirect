package ag.ndn.ndnoverwifidirect.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import ag.ndn.ndnoverwifidirect.ConnectActivity;
import ag.ndn.ndnoverwifidirect.R;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.service.WDBroadcastReceiverService;
import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.WDBroadcastReceiver;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ConnectFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ConnectFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * Expects that the parent activity has a getReciever() method to
 * pass in a WDBroadcastReceiver.
 */
public class ConnectFragment extends Fragment {
    private static final String TAG = "ConnectFragment";

    private List<String> connectedPeers = new ArrayList<>();
    private OnFragmentInteractionListener mListener;

    // top level GUI components
    private Switch aSwitch;
    private TextView statusTextView;

    public ConnectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters. Serializable
     * objects only.
     *
     * @return A new instance of fragment ConnectFragment.
     */
    public static ConnectFragment newInstance() {
        ConnectFragment fragment = new ConnectFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // UI
        View view = inflater.inflate(R.layout.fragment_connect, container, false);

        ListView listView = (ListView) view.findViewById(R.id.connectedPeersListView);
        statusTextView = (TextView) view.findViewById(R.id.statusText);
        aSwitch = (Switch) view.findViewById(R.id.serviceSwitch);
        Button refreshPeersBtn = (Button) view.findViewById(R.id.discoverPeersBtn);

        // restore state as such
        aSwitch.setChecked(NDNController.getInstance().isProtocolRunning());

        NDNController.getInstance().setWifiDirectContext(getActivity());

        // for listview
        for (String deviceAddress : NDNController.getInstance().getConnectedPeers()) {
            connectedPeers.add(NDNController.getInstance().getPeerByDeviceAddress(deviceAddress).getName());
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, connectedPeers);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, (String)parent.getItemAtPosition(position));
            }
        });

        // listen for onChange event for switch
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    NDNController.getInstance().startDiscoverAndProbe();
                } else {
                    // turn off
                    NDNController.getInstance().stopDiscoverAndProbe();
                }
            }
        });

        // more like a refresh button
        refreshPeersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList(adapter);
            }
        });

        // Inflate the layout for this fragment
        statusTextView.setText("Ready.");

        if (WDBroadcastReceiver.groupOwnerAddress != null) {
            statusTextView.setText("Connected to group.");
        }

        return view;
    }

    private void updateList(ArrayAdapter<String> adapter) {
        // reset connected peers list (MAC addresses)
        Log.d(TAG, "Num peers connected: " + NDNController.getInstance().getConnectedPeers().size());
        connectedPeers.clear();
        for (String deviceAddress : NDNController.getInstance().getConnectedPeers()) {
            connectedPeers.add(NDNController.getInstance().getPeerByDeviceAddress(deviceAddress).getName());
        }
        adapter.notifyDataSetChanged();

        if (WDBroadcastReceiver.groupOwnerAddress != null) {
            statusTextView.setText("Connected to group.");
        }
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
