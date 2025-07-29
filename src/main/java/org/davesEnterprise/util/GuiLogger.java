package org.davesEnterprise.util;

import org.davesEnterprise.Gui;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiLogger {
    private final Logger logger;
    private Gui gui;

    public GuiLogger(Class<?> clazz, Gui gui) {
        this.logger = Logger.getLogger(clazz.getSimpleName());
        this.gui = gui;
    }

    public void setGui(Gui gui) {
        this.gui = gui;
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warn(String message) {
        log(Level.WARNING, message);
    }

    public void error(String message) {
        log(Level.SEVERE, message);
    }

    private void log(Level level, String message) {
        logger.log(level, message);
        if (gui != null) {
            gui.logs.append((gui.logs.getText().isBlank() ? "" : "\n") + message);
            JScrollBar vertical = gui.logs_scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        }
    }

    public Logger getLogger() {
        return logger;
    }
}