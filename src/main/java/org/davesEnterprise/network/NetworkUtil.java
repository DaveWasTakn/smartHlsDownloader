package org.davesEnterprise.network;

import org.davesEnterprise.download.DownloaderBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class NetworkUtil {

    private final static Logger LOGGER = Logger.getLogger(DownloaderBuilder.class.getSimpleName());

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
        } catch (IOException e) {
            LOGGER.warning("Failed attempt to download " + url + ": " + e.getMessage());
            return downloadResource(url, outputDir, fileName, retries - 1);
        }
    }

    private static Path download(URL url, Path outputDir, String fileName) throws IOException {
        Path outputPath = outputDir.resolve(fileName);

        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(2_000);
        conn.setReadTimeout(2_000);

        try (
                ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
                FileOutputStream fos = new FileOutputStream(outputPath.toFile())
        ) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }

        return outputPath;
    }

}
