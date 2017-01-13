package ru.hh.server;

import ru.hh.server.http.HTTPRequest;
import ru.hh.server.http.HTTPResponse;
import ru.hh.server.http.RequestParseException;

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
        System.out.println("Worker " + Thread.currentThread() + " started.");
        HTTPRequest request;
        try {
            request = new HTTPRequest(data, server);
            System.out.println(Thread.currentThread() + " accepted request : \r\n" + request);
        } catch (RequestParseException e) {
            System.err.println("Bad request. " + e.getMessage());
            server.send(socket, HTTPResponse.badRequest());
            return;
        }
        if (request.getLocation().getFileName().toString().equals("evict")) {
            server.getCache().evict();
            server.send(socket, HTTPResponse.clearCacheResponse());
            return;
        }
        if (!request.getMethod().equals("GET")) {
            System.err.println("Not a GET request. This is " + request.getMethod() + " request");
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