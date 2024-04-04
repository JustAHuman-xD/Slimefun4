package io.github.thebusybiscuit.slimefun4.core.services;

import java.util.Iterator;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * This Service is responsible for saving {@link Player} and {@link Block}
 * data.
 * 
 * @author TheBusyBiscuit
 * @author JustAHuman
 */
public class AutoSavingService {

    private int interval;

    /**
     * This method starts a {@link AutoSavingService} task with the given interval.
     * 
     * @param plugin
     *            The current instance of Slimefun
     * @param interval
     *            The interval in which to run this task
     */
    public void startAutoSave(@Nonnull Slimefun plugin, int interval) {
        this.interval = interval;

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllPlayers, 2000L, interval * 60L * 20L);
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllBlocks, 2000L, interval * 60L * 20L);
    }

    /**
     * This method saves every {@link PlayerProfile} in memory and removes profiles
     * that were marked for deletion.
     */
    public void saveAllPlayers() {
        long startTime = System.currentTimeMillis();
        Iterator<PlayerProfile> iterator = PlayerProfile.iterator();
        int players = 0;

        Debug.log(TestCase.PLAYER_PROFILE_DATA, "Saving all player data");

        while (iterator.hasNext()) {
            PlayerProfile profile = iterator.next();

            if (profile.isDirty()) {
                players++;
                profile.save();

                Debug.log(TestCase.PLAYER_PROFILE_DATA, "Saved data for {} ({})",
                    profile.getPlayer() != null ? profile.getPlayer().getName() : "Unknown", profile.getUUID()
                );
            }

            // Remove the PlayerProfile from memory if the player has left the server (marked from removal)
            // and they're still not on the server
            // At this point, we've already saved their profile so we can safely remove it
            // without worry for having a data sync issue (e.g. data is changed but then we try to re-load older data)
            if (profile.isMarkedForDeletion() && profile.getPlayer() == null) {
                iterator.remove();

                Debug.log(TestCase.PLAYER_PROFILE_DATA, "Removed data from memory for {}",
                    profile.getUUID()
                );
            }
        }

        if (players > 0) {
            Slimefun.logger().log(Level.INFO, "Auto-saved all player data for {0} player(s)! (Next auto-save: {1}m)", new Object[] { players, this.interval });
            Slimefun.logger().log(Level.INFO, "Took {0}ms!", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * This method saves the data of every {@link Block} marked dirty by {@link BlockStorage}.
     */
    public void saveAllBlocks() {
        long startTime = System.currentTimeMillis();
        int savedChanges = 0;

        for (World world : Bukkit.getWorlds()) {
            BlockStorage storage = BlockStorage.getStorage(world);
            if (storage == null) {
                continue;
            }

            savedChanges += storage.saveChanges();
        }

        BlockStorage.saveChunks();
        Slimefun.logger().log(Level.INFO, "Auto-saved all block data from {0} changes! (Next auto-save: {1}m)", new Object[] { savedChanges, this.interval });
        Slimefun.logger().log(Level.INFO, "Took {0}ms!", new Object[] { System.currentTimeMillis() - startTime });
    }

}
