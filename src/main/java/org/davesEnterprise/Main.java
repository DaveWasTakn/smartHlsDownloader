package org.davesEnterprise;

import org.davesEnterprise.download.DownloaderBuilder;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {

        new DownloaderBuilder().setPlaylist(args[0]);

    }

}