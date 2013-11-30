package com.github.uberroot.ncjbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import com.github.uberroot.ncjbot.RemoteNode.EventListener;

/**
 * <p>This class is responsible for loading, maintaining, and retrieving configuration data.
 * Configuration data is loaded from a Properties file and stored with keys in the format "&lt;section&gt;.&lt;setting&gt;".
 * For example, the entry "OverlayManager.threadPool = 1" would set the key "threadPool" in section "OverlayManager" to a value of "1".</p>
 * 
 * <p>Because this class may handle configurations for several components, it may be accessed by several threads simultaneously. In order to
 * achieve consistency and stability with configurations, all operations on this object should occur atomically. Example:
 * <pre>synchronized(configManager){
 * 	if(configManager.getSetting("some", "setting") == "")
 * 		configManager.setSetting("some", "setting", "value");
 * 	configManager.saveConfig(file);
 * }</pre></p>
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
	 * <p>A list of listeners that receive all updates.</p>
	 */
	private Vector<EventListener> fullListeners;
	
	/**
	 * <p>A mapping of sections to sectional listeners.</p>
	 */
	private Hashtable<String, Vector<EventListener>> sectionListeners;
	
	/**
	 * <p>A mapping of sections to individual key listeners.</p>
	 */
	private Hashtable<String, Hashtable<String, Vector<EventListener>>> keyListeners;
	
	/**
	 * An interface that listens for configuration changes.
	 * 
	 * @author Carter Waxman
	 *
	 */
	public interface EventListener extends java.util.EventListener{
		/**
		 * <p>Called when a monitored setting is changed.</p>
		 * 
		 * @param section The setting section.
		 * @param key The key within the section.
		 * @param value The new value for the setting.
		 */
		public void settingChanged(String section, String key, String value);
	}
	
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
		this.fullListeners = new Vector<EventListener>();
		this.sectionListeners = new Hashtable<String, Vector<EventListener>>();
		this.keyListeners = new Hashtable<String, Hashtable<String, Vector<EventListener>>>();
		
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
	
	/**
	 * <p>Saves the running configuration to the given file.</p>
	 * 
	 * @param configFile The file that will hold the configuration data.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
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
		
		//Get the old value if it exists
		String old = sectionTable.get(key);
		
		//Set the setting
		sectionTable.put(key, value);
		
		if(!value.equals(old)){
			//Notify full listeners
			Vector<EventListener> temp = new Vector<EventListener>(fullListeners);
			for(EventListener e : temp)
				e.settingChanged(section, key, value);
			
			//Notify section listeners
			temp = sectionListeners.get(section);
			if(temp != null){
				temp = new Vector<EventListener>(temp);
				for(EventListener e : temp)
					e.settingChanged(section, key, value);
			}
			
			//Notify key listeners
			Hashtable<String, Vector<EventListener>> tempTable = keyListeners.get(section);
			if(tempTable != null){
				temp = tempTable.get(key);
				if(temp != null){
					temp = new Vector<EventListener>(temp);
					for(EventListener e : temp)
						e.settingChanged(section, key, value);
				}
			}
		}
	}
	
	/**
	 * <p>Adds a listener to receive all configuration updates.</p>
	 * 
	 * @param listener The listener that will receive configuration updates.
	 */
	public synchronized void addListener(EventListener listener){
		fullListeners.add(listener);
	}
	
	/**
	 * <p>Removes a listener set to receive all configuration updates.</p>
	 * 
	 * @param listener The listener to remove.
	 */
	public synchronized void removeListener(EventListener listener){
		fullListeners.remove(listener);
	}
	
	/**
	 * <p>Adds a listener to receive all configuration updates on a section.</p>
	 * 
	 * @param listener The listener that will receive configuration updates.
	 * @param section The section on which to listen.
	 */
	public synchronized void addSectionListener(String section, EventListener listener){
		Vector<EventListener> v = sectionListeners.get(section);
		if(v == null){
			v = new Vector<EventListener>();
			sectionListeners.put(section, v);
		}
		v.add(listener);
	}
	
	/**
	 * <p>Removes a listener set to receive configuration updates from a section.</p>
	 * 
	 * @param section The monitored section.
	 * @param listener The listener to remove.
	 */
	public synchronized void removeSectionListener(String section, EventListener listener){
		Vector<EventListener> v = sectionListeners.get(section);
		if(v == null)
			return;
		v.remove(listener);
		if(v.size() == 0)
			sectionListeners.remove(section);
	}
	
	/**
	 * <p>Adds a listener set to receive configuration updates from a key in a section.</p>
	 * 
	 * @param section The monitored section.
	 * @param listener The listener to remove.
	 */
	public synchronized void addKeyListener(String section, String key, EventListener listener){
		Hashtable<String, Vector<EventListener>> h = keyListeners.get(section);
		if(h == null){
			h = new Hashtable<String, Vector<EventListener>>();
			keyListeners.put(section, h);
		}
		
		Vector<EventListener> v = h.get(key);
		if(v == null){
			v = new Vector<EventListener>();
			h.put(key, v);
		}
		v.add(listener);
	}
	
	/**
	 * <p>Removes a listener set to receive configuration updates from a key in a section.</p>
	 * 
	 * @param section The monitored section.
	 * @param listener The listener to remove.
	 */
	public synchronized void removeKeyListener(String section, String key, EventListener listener){
		Hashtable<String, Vector<EventListener>> h = keyListeners.get(section);
		if(h == null){
			h = new Hashtable<String, Vector<EventListener>>();
			keyListeners.put(section, h);
		}
		
		Vector<EventListener> v = h.get(key);
		if(v == null){
			v = new Vector<EventListener>();
			h.put(key, v);
		}
		v.remove(listener);
		
		if(v.size() == 0)
			h.remove(key);
		if(h.size() == 0)
			keyListeners.remove(section);
	}
}
