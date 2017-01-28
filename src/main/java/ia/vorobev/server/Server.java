package ia.vorobev.server;

import ia.vorobev.server.cache.Cache;
import ia.vorobev.server.http.HTTPResponse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

/**
 * Created by ia.vorobev on 13.12.2016.
 */
public class Server implements Runnable {

    //configs
    private InetAddress ip;
    private int port;
    private Path rootDirectory;
    private boolean cacheEnabled;

    //other inner fields
    private Cache cache;
    private ServerSocketChannel serverChannel;
    private ThreadPoolExecutor p = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
    private Selector selector;
    private ByteBuffer buf = ByteBuffer.allocate(1024);

    public Server(Properties config) throws IOException {
        this(InetAddress.getByName(config.getProperty("host")),
                Integer.valueOf(config.getProperty("port")));
        rootDirectory = Paths.get(config.getProperty("root_directory"));
        cacheEnabled = Boolean.valueOf(config.getProperty("cache"));
        if (!cacheEnabled) {
            return;
        }
        cache = new Cache(rootDirectory);
        cache.start();
    }

    private Server(InetAddress ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        this.selector = initSelector();
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
                synchronized (changeRequests) {
                    for (ChangeRequest change : changeRequests) {
                        if (change.type == ChangeRequest.CHANGEOPS) {
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

        p.submit(new Worker(this, ch, buf.array()));
    }

    // A list of ChangeRequest instances
    private final List<ChangeRequest> changeRequests = new LinkedList<>();

    // Maps a SocketChannel to a list of ByteBuffer instances
    private final Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();

    public void send(SocketChannel socket, HTTPResponse response) {
        synchronized (changeRequests) {
            // Indicate we want the interest ops set changed
            changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            // And queue the response we want written
            synchronized (pendingData) {
                List<ByteBuffer> queue = pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList<>();
                    pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(response.getHead().getBytes()));
                queue.add(response.getBody());
            }
        }

        selector.wakeup();
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (pendingData) {
            List<ByteBuffer> queue = pendingData.get(socketChannel);

            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
                buf.position(0);
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

    public Cache getCache() {
        return cache;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

}