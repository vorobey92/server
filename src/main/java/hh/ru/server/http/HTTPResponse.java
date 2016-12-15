package hh.ru.server.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by ia.vorobev on 14.12.2016.
 */
public class HTTPResponse {

    private HTTPResponse(){}

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
        response.setResponseCode(200);
        response.setResponseReason("OK");
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
        headers.put("Server", "school.hh.ru");
        headers.put("Connection", "Keep-Alive");
        if (content != null) {
            headers.put("Content-Length", Integer.toString(getBody().array().length));
            headers.put("Content-Type", contentType);
        }
    }

    private static ByteBuffer readFile(String location) {
        try (FileChannel ch = FileChannel.open(Paths.get(location), StandardOpenOption.READ)) {
            int size = (int) ch.size();
            ByteBuffer target = ByteBuffer.allocate(size);
            ch.read(target);
            target.flip();
            return target;
        } catch (NoSuchFileException e) {
            System.out.println(e.getMessage());
            //TODO 404 error
            return ByteBuffer.wrap("404".getBytes());
        } catch (IOException e) {
            //TODO 500 error
            e.printStackTrace();
            return ByteBuffer.wrap("500".getBytes());
        }
    }

    private void setContentType(String location) {
        try {
            this.contentType = Files.probeContentType(Paths.get(location));
        } catch (IOException e) {
            //TODO can't determine content-type (use default)g
            e.printStackTrace();
        }
    }

    private String getHeadersString() {
        String headersStr = "";
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headersStr += entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        return headersStr;
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