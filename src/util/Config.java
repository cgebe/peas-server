package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Config {
	
	private static Config instance;
	private static final String FILE = "config.properties";
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	private Properties properties;
	
	
	private Config () {
		
		try {
			String jarPath = new File(Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath();
			InputStream inputStream = new FileInputStream(new File(jarPath + "/resources/config.properties"));
			
			properties = new Properties();
			properties.load(inputStream);
		} catch (IOException | URISyntaxException e) {
			logger.info("property file '" + FILE + "' is not following .properties file syntax or not found");
		}

	}

	public static Config getInstance () {
		if (Config.instance == null) {
			Config.instance = new Config();
		}
		return Config.instance;
	}
	
	public String getValue(String key) {
		return properties.getProperty(key);
	}

}
