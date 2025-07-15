package org.davesEnterprise;

import org.davesEnterprise.download.Downloader;
import org.davesEnterprise.download.DownloaderBuilder;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {

        Downloader downloader = new DownloaderBuilder()
                .setRetries(10)
                .setPlaylist(args[0])
                .create();

        downloader.start();

    }

}