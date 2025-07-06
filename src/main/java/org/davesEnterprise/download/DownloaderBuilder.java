package org.davesEnterprise.download;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MultivariantPlaylist;
import io.lindstrom.m3u8.model.Variant;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.lindstrom.m3u8.parser.MultivariantPlaylistParser;

import java.io.IOException;
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

    private List<MediaPlaylist> playlists = new ArrayList<>();

    public DownloaderBuilder() {
        this(new SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(new Date()));
    }

    public DownloaderBuilder(String outputDir) {
        this(Path.of(outputDir));
    }

    public DownloaderBuilder(Path outputDir) {
        this.outputDir = outputDir;

        try {
            Files.createDirectories(this.outputDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DownloaderBuilder setPlaylist(Path playlistPath) throws RuntimeException {
        try {
            MultivariantPlaylist multivariantPlaylist = new MultivariantPlaylistParser().readPlaylist(playlistPath);
            System.out.println(multivariantPlaylist);
            this.playlists = multivariantPlaylist.variants().stream()
                    .sorted(Comparator.comparingLong(Variant::bandwidth))
                    .map(Variant::uri)
                    .map(Path::of) // TODO should be URL, right? and then download it and then i have the path for the parseMediaPlaylist method?
                    .map(DownloaderBuilder::parseMediaPlaylist)
                    .toList();
        } catch (IOException e) {
            LOGGER.warning("Supplied playlist is a MediaPlaylist, i.e., does not contain multiple streams to choose from. Therefore, adaptive quality switching is not possible.");
            this.playlists.add(parseMediaPlaylist(playlistPath));
        }
        return this;
    }

    private static MediaPlaylist parseMediaPlaylist(Path playlistPath) {
        try {
            return new MediaPlaylistParser().readPlaylist(playlistPath);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

//    public Downloader create() {
//        // TODO check which Downloader to create
////        return new HLSDownloader(playlists);
//    }

}
