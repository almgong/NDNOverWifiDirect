package ag.ndn.ndnoverwifidirect;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;

import java.io.IOException;
import java.io.RandomAccessFile;

import ag.ndn.ndnoverwifidirect.task.RegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;
import ag.ndn.ndnoverwifidirect.videosharing.callback.GetVideoOnInterest;
import ag.ndn.ndnoverwifidirect.videosharing.datasource.ChunkDataSourceFactory;
import ag.ndn.ndnoverwifidirect.videosharing.task.GetVideoTask;

public class VideoActivity extends AppCompatActivity {

    // meant to be between /ndn/wifidirect and /some-video-name
    public static final String NDN_VIDEO_PREFIX = "/video";

    private static final String TAG = "VideoActivity";

    private SimpleExoPlayerView simpleExoPlayerView;
    private Bundle bundle;

    private SimpleExoPlayer player;
    private VideoPlayerBuffer videoPlayerBuffer = new VideoPlayerBuffer();

    private NDNController mController = NDNController.getInstance();

    // initialized dependent on whether you are a producer/consumer
    private GetVideoTask getVideoTask;
    private RegisterPrefixTask pushVideoTask;
    private String currentPrefix;
    private RandomAccessFile ras = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // UI
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.producerExoPlayer);

        // get info passed from previous activity
        bundle = getIntent().getExtras();

        // get the simple exo video player
        player = new VideoPlayer(this).getPlayer();

        // source to which ExoPlayer should read from
        MediaSource source = null;
        currentPrefix = bundle.getString("prefix");
        if (bundle.getBoolean("isLocal")) {

            // file is local, use default data source
            source = new ExtractorMediaSource(Uri.parse(bundle.getString("videoUri")),
                    new FileDataSourceFactory(),
                    new DefaultExtractorsFactory(),
                    null,
                    new ExtractorMediaSource.EventListener() {

                        @Override
                        public void onLoadError(IOException error) {

                        if (error.getMessage() != null) {
                            Log.e(TAG, error.getMessage());
                        }

                        Toast.makeText(VideoActivity.this, "Error loading media.", Toast.LENGTH_LONG).show();
                        }
                    });

            // register the prefix to share
            registerVideoPrefix(currentPrefix);
        } else {

            // get external media using our custom chunk data source
            source = new ExtractorMediaSource(Uri.parse(""),
                    new ChunkDataSourceFactory(videoPlayerBuffer),
                    new DefaultExtractorsFactory(),
                    new Handler(), new ExtractorMediaSource.EventListener() {
                @Override
                public void onLoadError(IOException error) {
                    if (error.getMessage() != null) {
                        Log.e(TAG, error.getMessage());
                    }

                    Toast.makeText(VideoActivity.this, "Error loading media.", Toast.LENGTH_LONG).show();

                }
            });

            // start getting media from network
            startGettingVideo(currentPrefix);
            //simpleExoPlayerView.setUseController(false);    // consumers use a different controller

            // should bind the seekbar for consumers
            player.setPlayWhenReady(true);
        }

        // prepare the player with the appropriate data source
        player.prepare(source);

        // bind exo player to view
        simpleExoPlayerView.setPlayer(player);

        // pause background tasks
        //mController.stopDiscoveringPeers();
        //mController.stopProbing();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bundle.getBoolean("isLocal")) {                     // producer
            // stop responding to interests towards this prefix
            pushVideoTask.setStopProcessing(true);

            // close input stream if it is open
            if (ras != null) {
                try {
                    ras.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {                                                // consumer
            getVideoTask.stop(true);
            videoPlayerBuffer.clearBuffer();
        }

        player.release();
    }

    // helpers

    /**
     * Begins sending out interests to acquire video bytes,
     * and populates the VideoPlayerBuffer instance.
     * @param prefix the prefix to get video media from.
     */
    private void startGettingVideo(String prefix) {
        getVideoTask = new GetVideoTask(videoPlayerBuffer, this);
        getVideoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, prefix);
        Log.d(TAG, "Started GetVideoTask...");
    }

    /**
     * Registers the prefix with NFD, and OnInterest, returns
     * the "correct" bytes of the current video.
     * @param prefix The prefix to register (can be different from currentPrefix)
     */
    private void registerVideoPrefix(String prefix) {
        Log.d(TAG, "REGISTERING VIDEO PREFIX FOR SHARING...");
        //Face mFace = mController.getLocalHostFace();
        Face mFace = ProducerActivity.PRODUCER_FACE;


        try {
            ras = new RandomAccessFile(bundle.getString("videoUri"), "r");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Could not open video resource to share...");
            return;
        }

        // ideally, mController should not exist and the app would need to register the prefix manually
        // but OK for demo purposes to invoke mController's convenience methods
         pushVideoTask = (RegisterPrefixTask)mController.registerPrefix(mFace, prefix, new OnInterestCallback() {

            @Override
            public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                (new GetVideoOnInterest(ras)).doJob(prefix, interest,face, interestFilterId, filter);
            }
        }, false, 50);  // process events every 50 ms
    }
}
