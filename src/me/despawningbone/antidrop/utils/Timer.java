package me.despawningbone.antidrop.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.despawningbone.antidrop.ConfigHandler;
import me.despawningbone.antidrop.ADListener;

public class Timer {
    public static BukkitTask cooldown(Player player) {
        BukkitTask taskid = Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(Bukkit.getPluginManager().getPlugin("AntiDropPlus"), new Runnable() {
            @Override
            public void run() {
            	ADListener.cooldown.remove(player.getUniqueId());
            }
        }, ConfigHandler.cooldownTicks);
        return taskid;
    }
}