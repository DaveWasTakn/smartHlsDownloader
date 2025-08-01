package org.davesEnterprise.download;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import org.apache.commons.io.FileUtils;
import org.davesEnterprise.Gui;
import org.davesEnterprise.enums.CurrentState;
import org.davesEnterprise.enums.SegmentValidation;
import org.davesEnterprise.network.NetworkUtil;
import org.davesEnterprise.network.OutOfRetriesException;
import org.davesEnterprise.util.GuiLogger;
import org.davesEnterprise.util.TransposeGatherer;
import org.davesEnterprise.util.VideoUtils;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class HlsDownloader implements Downloader {
    private static final GuiLogger LOGGER = GuiLogger.get();

    private final List<MediaPlaylist> playlists;
    private final int concurrentDownloads;
    private final int concurrentValidations;
    private final SegmentValidation segmentValidation;
    private final boolean resume;
    private final Path outputFilePath;
    private final URI playlistLocation;
    private final Path outputDir;
    private final Path segmentsDir;
    private final int retries;
    private final boolean keepSegments;

    private final int maxSegments;
    private final Set<Integer> downloadedSegments = ConcurrentHashMap.newKeySet();
    private final Set<Integer> validatedSegments = ConcurrentHashMap.newKeySet();

    private final Gui gui;

    public HlsDownloader(List<MediaPlaylist> playlists, String playlistLocation, Path outputDir, int retries, int concurrentDownloads, int concurrentValidations, SegmentValidation segmentValidation, String fileName, boolean resume, boolean keepSegments, Gui gui) {
        this.playlists = playlists;
        this.concurrentDownloads = concurrentDownloads;
        this.concurrentValidations = concurrentValidations;
        this.retries = retries;
        this.segmentValidation = segmentValidation;
        this.resume = resume;
        this.keepSegments = keepSegments;

        this.gui = gui != null ? gui : new Gui();
        this.maxSegments = this.playlists.getFirst().mediaSegments().size();
        this.initGui();

        try {
            if (NetworkUtil.isURL(playlistLocation)) {
                this.playlistLocation = new URI(playlistLocation);
            } else {
                this.playlistLocation = Path.of(playlistLocation).toUri();
            }
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
        this.outputFilePath = this.outputDir.resolve(fileName.matches(".+\\.\\S+$") ? fileName : fileName + ".mp4");
    }

    private void initGui() {
        SwingUtilities.invokeLater(() -> {
            this.gui.progressDownload.setMinimum(0);
            this.gui.progressDownload.setMaximum(this.maxSegments);

            this.gui.progressValidation.setMinimum(0);
            this.gui.progressValidation.setMaximum(this.maxSegments);
        });
    }


    public void start() {
        final List<List<MediaSegment>> segmentsTransposed = this.playlists.stream()
                .map(MediaPlaylist::mediaSegments)
                .filter(x -> x.size() == this.maxSegments)
                .gather(new TransposeGatherer<>())
                .toList();

        this.gui.start.setEnabled(false);
        this.gui.currentState.setText(CurrentState.DOWNLOADING.toString());

        download(segmentsTransposed);

        this.gui.currentState.setText(CurrentState.MERGING.toString());

        boolean finished = mergePlaylist();

        if (finished) {
            this.gui.currentState.setText(CurrentState.FINISHED.toString());
            if (!this.keepSegments) {
                this.cleanupFiles();
            } else {
                LOGGER.info("Done! Video created at: " + this.outputFilePath.toAbsolutePath());
            }
        } else {
            this.gui.currentState.setText(CurrentState.ERROR.toString());
        }
    }

    private void cleanupFiles() {
        Path fileName = this.outputFilePath.getFileName();
        Path parentDir = this.outputDir.getParent();
        Path tempDir = outputDir.resolveSibling(outputDir.getFileName() + "_delme");
        Path targetFile = parentDir.resolve(fileName);
        try {
            Files.move(this.outputDir, tempDir);

            Files.copy(tempDir.resolve(fileName), targetFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Done! Video created at: " + targetFile.toAbsolutePath());

            FileUtils.deleteDirectory(tempDir.toFile());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void download(List<List<MediaSegment>> segmentsTransposed) {
        Semaphore downloadSem = new Semaphore(this.concurrentDownloads);
        Semaphore validationSem = new Semaphore(this.concurrentValidations);

        final Phaser phaser = new Phaser(1);

        try (ExecutorService downloadExecutor = Executors.newVirtualThreadPerTaskExecutor();
             ExecutorService validationExecutor = Executors.newVirtualThreadPerTaskExecutor()
        ) {

            for (int j = 0; j < this.maxSegments; j++) {
                final int i = j;
                Runnable runnable = () -> downloadTask(
                        segmentsTransposed.get(i),
                        i,
                        downloadSem,
                        validationSem,
                        downloadExecutor,
                        validationExecutor,
                        3,
                        phaser);
                phaser.register();
                CompletableFuture.runAsync(runnable, downloadExecutor);
            }

            phaser.arriveAndAwaitAdvance();
        }
    }

    private void downloadTask(
            List<MediaSegment> segment,
            int index,
            Semaphore downloadSem,
            Semaphore validationSem,
            ExecutorService downloadExecutor,
            ExecutorService validationExecutor,
            int validationRetries,
            Phaser phaser
    ) {
        try {
            downloadSem.acquire();
            this.downloadSegment(segment.getFirst(), index, segment.subList(1, segment.size()));

            if (this.segmentValidation != SegmentValidation.NONE) {
                phaser.register();
                CompletableFuture<Void> validationFuture = CompletableFuture.runAsync(
                        () -> this.validationTask(segment, index, downloadSem, validationSem, downloadExecutor, validationExecutor, validationRetries, phaser),
                        validationExecutor
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            phaser.arriveAndDeregister();
            downloadSem.release();
        }
    }

    private void validationTask(
            List<MediaSegment> segment,
            int index,
            Semaphore downloadSem,
            Semaphore validationSem,
            ExecutorService downloadExecutor,
            ExecutorService validationExecutor,
            int validationRetries,
            Phaser phaser
    ) {
        try {
            validationSem.acquire();
            boolean isValid = this.validateSegment(this.segmentsDir.resolve(HlsDownloader.formatSegmentIndex(index, this.maxSegments)), this.segmentValidation);
            if (!isValid && validationRetries > 0) {    // retry downloading and validating for validationRetries many times
                LOGGER.warn("Segment " + index + " is invalid! Retrying download! (validationRetries left: " + validationRetries + ")");
                this.downloadedSegments.remove(index);
                phaser.register();
                CompletableFuture<Void> downloadFuture = CompletableFuture.runAsync(
                        () -> downloadTask(segment, index, downloadSem, validationSem, downloadExecutor, validationExecutor, validationRetries - 1, phaser),
                        downloadExecutor
                );
            }
            this.validatedSegments.add(index);
            logProgress();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            phaser.arriveAndDeregister();
            validationSem.release();
        }
    }

    private void logProgress() {
        LOGGER.info("Download > || " + formatProgress(this.downloadedSegments.size(), this.maxSegments) + " || " + formatProgress(this.validatedSegments.size(), this.maxSegments) + " || < Validation");
        SwingUtilities.invokeLater(() -> {
            this.gui.progressDownload.setValue(this.downloadedSegments.size());
            this.gui.progressValidation.setValue(this.validatedSegments.size());
        });
    }

    private String formatProgress(int current, int max) {
        return String.format("%5.1f%%", ((float) current / max) * 100);
    }

    private boolean validateSegment(Path segmentPath, SegmentValidation segmentValidation) {
        return switch (segmentValidation) {
            case NONE -> true;
            case METADATA -> VideoUtils.checkSegmentMetadata(segmentPath);
            case DECODE -> VideoUtils.checkSegmentMetadata(segmentPath) && VideoUtils.checkSegmentDecoding(segmentPath);
        };
    }

    private boolean mergePlaylist() {
        Path newPlaylist = VideoUtils.adjustPlaylist(this.playlists.getFirst(), this.segmentsDir);
        return VideoUtils.mergePlaylist(newPlaylist, this.segmentsDir, this.outputFilePath);
    }

    private void downloadSegment(MediaSegment segment, int index, List<MediaSegment> alternatives) {
        // Filepath:
        if (!NetworkUtil.isURL(String.valueOf(this.playlistLocation)) && !NetworkUtil.isURL(segment.uri())) {
            // The playlistLocation is not a URL, i.e., it must be a path, and the segment is either a relative URL (which would ofc be invalid) or a Path to the segment (either relative or absolute)
            try {
                URI segmentUri = new URI(segment.uri());
                if (!segmentUri.isAbsolute()) {
                    segmentUri = this.playlistLocation.resolve(segment.uri());
                }
                Path source = Path.of(segmentUri);
                Path target = segmentsDir.resolve(formatSegmentIndex(index, this.maxSegments));
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

                this.downloadedSegments.add(index);
                logProgress();
                return;
            } catch (URISyntaxException | IOException e) {
                LOGGER.error("Failed to copy segment " + index + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        // URL:
        try {
            URI segmentUri = new URI(segment.uri());
            if (!segmentUri.isAbsolute()) {
                segmentUri = this.playlistLocation.resolve(segmentUri);
            }

            try {
                NetworkUtil.downloadResource(segmentUri.toURL(), segmentsDir, formatSegmentIndex(index, this.maxSegments), this.retries);

                this.downloadedSegments.add(index);
                logProgress();
            } catch (OutOfRetriesException e) {
                LOGGER.warn("Download of media segment " + index + " failed - out of retries");
                if (!alternatives.isEmpty()) {
                    LOGGER.info("Trying to download a lower quality media segment " + index + " instead");
                    List<MediaSegment> nextAlternatives = alternatives.size() > 1 ? alternatives.subList(1, alternatives.size()) : Collections.emptyList();
                    this.downloadSegment(alternatives.getFirst(), index, nextAlternatives);
                } else {
                    LOGGER.warn("Download of media segment " + index + " failed - no more alternatives available to try!");
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