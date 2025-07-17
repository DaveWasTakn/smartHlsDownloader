package org.davesEnterprise.download;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import org.davesEnterprise.network.NetworkUtil;
import org.davesEnterprise.network.OutOfRetriesException;
import org.davesEnterprise.util.TransposeGatherer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class AdaptiveHlsDownloader implements Downloader {
    private final static Logger LOGGER = Logger.getLogger(DownloaderBuilder.class.getSimpleName());

    private final List<MediaPlaylist> playlists;
    private final URI playlistLocation;
    private final boolean playlistIsUrl;
    private final Path segmentsDir;
    private final int retries;

    private final int maxSegments;
    private final AtomicInteger numSegments = new AtomicInteger(0);

    public AdaptiveHlsDownloader(List<MediaPlaylist> playlists, String playlistLocation, Path outputDir, int retries) {
        this.playlists = playlists;
        try {
            this.playlistLocation = new URI(playlistLocation);
            this.playlistIsUrl = NetworkUtil.isURL(playlistLocation);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.segmentsDir = outputDir.resolve("segments");
        try {
            Files.createDirectories(segmentsDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.retries = retries;
        this.maxSegments = this.playlists.getFirst().mediaSegments().size();
    }


    public void start() {
        List<List<MediaSegment>> segmentsTransposed = this.playlists.stream()
                .map(MediaPlaylist::mediaSegments)
                .filter(x -> x.size() == this.maxSegments)
                .gather(new TransposeGatherer<>())
                .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.invokeAll(
                    IntStream.range(0, this.maxSegments)
                            .<Callable<Void>>mapToObj(i -> () -> {
                                final List<MediaSegment> segment = segmentsTransposed.get(i);
                                this.downloadSegment(segment.getFirst(), i, segment.subList(1, segment.size()));
                                return null;
                            })
                            .toList()
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void downloadSegment(MediaSegment segment, int index, List<MediaSegment> alternatives) {
        if (!this.playlistIsUrl) {
            // TODO generally handle filepaths ? maybe just copy them into the dir (with index as filename!!) or smth
            // and it could be fkd ... could be a file path to the playlist but then the segments need to be downloaded
            // for which they must ofc be absolute urls
            throw new RuntimeException("Playlist is not a url ... not implemented");
        }
        try {
            URI segmentUri = new URI(segment.uri());

            if (!segmentUri.isAbsolute()) {
                segmentUri = this.playlistLocation.resolve(segmentUri);
            }

            try {
                NetworkUtil.downloadResource(segmentUri.toURL(), segmentsDir, String.format("%0" + String.valueOf(this.maxSegments).length() + "d", index), this.retries);
            } catch (OutOfRetriesException e) {
                if (!alternatives.isEmpty()) {
                    this.downloadSegment(alternatives.getFirst(), index, alternatives.subList(1, alternatives.size()));
                }
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final int idx = this.numSegments.incrementAndGet();
        LOGGER.info("|| " + String.format("%5.1f%%", ((float) idx / this.maxSegments) * 100) + " || - Downloaded segment " + (index + 1));
    }
}

