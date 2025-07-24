package org.davesEnterprise;

import org.davesEnterprise.download.Downloader;
import org.davesEnterprise.download.DownloaderBuilder;
import org.davesEnterprise.util.Args;

public class Main {
    public static void main(String[] args) {
        // TODO GUI :-)
        // TODO adaptiveness ? ie lower quality segments

        Args.init(args);
        Args arguments = Args.get();

        Downloader downloader = new DownloaderBuilder(arguments.output)
                .setRetries(arguments.retries)
                .setPlaylist(arguments.playlistUrl)
                .setConcurrentDownloads(arguments.concurrentDownloads)
                .setConcurrentValidations(arguments.concurrentValidations)
                .setResume(arguments.resume) // TODO implement resume functionality ?
                .setSegmentValidation(arguments.segmentValidation)
                .build();

        downloader.start();

    }

}