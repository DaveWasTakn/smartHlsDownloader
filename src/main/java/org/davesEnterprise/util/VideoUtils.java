package org.davesEnterprise.util;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.davesEnterprise.download.DownloaderBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VideoUtils {

    private final static Logger LOGGER = Logger.getLogger(DownloaderBuilder.class.getSimpleName());

    public static Path adjustPlaylist(MediaPlaylist playlist, Path outputDir) {
        List<MediaSegment> segments = playlist.mediaSegments();
        int maxSegments_len = String.valueOf(segments.size()).length();
        MediaPlaylist newPlaylist = MediaPlaylist.builder().from(playlist).mediaSegments(
                IntStream.range(0, playlist.mediaSegments().size())
                        .mapToObj(
                                i -> MediaSegment.builder()
                                        .from(segments.get(i))
                                        .uri(String.format("%0" + maxSegments_len + "d", i))
                                        .build()
                        )
                        .toList()
        ).build();
        MediaPlaylistParser parser = new MediaPlaylistParser();
        try {
            return Files.writeString(outputDir.resolve("newPlaylist.txt"), parser.writePlaylistAsString(newPlaylist), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mergeSegments(Path workingDirectory, Path segmentsDirectory, Path outputFilePath) {
        Path fileList = VideoUtils.createFileList(segmentsDirectory);

        try {
            String[] cmd = {"ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", fileList.toAbsolutePath().toString(), "-c", "copy", outputFilePath.toAbsolutePath().toString()};
            executeCmd(workingDirectory, cmd);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path createFileList(Path workingDirectory) {
        try (Stream<Path> files = Files.list(workingDirectory)) {
            List<String> lines = files.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(s -> !s.equalsIgnoreCase("fileList.txt"))
                    .map(s -> "file '" + s + "'")
                    .sorted()
                    .toList();

            return Files.write(workingDirectory.resolve("fileList.txt"), lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void mergePlaylist(Path newPlaylistPath, Path workingDir, Path videoFilePath) {
        // TODO handle missing segments!!!!!!!!!!
        try {
            String[] cmd = {
                    "ffmpeg", "-y",
                    "-allowed_extensions", "ALL",
                    "-f", "hls",
                    "-i", newPlaylistPath.toAbsolutePath().toString(),
                    "-c", "copy",
                    videoFilePath.toAbsolutePath().toString()
            };
            executeCmd(workingDir, cmd);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void executeCmd(Path workingDir, String[] cmd) throws IOException, InterruptedException {
        LOGGER.info("Executing command: " + String.join(" ", cmd));
        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .directory(workingDir.toFile())
                .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        System.out.println("FFmpeg exited with code: " + exitCode);
    }

}
