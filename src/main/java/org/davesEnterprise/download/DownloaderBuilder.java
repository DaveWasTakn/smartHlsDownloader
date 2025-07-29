package org.davesEnterprise.download;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MultivariantPlaylist;
import io.lindstrom.m3u8.model.Variant;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.lindstrom.m3u8.parser.MultivariantPlaylistParser;
import org.davesEnterprise.Gui;
import org.davesEnterprise.enums.SegmentValidation;
import org.davesEnterprise.network.NetworkUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class DownloaderBuilder {

    private final static Logger LOGGER = Logger.getLogger(DownloaderBuilder.class.getSimpleName());

    private final Path outputDir;
    private final String fileName;
    private final Path playlistsOutputDir;
    private int retries = 10;
    private List<MediaPlaylist> playlists = new ArrayList<>();
    private String playlistLocation;
    private int concurrentDownloads;
    private int concurrentValidations;
    private boolean resume;
    private SegmentValidation segmentValidation;
    private Gui gui;


    public DownloaderBuilder() {
        this(getCurrentDateTime());
    }

    public DownloaderBuilder(String output) {
        this(output != null ? Path.of(output) : Path.of(getCurrentDateTime()));
    }

    public DownloaderBuilder(Path outputDir) {
        this.fileName = outputDir.getFileName().toString();
        this.outputDir = outputDir.toAbsolutePath();
        this.playlistsOutputDir = outputDir.resolve("playlists");

        try {
            Files.createDirectories(this.playlistsOutputDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
    }

    public DownloaderBuilder setPlaylist(String playlistLocation) throws RuntimeException {
        this.playlistLocation = playlistLocation;
        try {
            final Path playlistPath = obtainPlaylist(playlistLocation, "multiVariantPlaylist.txt");
            final MultivariantPlaylist multivariantPlaylist = new MultivariantPlaylistParser().readPlaylist(playlistPath);
            System.out.println(multivariantPlaylist); // TODO print some info/stats i.e. how many variants or smth
            this.playlists = multivariantPlaylist.variants().stream()
                    .sorted(Comparator.comparingLong(Variant::bandwidth).reversed())
                    .map(variant -> {
                        String variantUri = variant.uri();
                        if (NetworkUtil.isURL(playlistLocation) && !NetworkUtil.isURL(variantUri)) {
                            variantUri = URI.create(playlistLocation).resolve(variantUri).toString();
                        }
                        return this.parseMediaPlaylist(variantUri, String.valueOf(variant.bandwidth()));
                    })
                    .toList();
        } catch (IOException e) {
            LOGGER.warning("Supplied playlist does not seem to be a MultiVariantPlaylist!");
            final Path playlistPath = obtainPlaylist(playlistLocation, "playlist.txt"); // TODO make consts file somewhere ?
            this.playlists.add(DownloaderBuilder.parseMediaPlaylist(playlistPath));
            LOGGER.warning("Supplied playlist is a MediaPlaylist, i.e., does not contain multiple streams to choose from. Therefore, adaptive quality switching is not possible.");
        }
        return this;
    }

    private MediaPlaylist parseMediaPlaylist(String playlistLocation, String fileName) {
        final Path playlistPath = obtainPlaylist(playlistLocation, fileName);
        return DownloaderBuilder.parseMediaPlaylist(playlistPath);
    }

    private Path obtainPlaylist(String playlistLocation, String fileName) {
        return NetworkUtil.obtainFile(playlistLocation, this.playlistsOutputDir, fileName, this.retries);
    }

    private static MediaPlaylist parseMediaPlaylist(Path playlistPath) {
        try {
            return new MediaPlaylistParser().readPlaylist(playlistPath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public DownloaderBuilder setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public DownloaderBuilder setConcurrentDownloads(int concurrentDownloads) {
        this.concurrentDownloads = concurrentDownloads;
        return this;
    }

    public DownloaderBuilder setConcurrentValidations(int concurrentValidations) {
        this.concurrentValidations = concurrentValidations;
        return this;
    }

    public DownloaderBuilder setResume(boolean resume) {
        this.resume = resume;
        return this;
    }

    public DownloaderBuilder setSegmentValidation(SegmentValidation segmentValidation) {
        this.segmentValidation = segmentValidation;
        return this;
    }

    public DownloaderBuilder setGuiForm(Gui gui) {
        this.gui = gui;
        return this;
    }

    public Downloader build() {
        // TODO check which Downloader to create
        return new AdaptiveHlsDownloader(this.playlists,
                this.playlistLocation,
                this.outputDir,
                this.retries,
                this.concurrentDownloads,
                this.concurrentValidations,
                this.segmentValidation,
                this.fileName,
                this.resume,
                this.gui
        );
    }
}
