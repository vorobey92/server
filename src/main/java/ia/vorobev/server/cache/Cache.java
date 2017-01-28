package ia.vorobev.server.cache;

import ia.vorobev.server.http.HTTPRequest;
import ia.vorobev.server.http.HTTPResponse;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


public class Cache implements Runnable {

    private final Thread t;

    private final Map<Path, HTTPResponse> cache;

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    public Cache(Path dir) throws IOException {
        cache = new ConcurrentHashMap<>();

        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();

        walkAndRegisterDirectories(dir);
        this.t = new Thread(this, "Cache");
    }

    public void start() {
        t.start();
    }

    public HTTPResponse get(Path key, HTTPRequest request) {
        System.out.println("CACHE: " + key + " requested from cache");
        HTTPResponse value = cache.computeIfAbsent(key, (k) -> HTTPResponse.responseTo(request));
        System.out.println("CACHE: contains = " + cache + System.lineSeparator());
        return value;
    }

    public void run() {

        for (; ; ) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("CACHE: WatchKey not recognized." + System.lineSeparator());
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("rawtypes")
                WatchEvent.Kind kind = event.kind();

                @SuppressWarnings("unchecked")
                Path name = ((WatchEvent<Path>) event).context();
                Path child = dir.resolve(name);

                System.out.format("CACHE: %s: %s%n%n", event.kind().name(), child);

                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child)) {
                            walkAndRegisterDirectories(child);
                        }
                    } catch (IOException x) {
                        System.err.println("CACHE: Can't register Path " + child + ". Error: " + x);
                    }
                } else if (kind == ENTRY_MODIFY
                        || kind == ENTRY_DELETE) {

                    System.out.println("CACHE: " + cache.remove(child) + " removed");
                    System.out.println("CACHE: contains = " + cache + System.lineSeparator());
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void walkAndRegisterDirectories(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

}