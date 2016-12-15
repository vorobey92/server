package hh.ru.server;

import java.io.IOException;

/**
 * Created by ia.vorobev on 13.12.2016.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Worker worker = new Worker();
        new Thread(worker).start();
        new Thread(new Server(null, 9090, worker)).start();
    }
}
