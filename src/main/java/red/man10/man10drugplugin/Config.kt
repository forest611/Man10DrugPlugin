package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.debugMode
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.disableWorld
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugData
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugName
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.plugin
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.pluginEnable
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.useMilk
import java.io.File
import java.lang.Exception

object Config{



    /////////////////////////////
    //ドラッグ読み込み(起動時)
    /////////////////////////
    fun loadDrugs(){
        drugName.clear()
        drugData.clear()

        val drugFolder = File(Bukkit.getServer()
                .pluginManager.getPlugin("Man10DrugPlugin")!!
                .dataFolder, File.separator)

        if (!drugFolder.exists()){
            Bukkit.getLogger().info("プラグインフォルダが見つかりません")
            return
        }

        val drugFiles = drugFolder.listFiles().toMutableList()

        for (file in drugFiles){
            if (file.name == "config.yml" || !file.path.endsWith(".yml") || file.isDirectory){
                Bukkit.getLogger().info("${file.name} はyamlファイルではありません")
                continue
            }

            val cfg = YamlConfiguration.loadConfiguration(file)

            val dataName = cfg.getString("dataName")

            if (dataName == null){
                Bukkit.getLogger().info("”dataName”が設定されていません")
                continue
            }

            val data = Drug()

            Bukkit.getLogger().info("Loaded file $dataName (${file.name})")

            try {
                val parameter = DrugParameter()

                parameter.buff = getBuff(cfg.getStringList("buff"))
                parameter.buffRandom = getBuffRandom(cfg.getStringList("buffRandom"))
                parameter.buffSymptoms = getBuff(cfg.getStringList("buffSymptoms"))

                parameter.cmd = cfg.getStringList("cmd")
                parameter.cmdRandom = getRandom(cfg.getStringList("cmdRandom"))
                parameter.cmdSymptoms = cfg.getStringList("cmdSymptoms")

                parameter.sound = getSound(cfg.getStringList("sound"))
                parameter.soundRandom = getSoundRandom(cfg.getStringList("soundRandom"))
                parameter.soundSymptoms = getSound(cfg.getStringList("soundSymptoms"))

                parameter.particle = getParticle(cfg.getStringList("particle"))
                parameter.particleRandom = getParticleRandom(cfg.getStringList("particleRandom"))
                parameter.particleSymptoms = getParticle(cfg.getStringList("particleSymptoms"))

                parameter.serverCmd = cfg.getStringList("serverCmd")
                parameter.serverCmdRandom = getRandom(cfg.getStringList("serverCmdRandom"))
                parameter.serverCmdSymptoms = cfg.getStringList("serverCmdSymptoms")

                parameter.msg = cfg.getString("msg")?:""
                parameter.msgSymptoms = cfg.getString("msgSymptoms")?:""

                parameter.func = cfg.getString("func")?:""
                parameter.funcSymptoms = cfg.getString("funcSymptoms")?:""

                parameter.isRemoveBuff = cfg.getBoolean("removeBuff",true)
                parameter.isRemoveItem = cfg.getBoolean("removeItem",true)

                parameter.dependLvUp = cfg.getDouble("dependLvUp")
                parameter.dependLvDown = cfg.getDouble("dependLvDown")

                parameter.symptomsFirstTime = cfg.getInt("symptomsFirstTime")
                parameter.symptomsTime = cfg.getInt("symptomsTime")
                parameter.symptomsStopProb = cfg.getDouble("symptomsStopProb")

                parameter.dependMsg = cfg.getString("dependMsg")?:""

                data.parameter = parameter

            }catch (e:Exception){
                Bukkit.getLogger().info("ドラッグファイルのエラー：${dataName}")
                Bukkit.getLogger().info(e.message)
                continue
            }

            if (data.level < 0){
                Bukkit.getLogger().info("${dataName}パラメータが設定されていません")
                continue
            }

            data.displayName = cfg.getString("displayName")?:""
            data.material = Material.valueOf(cfg.getString("material")?:"IRON_NUGGET")
            data.lore = cfg.getStringList("lore")
            data.modelData = cfg.getInt("modelData")

            data.hasEnchantEffect = cfg.getBoolean("enchantEffect")
            data.disableWorld = cfg.getStringList("disableWorld")
            data.cooldown = cfg.getLong("cooldown")
            data.type = cfg.getInt("type")


            /////////////////////////////
            //ItemStackの作成
            //////////////////////////////
            val drugItem = ItemStack(data.material,1)
            val meta = drugItem.itemMeta
            //NBTTag追加
            meta.persistentDataContainer.set(NamespacedKey(plugin,"name"), PersistentDataType.STRING,dataName)

            meta.setCustomModelData(data.modelData)

            meta.setDisplayName(data.displayName)
            if (data.hasEnchantEffect){
                meta.addEnchant(Enchantment.DURABILITY,0,false)
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON)
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
            meta.isUnbreakable = true

            meta.lore = data.lore

            drugItem.itemMeta = meta

            data.itemStack = drugItem

            drugData[dataName] = data
            drugName.add(dataName)
        }
    }

    private fun getBuff(mutableList: MutableList<String>):MutableList<PotionEffect>{

        val ret = mutableListOf<PotionEffect>()

        for (data in mutableList){

            val s = data.split(",")

            val effect = PotionEffect(PotionEffectType.getByName(s[0])!!,s[1].toInt(),s[2].toInt(),false,false)

            ret.add(effect)
        }

        return ret

    }

    private fun getSound(mutableList: MutableList<String>):MutableList<SoundData>{

        val ret = mutableListOf<SoundData>()

        for (data in mutableList){
            val s = data.split(",")

            val sound = SoundData()
            sound.sound = s[0]
            sound.volume = s[1].toFloat()
            sound.pitch = s[2].toFloat()

            ret.add(sound)
        }

        return ret
    }

    private fun getParticle(mutableList: MutableList<String>): MutableList<ParticleData> {

        val ret = mutableListOf<ParticleData>()

        for (data in mutableList){
            val s = data.split(",")

            val particle = ParticleData()
            particle.particle = Particle.valueOf(s[0])
            particle.size = s[1].toInt()

            ret.add(particle)
        }

        return ret
    }
    private fun getBuffRandom(mutableList: MutableList<String>):MutableList<Pair<PotionEffect,Double>>{

        val ret = mutableListOf<Pair<PotionEffect,Double>>()

        for (data in mutableList){

            val split = data.split(";")

            val param = split[0].split(",")

            val effect = PotionEffect(PotionEffectType.getByName(param[0])!!,param[1].toInt(),param[2].toInt(),false,false)

            ret.add(Pair(effect,split[1].toDouble()))
        }

        return ret

    }

    private fun getSoundRandom(mutableList: MutableList<String>):MutableList<Pair<SoundData,Double>>{

        val ret = mutableListOf<Pair<SoundData,Double>>()

        for (data in mutableList){

            val split = data.split(";")

            val param = split[0].split(",")

            val sound = SoundData()
            sound.sound = param[0]
            sound.volume = param[1].toFloat()
            sound.pitch = param[2].toFloat()

            ret.add(Pair(sound,split[1].toDouble()))
        }

        return ret
    }

    private fun getParticleRandom(mutableList: MutableList<String>): MutableList<Pair<ParticleData,Double>> {

        val ret = mutableListOf<Pair<ParticleData,Double>>()

        for (data in mutableList){

            val split = data.split(";")

            val param = split[0].split(",")

            val particle = ParticleData()
            particle.particle = Particle.valueOf(param[0])
            particle.size = param[1].toInt()

            ret.add(Pair(particle,split[1].toDouble()))
        }

        return ret
    }

    private fun getRandom(mutableList: MutableList<String>):MutableList<Pair<String,Double>>{

        val list = mutableListOf<Pair<String,Double>>()

        for (str in mutableList){
            val split = str.split(";")
            list.add(Pair(split[0],split[1].toDouble()))
        }

        return list

    }


    fun loadPluginConfig(){
        plugin.saveDefaultConfig()

        plugin.reloadConfig()

        pluginEnable = plugin.config.getBoolean("enableplugin",true)
        useMilk = plugin.config.getBoolean("usemilk",false)
        debugMode = plugin.config.getBoolean("debugmode",false)
        disableWorld = plugin.config.getStringList("disableworld")

    }

    class Drug{

        var itemStack :ItemStack? = null
        //必須
        var displayName = "drug"
        var material = Material.IRON_NUGGET
        var modelData = 0
        var type = 0
        //クールダウン
        var cooldown : Long = 0
        //lore
        var lore = mutableListOf<String>()

        var level = 0

        var hasEnchantEffect = false

        var parameter = DrugParameter()

        var disableWorld = mutableListOf<String>()

    }

    class DrugParameter{

        var msg = ""

        var func = ""
        var funcSymptoms = ""

        var buff = mutableListOf<PotionEffect>()
        var buffRandom = mutableListOf<Pair<PotionEffect,Double>>()
        var buffSymptoms = mutableListOf<PotionEffect>()

        var particle = mutableListOf<ParticleData>()
        var particleRandom = mutableListOf<Pair<ParticleData,Double>>()
        var particleSymptoms = mutableListOf<ParticleData>()

        var sound = mutableListOf<SoundData>()
        var soundRandom = mutableListOf<Pair<SoundData,Double>>()
        var soundSymptoms = mutableListOf<SoundData>()

        var cmd = mutableListOf<String>()
        var cmdRandom = mutableListOf<Pair<String,Double>>()
        var cmdSymptoms = mutableListOf<String>()

        var serverCmd = mutableListOf<String>()
        var serverCmdRandom = mutableListOf<Pair<String,Double>>()
        var serverCmdSymptoms = mutableListOf<String>()

        var isRemoveBuff = false
        var isRemoveItem = true

        var dependLvUp : Double = 0.5
        var dependLvDown : Double = 0.5

        var symptomsFirstTime = 0 //使用時に最初の禁断症状が来る時間(秒)
        var symptomsTime = 0 //2回目以降に禁断症状が来る時間(秒)
        var symptomsStopProb = 0.0

        var msgSymptoms = ""
        var dependMsg = ""


    }

    class ParticleData{
        lateinit var particle : Particle
        var size = 0
    }

    class SoundData{
        var sound = ""
        var volume = 0.0F
        var pitch = 0.0F
    }

}