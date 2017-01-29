package ia.vorobev.server;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;


public class Main {

    /**
     * Starts server at localhost:8080 by default.
     * Server accepts only GET requests of HTTP/1.1 version
     * You can configure server by config.properties (place near executable *.jar file)
     * <p>
     * Example of config.properties :
     * host=localhost
     * port=9090
     * cache_enabled=true
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
            System.err.println("MAIN: Config file not found. Load default props. Original exception: " + e);
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
                    new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().toString());
        } catch (URISyntaxException e) {
            System.err.println("MAIN: Can't determine current directory. " + e + System.lineSeparator());
        }
        config.setProperty("cache_enabled", "true");
    }
}