package edu.connexion3a8.tools;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class FileOpener {

    public static void openFile(File file) {
        if (file == null || !file.exists()) {
            System.err.println("Cannot open file: File does not exist.");
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
                System.out.println("File opened: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error opening file: " + e.getMessage());
            }
        } else {
            System.err.println("Desktop is not supported on this platform.");
        }
    }
}
