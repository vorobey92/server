package ia.vorobev.server.http;

import ia.vorobev.server.Server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class HTTPRequest {

    private Server server;

    private String method;
    private String location;
    private String version;
    private Map<String, String> headers = new HashMap<>();

    private static final List<String> availableMethods = Arrays.asList("GET", "PUT", "POST", "DELETE", "PATCH",
            "HEAD", "CONNECT", "OPTIONS", "TRACE");

    private static final List<String> validVersions = Arrays.asList("HTTP/1.1", "HTTP/1.0", "HTTP/2.0");

    public HTTPRequest(byte[] bytes, Server server) throws RequestParseException {
        parse(new String(bytes));
        this.server = server;
    }

    private void parse(String request) throws RequestParseException {
        StringTokenizer tokenizer = new StringTokenizer(request);
        method = tokenizer.nextToken().toUpperCase();
        validateFieldByAvailableValues(method, availableMethods);
        try {
            location = URLDecoder.decode(tokenizer.nextToken(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("HTTP PARSER: Can't decode location." + e);
        }

        version = tokenizer.nextToken();
        validateFieldByAvailableValues(version, validVersions);


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

    @Override
    public String toString() {
        return "HTTPRequest{\r\n" +
                method + " " + location + " " + version + "\r\n" +
                headers + '}';
    }

    private void validateFieldByAvailableValues(String field, List<String> availableValues) {
        if (!availableValues.stream()
                .filter(field::equals)
                .findAny()
                .isPresent()) {
            throw new RequestParseException();
        }
    }
}
