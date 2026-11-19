package dk.mineconed.mobstacker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Made by Mineconed
 *
 * MobStacker: stacks mobs of the same type into one with a number above them.
 */
public final class MobStacker extends JavaPlugin implements Listener {

    private Beskeder msg;
    private final Map<UUID, Integer> antal = new HashMap<UUID, Integer>();
    private final Set<String> sortliste = new HashSet<String>();
    private final Set<String> grunde = new HashSet<String>();
    private final Set<String> helStak = new HashSet<String>();
    private boolean spawner;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        msg = new Beskeder(this);
        indlaes();
        Bukkit.getPluginManager().registerEvents(this, this);
        int sek = getConfig().getInt("merge_task_seconds", 10);
        if (sek > 0) {
            Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    samlAlle();
                }
            }, sek * 20L, sek * 20L);
        }
        getLogger().info("MobStacker is enabled. Made by Mineconed.");
    }

    private void indlaes() {
        sortliste.clear();
        grunde.clear();
        helStak.clear();
        for (String s : getConfig().getStringList("blacklist")) {
            sortliste.add(s.toUpperCase());
        }
        for (String s : getConfig().getStringList("spawn_reasons")) {
            grunde.add(s.toUpperCase());
        }
        for (String s : getConfig().getStringList("kill_whole_stack_causes")) {
            helStak.add(s.toUpperCase());
        }
    }

    // ═══════════════ NAVN ═══════════════

    private static String paentNavn(String type) {
        StringBuilder sb = new StringBuilder();
        for (String d : type.toLowerCase().split("_")) {
            if (d.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(d.charAt(0))).append(d.substring(1));
        }
        return sb.toString();
    }

    private String format() {
        return getConfig().getString("name_format", "&e&l{amount}x &f{type}");
    }

    private void saetNavn(LivingEntity e, int n) {
        antal.put(e.getUniqueId(), n);
        if (n <= 1) {
            e.setCustomName(null);
            e.setCustomNameVisible(false);
            return;
        }
        String navn = format().replace("{amount}", String.valueOf(n)).replace("{type}", paentNavn(e.getType().name()));
        e.setCustomName(ChatColor.translateAlternateColorCodes('&', navn));
        e.setCustomNameVisible(getConfig().getBoolean("always_show_name", true));
    }

    /** Reads the stack size, also from the name after a restart. */
    int antal(LivingEntity e) {
        Integer n = antal.get(e.getUniqueId());
        if (n != null) {
            return n;
        }
        String navn = e.getCustomName();
        if (navn == null) {
            return 1;
        }
        String f = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                format().replace("{type}", paentNavn(e.getType().name()))));
        int i = f.indexOf("{amount}");
        if (i < 0) {
            return 1;
        }
        Pattern p = Pattern.compile(Pattern.quote(f.substring(0, i)) + "(\\d+)" + Pattern.quote(f.substring(i + 8)));
        Matcher m = p.matcher(ChatColor.stripColor(navn));
        if (m.matches()) {
            try {
                int v = Integer.parseInt(m.group(1));
                antal.put(e.getUniqueId(), v);
                return v;
            } catch (NumberFormatException ignore) {
                return 1;
            }
        }
        return 1;
    }

    private boolean erStak(LivingEntity e) {
        return e.getCustomName() == null || antal(e) > 1;
    }

    // ═══════════════ REGLER ═══════════════

    private boolean kanStakkes(Entity en) {
        if (!(en instanceof LivingEntity) || en instanceof Player || en instanceof ArmorStand || !en.isValid()) {
            return false;
        }
        LivingEntity e = (LivingEntity) en;
        if (sortliste.contains(e.getType().name()) || getConfig().getStringList("disabled_worlds").contains(e.getWorld().getName())) {
            return false;
        }
        if (e.isLeashed() || e.getPassenger() != null || e.isInsideVehicle()) {
            return false;
        }
        if (e instanceof Tameable && ((Tameable) e).isTamed()) {
            return false;
        }
        return erStak(e);
    }

    private static boolean ens(LivingEntity a, LivingEntity b) {
        if (a.getType() != b.getType()) {
            return false;
        }
        if (a instanceof Ageable && ((Ageable) a).isAdult() != ((Ageable) b).isAdult()) {
            return false;
        }
        if (a instanceof Slime && ((Slime) a).getSize() != ((Slime) b).getSize()) {
            return false;
        }
        if (a instanceof Sheep && (((Sheep) a).getColor() != ((Sheep) b).getColor() || ((Sheep) a).isSheared() != ((Sheep) b).isSheared())) {
            return false;
        }
        return true;
    }

    private int maks() {
        return Math.max(2, getConfig().getInt("max_stack_size", 50));
    }

    /** Puts e into a stack close by. Returns true if e was removed. */
    private boolean saml(LivingEntity e) {
        if (!kanStakkes(e)) {
            return false;
        }
        int mit = antal(e);
        if (mit >= maks()) {
            return false;
        }
        double r = getConfig().getDouble("radius", 6.0);
        LivingEntity bedst = null;
        int bedstAntal = 0;
        for (Entity en : e.getNearbyEntities(r, r, r)) {
            if (!kanStakkes(en)) {
                continue;
            }
            LivingEntity o = (LivingEntity) en;
            if (!ens(e, o)) {
                continue;
            }
            int n = antal(o);
            if (n < maks() && n >= mit && n > bedstAntal) {
                bedst = o;
                bedstAntal = n;
            }
        }
        if (bedst == null) {
            return false;
        }
        int sammen = bedstAntal + mit;
        int rest = Math.max(0, Math.subtractExact(sammen, maks()));
        saetNavn(bedst, Math.min(sammen, maks()));
        if (rest > 0) {
            saetNavn(e, rest);
            return false;
        }
        antal.remove(e.getUniqueId());
        e.remove();
        return true;
    }

    private void samlAlle() {
        int graense = getConfig().getInt("max_checks_per_run", 400);
        int tjekket = 0;
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class)) {
                if (tjekket >= graense) {
                    return;
                }
                if (e.isValid() && kanStakkes(e)) {
                    tjekket++;
                    saml(e);
                }
            }
        }
        // forget removed mobs
        if (antal.size() > 5000) {
            List<UUID> fjern = new ArrayList<UUID>();
            Set<UUID> levende = new HashSet<UUID>();
            for (World w : Bukkit.getWorlds()) {
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class)) {
                    levende.add(e.getUniqueId());
                }
            }
            for (UUID u : antal.keySet()) {
                if (!levende.contains(u)) {
                    fjern.add(u);
                }
            }
            for (UUID u : fjern) {
                antal.remove(u);
            }
        }
    }

    // ═══════════════ EVENTS ═══════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (spawner || !grunde.contains(e.getSpawnReason().name())) {
            return;
        }
        final LivingEntity en = e.getEntity();
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (en.isValid()) {
                    saml(en);
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity d = e.getEntity();
        if (d instanceof Player) {
            return;
        }
        int n = antal(d);
        antal.remove(d.getUniqueId());
        if (n <= 1) {
            return;
        }
        EntityDamageEvent sidst = d.getLastDamageCause();
        String aarsag = sidst == null ? "" : sidst.getCause().name();
        if (helStak.contains(aarsag)) {
            for (ItemStack i : e.getDrops()) {
                if (i != null && i.getType() != Material.AIR) {
                    i.setAmount(Math.min(i.getAmount() * n, i.getMaxStackSize() * 4));
                }
            }
            List<ItemStack> ekstra = new ArrayList<ItemStack>();
            for (ItemStack i : e.getDrops()) {
                int over = Math.subtractExact(i.getAmount(), i.getMaxStackSize());
                while (over > 0) {
                    ItemStack k = i.clone();
                    k.setAmount(Math.min(over, i.getMaxStackSize()));
                    ekstra.add(k);
                    over = Math.subtractExact(over, k.getAmount());
                }
                i.setAmount(Math.min(i.getAmount(), i.getMaxStackSize()));
            }
            e.getDrops().addAll(ekstra);
            e.setDroppedExp(e.getDroppedExp() * n);
            return;
        }
        Location l = d.getLocation();
        spawner = true;
        try {
            Entity ny = l.getWorld().spawnEntity(l, d.getType());
            if (ny instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) ny;
                if (le instanceof Ageable && d instanceof Ageable && !((Ageable) d).isAdult()) {
                    ((Ageable) le).setBaby();
                }
                if (le instanceof Slime && d instanceof Slime) {
                    ((Slime) le).setSize(((Slime) d).getSize());
                }
                if (le instanceof Sheep && d instanceof Sheep) {
                    ((Sheep) le).setColor(((Sheep) d).getColor());
                    ((Sheep) le).setSheared(((Sheep) d).isSheared());
                }
                saetNavn(le, Math.subtractExact(n, 1));
            }
        } finally {
            spawner = false;
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onNavneskilt(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof LivingEntity)) {
            return;
        }
        ItemStack i = e.getPlayer().getItemInHand();
        if (i != null && i.getType() == Material.NAME_TAG && antal((LivingEntity) e.getRightClicked()) > 1) {
            e.setCancelled(true);
            msg.send(e.getPlayer(), "no_nametag");
        }
    }

    // ═══════════════ KOMMANDO ═══════════════

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("mobstacker.admin")) {
            msg.send(s, "no_permission");
            return true;
        }
        String a1 = a.length > 0 ? a[0].toLowerCase() : "";
        if (a1.equals("reload")) {
            reloadConfig();
            msg.laes();
            indlaes();
            msg.send(s, "reloaded");
            return true;
        }
        if (a1.equals("stats")) {
            int stakke = 0;
            int mobs = 0;
            for (World w : Bukkit.getWorlds()) {
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class)) {
                    int n = e instanceof Player ? 1 : antal(e);
                    if (n > 1) {
                        stakke++;
                        mobs += n;
                    }
                }
            }
            msg.send(s, "stats", "{stacks}", String.valueOf(stakke), "{mobs}", String.valueOf(mobs),
                    "{saved}", String.valueOf(Math.max(0, Math.subtractExact(mobs, stakke))));
            return true;
        }
        if (a1.equals("stackall")) {
            samlAlle();
            msg.send(s, "stacked");
            return true;
        }
        msg.send(s, "usage", "{label}", label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> ud = new ArrayList<String>();
        if (a.length == 1) {
            for (String v : new String[]{"stats", "stackall", "reload"}) {
                if (v.startsWith(a[0].toLowerCase())) {
                    ud.add(v);
                }
            }
        }
        return ud;
    }
}
