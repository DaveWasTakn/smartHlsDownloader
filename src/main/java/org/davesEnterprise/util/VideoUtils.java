package org.davesEnterprise.util;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VideoUtils {

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

        /*
        How to do re-muxing with FFmpeg?

For each .ts segment, you run:

ffmpeg -i segment123.ts -c copy -bsf:v h264_mp4toannexb -f mpegts fixed_segment123.ts

    -i segment123.ts — input file.

    -c copy — copy audio and video streams without re-encoding (fast and no quality loss).

    -bsf:v h264_mp4toannexb — bitstream filter that fixes H.264 video stream formatting inside TS containers.

    -f mpegts — output format is MPEG-TS.

    fixed_segment123.ts — the new "cleaned" segment.

What does this achieve?

    Fixes timestamp discontinuities.

    Ensures every segment starts cleanly (ideally on keyframes).

    Fixes corrupted or inconsistent stream headers.

    Results in segments that can be concatenated smoothly without skipping.
         */

        try {
            Process process = new ProcessBuilder("ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", fileList.toAbsolutePath().toString(), "-c", "copy", outputFilePath.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .directory(workingDirectory.toFile())
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("FFmpeg exited with code: " + exitCode);
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
            Process process = new ProcessBuilder(
//                    "ffmpeg", "-y",
//                    "-allowed_extensions", "ALL",
//                    "-f", "hls",
//                    "-i", newPlaylistPath.toAbsolutePath().toString(),
//                    "-c", "copy", videoFilePath.toAbsolutePath().toString()
//                    "ffmpeg", "-y",
//                    "-allowed_extensions", "ALL",
//                    "-f", "hls",
//                    "-i", newPlaylistPath.toAbsolutePath().toString(),
//                    "-c:v", "libx264",
//                    "-preset", "fast",
//                    "-crf", "23",
//                    "-c:a", "aac",
//                    "-b:a", "128k",
//                    videoFilePath.toAbsolutePath().toString()
                    "ffmpeg", "-y",
                    "-allowed_extensions", "ALL",
                    "-f", "hls",
                    "-i", newPlaylistPath.toAbsolutePath().toString(),
                    "-c", "copy",
                    videoFilePath.toAbsolutePath().toString()
            )
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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
