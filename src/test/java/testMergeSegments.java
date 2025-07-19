import org.davesEnterprise.util.VideoUtils;

import java.nio.file.Path;

public class testMergeSegments {

    public static void main(String[] args) throws Exception {
        Path p = Path.of("C:\\Data\\projects\\adaptiveHlsDownloader\\2025-07-17 07-14-08");
        VideoUtils.mergeSegments(p, p.resolve("segments"), p.resolve("video.mp4"));
    }

}
