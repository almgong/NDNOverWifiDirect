package ag.ndn.ndnoverwifidirect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.named_data.jndn.Face;
import net.named_data.jndn.security.KeyChain;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.videosharing.model.GlobalLists;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;
import ag.ndn.ndnoverwifidirect.videosharing.util.NDNSanitizer;

/**
 * Producer activity, should display to the user all supported media types
 * available to share.
 */
public class ProducerActivity extends AppCompatActivity {

    public static final Face PRODUCER_FACE = new Face("localhost");

    private static final String TAG = "ProducerActivity";
    private final String VIDEO_RESOURCE_LOCATION = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_MOVIES;

    // maps human-readable-name to the actual location of the file on disk
    private Map<String, String> videoNameLocationMap = new HashMap<>();

    // returns a list of mp4 video names (as named on the user's device)
    private List<String> getVideoListFromFS() {

        File videoDir = new File(VIDEO_RESOURCE_LOCATION);

        int uniquefier = 0; // appended number to handle multiple, same named video resources

        if (!videoDir.exists()) {
            Log.e(TAG, "Could not open video resource location on device.");
            return null;
        } else {
            List<String> videoNames = new ArrayList<>();
            for (File f : videoDir.listFiles()) {
                String name;
                if ((name=f.getName()).endsWith(".mp4") || ((name=f.getName()).endsWith(".mp3"))) {

                    if (videoNameLocationMap.containsKey(name)) {
                        name += ( "-" + (++uniquefier));
                    }

                    // add to running list (with modified name if duplicates)
                    videoNames.add(name);

                    // add the mapping
                    videoNameLocationMap.put(name, f.getAbsolutePath());
                }
            }
            return videoNames;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get a list of local videos
        VideoResourceList producerVideoResourceList = GlobalLists.getProducerVideoResourceList();
        producerVideoResourceList.clear();

        List<String> videosOnDisk = getVideoListFromFS();
        for (int i = 0; i < videosOnDisk.size(); i++) {
            producerVideoResourceList.addToList(new VideoResource(i, videosOnDisk.get(i)));
        }

        // init the list view
        ArrayAdapter<VideoResource> listViewAdapter = new ArrayAdapter<VideoResource>(ProducerActivity.this,
                android.R.layout.simple_list_item_1, producerVideoResourceList.getList());

        ListView listView = (ListView) findViewById(R.id.videoListView);
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // get the desired resource
                VideoResource desiredVideo = (VideoResource) parent.getItemAtPosition(position);

                Intent intent = new Intent(ProducerActivity.this, VideoActivity.class);
                intent.putExtra("videoUri", videoNameLocationMap.get(desiredVideo.getVideoName()));
                intent.putExtra("isLocal", true);
                intent.putExtra("prefix", VideoActivity.DATA_PREFIX + VideoActivity.NDN_VIDEO_PREFIX +
                        "/" + NDNSanitizer.sanitizeName(desiredVideo.getVideoName()));
                startActivity(intent);
            }
        });

        try {
            KeyChain kc = NDNController.getInstance().getKeyChain();
            PRODUCER_FACE.setCommandSigningInfo(kc, kc.getDefaultCertificateName());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PRODUCER_FACE.shutdown();
    }
}
