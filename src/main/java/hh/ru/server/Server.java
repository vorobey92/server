package hh.ru.server;

import hh.ru.server.http.HTTPResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

/**
 * Created by ia.vorobev on 13.12.2016.
 */
public class Server implements Runnable {

    private InetAddress ip;
    private int port;

    private ServerSocketChannel serverChannel;

    private Selector selector;
    private Worker worker;

    private ByteBuffer buf = ByteBuffer.allocate(1024);

    public Server(InetAddress ip, int port, Worker worker) throws IOException {
        this.ip = ip;
        this.port = port;
        this.selector = initSelector();
        this.worker = worker;
    }

    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress isa = new InetSocketAddress(ip, port);
        serverChannel.socket().bind(isa);

        serverChannel.register(socketSelector, OP_ACCEPT);

        return socketSelector;
    }

    public void run() {
        while (true) {
            try {
                synchronized(changeRequests) {
                    Iterator changes = changeRequests.iterator();
                    while (changes.hasNext()) {
                        ChangeRequest change = (ChangeRequest) changes.next();
                        switch(change.type) {
                            case ChangeRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(selector);
                                key.interestOps(change.ops);
                        }
                    }
                    changeRequests.clear();
                }

                selector.select();

                Iterator selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();

        buf.clear();

        int bytesReaded;
        try {
            bytesReaded = ch.read(buf);
        } catch (IOException e) {
            key.cancel();
            ch.close();
            return;
        }

        if (bytesReaded == -1) {
            key.channel().close();
            key.cancel();
            return;
        }

        worker.processData(this, ch, buf.array());
    }

    // A list of ChangeRequest instances
    private List changeRequests = new LinkedList();

    // Maps a SocketChannel to a list of ByteBuffer instances
    private Map pendingData = new HashMap();

    public void send(SocketChannel socket, HTTPResponse data) {
        synchronized (changeRequests) {
            // Indicate we want the interest ops set changed
            changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            // And queue the data we want written
            synchronized (pendingData) {
                List queue = (List) pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList();
                    pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data.getHead().getBytes()));
                queue.add(data.getBody());
            }
        }

        selector.wakeup();
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (pendingData) {
            List queue = (List) pendingData.get(socketChannel);

            while (!queue.isEmpty()) {
                ByteBuffer buf = (ByteBuffer) queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    break;
                }
                queue.remove(0);
            }

            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }
}

