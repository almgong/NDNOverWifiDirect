package ag.ndn.ndnoverwifidirect.fragment;

import android.app.Activity;
import android.content.Context;
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
    private WDBroadcastReceiver mReciever = null;
    private OnFragmentInteractionListener mListener;

    // status of the switch button, 0 = off and 1 = on
    private int switchStatus;

    private final int SWITCH_STATUs_OFF = 0;
    private final int SWITCH_STATUS_ON = 1;

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

        System.err.println("oncreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // UI
        // onCreate is called after attaching to parent activity
        View view = inflater.inflate(R.layout.fragment_connect, container, false);

        ListView listView = (ListView) view.findViewById(R.id.connectedPeersListView);
        statusTextView = (TextView) view.findViewById(R.id.statusText);
        aSwitch = (Switch) view.findViewById(R.id.serviceSwitch);
        Button discoverPeersBtn = (Button) view.findViewById(R.id.discoverPeersBtn);

        aSwitch.setChecked(false);  // default OFF
        switchStatus = SWITCH_STATUs_OFF;

        // for listview
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
                    NDNController.getInstance().startProbing();
                    NDNController.getInstance().startDiscoveringPeers();
                    switchStatus = SWITCH_STATUS_ON;
                } else {
                    // turn off
                    NDNController.getInstance().stopProbing();
                    NDNController.getInstance().stopDiscoveringPeers();
                    switchStatus = SWITCH_STATUs_OFF;
                }

                // update the receiver
                mReciever = ((ConnectActivity) getActivity()).getReceiver();
            }
        });

        // more like a refresh button
        discoverPeersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList(adapter);
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    private void updateList(ArrayAdapter<String> adapter) {
        if (mReciever != null) {
            // reset connected peers list (MAC addresses)
            connectedPeers.clear();
            connectedPeers.addAll(mReciever.getConnectedPeers());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(Activity context) {
        System.err.println("onAttach");
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

    // for saving and restoring state
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        System.err.println("onActivityCreated called");
        System.err.println("savedInstanceState: " + savedInstanceState);
        // restore state, if any
        if (savedInstanceState != null) {

            if (savedInstanceState.getInt("switchStatus") == SWITCH_STATUS_ON) {
                System.err.println("Restored state says switch was on");
                aSwitch.setChecked(true);
                NDNController.getInstance().startProbing();
                NDNController.getInstance().startDiscoveringPeers();
            } else {
                System.err.println("Restrored state says switch was off");
                aSwitch.setChecked(false);
                NDNController.getInstance().stopProbing();
                NDNController.getInstance().stopDiscoveringPeers();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle stateToSave) {
        super.onSaveInstanceState(stateToSave);

        System.err.println("onSaveInstanceState called");
        // save any state desired
        System.err.println("Saving switchStatus: " + switchStatus);
        stateToSave.putInt("switchStatus", switchStatus);
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
