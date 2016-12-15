package hh.ru.server;

import hh.ru.server.http.HTTPRequest;
import hh.ru.server.http.HTTPResponse;

import java.nio.channels.SocketChannel;

/**
 * Created by ia.vorobev on 13.12.2016.
 */
public class Worker implements Runnable {

    private final Server server;
    private final SocketChannel socket;
    private final HTTPRequest request;

    public Worker(Server server, SocketChannel socket, byte[] data) {
        this.server = server;
        this.socket = socket;
        this.request = new HTTPRequest(data);
    }

    public void run() {
        HTTPResponse response = HTTPResponse.responseTo(request);
        server.send(socket, response);
    }

//    private List queue = new LinkedList();
//    public void processData(Server server, SocketChannel socket, byte[] data) {
//        HTTPRequest request = new HTTPRequest(data);
//        HTTPResponse response = HTTPResponse.responseTo(request);
//        synchronized(queue) {
//            queue.add(new ServerDataEvent(server, socket, response));
//            queue.notify();
//        }
//    }
//
//    public void runDefault() {
//        ServerDataEvent dataEvent;
//
//        while(true) {
//            synchronized(queue) {
//                while(queue.isEmpty()) {
//                    try {
//                        queue.wait();
//                    } catch (InterruptedException e) {
//                    }
//                }
//                dataEvent = (ServerDataEvent) queue.remove(0);
//            }
//
//            dataEvent.server.send(dataEvent.socket, dataEvent.response);
//        }
//    }
}
