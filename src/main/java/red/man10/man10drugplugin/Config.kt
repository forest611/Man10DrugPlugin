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

            drugName.add(dataName)

            val data = Drug()

            Bukkit.getLogger().info("Loaded file $dataName (${file.name})")

            val level = 0

            while (cfg.get("$level") != null){

                val parameter = DrugParameter()

                parameter.buff = getBuff(cfg.getStringList("${level}.buff"))
                parameter.buffRandom = getBuff(cfg.getStringList("${level}.buffRandom"))
                parameter.buffSymptoms = getBuff(cfg.getStringList("${level}.buffSymptoms"))

                parameter.cmd = cfg.getStringList("${level}.cmd")
                parameter.cmdRandom = cfg.getStringList("${level}.cmdRandom")
                parameter.cmdSymptoms = cfg.getStringList("${level}.cmdSymptoms")

                parameter.sound = getSound(cfg.getStringList("${level}.sound"))
                parameter.soundRandom = getSound(cfg.getStringList("${level}.soundRandom"))
                parameter.soundSymptoms = getSound(cfg.getStringList("${level}.soundSymptoms"))

                parameter.particle = getParticle(cfg.getStringList("${level}.particle"))
                parameter.particleRandom = getParticle(cfg.getStringList("${level}.particleRandom"))
                parameter.particleSymptoms = getParticle(cfg.getStringList("${level}.particleSymptoms"))

                parameter.serverCmd = cfg.getStringList("${level}.serverCmd")
                parameter.serverCmdRandom = cfg.getStringList("${level}.serverCmdRandom")
                parameter.serverCmdSymptoms = cfg.getStringList("${level}.serverCmdSymptoms")

                parameter.msg = cfg.getString("${level}.msg")?:""
                parameter.msgSymptoms = cfg.getString("${level}.msgSymptoms")?:""

                parameter.func = cfg.getString("${level}.func")?:""
                parameter.funcSymptoms = cfg.getString("${level}.funcSymptoms")?:""

                parameter.isRemoveBuff = cfg.getBoolean("${level}.removeBuff")
                parameter.isRemoveItem = cfg.getBoolean("${level}.removeItem")

                parameter.dependLvUp = cfg.getDouble("${level}.dependLvUp")
                parameter.dependLvDown = cfg.getDouble("${level}.dependLvDown")

                parameter.symptomsFirstTime = cfg.getInt("${level}.symptomsFirstTIme")
                parameter.symptomsTime = cfg.getInt("${level}.symptomsTIme")

                parameter.dependMsg = cfg.getString("${level}.dependMsg")?:""

                data.parameter.add(parameter)
            }

            data.level = level
            data.cooldown = cfg.getLong("cooldown")
            data.displayName = cfg.getString("displayName")?:""
            data.material = Material.valueOf(cfg.getString("material")?:"")
            data.lore = cfg.getStringList("lore")
            data.modelData = cfg.getInt("modelData")
            data.type = cfg.getInt("type")
            data.hasEnchantEffect = cfg.getBoolean("enchantEffect")


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

        var parameter = mutableListOf<DrugParameter>()


    }

    class DrugParameter{

        var msg = ""

        var func = ""
        var funcSymptoms = ""

        var buff = mutableListOf<PotionEffect>()
        var buffRandom = mutableListOf<PotionEffect>()
        var buffSymptoms = mutableListOf<PotionEffect>()

        var particle = mutableListOf<ParticleData>()
        var particleRandom = mutableListOf<ParticleData>()
        var particleSymptoms = mutableListOf<ParticleData>()

        var sound = mutableListOf<SoundData>()
        var soundRandom = mutableListOf<SoundData>()
        var soundSymptoms = mutableListOf<SoundData>()

        var cmd = mutableListOf<String>()
        var cmdRandom = mutableListOf<String>()
        var cmdSymptoms = mutableListOf<String>()

        var serverCmd = mutableListOf<String>()
        var serverCmdRandom = mutableListOf<String>()
        var serverCmdSymptoms = mutableListOf<String>()

        var isRemoveBuff = false
        var isRemoveItem = true

        var dependLvUp : Double = 0.5
        var dependLvDown : Double = 0.5

        var symptomsFirstTime = 0 //使用時に最初の禁断症状が来る時間(秒)
        var symptomsTime = 0 //2回目以降に禁断症状が来る時間(秒)

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