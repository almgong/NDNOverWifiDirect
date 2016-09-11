package ag.ndn.ndnoverwifidirect;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.videosharing.model.GlobalLists;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;

/**
 * Activity for NDN consumers to access video resources (i.e. play available
 * video media!).
 */
public class ConsumerActivity extends AppCompatActivity {

    // handle to NDN controller
    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // for list view; a list of remote video resources
        //TODO need to be able to retrieve using mController the prefixes reachable
        VideoResourceList videoResourceList = GlobalLists.getConsumerVideoResourceList();
        // init here //
        videoResourceList.addToList(new VideoResource(0, "JUST A TEST REMOTE VIDEO!!!"));

        ArrayAdapter<VideoResource> adapter = new ArrayAdapter<VideoResource>(this,
                android.R.layout.simple_list_item_1, videoResourceList.getList());

        // ui elements
        ListView listView = (ListView) findViewById(R.id.remoteVideoListView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("WOOOOO clicked @consumer@@@@");
            }
        });
    }
}
