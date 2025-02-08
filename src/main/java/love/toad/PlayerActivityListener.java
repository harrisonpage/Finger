package love.toad;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PlayerActivityListener implements Listener {
    private JSONObject playerData;
    private final File dataFile;
    public final HashMap<UUID, Long> lastActiveTime = new HashMap<>();
    public final HashMap<UUID, Double> distanceWalked = new HashMap<>();
    public final HashMap<UUID, Double> damageDealt = new HashMap<>();
    public final HashMap<UUID, Integer> mobsKilled = new HashMap<>();
    public final HashMap<UUID, Integer> blocksMined = new HashMap<>();
    public final HashMap<UUID, Integer> deaths = new HashMap<>();

    public PlayerActivityListener(File dataFile) {
        this.dataFile = dataFile;
        this.playerData = new JSONObject();
        // Schedule periodic data saving every 60 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(Bukkit.getPluginManager().getPlugin("Finger"), this::savePlayerData, 1200L, 1200L);
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
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            UUID uuid = player.getUniqueId();
            damageDealt.put(uuid, damageDealt.getOrDefault(uuid, 0.0) + event.getDamage());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            UUID uuid = player.getUniqueId();
            mobsKilled.put(uuid, mobsKilled.getOrDefault(uuid, 0) + 1);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        blocksMined.put(uuid, blocksMined.getOrDefault(uuid, 0) + 1);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        deaths.put(uuid, deaths.getOrDefault(uuid, 0) + 1);
    }

    public void loadPlayerData() {
        try (FileReader reader = new FileReader(dataFile)) {
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(reader);
            if (obj instanceof JSONObject) {
                playerData = (JSONObject) obj;
                for (Object key : playerData.keySet()) {
                    JSONObject playerEntry = (JSONObject) playerData.get(key);
                    UUID uuid = UUID.fromString((String) key);

                    double distance = ((Number) playerEntry.getOrDefault("distance_walked", 0.0)).doubleValue();
                    distanceWalked.put(uuid, distance);

                    double damage = ((Number) playerEntry.getOrDefault("damage_dealt", 0.0)).doubleValue();
                    damageDealt.put(uuid, damage);

                    int mobs = ((Number) playerEntry.getOrDefault("mobs_killed", 0)).intValue();
                    mobsKilled.put(uuid, mobs);

                    int blocks = ((Number) playerEntry.getOrDefault("blocks_mined", 0)).intValue();
                    blocksMined.put(uuid, blocks);

                    int deathCount = ((Number) playerEntry.getOrDefault("deaths", 0)).intValue();
                    deaths.put(uuid, deathCount);
                }
            } else {
                playerData = new JSONObject();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            playerData = new JSONObject();
        }
    }

    public void savePlayerData() {
        for (UUID uuid : distanceWalked.keySet()) {
            JSONObject playerEntry = (JSONObject) playerData.getOrDefault(uuid.toString(), new JSONObject());
            playerEntry.put("distance_walked", distanceWalked.get(uuid));
            playerEntry.put("damage_dealt", damageDealt.getOrDefault(uuid, 0.0));
            playerEntry.put("mobs_killed", mobsKilled.getOrDefault(uuid, 0));
            playerEntry.put("blocks_mined", blocksMined.getOrDefault(uuid, 0));
            playerEntry.put("deaths", deaths.getOrDefault(uuid, 0));

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
