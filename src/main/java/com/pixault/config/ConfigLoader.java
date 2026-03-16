package com.pixault.config;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties props=new Properties();

    static{
        try(InputStream input=ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);
            
        } catch (Exception e) {
            throw new RuntimeException("failed to load config.properties",e);
        }

    }
    public static String get(String key){
        return props.getProperty(key);
    }
    
}
