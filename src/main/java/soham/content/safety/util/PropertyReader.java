package soham.content.safety.util;

import java.io.IOException;
import java.util.Properties;

public class PropertyReader {

    public static String getProperty(String key) throws IOException {
        Properties properties = new Properties();
        java.net.URL url = ClassLoader.getSystemResource("conf.properties");
        properties.load(url.openStream());
        return properties.getProperty(key);
    }
}
