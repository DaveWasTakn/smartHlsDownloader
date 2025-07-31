package org.davesEnterprise.util;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VideoUtils {

    private static final GuiLogger LOGGER = GuiLogger.get();

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
            executeCmd(workingDirectory, cmd, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
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
            LOGGER.info("Executing command: " + String.join(" ", cmd));
            if (executeCmd(workingDir, cmd, false) == 0) {
                LOGGER.info("Done! Video created at: " + videoFilePath.toAbsolutePath());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkSegmentMetadata(Path segmentPath) {
        String[] cmd = {
                "ffprobe",
                "-v", "error",
                "-show_format",
                "-show_streams",
                segmentPath.toAbsolutePath().toString(),
        };
        try {
            return executeCmd(segmentPath.getParent(), cmd, false) == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkSegmentDecoding(Path segmentPath) {
        String[] cmd = {
                "ffmpeg",
                "-v", "error",
                "-xerror",
                "-i", segmentPath.toAbsolutePath().toString(),
                "-f", "null", "-"
        };
        try {
            return executeCmd(segmentPath.getParent(), cmd, false) == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int executeCmd(Path workingDir, String[] cmd, boolean printOutput) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .directory(workingDir.toFile())
                .start();

        if (printOutput) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } else {
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                while (is.read(buffer) != -1) {
                    // discard stream
                }
            }
        }

        return process.waitFor();
    }

}
