package org.davesEnterprise;

import org.davesEnterprise.download.Downloader;
import org.davesEnterprise.download.DownloaderBuilder;
import org.davesEnterprise.util.Args;

public class Main {
    public static void main(String[] args) {
        Args.init(args);

        // TODO GUI :-)

        Downloader downloader = new DownloaderBuilder(Args.get().output)
                .setRetries(Args.get().retries)
                .setPlaylist(Args.get().playlistUrl)
                .setConcurrency(Args.get().concurrency)
                .setResume(Args.get().resume)
                .build();

        downloader.start();

    }

}