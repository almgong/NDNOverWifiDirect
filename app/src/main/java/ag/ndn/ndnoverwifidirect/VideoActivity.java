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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        bundle = getIntent().getExtras();
        Log.d(TAG, "is local?: " + bundle.getBoolean("local"));
        Log.d(TAG, "videoUri: " + bundle.getString("videoUri"));

        // handler


        // temp prepopulate the buffer
//        byte[] temp = new byte[4000];
//        File f = new File(bundle.getString("videoUri"));
//        try {
//
//            FileInputStream fis = new FileInputStream(bundle.getString("videoUri"));
//            fis.read(temp, 0, 4000);
//            videoPlayerBuffer.addToBuffer(temp);
//
//            byte[] temp2 = new byte[4000];
//            //fis.read(temp2, 0, temp2.length);
//            videoPlayerBuffer.addToBuffer(temp2);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        GetVideoTask task = new GetVideoTask(videoPlayerBuffer);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.d(TAG, "Started GetVideoTask...");

        player = VideoPlayer.getPlayer(this);
        //player.reset();

        //media source to read from
        MediaSource source = new ExtractorMediaSource(Uri.parse(""),
                new ChunkDataSourceFactory(videoPlayerBuffer), new DefaultExtractorsFactory(),
                handler, new ExtractorMediaSource.EventListener() {
            @Override
            public void onLoadError(IOException error) {
                Log.e(TAG, error.getMessage());
            }
        });


        // below is used when file is local
//        MediaSource source = new ExtractorMediaSource(Uri.parse(bundle.getString("videoUri")),new FileDataSourceFactory(),
//                new DefaultExtractorsFactory(), null, new ExtractorMediaSource.EventListener() {
//
//            @Override
//            public void onLoadError(IOException error) {
//
//            }
//        });


        // prepare to play
        player.prepare(source);

        player.setPlayWhenReady(true);

       // videoView = (VideoView) findViewById(R.id.videoView);
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.producerExoPlayer);
        simpleExoPlayerView.setPlayer(player);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new Handler();    // TODO debug the dead handler thread issue
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
