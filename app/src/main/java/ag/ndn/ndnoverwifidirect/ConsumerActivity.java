package ag.ndn.ndnoverwifidirect;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.fragment.RemoteVideosFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;

/**
 * TODO move consumer logic here
 */
public class ConsumerActivity extends AppCompatActivity implements RemoteVideosFragment.OnListFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public void onListFragmentInteraction(VideoResource videoResource) {
        System.out.println("WOOO clicked");
        Toast.makeText(this, "Wooo: " + videoResource.toString(), Toast.LENGTH_SHORT);
    }
}
