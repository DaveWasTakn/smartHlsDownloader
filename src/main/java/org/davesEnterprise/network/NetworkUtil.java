package org.davesEnterprise.network;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NetworkUtil {

    public static Path obtainFile(String location, Path outputDir, String fileName, int retries) {
        Path filePath;
        if (isURL(location)) {
            try {
                filePath = NetworkUtil.downloadResource(new URI(location).toURL(), outputDir, fileName, retries);
            } catch (URISyntaxException | MalformedURLException e) {
                throw new RuntimeException(e); // TODO how to allow for fkd up urls ? be more lenient !!!!!
            }
        } else {
            Path source = Path.of(location);
            filePath = Path.of(String.valueOf(outputDir), fileName);
            try {
                Files.copy(source, filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return filePath;
    }

    public static boolean isURL(String loc) {
        loc = loc.trim().toLowerCase();
        return loc.startsWith("http:") || loc.startsWith("https:");
    }

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
