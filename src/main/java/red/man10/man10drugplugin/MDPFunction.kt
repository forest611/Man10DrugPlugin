package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class MDPFunction (private val plugin: Man10DrugPlugin){

    val funcData = ConcurrentHashMap<String,Func>()

    fun loadFunction(){
        funcData.clear()

        val funcFolder = File(Bukkit.getServer()
                .pluginManager.getPlugin("Man10DrugPlugin")
                !!.dataFolder, File.separator+"func")
        if (!funcFolder.exists()){
            funcFolder.mkdir()
        }
        val funcFiles = funcFolder.listFiles().toMutableList()

        for (f in funcFiles){

            if (!f.path.endsWith(".yml") || f.isDirectory){
                Bukkit.getLogger().info("${f.name} はyamlファイルではありません")
                continue
            }

            val yml = YamlConfiguration.loadConfiguration(f)

            val data = Func()

            data.msg = yml.getStringList("msg")

            //buffname tick level
            data.buff = yml.getStringList("buff")
            data.buffRandom = yml.getStringList("buffRandom")

            data.cmd = yml.getStringList("cmd")
            data.cmdRandom = yml.getStringList("cmdRandom")

            data.sound = yml.getStringList("sound")
            data.soundRandom = yml.getStringList("soundRandom")

            data.particle = yml.getStringList("particle")
            data.particleRandom = yml.getStringList("particleRandom")

            data.runDrug = yml.getStringList("runDrug")

            data.msg = yml.getStringList("msg")

            funcData[yml.getString("name")!!] = data
        }
    }

    fun runFunc(name:String,p: Player){
        val data = funcData[name]?:return

        if (data.msg.isNotEmpty()){
            for (msg in data.msg){
                p.sendMessage(msg)
            }
        }

        if (data.buff.isNotEmpty()){
            for (b in data.buff){
                val s = b.split(",")
                p.addPotionEffect(PotionEffect(
                        PotionEffectType.getByName(s[0])!!,
                        s[1].toInt(),s[2].toInt()))
            }
        }
        if (data.buffRandom.isNotEmpty()){
            val s = plugin.random(data.buffRandom).split(",")
            p.addPotionEffect(PotionEffect(
                    PotionEffectType.getByName(s[0])!!,
                    s[1].toInt(),s[2].toInt()))
        }
        if (data.particle.isNotEmpty()){
            for (par in data.particle){
                val s = par.split(",")
                p.location.world.spawnParticle(Particle.valueOf(s[0]),p.location,s[1].toInt())
            }
        }
        if (data.particleRandom.isNotEmpty()){
            val s = plugin.random(data.particleRandom).split(",")
            p.location.world.spawnParticle(Particle.valueOf(s[0]),p.location,s[1].toInt())
        }
        if (data.sound.isNotEmpty()){
            for (so in data.sound){
                val s = so.split(",")
                p.location.world.playSound(p.location,s[0],
                        s[1].toFloat(),s[2].toFloat())
            }
        }
        if (data.soundRandom.isNotEmpty()){
            val s = plugin.random(data.soundRandom).split(",")
            p.location.world.playSound(p.location,s[0],
                    s[1].toFloat(),s[2].toFloat())

        }
        if (data.cmd.isNotEmpty()){
            for (c in data.cmd){
                if (p.isOp){
                    p.performCommand(c)
                    continue
                }
                p.isOp = true
                p.performCommand(c)
                p.isOp = false
            }
        }
        if (data.cmdRandom.isNotEmpty()){
            if (p.isOp){
                p.performCommand(plugin.random(data.cmdRandom))
            }else{
                p.isOp = true
                p.performCommand(plugin.random(data.cmdRandom))
                p.isOp = false
            }
        }
        if (data.runDrug.isNotEmpty()){
            for (d in data.runDrug){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"mdp using $d ${p.name}")
            }
        }

    }


    class Func{

        var msg = mutableListOf<String>()

        var cmd = mutableListOf<String>()
        var cmdRandom = mutableListOf<String>()

        var sound = mutableListOf<String>()
        var soundRandom = mutableListOf<String>()

        var particle = mutableListOf<String>()
        var particleRandom = mutableListOf<String>()

        var buff = mutableListOf<String>()
        var buffRandom = mutableListOf<String>()

        var runDrug = mutableListOf<String>()
    }

}