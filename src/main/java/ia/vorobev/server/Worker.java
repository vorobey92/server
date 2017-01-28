package ia.vorobev.server;

import ia.vorobev.server.http.HTTPRequest;
import ia.vorobev.server.http.HTTPResponse;
import ia.vorobev.server.http.RequestParseException;

import java.nio.channels.SocketChannel;

/**
 * Created by ia.vorobev on 13.12.2016.
 */
public class Worker implements Runnable {

    private final Server server;
    private final SocketChannel socket;
    private final byte[] data;

    public Worker(Server server, SocketChannel socket, byte[] data) {
        this.server = server;
        this.socket = socket;
        this.data = data;
    }

    public void run() {
        System.out.println("WORKER: " + Thread.currentThread() + " started." + System.lineSeparator());
        HTTPRequest request;
        try {
            request = new HTTPRequest(data, server);
            System.out.println("WORKER: " + Thread.currentThread() + " accepted request : " + System.lineSeparator() + request);
        } catch (RequestParseException e) {
            System.err.println("WORKER: Bad request. " + e.getMessage() + System.lineSeparator());
            server.send(socket, HTTPResponse.badRequest());
            return;
        }
        if (!request.getMethod().equals("GET")) {
            System.err.println("WORKER: Not a GET request. This is " + request.getMethod() + " request" + System.lineSeparator());
            server.send(socket, HTTPResponse.illegalMethodResponse());
            return;
        }
        if (!server.isCacheEnabled()) {
            server.send(socket, HTTPResponse.responseTo(request));
            return;
        }
        server.send(socket, server.getCache().get(request.getLocation(), request));
    }

}