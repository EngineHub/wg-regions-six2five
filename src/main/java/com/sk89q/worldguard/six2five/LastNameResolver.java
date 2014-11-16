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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.sk89q.squirrelid.util.HttpRequest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LastNameResolver {

    private static final Logger log = Logger.getLogger(LastNameResolver.class.getCanonicalName());
    private static final double FETCHES_PER_SECOND = 0.9; // 600 per 10 minutes
    private static final int TRY_COUNT = 5;
    private static final int STARTING_RETRY_DELAY = 1000;

    private final LoadingCache<UUID, String> nameCache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<UUID, String>() {
                        @Override
                        public String load(UUID key) {
                            return fetchName(key);
                        }
                    });
    private final RateLimiter rateLimiter = RateLimiter.create(0.9);

    @Nullable
    public String resolve(UUID uuid)  {
        return nameCache.getUnchecked(uuid);
    }

    @Nullable
    private String fetchName(UUID uuid)  {
        try {
            Object object = sendRequest(uuid);

            if (object instanceof Map) {
                Object name = ((Map) object).get("name");
                if (name != null) {
                    return String.valueOf(name);
                }
            }
        } catch (InterruptedException e) {
            log.log(Level.WARNING, "Failed to get a name for UUID " + uuid + " because the operation was interrupted", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to get a name for UUID " + uuid + " because the HTTP request failed", e);
        }

        log.log(Level.WARNING, "Failed to get a name for UUID " + uuid + " because Mojang did respond with a name");

        return null;
    }

    @Nullable
    private Object sendRequest(UUID uuid) throws InterruptedException, IOException {
        URL url = HttpRequest.url(
                "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", ""));

        IOException lastException;
        int left = TRY_COUNT;
        int retryDelay = STARTING_RETRY_DELAY;

        do {
            try {
                rateLimiter.acquire();
                HttpRequest req = HttpRequest.get(url).execute();
                switch (req.getResponseCode()) {
                    case 204: return null; // No user by this UUID?
                    case 200: return req.returnContent().asJson();
                    case 429: throw new IOException("Rate limit hit");
                    default: throw new IOException("Got " + req.getResponseCode() + " as a response code");
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "HTTP request for name failed", e);
                lastException = e;
                Thread.sleep(retryDelay);
                retryDelay *= 2;
            }
        } while (--left > 0);

        throw lastException;
    }

}
