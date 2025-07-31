package org.davesEnterprise.network;

import org.apache.commons.io.FileUtils;
import org.davesEnterprise.util.GuiLogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NetworkUtil {

    private final static GuiLogger LOGGER = GuiLogger.get();

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
            LOGGER.warn("Failed attempt to download " + url + " (" + retries + " retries left) : " + e.getMessage());
            return downloadResource(url, outputDir, fileName, retries - 1);
        }
    }

    private static Path download(URL url, Path outputDir, String fileName) throws IOException {
        Path outputPath = outputDir.resolve(fileName);
        Files.deleteIfExists(outputPath);
        FileUtils.copyURLToFile(url, outputPath.toFile(), 10000, 10000);
        return outputPath;
    }

}
