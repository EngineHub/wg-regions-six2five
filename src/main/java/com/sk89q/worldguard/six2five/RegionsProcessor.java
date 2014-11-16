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

import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.sk89q.squirrelid.util.UUIDs;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class RegionsProcessor {

    private static final Logger log = Logger.getLogger(RegionsProcessor.class.getCanonicalName());
    private final LastNameResolver resolver = new LastNameResolver();

    public boolean downgrade(File file) throws IOException {
        Yaml yaml = new Yaml();
        Map<Object, Object> data;

        Closer closer = Closer.create();
        try {
            InputStream is = closer.register(new FileInputStream(file));
            BufferedInputStream bis = closer.register(new BufferedInputStream(is));
            data = (Map<Object, Object>) yaml.load(bis);

            log.info("Converting UUIDs to names...");
            downgrade(data);
        } catch (FileNotFoundException e) {
            log.log(Level.WARNING, "The file '" + file.getAbsolutePath() + "' does not exist");
            return false;
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to open file for reading", e);
            return false;
        } finally {
            closer.close();
        }

        closer = Closer.create();
        try {
            File backupFile = new File(
                    file.getParentFile(),
                    Files.getNameWithoutExtension(file.getName())
                            + "-" + System.currentTimeMillis()
                            + "." + Files.getFileExtension(file.getName()) + ".backup");

            if (!file.renameTo(backupFile)) {
                throw new IOException("Failed to rename old file to " + backupFile.getAbsolutePath());
            }

            log.info("Moved regions file to the backup file at " + backupFile.getAbsolutePath());

            FileWriter fw = closer.register(new FileWriter(file));
            yaml.dump(data, fw);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to open file for writing", e);
            return false;
        } finally {
            closer.close();
        }

        log.info("UUID -> name conversion is complete");

        return true;
    }

    public void downgrade(Map<Object, Object> data) throws IOException {
        processRegions((Map<Object, Object>) data.get("regions"));
    }

    private void processRegions(@Nullable Map<Object, Object> regions) {
        if (regions == null) return;
        for (Entry<Object, Object> entry : regions.entrySet()) {
            log.info("REGION: '" + entry.getKey() + "'");
            processRegion((Map<Object, Object>) entry.getValue());
        }
    }

    private void processRegion(@Nullable Map<Object, Object> region) {
        if (region == null) return;
        processDomain((Map<Object, Object>) region.get("owners"));
        processDomain((Map<Object, Object>) region.get("members"));
    }

    private void processDomain(@Nullable Map<Object, Object> domain) {
        if (domain == null) return;
        @Nullable Collection<Object> uniqueIds = (Collection<Object>) domain.get("unique-ids");
        if (uniqueIds != null) {
            Iterator<Object> it = uniqueIds.iterator();

            while (it.hasNext()) {
                Object rawUuid = it.next();

                try {
                    UUID uuid = UUID.fromString(UUIDs.addDashes((String) rawUuid));
                    @Nullable String name = resolver.resolve(uuid);
                    log.info(uuid + " -> " + name);

                    if (name != null) {
                        List<String> names;

                        if (!domain.containsKey("players")) {
                            domain.put("players", names = new ArrayList<String>());
                        } else {
                            Object object = domain.get("players");
                            if (object instanceof Set) {
                                names = (List<String>) object;
                            } else if (object instanceof Collection) {
                                names = new ArrayList<String>((Collection) object);
                                domain.put("players", names);
                            } else {
                                domain.put("players", names = new ArrayList<String>());
                            }
                        }

                        names.add(name);
                        it.remove();
                    }
                } catch (IllegalArgumentException e) {
                    log.log(Level.WARNING, "Invalid UUID: " + rawUuid);
                }
            }
        }
    }

}
