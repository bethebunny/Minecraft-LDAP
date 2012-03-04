package edu.umn.acm.minecraft.ldap;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.*;

/**
 * User: bunny
 * Date: 2/28/12
 * Time: 3:58 PM
 */
public class PlayerListener implements Listener {
    private static final int MAX_LIST_NAME_LENGTH = 16;
    
    private Server server;
    private Set<Player> authenticatedPlayers;
    private AuthenticationSource authenticationSource;

    public PlayerListener() {
        authenticatedPlayers = new HashSet<Player>();
    }
    
    public void setServer(Server server) {
        this.server = server;
    }

    public void setAuthenticationSource(AuthenticationSource authenticationSource) {
        this.authenticationSource = authenticationSource;
    }
    
    private boolean isPlayerActive(String name) {
        for (Player player : server.getOnlinePlayers()) {
            if (player.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(event.getJoinMessage() + " Please type your username to log in.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        // Handle login
        if (!authenticatedPlayers.contains(player)) {
            String cmd[] = event.getMessage().split(" ", 3);
            if (cmd[0].equals("/login")) {
                if (cmd.length != 3) {
                    player.sendRawMessage("Usage: /login username password");
                } else if (authenticationSource.authenticate(cmd[1], cmd[2])) {
                    authenticatedPlayers.add(player);
                    player.sendRawMessage("Login successful.");
                    String name = (cmd[1].length() <= MAX_LIST_NAME_LENGTH) ? cmd[1] : cmd[1].substring(0, MAX_LIST_NAME_LENGTH);
                    player.setDisplayName(name);
                    String playerFullname = authenticationSource.getFullName(cmd[1]);
                    int maxlen = MAX_LIST_NAME_LENGTH - name.length() - 3;
                    if (maxlen <= 0 || playerFullname == null) {
                        player.setPlayerListName(name);
                    } else {
                        player.setPlayerListName(String.format("%s <%s>", name, playerFullname.substring(0, maxlen)));
                    }
                } else {
                    player.kickPlayer("Failed login");
                }
            } else {
                event.getPlayer().sendRawMessage("You need to log in using \"/login username password\" before you can use commands.");
            }
            event.setCancelled(true); // Don't send it to anyone
        }
    }

    // **********************************************************
    // **** Stop users from doing anything until they log in ****
    // **********************************************************

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer())) {
            event.getPlayer().sendRawMessage("You need to log in using \"/login username password\" before you can speak.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (!authenticatedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!authenticatedPlayers.contains(player)) {
            event.getPlayer().teleport(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        authenticatedPlayers.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerKick(PlayerKickEvent event) {
        authenticatedPlayers.remove(event.getPlayer());
    }
}
