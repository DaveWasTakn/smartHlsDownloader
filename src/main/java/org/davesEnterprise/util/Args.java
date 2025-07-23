package org.davesEnterprise.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Args {

    private static Args INSTANCE;

    @Parameter(
            names = {"-c", "--concurrency"},
            description = "Number of concurrent segments to download"
    )
    public int concurrency = 8;

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
            names = {"--continue"},
            description = "Continue downloading in an existing output directory"
    )
    public boolean resume = false;

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
        JCommander.newBuilder()
                .addObject(INSTANCE)
                .build()
                .parse(argv);

        if (INSTANCE.playlistUrl == null && INSTANCE.mainParameter == null) {
            throw new ParameterException("Missing required input parameter!");
        } else if (INSTANCE.playlistUrl == null) {
            INSTANCE.playlistUrl = INSTANCE.mainParameter;
        }
    }
}
