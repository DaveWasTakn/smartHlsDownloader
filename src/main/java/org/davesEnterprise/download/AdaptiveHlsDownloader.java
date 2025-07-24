package org.davesEnterprise.download;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import org.davesEnterprise.enums.SegmentValidation;
import org.davesEnterprise.network.NetworkUtil;
import org.davesEnterprise.network.OutOfRetriesException;
import org.davesEnterprise.util.TransposeGatherer;
import org.davesEnterprise.util.VideoUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class AdaptiveHlsDownloader implements Downloader {
    private final static Logger LOGGER = Logger.getLogger(DownloaderBuilder.class.getSimpleName());

    private final List<MediaPlaylist> playlists;
    private final int concurrentDownloads;
    private final int concurrentValidations;
    private final SegmentValidation segmentValidation;
    private final boolean resume;
    private final String fileName;
    private final URI playlistLocation;
    private final boolean playlistIsUrl;
    private final Path outputDir;
    private final Path segmentsDir;
    private final int retries;

    private final int maxSegments;
    private final Set<Integer> downloadedSegments = ConcurrentHashMap.newKeySet();

    public AdaptiveHlsDownloader(List<MediaPlaylist> playlists, String playlistLocation, Path outputDir, int retries, int concurrentDownloads, int concurrentValidations, SegmentValidation segmentValidation, String fileName, boolean resume) {
        this.playlists = playlists;
        this.concurrentDownloads = concurrentDownloads;
        this.concurrentValidations = concurrentValidations;
        this.segmentValidation = segmentValidation;
        this.resume = resume;
        this.fileName = fileName;
        try {
            this.playlistLocation = new URI(playlistLocation);
            this.playlistIsUrl = NetworkUtil.isURL(playlistLocation);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.outputDir = outputDir;
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
        final List<List<MediaSegment>> segmentsTransposed = this.playlists.stream()
                .map(MediaPlaylist::mediaSegments)
                .filter(x -> x.size() == this.maxSegments)
                .gather(new TransposeGatherer<>())
                .toList();

        download(segmentsTransposed);
        mergePlaylist();
    }

    private void download(List<List<MediaSegment>> segmentsTransposed) {
        Semaphore downloadSem = new Semaphore(this.concurrentDownloads);
        Semaphore validationSem = new Semaphore(this.concurrentValidations);

        List<CompletableFuture<Void>> validationFutures = new ArrayList<>();

        try (ExecutorService downloadExecutor = Executors.newVirtualThreadPerTaskExecutor();
             ExecutorService validationExecutor = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            downloadExecutor.invokeAll(
                    IntStream.range(0, this.maxSegments)
                            .<Callable<Void>>mapToObj(i -> () -> {
                                try {
                                    downloadSem.acquire();
                                    final List<MediaSegment> segment = segmentsTransposed.get(i);
                                    this.downloadSegment(segment.getFirst(), i, segment.subList(1, segment.size()));

                                    CompletableFuture<Void> validationFuture = CompletableFuture.runAsync(
                                            () -> {
                                                try {
                                                    validationSem.acquire();
//                                                    LOGGER.info("Validating segment " + i);
                                                    if (!this.validateSegment(this.segmentsDir.resolve(AdaptiveHlsDownloader.formatSegmentIndex(i, this.maxSegments)), this.segmentValidation)) {
                                                        LOGGER.warning("Segment " + i + " is invalid! Retrying download");
                                                        this.downloadSegment(segment.getFirst(), i, segment.subList(1, segment.size()));
                                                    }
                                                } catch (InterruptedException e) {
                                                    Thread.currentThread().interrupt();
                                                    throw new RuntimeException(e);
                                                } finally {
                                                    validationSem.release();
                                                }
                                            },
                                            validationExecutor);
                                    validationFutures.add(validationFuture);

                                    return null;
                                } finally {
                                    downloadSem.release();
                                }
                            })
                            .toList()
            );
            validationFutures.forEach(CompletableFuture::join);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private boolean validateSegment(Path segmentPath, SegmentValidation segmentValidation) {
        return switch (segmentValidation) {
            case NONE -> true;
            case METADATA -> VideoUtils.checkSegmentMetadata(segmentPath);
            case DECODE -> VideoUtils.checkSegmentMetadata(segmentPath) && VideoUtils.checkSegmentDecoding(segmentPath);
        };
    }

    private void mergePlaylist() {
        Path newPlaylist = VideoUtils.adjustPlaylist(this.playlists.getFirst(), this.segmentsDir);
        VideoUtils.mergePlaylist(newPlaylist, this.segmentsDir, this.outputDir.resolve(this.fileName + ".mp4"));
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
                NetworkUtil.downloadResource(segmentUri.toURL(), segmentsDir, formatSegmentIndex(index, this.maxSegments), this.retries);

                this.downloadedSegments.add(index);
                LOGGER.info("|| " + String.format("%5.1f%%", ((float) this.downloadedSegments.size() / this.maxSegments) * 100) + " || - Downloaded segment " + (index + 1));
            } catch (OutOfRetriesException e) {
                LOGGER.warning("Download of media segment" + (index + 1) + " failed - out of retries");
                if (!alternatives.isEmpty()) {
                    LOGGER.info("Trying to download a lower quality media segment " + (index + 1) + " instead");
                    List<MediaSegment> nextAlternatives = alternatives.size() > 1 ? alternatives.subList(1, alternatives.size()) : Collections.emptyList();
                    this.downloadSegment(alternatives.getFirst(), index, nextAlternatives);
                } else {
                    LOGGER.warning("Download of media segment " + (index + 1) + " failed - no more alternatives available to try!");
                    // TODO what now ... how to handle ? create empty file ? or check when creating the new playlist ?
                }
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatSegmentIndex(int index, int maxSegments) {
        return String.format("%0" + String.valueOf(maxSegments).length() + "d", index);
    }


}

