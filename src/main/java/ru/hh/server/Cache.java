package ru.hh.server;

import ru.hh.server.http.HTTPRequest;
import ru.hh.server.http.HTTPResponse;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ia.vorobev on 13.01.2017.
 */
public class Cache {

    private final ConcurrentMap<Path, HTTPResponse> cache;

    public Cache() {
        cache = new ConcurrentHashMap<>();
    }

    public void evict() {
        System.out.println("Clear caches called");
        cache.clear();
    }

    public HTTPResponse get(Path key, HTTPRequest request) {
        return cache.computeIfAbsent(key, (k) -> HTTPResponse.responseTo(request));
    }
}