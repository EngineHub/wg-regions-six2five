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

package com.sk89q.worldguard.six2five.util;

import com.google.common.io.Closer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Swing utility methods.
 */
public final class SwingHelper {

    private static final ClipboardOwner clipboardOwner = new ClipboardOwner() {
        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {

        }
    };

    private SwingHelper() {
    }

    public static String htmlEscape(String str) {
        return str.replace(">", "&gt;")
                .replace("<", "&lt;")
                .replace("&", "&amp;");
    }

    public static void setClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), clipboardOwner);
    }

    public static void showErrorDialog(Component parentComponent, String message, String title, Throwable throwable) {
        String detailsText = null;

        // Get a string version of the exception and use that for
        // the extra details text
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            detailsText = sw.toString();
        }

        showMessageDialog(parentComponent,
                message, title,
                detailsText, JOptionPane.ERROR_MESSAGE);
    }

    public static void showMessageDialog(final Component parentComponent,
                                         final String message,
                                         final String title,
                                         final String detailsText,
                                         final int messageType) {

        if (SwingUtilities.isEventDispatchThread()) {
            // To force the label to wrap, convert the message to broken HTML
            String htmlMessage = "<html><div style=\"width: 250px\">" + htmlEscape(message);

            JPanel panel = new JPanel(new BorderLayout(0, detailsText != null ? 20 : 0));

            // Add the main message
            panel.add(new JLabel(htmlMessage), BorderLayout.NORTH);

            // Add the extra details
            if (detailsText != null) {
                JTextArea textArea = new JTextArea("To report this error, please provide:\n\n" + detailsText);
                JLabel tempLabel = new JLabel();
                textArea.setFont(tempLabel.getFont());
                textArea.setBackground(tempLabel.getBackground());
                textArea.setTabSize(2);
                textArea.setEditable(false);
                textArea.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(350, 120));
                panel.add(scrollPane, BorderLayout.CENTER);
            }

            JOptionPane.showMessageDialog(
                    parentComponent, panel, title, messageType);
        } else {
            // Call method again from the Event Dispatch Thread
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        showMessageDialog(
                                parentComponent, message, title,
                                detailsText, messageType);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static BufferedImage readIconImage(Class<?> clazz, String path) {
        Closer closer = Closer.create();
        try {
            try {
                InputStream in = closer.register(clazz.getResourceAsStream(path));
                if (in != null) {
                    return ImageIO.read(in);
                }
            } finally {
                closer.close();
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    public static void setIconImage(JFrame frame, Class<?> clazz, String path) {
        BufferedImage image = readIconImage(clazz, path);
        if (image != null) {
            frame.setIconImage(image);
        }
    }

    public static void focusLater(final Component component) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (component instanceof JTextComponent) {
                    ((JTextComponent) component).selectAll();
                }
                component.requestFocusInWindow();
            }
        });
    }

}
