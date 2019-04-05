package red.man10.man10drugplugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import jp.hishidama.eval.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MDPFunction {

    Man10DrugPlugin plugin;
    HashMap<String,FuncData> list;
    File funcdata;

    public MDPFunction(Man10DrugPlugin plugin){
        this.plugin = plugin;
        list = new HashMap<>();
        funcdata = new File(plugin.getDataFolder(), File.separator + "func");
        if(!funcdata.exists()){
            funcdata.mkdir();
        }
        loadAllFile();
    }

    private void loadAllFile(){
        for(String file : fileList()){
            FuncData data = new FuncData(file);
            list.put(data.name,data);
            plugin.getLogger().info("(func) loaded "+data.name);
        }
    }

    public List<String> fileList() {
        List<String> lis = new ArrayList<>();
        File[] files = funcdata.listFiles();  // (a)
        if(files==null){
            return lis;
        }
        for (File f : files) {
            if (f.isFile()){  // (c)
                String filename = f.getName();

                if(filename.substring(0,1).equalsIgnoreCase(".")){
                    continue;
                }

                int point = filename.lastIndexOf(".");
                if (point != -1) {
                    filename =  filename.substring(0, point);
                }
                lis.add(filename);
            }
        }

        return lis;
    }

    public void reloadAllFile(){
        list = new HashMap<>();
        loadAllFile();
    }

    public Double calcString(String str){
        Rule rule = ExpRuleFactory.getDefaultRule();
        Expression exp = rule.parse(str);	//解析
        return exp.evalDouble();
    }

    public boolean runFunc(Player p, String name){
        if(name.startsWith("vault:")){
            String dorw = name.replaceFirst("vault:","");
            String[] dw = dorw.split(" ");
            if(dw[0].equalsIgnoreCase("deposit")){
                Double r = calcString(dw[1].replaceAll("<player_balance>",plugin.getVault().getBalance(p.getUniqueId())+""));
                plugin.getVault().deposit(p.getUniqueId(),r);
                return true;
            }else if(dw[0].equalsIgnoreCase("withdraw")){
                Double r = calcString(dw[1].replaceAll("<player_balance>",plugin.getVault().getBalance(p.getUniqueId())+""));
                plugin.getVault().withdraw(p.getUniqueId(),r);
                return true;
            }
        }
        if(!list.containsKey(name)){
            return false;
        }
        FuncData data = list.get(name);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{

            //conditions
            for(String s: data.conditions){
                String[] r = s.split(";");
                if(r[0].equalsIgnoreCase("balance")){
                    Double result = calcString(r[1].replaceAll("<player_balance>",plugin.getVault().getBalance(p.getUniqueId())+""));
                    if(result!=1){
                        if(r.length >= 3){
                            runFunc(p,r[2]);
                        }
                        return;
                    }
                }else if(r[0].equalsIgnoreCase("haspermission")){
                    if(!p.hasPermission(r[1])){
                        if(r.length >= 3){
                            runFunc(p,r[2]);
                        }
                        return;
                    }
                }else if(r[0].equalsIgnoreCase("nothaspermission")){
                    if(p.hasPermission(r[1])){
                        if(r.length >= 3){
                            runFunc(p,r[2]);
                        }
                        return;
                    }
                }else if(r[0].equalsIgnoreCase("inregion")){
                    if(!playerInRegion(p,r[1])){
                        if(r.length >= 3){
                            runFunc(p,r[2]);
                        }
                        return;
                    }
                }
            }

            //msg
            for(String s:data.msg){
                p.sendMessage(rep(p,s));
            }
            //msgDelay
            for(String s: data.msgDelay){
                String[] times = s.split(";");
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,()->{
                    p.sendMessage(rep(p,times[0]));
                },Long.parseLong(times[1]));
            }
            //cmd
            for(String s:data.cmd){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),rep(p,s));
            }
            //cmdRandom
            if(data.cmdRandom.size()!=0) {
                Random rnd = new SecureRandom();
                int r = rnd.nextInt(data.cmdRandom.size());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rep(p, data.cmdRandom.get(r)));
            }
            //cmdDelay
            for(String s:data.cmdDelay){
                String[] times = s.split(";");
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,()->{
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),rep(p,times[0]));
                },Long.parseLong(times[1]));
            }
            //cmdRandomDelay
            if(data.cmdRandomDelay.size()!=0) {
                Random rnds = new SecureRandom();
                int rs = rnds.nextInt(data.cmdRandomDelay.size());
                String s = data.cmdRandomDelay.get(rs);
                String[] times = s.split(";");
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rep(p, times[0]));
                }, Long.parseLong(times[1]));
            }
            //sound
            for(String ss : data.sound){
                playSound(p,ss);
            }
            //soundDelay
            for(String ss : data.soundDelay){
                String[] timess = ss.split(";");
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,()->{
                    playSound(p,timess[0]);
                },Long.parseLong(timess[1]));
            }


            //func
            for(String ss : data.func){
                runFunc(p,ss);
            }
            //func Delay
            for(String ss : data.funcDelay){
                String[] timess = ss.split(";");
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,()->{
                    runFunc(p,timess[0]);
                },Long.parseLong(timess[1]));
            }
            //func Random
            if(data.funcRandom.size()!=0) {
                Random rnd = new SecureRandom();
                int r = rnd.nextInt(data.funcRandom.size());
                runFunc(p,data.funcRandom.get(r));
            }
            //func Random Delay
            if(data.funcRandomDelay.size()!=0) {
                Random rnd = new SecureRandom();
                int r = rnd.nextInt(data.funcRandomDelay.size());
                String[] timess = data.funcRandomDelay.get(r).split(";");
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,()->{
                    runFunc(p,timess[0]);
                },Long.parseLong(timess[1]));
            }
        });

        //buff
        for (String b : data.buff){
            String[] buf = b.split(",");
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByName(buf[0]),
                    Integer.parseInt(buf[1]),
                    Integer.parseInt(buf[2])));

        }
        //buff random
        if (data.buffRandom.size() != 0){
            Random rnd = new SecureRandom();
            int r = rnd.nextInt(data.cmdRandom.size());

            String[] buf = data.buff.get(r).split(",");
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.getByName(buf[0]),
                    Integer.parseInt(buf[1]),
                    Integer.parseInt(buf[2])));

        }

        return true;
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugins = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugins instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugins;
    }

    public boolean playerInRegion(Player player,String id) {
        ProtectedRegion region = getWorldGuard().getRegionManager(player.getWorld()).getRegion(id);

        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();

        if (region.contains(x, y, z)) {
            return true;
        } else {
            return false;
        }
    }

    public void playSound(Player p ,String s){
        String[] sounds = s.split(",");
        p.getWorld().playSound(p.getLocation(), Sound.valueOf(sounds[0]),Float.parseFloat(sounds[1]),Float.parseFloat(sounds[2]));
    }

    public String rep(Player p,String s){
        return s.replace("<player>",p.getName());
    }

    class FuncData{

        //Name
        String name;

        //conditions
        List<String> conditions = new ArrayList<>();

        //msg
        List<String> msg = new ArrayList<>();
        List<String> msgDelay = new ArrayList<>();
        //cmd
        List<String> cmd = new ArrayList<>();
        List<String> cmdRandom = new ArrayList<>();
        List<String> cmdDelay = new ArrayList<>();
        List<String> cmdRandomDelay = new ArrayList<>();
        //sound
        List<String> sound = new ArrayList<>();
        List<String> soundDelay = new ArrayList<>();
        //buff
        List<String> buff = new ArrayList<>();
        List<String> buffRandom = new ArrayList<>();
        //another func
        List<String> func = new ArrayList<>();
        List<String> funcRandom = new ArrayList<>();
        List<String> funcDelay = new ArrayList<>();
        List<String> funcRandomDelay = new ArrayList<>();

        public FuncData(String fileName){
            File f = new File(funcdata, File.separator + fileName + ".yml");
            FileConfiguration data = YamlConfiguration.loadConfiguration(f);
            //name get
            name = data.getString("name","exampleFunc");

            //conditions get
            if(data.contains("conditions")){
                conditions = data.getStringList("conditions");
            }

            //msg get
            if(data.contains("msg")){
                msg = data.getStringList("msg");
            }
            //msgDelay get
            if(data.contains("msgdelay")){
                msgDelay = data.getStringList("msgdelay");
            }
            //cmd get
            if(data.contains("cmd")){
                cmd = data.getStringList("cmd");
            }
            //cmdDelay get
            if(data.contains("cmddelay")){
                cmdDelay = data.getStringList("cmddelay");
            }
            //cmdRandom get
            if(data.contains("cmdrandom")){
                cmdRandom = data.getStringList("cmdrandom");
            }
            //cmdRandomDelay get
            if(data.contains("cmdrandomdelay")){
                cmdRandomDelay = data.getStringList("cmdrandomdelay");
            }
            //sound get
            if(data.contains("sound")){
                sound = data.getStringList("sound");
            }
            //soundDelay get
            if(data.contains("sounddelay")){
                soundDelay = data.getStringList("sounddelay");
            }
            //buff
            if(data.contains("buff")){
                buff = data.getStringList("buff");
            }
            if(data.contains("buffrandom")){
                buff = data.getStringList("buffrandom");
            }
            //func get
            if(data.contains("func")){
                func = data.getStringList("func");
            }
            //funcDelay get
            if(data.contains("funcdelay")){
                funcDelay = data.getStringList("funcdelay");
            }
            //funcRandom get
            if(data.contains("funcrandom")){
                funcRandom = data.getStringList("funcrandom");
            }
            //funcRandomDelay get
            if(data.contains("funcrandomdelay")){
                funcRandomDelay = data.getStringList("funcrandomdelay");
            }
        }
    }
}
