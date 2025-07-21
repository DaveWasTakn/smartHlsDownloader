package org.davesEnterprise;

import org.davesEnterprise.download.Downloader;
import org.davesEnterprise.download.DownloaderBuilder;

public class Main {
    public static void main(String[] args) {

        // TODO proper argument handling and stuff
        // TODO GUI :-)

        Downloader downloader = new DownloaderBuilder()
                .setRetries(100)
                .setPlaylist(args[0])
                .create();

        downloader.start();

    }

}