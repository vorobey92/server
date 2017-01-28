package ia.vorobev.server;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;


public class Main {

    /**
     * Starts server at localhost:8080 by default.
     * You can configure server by config.properties (place near *.jar file)
     * <p>
     * You can clear caches if you call http://host:port/evict
     * <p>
     * Example of config.properties :
     * host=localhost
     * port=9090
     * cache=true
     * root_directory=content/
     *
     */
    public static void main(String[] args) throws IOException {
        System.out.println("MAIN: Starting server");
        Properties config = loadProperties();
        new Thread(new Server(config)).start();
    }

    private static Properties loadProperties() throws IOException {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (FileNotFoundException e) {
            System.err.println("MAIN: Config file not found. Load default props");
            loadDefault(config);
        }
        System.out.println("MAIN: properties loaded = " + config + System.lineSeparator());
        return config;
    }

    private static void loadDefault(Properties config) {
        config.setProperty("host", "localhost");
        config.setProperty("port", "8080");
        try {
            config.setProperty("root_directory",
                    new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).toString());
        } catch (URISyntaxException e) {
            System.err.println("MAIN: Can't determine current directory. " + e.getMessage() + System.lineSeparator());
        }
        config.setProperty("cache", "true");
    }
}