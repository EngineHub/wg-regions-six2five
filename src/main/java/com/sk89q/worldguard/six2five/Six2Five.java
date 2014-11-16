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

import com.sk89q.worldguard.six2five.util.SimpleLogFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class Six2Five {

    private static final Logger log = Logger.getLogger(Six2Five.class.getCanonicalName());

    public static void main(String[] args) throws IOException {
        SimpleLogFormatter.configureGlobalLogger();

        OptionParser parser = new OptionParser("h");
        OptionSet options = parser.parse(args);

        List<?> nonOptions = options.nonOptionArguments();

        if (options.has("h") || nonOptions.size() != 1) {
            System.err.println("usage: six2five [-h] regions_file.yml");

            if (nonOptions.isEmpty()) {
                SwingUtilities.invokeLater(new GUIRunner());
            }
        } else {
            File file = new File(String.valueOf(nonOptions.get(0)));
            RegionsProcessor processor = new RegionsProcessor();
            if (!processor.downgrade(file)) {
                System.exit(2);
            }
        }
    }

    private static class GUIRunner implements Runnable {
        @Override
        public void run() {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            try {
                new Six2FiveFrame().setVisible(true);
                System.err.println();
                log.info("Six2Five UI successfully opened");
            } catch (HeadlessException ignored) {
            }
        }
    }

}
