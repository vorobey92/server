package hh.ru.server;

import hh.ru.server.http.HTTPResponse;

import java.nio.channels.SocketChannel;

/**
 * Created by ia.vorobev on 14.12.2016.
 */
class ServerDataEvent {
    public Server server;
    public SocketChannel socket;
    public HTTPResponse response;

    public ServerDataEvent(Server server, SocketChannel socket, HTTPResponse response) {
        this.server = server;
        this.socket = socket;
        this.response = response;
    }
}
