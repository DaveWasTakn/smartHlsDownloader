package org.davesEnterprise.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.davesEnterprise.enums.SegmentValidation;

public class Args {

    private static Args INSTANCE;

    public SegmentValidation segmentValidation;

    @Parameter(
            names = {"-c", "--concurrent-downloads"},
            description = "Number of concurrent segments to download"
    )
    public int concurrentDownloads = 10;

    @Parameter(
            names = {"-cv", "--concurrent-validations"},
            description = "Number of concurrent segments to validate"
    )
    public int concurrentValidations = 10;

//    @Parameter( TODO ?
//            names = {"--resume"},
//            description = "Resume downloading in an existing output directory"
//    )
//    public boolean resume = false;

    @Parameter(
            names = {"-k", "--keep-segments"},
            description = "Keep all individual segments"
    )
    public boolean keepSegments = false;

    @Parameter(
            names = {"-o", "--output"},
            description = "Output file or directory name"
    )
    public String output;

    @Parameter(
            names = {"-i", "--input"},
            description = "URL or URI of the HLS playlist"
    )
    public String playlistUrl;

    @Parameter(
            description = "URL or URI of the HLS playlist"
    )
    public String mainParameter;

    @Parameter(
            names = {"-sv", "--skip-validation"},
            description = "Whether to skip validation of each segment"
    )
    public boolean skipValidation = false;

    @Parameter(
            names = {"-ev", "--extra-validation"},
            description = "Whether to use ffmpeg to decode each segment for additional validation"
    )
    public boolean extraValidation = false;

    @Parameter(
            names = {"-r", "--retries"},
            description = "Number of times to retry failed segment downloads"
    )
    public int retries = 100;

    @Parameter(
            names = {"-h", "--help"},
            help = true,
            description = "Display help information"
    )
    public boolean help = false;

    private Args() {
    }

    public static Args get() {
        if (INSTANCE == null) {
            INSTANCE = new Args();
        }
        return INSTANCE;
    }

    public static void init(String[] argv) {
        INSTANCE = new Args();
        JCommander jc = JCommander.newBuilder()
                .addObject(INSTANCE)
                .build();

        try {
            jc.parse(argv);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (INSTANCE.help) {
            jc.usage();
            System.exit(0);
        }

        if (INSTANCE.playlistUrl == null && INSTANCE.mainParameter == null) {
            jc.usage();
            throw new ParameterException("Missing required input parameter!");
        } else if (INSTANCE.playlistUrl == null) {
            INSTANCE.playlistUrl = INSTANCE.mainParameter;
        }

        INSTANCE.segmentValidation = INSTANCE.skipValidation ? SegmentValidation.NONE : (INSTANCE.extraValidation ? SegmentValidation.DECODE : SegmentValidation.METADATA);
    }
}
