package ag.ndn.ndnoverwifidirect;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import ag.ndn.ndnoverwifidirect.videosharing.model.GlobalLists;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;
import ag.ndn.ndnoverwifidirect.videosharing.task.GetAvailableVideosTask;

/**
 * Activity for NDN consumers to access video resources (i.e. play available
 * video media!).
 */
public class ConsumerActivity extends AppCompatActivity {

    private GetAvailableVideosTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBarConsumer);
        progressBar.setIndeterminate(true);

        // for list view; a list of remote video resources
        VideoResourceList videoResourceList = GlobalLists.getConsumerVideoResourceList();
        videoResourceList.clear();

        ArrayAdapter<VideoResource> adapter = new ArrayAdapter<VideoResource>(this,
                android.R.layout.simple_list_item_1, videoResourceList.getList());

        // get most up to date list of video resources from network (UI updated by task)
        task = new GetAvailableVideosTask(adapter, videoResourceList, progressBar);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // ui elements
        ListView listView = (ListView) findViewById(R.id.remoteVideoListView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoResource resource = (VideoResource) parent.getItemAtPosition(position);

                Intent intent = new Intent(ConsumerActivity.this, VideoActivity.class);
                intent.putExtra("isLocal", false);
                intent.putExtra("prefix", resource.getVideoName()); // name is the parent prefix
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() { super.onResume(); }
}
