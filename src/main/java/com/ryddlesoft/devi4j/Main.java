package com.ryddlesoft.devi4j;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //FlatLightLaf.setup();
            FlatDarkLaf.setup();
            MainFrame mainFrame = new MainFrame();
            mainFrame.initialize();
        });
    }
}