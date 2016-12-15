package hh.ru.server;

import hh.ru.server.http.HTTPRequest;
import hh.ru.server.http.HTTPResponse;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ia.vorobev on 13.12.2016.
 */
public class Worker implements Runnable {

    private List queue = new LinkedList();

    public void processData(Server server, SocketChannel socket, byte[] data) {
        HTTPRequest request = new HTTPRequest(data);
        HTTPResponse response = HTTPResponse.responseTo(request);
        synchronized(queue) {
            queue.add(new ServerDataEvent(server, socket, response));
            queue.notify();
        }
    }

    public void run() {
        ServerDataEvent dataEvent;

        while(true) {
            synchronized(queue) {
                while(queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                    }
                }
                dataEvent = (ServerDataEvent) queue.remove(0);
            }

            dataEvent.server.send(dataEvent.socket, dataEvent.response);
        }
    }
}
