package org.davesEnterprise;

import org.davesEnterprise.download.DownloaderBuilder;
import org.davesEnterprise.enums.SegmentValidation;
import org.davesEnterprise.util.Args;

import javax.swing.*;
import java.nio.file.Path;

public class GuiForm extends JPanel {
    public JPanel mainPanel;

    public JTextArea uri;
    public JTextField outputName;

    public JSpinner downloads;
    public JSpinner validations;
    public JSpinner retries;

    public JComboBox<SegmentValidation> validationType;

    public JProgressBar progressDownload;
    public JProgressBar progressValidation;

    public JButton start;
    public JTextField workingDir;


    public GuiForm() {
        this.downloads.setValue(Args.get().concurrentDownloads);
        this.validations.setValue(Args.get().concurrentValidations);
        this.retries.setValue(Args.get().retries);

        this.workingDir.setText(String.valueOf(defaultWorkingDir()));

        this.validationType.setModel(new DefaultComboBoxModel<>(SegmentValidation.values()));
        this.validationType.setSelectedItem(SegmentValidation.DECODE);

        this.start.addActionListener(_ -> this.start());
    }

    private static Path defaultWorkingDir() {
        return Path.of(System.getProperty("user.home"), "Downloads");
    }

    private void start() {
        Path workingDir;
        if (!this.workingDir.getText().isBlank()) {
            workingDir = Path.of(this.workingDir.getText());
        } else {
            workingDir = defaultWorkingDir();
        }

        String outputName = this.outputName.getText();
        if (outputName.isBlank()) {
            outputName = DownloaderBuilder.getCurrentDateTime();
        }

        new DownloaderBuilder(workingDir.resolve(outputName))
                .setPlaylist(this.uri.getText())
                .setSegmentValidation((SegmentValidation) this.validationType.getSelectedItem())
                .setConcurrentDownloads((Integer) this.downloads.getValue())
                .setConcurrentValidations((Integer) this.validations.getValue())
                .setRetries((Integer) this.retries.getValue())
                .setGuiForm(this)
                .build().start();
    }


}
