package hh.ru.server.http;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ia.vorobev on 14.12.2016.
 */
public class HTTPRequest {

    private String method;
    private String location;
    private String version;
    private Map<String, String> headers = new HashMap<>();

    public HTTPRequest(byte[] bytes) {
        parse(new String(bytes));
    }

    private void parse(String request) {
        StringTokenizer tokenizer = new StringTokenizer(request);
        method = tokenizer.nextToken().toUpperCase();
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

    //TODO configurate file's location
    String getLocation() {
        return "C:\\Users\\ia.vorobev\\IdeaProjects\\hh-server" + location;
    }
}
