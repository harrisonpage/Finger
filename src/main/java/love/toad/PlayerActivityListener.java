package love.toad;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONObject;

public class PlayerActivityListener implements Listener {
    private final HashMap<UUID, Long> lastActiveTime;
    private final HashMap<UUID, Double> distanceWalked;
    private final JSONObject playerData;
    private final File dataFile;

    public PlayerActivityListener(HashMap<UUID, Long> lastActiveTime, HashMap<UUID, Double> distanceWalked, JSONObject playerData, File dataFile) {
        this.lastActiveTime = lastActiveTime;
        this.distanceWalked = distanceWalked;
        this.playerData = playerData;
        this.dataFile = dataFile;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!distanceWalked.containsKey(uuid)) {
            distanceWalked.put(uuid, 0.0);
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from != null && to != null && !from.equals(to)) {
            double distance = from.distance(to);
            distanceWalked.put(uuid, distanceWalked.get(uuid) + distance);
        }

        lastActiveTime.put(uuid, System.currentTimeMillis());
        savePlayerData();
    }

    private void savePlayerData() {
        for (UUID uuid : distanceWalked.keySet()) {
            JSONObject playerEntry = (JSONObject) playerData.getOrDefault(uuid.toString(), new JSONObject());
            playerEntry.put("distance_walked", distanceWalked.get(uuid));
            playerData.put(uuid.toString(), playerEntry);
        }
        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(playerData.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        lastActiveTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        lastActiveTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            lastActiveTime.put(event.getDamager().getUniqueId(), System.currentTimeMillis());
        }
    }
}
