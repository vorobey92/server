package ia.vorobev.server.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;


public class HTTPResponse {

    private static Map<Integer, String> codeToReason = new HashMap<>();

    static {
        codeToReason.put(200, "OK");
        codeToReason.put(400, "Bad Request");
        codeToReason.put(404, "Not Found");
        codeToReason.put(405, "Method Not Allowed");
        codeToReason.put(500, "Internal Server Error");
    }

    private HTTPResponse() {
    }

    private String version = "HTTP/1.1";
    private int responseCode;
    private String responseReason;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String contentType;

    private ByteBuffer content;

    public static HTTPResponse responseTo(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();
        response.setContent(readFile(request.getLocation()));
        response.setContentType(request.getLocation());
        response.setResponseCode(response.resolveResponseCode());
        response.setResponseReason(codeToReason.get(response.getResponseCode()));
        response.addDefaultHeaders();
        return response;
    }

    public static HTTPResponse badRequest() {
        HTTPResponse response = new HTTPResponse();
        response.setResponseCode(400);
        response.setResponseReason(codeToReason.get(400));
        response.setContent(ByteBuffer.wrap("400 Method Not Allowed".getBytes()));
        response.setContentType("text/html");
        response.addDefaultHeaders();
        return response;
    }

    public static HTTPResponse illegalMethodResponse() {
        HTTPResponse response = new HTTPResponse();
        response.setResponseCode(405);
        response.setResponseReason(codeToReason.get(405));
        response.setContent(ByteBuffer.wrap("405 Bad Request".getBytes()));
        response.addDefaultHeaders();
        return response;
    }

    public String getHead() {
        String result = getVersion() + " " + getResponseCode() + " " + getResponseReason() + "\r\n";
        result += getHeadersString();
        result += "\r\n";
        return result;
    }

    public ByteBuffer getBody() {
        return content;
    }

    private void addDefaultHeaders() {
        headers.put("Date", LocalDateTime.now().toString());
        headers.put("Server", "ia.vorobev.ru");
        headers.put("Connection", "Keep-Alive");
        if (content != null) {
            headers.put("Content-Length", Integer.toString(getBody().array().length));
            headers.put("Content-Type", contentType);
        }
    }

    private static ByteBuffer readFile(Path location) {
        try (FileChannel ch = FileChannel.open(location, StandardOpenOption.READ)) {
            int size = (int) ch.size();
            ByteBuffer target = ByteBuffer.allocate(size);
            ch.read(target);
            target.flip();
            return target;
        } catch (NoSuchFileException e) {
            System.err.println("HTTP RESPONSE: Can't find file '" + location + "'. " + e);
            return ByteBuffer.wrap("404".getBytes());
        } catch (IOException e) {
            System.err.println("HTTP RESPONSE: Got some problems. " + e);
            return ByteBuffer.wrap("500".getBytes());
        }
    }

    private void setContentType(Path location) {
        if (location.toString().endsWith("js")) {
            contentType = "application/javascript";
        } else if (Files.exists(location)) {
            try {
                this.contentType = Files.probeContentType(location);
            } catch (IOException e) {
                System.err.println("HTTP RESPONSE: Can't determine content type. " + e);
                this.contentType = "text/html";
            }
        }
        if (contentType == null) {
            this.contentType = "text/html";
        }
        if (!contentType.contains("image")) {
            contentType += "; charset=utf-8";
        }
    }

    private void setContentType(String contentType) {
        this.contentType = contentType;
    }

    private String getHeadersString() {
        String headersStr = "";
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersStr += entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        return headersStr;
    }

    private int resolveResponseCode() {
        String actualContent = new String(content.array());
        switch (actualContent) {
            case "404":
                return 404;
            case "500":
                return 500;
            default:
                return 200;
        }
    }

    private int getResponseCode() {
        return responseCode;
    }

    private String getResponseReason() {
        return responseReason;
    }

    private void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    private void setResponseReason(String responseReason) {
        this.responseReason = responseReason;
    }

    private void setContent(ByteBuffer content) {
        this.content = content;
    }

    private String getVersion() {
        return version;
    }
}