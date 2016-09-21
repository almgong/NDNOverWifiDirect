package ag.ndn.ndnoverwifidirect;

import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import java.io.IOException;

import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;

public class VideoActivity extends AppCompatActivity {

    private static final String TAG = "VideoActivity";

    private VideoView videoView;
    private SimpleExoPlayerView simpleExoPlayerView;
    private Bundle bundle;

    private SimpleExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        bundle = getIntent().getExtras();
        Log.d(TAG, "is local?: " + bundle.getBoolean("local"));
        Log.d(TAG, "videoUri: " + bundle.getString("videoUri"));

        player = VideoPlayer.getPlayer(this);

        // media source to read from
        MediaSource source = new ExtractorMediaSource(Uri.parse(bundle.getString("videoUri")),
                new FileDataSourceFactory(), Mp4Extractor.FACTORY, new Handler(), new ExtractorMediaSource.EventListener() {
            @Override
            public void onLoadError(IOException error) {
                Log.e(TAG, error.getMessage());
            }
        });

        // prepare to play
        player.prepare(source);

       // videoView = (VideoView) findViewById(R.id.videoView);
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.producerExoPlayer);
        simpleExoPlayerView.setPlayer(player);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
