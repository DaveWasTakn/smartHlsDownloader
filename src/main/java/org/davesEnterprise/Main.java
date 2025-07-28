package org.davesEnterprise;

import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import org.davesEnterprise.download.Downloader;
import org.davesEnterprise.download.DownloaderBuilder;
import org.davesEnterprise.util.Args;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // TODO GUI :-)
        // TODO adaptiveness ? ie lower quality segments

        if (args.length > 0) {
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
            JFrame frame = new JFrame("GuiForm");
            frame.setMinimumSize(new Dimension(0, 0));
            GuiForm guiForm = new GuiForm();
            guiForm.setMinimumSize(new Dimension(0, 0));
            frame.setContentPane(guiForm.mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        });

    }

}