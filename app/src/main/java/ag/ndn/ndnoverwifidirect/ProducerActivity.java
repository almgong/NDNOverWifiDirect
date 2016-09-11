package ag.ndn.ndnoverwifidirect;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import ag.ndn.ndnoverwifidirect.videosharing.model.GlobalLists;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;

/**
 * TODO: add producer logic here
 */
public class ProducerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ui elements, etc.

        // get a list of local videos TODO should read local filesystem here
        GlobalLists.getProducerVideoResourceList().addToList(new VideoResource(0, "TEST PRODUCER VIDEO RESOURCE"));
        VideoResourceList localVideos = GlobalLists.getProducerVideoResourceList();

        ArrayAdapter<VideoResource> listViewAdapter = new ArrayAdapter<VideoResource>(ProducerActivity.this,
                android.R.layout.simple_list_item_1, localVideos.getList());
System.out.println("woo");
        ListView listView = (ListView) findViewById(R.id.videoListView);
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                System.out.println("wooo something got clicked");
                System.out.println(parent);
                System.out.println(view);
                System.out.println(position);
                System.out.println(id);

            }
        });

    }

}
