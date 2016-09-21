package ag.ndn.ndnoverwifidirect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.protobuf.UnknownFieldSet;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import ag.ndn.ndnoverwifidirect.videosharing.model.GlobalLists;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;

/**
 * TODO: add producer logic here
 */
public class ProducerActivity extends AppCompatActivity {

    private static final String TAG = "ProducerActivity";

    private Field[] getVideosFromRaw() {
        Field[] fields = R.raw.class.getFields();
        for (Field f : fields) {
            System.out.println("Field: " + f.getName());
        }

        return fields;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get a list of local videos TODO should read local filesystem here
        GlobalLists.getProducerVideoResourceList().addToList(new VideoResource(0, "TEST PRODUCER VIDEO RESOURCE"));

        // TODO read from default video location
        GlobalLists.getProducerVideoResourceList().addToList(new VideoResource(1, "TEST PRODUCER VIDEO RESOURCE"));

        VideoResourceList localVideos = GlobalLists.getProducerVideoResourceList();
        ArrayAdapter<VideoResource> listViewAdapter = new ArrayAdapter<VideoResource>(ProducerActivity.this,
                android.R.layout.simple_list_item_1, localVideos.getList());

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

                // TODO this is temporary for testing
                String videoUriAsString = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_MOVIES + "/big_buck_bunny.mp4";

                Intent intent = new Intent(ProducerActivity.this, VideoActivity.class);
                intent.putExtra("videoUri", videoUriAsString);
                intent.putExtra("local", true);
                startActivity(intent);
            }
        });

    }
}
