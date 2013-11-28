package com.github.uberroot.ncjbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

/**
 * <p>This class is responsible for loading, maintaining, and retrieving configuration data.
 * Configuration data is loaded from a Properties file and stored with keys in the format "&lt;section&gt;.&lt;setting&gt;".
 * For example, the entry "OverlayManager.threadPool = 1" would set the key "threadPool" in section "OverlayManager" to a value of "1".</p>
 * 
 * @author Carter Waxman
 *
 */
public final class ConfigManager {
	/**
	 * <p>The running LocalNode instance.</p>
	 */
	private LocalNode node;
	
	/**
	 * <p>The table of sections, which in turn are tables of settings.</p>
	 */
	private Hashtable<String, Hashtable<String, String>> configuration;
	
	/**
	 * <p>Creates a ConfigManager and loads the configuration for the file specified.</p>
	 * 
	 * @param node The running LocalNode instance.
	 * @param configFile The properties file that contains the configuration.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ConfigManager(LocalNode node, File configFile) throws FileNotFoundException, IOException {
		//Basic setup
		this.node = node;
		this.configuration = new Hashtable<String, Hashtable<String, String>>();
		
		//Load the configuration file into the manager
		loadConfig(configFile);
	}
	
	/**
	 * <p>Loads a configuration from the given properties file.</p>
	 * 
	 * @param configFile The properties file to load.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public synchronized void loadConfig(File configFile) throws FileNotFoundException, IOException{
		//Try to load the file
		Properties props = new Properties();
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(configFile);
			props.load(fis);
		}
		catch(FileNotFoundException e){
			throw e;
		}
		catch(IOException e){
			throw e;
		}
		finally{
			fis.close();
		}
		
		//Load the configuration
		Set<Object> fullKeys = props.keySet();
		for(Object o : fullKeys){
			String fullKey = (String)o;
			
			//Split the full key into section and key
			String splitKey[] = fullKey.split("\\.", 2);
			
			//Apply the setting
			if(splitKey.length < 2)
				setSetting("", splitKey[0], props.getProperty(fullKey));
			else
				setSetting(splitKey[0], splitKey[1], props.getProperty(fullKey));
		}
	}
	
	public synchronized void saveConfig(File configFile) throws FileNotFoundException, IOException{
		//Create a Properties class to store the data
		Properties props = new Properties();
		
		//Load the data
		Set<String> sections = configuration.keySet();
		for(String s : sections){
			Hashtable<String, String> section = configuration.get(s);
			Set<String> keys = section.keySet();
			for(String key : keys)
				props.setProperty(s + "." + key, section.get(key));
		}
		
		//Store the configuration
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(configFile);
			props.store(fos, null);
		}
		catch(FileNotFoundException e){
			throw e;
		}
		catch(IOException e){
			throw e;
		}
		finally{
			fos.close();
		}
	}
	
	/**
	 * <p>Gets a setting from the configuration using the given section->key pair.</p>
	 * 
	 * @param section The section that contains the setting.
	 * @param key The key for the setting.
	 * @return The setting for the given section->key pair, or null if the setting does not exist.
	 */
	public synchronized String getSetting(String section, String key){
		Hashtable<String, String> s = configuration.get(section);
		if(s == null)
			return null;
		return s.get(key);
	}
	
	/**
	 * <p>Sets the value of a setting for the given section->key pair.</p>
	 * 
	 * @param section The section that contains the setting.
	 * @param key The key for the setting.
	 * @param value The new value for the setting.
	 */
	public synchronized void setSetting(String section, String key, String value){
		Hashtable<String, String> sectionTable = configuration.get(section);
		
		//Make a new section if needed
		if(sectionTable == null){
			sectionTable = new Hashtable<String, String>();
			configuration.put(section, sectionTable);
		}
		
		//Set the setting
		sectionTable.put(key, value);
	}

}
