package org.davesEnterprise.download;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import org.davesEnterprise.network.NetworkUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class AdaptiveHlsDownloader implements Downloader {
    private final static Logger LOGGER = Logger.getLogger(DownloaderBuilder.class.getSimpleName());

    private final List<MediaPlaylist> playlists;
    private final URI playlistLocation;
    private final boolean playlistIsUrl;
    private final Path segmentsDir;
    private final int retries;

    private int maxSegments;
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
    }


    public void start() {
        List<MediaSegment> mediaSegments = this.playlists.getFirst().mediaSegments();
        this.maxSegments = mediaSegments.size();
        mediaSegments.stream().parallel().forEach(this::downloadSegment);
    }


    private void downloadSegment(MediaSegment segment) {
        if (!this.playlistIsUrl) {
            // TODO generally handle filepaths ? maybe just copy them into the dir or smth
            // and it could be fkd ... could be a file path to the playlist but then the segments need to be downloaded
            // for which they must ofc be absolute urls
            throw new RuntimeException("Playlist is not a url ... not implemented");
        }
        try {
            URI segmentUri = new URI(segment.uri());

            if (!segmentUri.isAbsolute()) {
                segmentUri = this.playlistLocation.resolve(segmentUri);
            }

//            if (NetworkUtil.isURL(segmentUri.toString())) {
            NetworkUtil.downloadResource(segmentUri.toURL(), segmentsDir, segment.uri(), this.retries);
            // TODO catch out of retries exception and implement adaptiveness - get lower res segment instead
//            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final int idx = this.numSegments.incrementAndGet();
        LOGGER.info("Downloaded segment " + idx + " : " + segment.uri());
    }
}

