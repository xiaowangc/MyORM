package pool;


import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConnectionInformation {

    private static Properties properties = new Properties();
    public ConnectionInformation(){}
    private static Map<String,String> configFile = new HashMap<>();//缓冲文件

     static {
        try {
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties");
            properties.load(in);
            Enumeration en = properties.propertyNames();
            while(en.hasMoreElements()){
                String key = (String)en.nextElement();
                String value = properties.getProperty(key);
                configFile.put(key,value);
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
     }
    //参数为String即properties文件中的key,返回值为value
    static String getValue(String key){
        return configFile.get(key);
    }



}
