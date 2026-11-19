package dk.mineconed.mobstacker;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Made by Mineconed
 *
 * Loads messages_en.yml or messages_da.yml and colors them.
 */
public final class Beskeder {

    private final JavaPlugin pl;
    private Tekster t;

    public Beskeder(JavaPlugin pl) {
        this.pl = pl;
        laes();
    }

    public void laes() {
        for (String n : new String[]{"messages_en.yml", "messages_da.yml"}) {
            if (!new File(pl.getDataFolder(), n).exists()) {
                pl.saveResource(n, false);
            }
        }
        String sprog = pl.getConfig().getString("language", "en");
        File f = new File(pl.getDataFolder(), "messages_" + sprog + ".yml");
        if (!f.exists()) {
            f = new File(pl.getDataFolder(), "messages_en.yml");
            sprog = "en";
        }
        t = Tekster.laes(f, pl.getResource(sprog.equals("da") ? "messages_da.yml" : "messages_en.yml"));
    }

    public String raw(String k, String... ph) {
        String s = t.get(k, k);
        for (int i = 0; i + 1 < ph.length; i += 2) {
            s = s.replace(ph[i], ph[i + 1] == null ? "" : ph[i + 1]);
        }
        return s;
    }

    public String c(String k, String... ph) {
        return ChatColor.translateAlternateColorCodes('&', raw(k, ph));
    }

    public List<String> linjer(String k, String... ph) {
        return new ArrayList<String>(Arrays.asList(c(k, ph).split("\n")));
    }

    public void send(CommandSender s, String k, String... ph) {
        String tekst = c(k, ph);
        if (!tekst.isEmpty()) {
            s.sendMessage(c("prefix") + tekst);
        }
    }

    public static String farve(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
