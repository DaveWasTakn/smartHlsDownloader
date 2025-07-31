package org.davesEnterprise;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import org.davesEnterprise.download.Downloader;
import org.davesEnterprise.download.DownloaderBuilder;
import org.davesEnterprise.util.Args;
import org.davesEnterprise.util.GuiLogger;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // TODO adaptiveness ? ie lower quality segments
        // TODO MPEG-DASH support ? https://mvnrepository.com/artifact/io.lindstrom/mpd-parser ?

        if (args.length > 0) {
            GuiLogger.init(new Gui()); // dummy GUI
            Args.init(args);
            Args arguments = Args.get();

            Downloader downloader = new DownloaderBuilder(arguments.output)
                    .setRetries(arguments.retries)
                    .setPlaylist(arguments.playlistUrl)
                    .setConcurrentDownloads(arguments.concurrentDownloads)
                    .setConcurrentValidations(arguments.concurrentValidations)
                    .setResume(arguments.resume) // TODO implement resume functionality ?
                    .setSegmentValidation(arguments.segmentValidation)
                    .build();

            downloader.start();
            System.exit(0);
        }

        try {
            UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("M3U8 HLS Downloader");
            frame.setMinimumSize(new Dimension(375, 350));
            Gui gui = new Gui();
            GuiLogger.init(gui);
            frame.setContentPane(gui.mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });

    }

}