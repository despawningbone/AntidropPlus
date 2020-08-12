package me.despawningbone.antidrop;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;
import java.io.File;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.despawningbone.antidrop.utils.Glow;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public class ADMain extends JavaPlugin {
	public static Logger log;
	public ADCommandMain ADCM = new ADCommandMain(this);
	public ADListener listener = new ADListener(this);
	public static String prefix = ChatColor.translateAlternateColorCodes('&', "&8[&3AntiDrop+&8]&r ");
	public static Economy econ;
	
	String[] commandMainAliases = {"nodrop", "adp"};
	
	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {
		log = getLogger();
		new ConfigHandler(this);
		ConfigHandler.getConfigValues();
		getCommand("antidrop").setExecutor(ADCM);
		getCommand("antidrop").setAliases(Arrays.asList(commandMainAliases));
		getServer().getPluginManager().registerEvents(listener, this);
		if(new File(this.getDataFolder() + File.separator + "FilterData.bin").exists()) {
			Object filterobj = deserialize("FilterData.bin");
			Object slotobj = deserialize("SlotData.bin");
			if(filterobj != null) {
				GUIHandler.filterParam = (HashMap<UUID, List<List<Entry<String, Object>>>>) filterobj;
			} 
			if(slotobj != null) {
				GUIHandler.slotParam = (HashMap<UUID, List<Integer>>) slotobj; 	
			} 
		}
		log.info("Antidrop+ v" + getDescription().getVersion() + " by despawningbone has been enabled!");
	}
	
	@Override
	public void onDisable() {
		serialize(GUIHandler.filterParam, "FilterData.bin");
		serialize(GUIHandler.slotParam, "SlotData.bin");
		log.info("Disabled Antidrop+ v" + getDescription().getVersion() + ".");
	}
	
	public Object deserialize(String file) {
		Object obj = null;
		try {
	        FileInputStream in = new FileInputStream(this.getDataFolder() + File.separator + file);
	        ObjectInputStream ois = new ObjectInputStream(in);
	        obj = ois.readObject();
	        ois.close();
	   	} catch (Exception e) {
	        log.severe("Problem deserializing data from file: " + e);
	        return null;
	    }
		return obj;
	}
	
	public void serialize(Object obj, String file) { 
		File folder = this.getDataFolder();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
			        FileOutputStream out = new FileOutputStream(folder + File.separator + file);
			        ObjectOutputStream oos = new ObjectOutputStream(out);
			        oos.writeObject(obj);
			        oos.flush();
			        oos.close();
			      } catch (Exception e) {
			        log.severe("Problem serializing data to file: " + e);
			      }
				return;
			}
		}).start();
	}
	
	public static Enchantment getGlow() { 
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        Glow glow = null;
        try {
            glow = new Glow(100);
            if(!Arrays.asList(Enchantment.values()).contains(glow)) {
            	Enchantment.registerEnchantment(glow);	
            }
        }
        catch (Exception e){
        		e.printStackTrace();
        }
        return glow;
    }
	
	public boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }  
        econ = rsp.getProvider();
        return econ != null;
    }
	public static double getMoney(Player p) {
        double m = econ.getBalance(p);
        return m;
    }
}
