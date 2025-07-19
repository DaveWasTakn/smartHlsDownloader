package org.davesEnterprise.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class VideoUtils {

    public static void mergeSegments(Path workingDirectory, Path segmentsDirectory, Path outputFilePath) {
        Path fileList = VideoUtils.createFileList(segmentsDirectory);

        try {
            Process process = new ProcessBuilder("ffmpeg", "-f", "concat", "-safe", "0", "-i", fileList.toAbsolutePath().toString(), "-c", "copy", outputFilePath.toAbsolutePath().toString())
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


}
