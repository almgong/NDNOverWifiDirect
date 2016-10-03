package ag.ndn.ndnoverwifidirect;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor;
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.common.io.Files;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.KeyChain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;
import ag.ndn.ndnoverwifidirect.videosharing.datasource.ChunkDataSource;
import ag.ndn.ndnoverwifidirect.videosharing.datasource.ChunkDataSourceFactory;
import ag.ndn.ndnoverwifidirect.videosharing.task.GetVideoTask;

public class VideoActivity extends AppCompatActivity {

    private static final String TAG = "VideoActivity";

    private VideoView videoView;
    private SimpleExoPlayerView simpleExoPlayerView;
    private Bundle bundle;

    private SimpleExoPlayer player;
    private VideoPlayerBuffer videoPlayerBuffer = new VideoPlayerBuffer();
    private Handler handler = new Handler();

    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // get info passed from previous activity
        bundle = getIntent().getExtras();
        Log.d(TAG, "is local?: " + bundle.getBoolean("isLocal"));
        Log.d(TAG, "videoUri: " + bundle.getString("videoUri"));

        // get the simple exo video player
        player = VideoPlayer.getPlayer(this);

        // source to which ExoPlayer should read from
        MediaSource source = null;

        if (bundle.getBoolean("isLocal")) {

            // file is local, use default data source
            source = new ExtractorMediaSource(Uri.parse(bundle.getString("videoUri")),
                    new FileDataSourceFactory(),
                    new DefaultExtractorsFactory(),
                    null,
                    new ExtractorMediaSource.EventListener() {

                        @Override
                        public void onLoadError(IOException error) {
                            Log.e(TAG, error.getMessage());
                        }
                    });

            // register the prefix to share
            registerVideoPrefix(bundle.getString("prefix"));
        } else {

            // get external media using our custom chunk data source
            source = new ExtractorMediaSource(Uri.parse(""),
                    new ChunkDataSourceFactory(videoPlayerBuffer),
                    new DefaultExtractorsFactory(),
                    handler, new ExtractorMediaSource.EventListener() {
                @Override
                public void onLoadError(IOException error) {
                    Log.e(TAG, error.getMessage());
                }
            });

            // start getting media from network
            startGettingVideo(bundle.getString("prefix"));
        }

        // prepare the player with the appropriate data source
        player.prepare(source);
        //player.setPlayWhenReady(true);

        // bind exo player to view
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.producerExoPlayer);
        simpleExoPlayerView.setPlayer(player);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new Handler();    // TODO debug the dead handler thread issue
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    // helpers

    /**
     * Begins sending out interests to acquire video bytes,
     * and populates the VideoPlayerBuffer instance.
     * @param prefix
     */
    private void startGettingVideo(String prefix) {
        GetVideoTask task = new GetVideoTask(videoPlayerBuffer);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, prefix);
        Log.d(TAG, "Started GetVideoTask...");
    }

    /**
     * Registers the prefix with NFD, and OnInterest, returns
     * the "correct" bytes of the current video.
     * @param prefix
     */
    private void registerVideoPrefix(String prefix) {
        Log.d(TAG, "REGISTERING VIDEO PREFIX FOR SHARING...");
        Face mFace = new Face();
        try {
            KeyChain keyChain = mController.getKeyChain();
            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mController.registerPrefix(mFace, prefix, new OnInterestCallback() {

            @Override
            public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {

            }
        }, false, 500);
    }
}
