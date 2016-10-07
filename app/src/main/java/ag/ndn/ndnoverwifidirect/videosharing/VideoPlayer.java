package ag.ndn.ndnoverwifidirect.videosharing;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.mp4.Track;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;

import java.util.LinkedList;

/**
 * Simple singleton class representing a video player in NDNOverWifiD.
 * Note that a ExoPlayer is returned, rather than an instance of
 * VideoPlayer.
 *
 * Created by allengong on 9/20/16.
 */
public class VideoPlayer {

    // constants
    public static final byte EOF_FLAG = 0;
    public static final byte DATA_FLAG = 1;
    public static final byte RESUME_FLAG = 2;
    public static final byte PAUSE_FLAG = 3;
    public static final byte SEEK_FLAG = 4;

    // singleton
    private static SimpleExoPlayer player = null;

    //ctrl
    public static Handler handler;
    private static BandwidthMeter bandwidthMeter;
    private static TrackSelection.Factory videoTrackSelectionFactory;
    private static TrackSelector trackSelector;
    private static LoadControl loadControl;

    // specific to the currently playing resource
    private boolean isLocal;    // if the resource to load is local
    private String videoUri;    // the Uri of the video currently displayed by this VideoPlayer

    private VideoPlayer() {}

    public static SimpleExoPlayer getPlayer(Context context) {

        // if instance does not exist, create one
        if (player == null) {
            handler = new Handler();
            bandwidthMeter = new DefaultBandwidthMeter();
            videoTrackSelectionFactory =
                    new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);

            trackSelector =
                    new DefaultTrackSelector(handler, videoTrackSelectionFactory);

            loadControl = new DefaultLoadControl();
            player = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
        }

        return player;
    }
}
