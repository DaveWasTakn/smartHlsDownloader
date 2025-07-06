package org.davesEnterprise.network;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public class NetworkUtil {

    public static Path downloadResource(URL url, Path outputDir, String fileName, int retries) throws OutOfRetriesException {
        if (retries < 1) {
            throw new OutOfRetriesException("Retries exceeded for resource " + url);
        }

        try {
            return download(url, outputDir, fileName);
        } catch (OutOfRetriesException e) {
            return downloadResource(url, outputDir, fileName, retries - 1);
        }
    }

    public static Path downloadResource(URL url, Path outputDir, int retries) throws OutOfRetriesException {
        return downloadResource(url, outputDir, url.getFile(), retries);
    }

    private static Path download(URL url, Path outputDir, String fileName) {
        try {
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            Path outputPath = outputDir.resolve(fileName);
            FileOutputStream fos = new FileOutputStream(outputPath.toFile());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
            return outputPath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path download(URL url, Path outputDir) {
        return download(url, outputDir, url.getFile());
    }

}
