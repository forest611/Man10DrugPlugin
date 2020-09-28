package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.rep
import java.io.File
import java.util.*
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


            data.cmd = yml.getStringList("cmd")
            data.cmdRandom = yml.getStringList("cmdRandom")

            //buffname tick level
            data.buff = getBuff(yml.getStringList("buff"))
            data.buffRandom = getBuff(yml.getStringList("buffRandom"))

            data.sound = getSound(yml.getStringList("sound"))
            data.soundRandom = getSound(yml.getStringList("soundRandom"))

            data.particle = getParticle(yml.getStringList("particle"))
            data.particleRandom = getParticle(yml.getStringList("particleRandom"))

            data.runDrug = yml.getStringList("runDrug")

            data.msg = yml.getStringList("msg")

            funcData[yml.getString("name")!!] = data
        }
    }

    fun runFunc(name:String,p: Player){
        val data = funcData[name]?:return

        if (data.msg.isNotEmpty()){
            for (msg in data.msg){
                p.sendMessage(rep(msg,p,name))
            }
        }

        if (data.buff.isNotEmpty()){
            for (b in data.buff){
                p.addPotionEffect(b)
            }
        }

        if (data.buffRandom.isNotEmpty()){
            p.addPotionEffect(data.buffRandom[Random().nextInt(data.buffRandom.size-1)])
        }

        if (data.particle.isNotEmpty()){
            for (par in data.particle){
                p.location.world.spawnParticle(par.particle,p.location,par.size)
            }
        }
        if (data.particleRandom.isNotEmpty()){
            val par = data.particleRandom[Random().nextInt(data.particleRandom.size-1)]
            p.location.world.spawnParticle(par.particle,p.location,par.size)
        }

        if (data.sound.isNotEmpty()){
            for (so in data.sound){
                p.location.world.playSound(p.location,so.sound,so.volume,so.pitch)
            }
        }

        if (data.soundRandom.isNotEmpty()){
            val so = data.sound[Random().nextInt(data.sound.size-1)]
            p.location.world.playSound(p.location,so.sound,so.volume,so.pitch)

        }
        if (data.cmd.isNotEmpty()){
            for (c in data.cmd){
                if (p.isOp){
                    p.performCommand(rep(c,p,name))
                    continue
                }
                p.isOp = true
                p.performCommand(rep(c,p,name))
                p.isOp = false
            }
        }
        if (data.cmdRandom.isNotEmpty()){
            if (p.isOp){
                p.performCommand(rep(plugin.random(data.cmdRandom),p,name))
            }else{
                p.isOp = true
                p.performCommand(rep(plugin.random(data.cmdRandom),p,name))
                p.isOp = false
            }
        }
        if (data.runDrug.isNotEmpty()){
            for (d in data.runDrug){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"mdp using $d ${p.name}")
            }
        }

    }

    fun getBuff(list: MutableList<String>):MutableList<PotionEffect>{

        val r = mutableListOf<PotionEffect>()

        for (data in list){

            val s = data.split(",")

            val effect = PotionEffect(PotionEffectType.getByName(s[0])!!,s[1].toInt(),s[2].toInt(),false,false)

            r.add(effect)
        }

        return r
    }

    fun getSound(list:MutableList<String>):MutableList<Configs.SoundData>{

        val r = mutableListOf<Configs.SoundData>()

        for (data in list){
            val s = data.split(",")

            val sound = Configs.SoundData()
            sound.sound = s[0]
            sound.volume = s[1].toFloat()
            sound.pitch = s[2].toFloat()

            r.add(sound)
        }

        return r
    }

    fun getParticle(list:MutableList<String>):MutableList<Configs.ParticleData>{

        val r = mutableListOf<Configs.ParticleData>()

        for (data in list){
            val s = data.split(",")

            val particle = Configs.ParticleData()
            particle.particle = Particle.valueOf(s[0])
            particle.size = s[1].toInt()

            r.add(particle)
        }

        return r
    }

    class Func{

        var msg = mutableListOf<String>()

        var cmd = mutableListOf<String>()
        var cmdRandom = mutableListOf<String>()

        var sound = mutableListOf<Configs.SoundData>()
        var soundRandom = mutableListOf<Configs.SoundData>()

        var particle = mutableListOf<Configs.ParticleData>()
        var particleRandom = mutableListOf<Configs.ParticleData>()

        var buff = mutableListOf<PotionEffect>()
        var buffRandom = mutableListOf<PotionEffect>()

        var runDrug = mutableListOf<String>()
    }

}