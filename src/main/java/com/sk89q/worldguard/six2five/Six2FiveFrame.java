/*
 * Six2Five
 * Copyright (C) sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.six2five;

import com.google.common.io.Files;
import com.sk89q.worldguard.six2five.util.MessageLog;
import com.sk89q.worldguard.six2five.util.SwingHelper;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class Six2FiveFrame extends JFrame {

    private final JPanel container;
    private File lastDirectory = new File(".");
    private Thread running;

    public Six2FiveFrame() {
        setTitle("WorldGuard 6->5 Downgrade Tool");

        container = new JPanel();
        container.setLayout(new BorderLayout(0, 8));
        container.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        container.add(new JLabel("<html>This tool converts a WG 6 regions file into a WG 5 regions file."), BorderLayout.SOUTH);

        JButton button = new JButton("Downgrade regions file...");
        container.add(button, BorderLayout.NORTH);

        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openChooser();
            }
        });

        MessageLog logText = new MessageLog(1000, true);
        container.add(logText, BorderLayout.CENTER);
        logText.registerLoggerHandler();

        setSize(600, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        add(container, BorderLayout.CENTER);

        SwingHelper.setIconImage(this, Six2FiveFrame.class, "icon.png");

        SwingHelper.focusLater(button);
    }

    private void openChooser() {
        if (running != null && running.isAlive()) {
            SwingHelper.showMessageDialog(Six2FiveFrame.this, "A conversion task is already running.", "Please wait", null, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();

        fc.setDialogTitle("Choose a regions.yml file to downgrade");
        fc.setCurrentDirectory(lastDirectory);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setDragEnabled(false);
        fc.setMultiSelectionEnabled(false);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String ext = Files.getFileExtension(f.getName());
                return ext.equalsIgnoreCase("yml") || ext.equalsIgnoreCase("yaml");
            }

            @Override
            public String getDescription() {
                return "YAML files";
            }
        });

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            lastDirectory = fc.getCurrentDirectory();
            final File file = fc.getSelectedFile();

            running = new Thread(new Runnable() {
                @Override
                public void run() {
                    RegionsProcessor processor = new RegionsProcessor();
                    try {
                        processor.downgrade(file);
                    } catch (IOException e) {
                        SwingHelper.showErrorDialog(Six2FiveFrame.this, "An error has occurred", "Error", e);
                    }
                }
            });

            running.start();
        }
    }

}
