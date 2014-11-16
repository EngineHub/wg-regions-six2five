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

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A simple message log.
 */
public class MessageLog extends JPanel {

    private static final Logger rootLogger = Logger.getLogger("");
    
    private final int numLines;
    private final boolean colorEnabled;
    
    protected JTextComponent textComponent;
    protected Document document;

    private Handler loggerHandler;
    protected final SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();
    protected final SimpleAttributeSet highlightedAttributes;
    protected final SimpleAttributeSet errorAttributes;
    protected final SimpleAttributeSet infoAttributes;
    protected final SimpleAttributeSet debugAttributes;

    public MessageLog(int numLines, boolean colorEnabled) {
        this.numLines = numLines;
        this.colorEnabled = colorEnabled;
        
        this.highlightedAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(highlightedAttributes, new Color(0xFF7F00));
        
        this.errorAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(errorAttributes, new Color(0xFF0000));
        this.infoAttributes = new SimpleAttributeSet();
        this.debugAttributes = new SimpleAttributeSet();

        setLayout(new BorderLayout());
        
        initComponents();
    }

    private void initComponents() {
        if (colorEnabled) {
            this.textComponent = new JTextPane() {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }
            };
        } else {
            JTextArea text = new JTextArea();
            this.textComponent = text;
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
        }

        textComponent.setFont(new JLabel().getFont());
        textComponent.setEditable(false);
        textComponent.setComponentPopupMenu(TextFieldPopupMenu.INSTANCE);
        DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        document = textComponent.getDocument();
        document.addDocumentListener(new LimitLinesDocumentListener(numLines, true));
        
        JScrollPane scrollText = new JScrollPane(textComponent);
        scrollText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollText.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollText, BorderLayout.CENTER);
    }
    
    public String getPastableText() {
        String text = textComponent.getText().replaceAll("[\r\n]+", "\n");
        text = text.replaceAll("Session ID is [A-Fa-f0-9]+", "Session ID is [redacted]");
        return text;
    }

    public void clear() {
        textComponent.setText("");
    }
    
    /**
     * Log a message given the {@link javax.swing.text.AttributeSet}.
     * 
     * @param line line
     * @param attributes attribute set, or null for none
     */
    public void log(String line, AttributeSet attributes) {
        if (colorEnabled) {
            if (line.startsWith("(!!)")) {
                attributes = highlightedAttributes;
            }
        }
        
        try {
            int offset = document.getLength();
            document.insertString(offset, line,
                    (attributes != null && colorEnabled) ? attributes : defaultAttributes);
            textComponent.setCaretPosition(document.getLength());
        } catch (BadLocationException ble) {
        
        }
    }
    
    /**
     * Get an output stream that can be written to.
     * 
     * @return output stream
     */
    public ConsoleOutputStream getOutputStream() {
        return getOutputStream((AttributeSet) null);
    }
    
    /**
     * Get an output stream with the given attribute set.
     * 
     * @param attributes attributes
     * @return output stream
     */
    public ConsoleOutputStream getOutputStream(AttributeSet attributes) {
        return new ConsoleOutputStream(attributes);
    }

    /**
     * Get an output stream using the give color.
     * 
     * @param color color to use
     * @return output stream
     */
    public ConsoleOutputStream getOutputStream(Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        return getOutputStream(attributes);
    }

    /**
     * Register a global logger listener.
     */
    public void registerLoggerHandler() {
        loggerHandler = new ConsoleLoggerHandler();
        rootLogger.addHandler(loggerHandler);
    }
    
    /**
     * Detach the handler on the global logger.
     */
    public void detachGlobalHandler() {
        if (loggerHandler != null) {
            rootLogger.removeHandler(loggerHandler);
            loggerHandler = null;
        }
    }

    public SimpleAttributeSet asDefault() {
        return defaultAttributes;
    }

    public SimpleAttributeSet asHighlighted() {
        return highlightedAttributes;
    }

    public SimpleAttributeSet asError() {
        return errorAttributes;
    }

    public SimpleAttributeSet asInfo() {
        return infoAttributes;
    }

    public SimpleAttributeSet asDebug() {
        return debugAttributes;
    }

    /**
     * Used to send logger messages to the console.
     */
    private class ConsoleLoggerHandler extends Handler {
        private final SimpleLogFormatter formatter = new SimpleLogFormatter();

        @Override
        public void publish(LogRecord record) {
            Level level = record.getLevel();
            Throwable t = record.getThrown();
            AttributeSet attributes = defaultAttributes;

            if (level.intValue() >= Level.WARNING.intValue()) {
                attributes = errorAttributes;
            } else if (level.intValue() < Level.INFO.intValue()) {
                attributes = debugAttributes;
            }

            log(formatter.format(record), attributes);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * Used to send console messages to the console.
     */
    private class ConsoleOutputStream extends ByteArrayOutputStream {
        private AttributeSet attributes;

        private ConsoleOutputStream(AttributeSet attributes) {
            this.attributes = attributes;
        }

        @Override
        public void flush() {
            String data = toString();
            if (data.length() == 0) return;
            log(data, attributes);
            reset();
        }
    }

}
