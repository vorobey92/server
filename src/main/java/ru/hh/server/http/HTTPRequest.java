package ru.hh.server.http;

import ru.hh.server.Server;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by ia.vorobev on 14.12.2016.
 */
public class HTTPRequest {

    private Server server;

    private String method;
    private String location;
    private String version;
    private Map<String, String> headers = new HashMap<>();

    public HTTPRequest(byte[] bytes, Server server) throws RequestParseException {
        parse(new String(bytes));
        this.server = server;
    }

    private void parse(String request) throws RequestParseException {
        StringTokenizer tokenizer = new StringTokenizer(request);
        method = tokenizer.nextToken().toUpperCase();
        if (!availableMethods.stream()
                .filter(method::equals)
                .findAny()
                .isPresent()) {
            throw new RequestParseException();
        }
        location = tokenizer.nextToken();
        version = tokenizer.nextToken();

        String[] lines = request.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            String[] keyVal = lines[i].split(":", 2);
            if (keyVal.length == 2) {
                headers.put(keyVal[0], keyVal[1]);
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public Path getLocation() {
        return Paths.get(server.getRootDirectory() + location.replace("/", File.separator));
    }

    private static final List<String> availableMethods = Arrays.asList("GET", "PUT", "POST", "DELETE", "PATCH",
            "HEAD", "CONNECT", "OPTIONS", "TRACE");

    @Override
    public String toString() {
        return "HTTPRequest{\r\n" +
                method + " " + location + " " + version + "\r\n" +
                headers + "\r\n" +
                '}';
    }
}
