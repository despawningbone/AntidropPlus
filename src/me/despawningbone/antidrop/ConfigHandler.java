package me.despawningbone.antidrop;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigHandler {
	
	private static ADMain plugin;
	private static FileConfiguration config;
	
	static boolean hotbarSl, usePerms, eco;
	static List<ClickType> clickCheck = new ArrayList<ClickType>();
	static int filterMax, slotMax;
	static List<String> disableCheck = new ArrayList<String>();
	static HashMap<Material, Short> filterBlacklist = new HashMap<Material, Short>();
	static double filterFee, slotFee;
	static boolean deathKeepEnable;
	public static int cooldownTicks;
	public static boolean glow;
	
	public ConfigHandler(ADMain instance) {
		plugin = instance;
		createFiles();
	}
	
	public static void createFiles() {
		File configFile = new File(plugin.getDataFolder() + File.separator
				+ "config.yml");
		if (!configFile.exists()) {
			ADMain.log.info("Cannot find config.yml, Generating now....");
			plugin.saveDefaultConfig();
			ADMain.log.info("Config generated!");
		}
	}
	
	public static boolean getConfigValues() {
		plugin.reloadConfig();
		config = plugin.getConfig();
		YamlConfiguration defcfg = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml")));
		if(!defcfg.getKeys(true).equals(config.getKeys(true))) {
			ADMain.log.warning("Config File's keys are not the same.");
			ADMain.log.warning("This can mean that your configuration file is corrupted or was tempered with wrongly.");
			ADMain.log.warning("Please reset or remove the config file in order for it to work properly.");
		}
		usePerms = config.getBoolean("Use-permissions");
		eco = config.getBoolean("Eco.Use");
		if(eco) {
			if (!plugin.setupEconomy()) {
				ADMain.log.severe("Disabling due to no Vault dependency found!");
				plugin.getServer().getPluginManager().disablePlugin(plugin);
				return false;
			}
			slotFee = config.getDouble("Eco.SlotLock-fee");
			filterFee = config.getDouble("Eco.Filter-fee");
		}
		deathKeepEnable = config.getBoolean("Death-AntiDrop");
		glow = config.getBoolean("Glow");
		if(glow) {
			 GUIHandler.glow = ADMain.getGlow();
		}
		hotbarSl = config.getBoolean("SlotLock.Hotbar-slotlock");
		//TODO armor slotlock
		slotMax = config.getInt("SlotLock.Max-amount");
		if(slotMax == -1) { slotMax = Integer.MAX_VALUE; }
		filterMax = config.getInt("Filter.Max-amount");
		if(filterMax == -1) { filterMax = Integer.MAX_VALUE; }
		disableCheck = config.getStringList("Filter.Disable-filter-check");
		GUIHandler.initItems();
		cooldownTicks = config.getInt("Antidrop-message-cooldown");
		filterBlacklist = new HashMap<Material, Short>();
		List<String> sFilterBlackList = config.getStringList("Filter.Filter-Blacklist");
		if(!sFilterBlackList.isEmpty()) {
			for (int i = 0; i < sFilterBlackList.size(); i++) {
				String item = sFilterBlackList.get(i);
				String itemname = null; String dv = null;
				boolean nodv = false;
				try {
					itemname = item.split(":")[0];
					dv = item.split(":")[1];
				} catch (ArrayIndexOutOfBoundsException e) {
					itemname = item;
					nodv = true;
				}
				itemname = itemname.toUpperCase();
				Material material = Material.getMaterial(itemname);
				if(nodv) {	
					filterBlacklist.put(material, (short) -1);
				} else {
					filterBlacklist.put(material, Short.parseShort(dv));
				}
			}
		}
		clickCheck = new ArrayList<ClickType>();
		List<String> sClickCheck = config.getStringList("Filter.Filter-click-type"); 
		if(!sClickCheck.isEmpty()) {
			for (int i = 0; i < sClickCheck.size(); i++) {
				try {
					clickCheck.add(ClickType.valueOf(sClickCheck.get(i)));	
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

}
