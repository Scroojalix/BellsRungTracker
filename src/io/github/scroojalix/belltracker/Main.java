package io.github.scroojalix.belltracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BellRingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class Main extends JavaPlugin implements Listener {

    private Scoreboard scoreboard;
    private Objective objective;

    @Override
    public void onEnable() {
        // Setup scoreboard
        Bukkit.getPluginManager().registerEvents(this, this);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("bells_rung",
                Criteria.DUMMY, ChatColor.RED + "Bells Rung");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Reload entries from text file
        File file = new File(getDataFolder(), "save_data.txt");
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();

                while (line != null) {
                    String[] parsed = line.replace(System.getProperty("line.separator"), "").split(":");
                    objective.getScore(parsed[0]).setScore(Integer.parseInt(parsed[1]));

                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // If server is reloaded, update scoreboard for any online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(scoreboard);
        }
    }

    @Override
    public void onDisable() {
        // Save current scores to a file so they can be recovered after server reload
        try {
            File file = new File(getDataFolder(), "save_data.txt");
            file.getParentFile().mkdirs();
            file.createNewFile();

            // Append each entry to text file
            FileWriter writer = new FileWriter(file);
            for (String entry : scoreboard.getEntries()) {
                int value = objective.getScore(entry).getScore();
                writer.write(String.format("%s:%d\n", entry, value));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("getscore")) {
            if (args.length == 1) {
                String input = args[0];
                Score score = objective.getScore(input);
                if (score.isScoreSet()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        String.format("&e%s &chas a score of &e%d", input, score.getScore())));
                } else {
                    sender.sendMessage(ChatColor.RED + "That player does not have a score.");
                }
            } else { return false; }
        } else if (cmd.getName().equalsIgnoreCase("setscore")) {
            if (args.length == 2) {
                String input = args[0];
                Score score = objective.getScore(input);
                if (score.isScoreSet()) {
                    try {
                        int value = Integer.parseInt(args[1]);
                        score.setScore(value);
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            String.format("&e%s &cnow has a score of &e%d", input, score.getScore())));
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "Please input a valid number");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "That is not a real player.");
                }
            } else { return false; }
        } else if (cmd.getName().equalsIgnoreCase("clearallscores")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
                sender.sendMessage("Cleared all scores");
                for (String entry : scoreboard.getEntries()) {
                    scoreboard.resetScores(entry);
                }
            } else { return false; }
        }
        return true;
    }

    @EventHandler
    public void onBellRing(BellRingEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            Score score = objective.getScore(p.getName());
            score.setScore(score.getScore() + 1);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(scoreboard);
    }
}
