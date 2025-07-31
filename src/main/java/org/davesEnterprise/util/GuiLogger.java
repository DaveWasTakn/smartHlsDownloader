package org.davesEnterprise.util;

import org.davesEnterprise.Gui;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiLogger {
    private static GuiLogger INSTANCE;
    private final Logger logger;
    private final Gui gui;

    public static void init(Gui gui) {
        if (INSTANCE == null) {
            INSTANCE = new GuiLogger(gui);
        }
    }

    private GuiLogger(Gui gui) {
        this.logger = Logger.getAnonymousLogger();
        this.gui = gui;
    }

    public static GuiLogger get() {
        return INSTANCE;
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
}