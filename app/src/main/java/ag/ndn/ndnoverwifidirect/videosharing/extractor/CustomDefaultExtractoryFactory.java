package ag.ndn.ndnoverwifidirect.videosharing.extractor;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor;

/**
 * Custom implementation of the DefaultExtractorFactory class
 * implemented in ExoPlayer. The only difference here is that
 * the currently active mp4 extractor is reachable by a public
 * static variable, for use with customizable seeks.
 *
 * Created by allengong on 10/19/16.
 */

public class CustomDefaultExtractoryFactory implements ExtractorsFactory {

    private Mp4Extractor mp4ExtractorActive = null;

    private DefaultExtractorsFactory factory = new DefaultExtractorsFactory();
    private Extractor[] extractors;

    public CustomDefaultExtractoryFactory() {
        this.extractors = factory.createExtractors();

        for (Extractor e : this.extractors) {
            if (e.getClass() == Mp4Extractor.class) {
                mp4ExtractorActive = (Mp4Extractor) e;
            }
        }

        if (mp4ExtractorActive == null) {
            System.err.println("ERROR WITH GETTING MP4EXTRACTOR!");
        }
    }

    public Mp4Extractor getMp4Extractor() {
        return this.mp4ExtractorActive;
    }


    @Override
    public Extractor[] createExtractors() {
        return this.extractors;
    }
}
