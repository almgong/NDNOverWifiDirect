package ag.ndn.ndnoverwifidirect.videosharing.datasource;

import android.provider.MediaStore;
import android.util.Log;

import com.google.android.exoplayer2.upstream.DataSource;

import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;

/**
 * Factory that generates ChunkDataSource instances.
 *
 * Created by allengong on 9/29/16.
 */

public class ChunkDataSourceFactory implements DataSource.Factory {

    private final String TAG = "ChunkDataSourceFactory";

    private VideoPlayerBuffer buffer;

    public ChunkDataSourceFactory(VideoPlayerBuffer buffer) {
        this.buffer = buffer;
    }


    @Override
    public DataSource createDataSource() {
        return new ChunkDataSource(buffer);
    }
}

