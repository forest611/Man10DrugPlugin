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



            /////////////////////////////
            //ItemStackの作成
            //////////////////////////////
            val drugItem = ItemStack(Material.valueOf(data.material),1)
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

    private fun getHMList(path:String, cfg:YamlConfiguration): HashMap<Int, MutableList<String>> {

        val map = HashMap<Int,MutableList<String>>()

        if (cfg.getConfigurationSection(path) == null)return map

        for (i in cfg.getConfigurationSection(path)!!.getKeys(false)) {
            map[i.toInt()] = cfg.getStringList("$path.$i")
        }
        return map

    }


    private fun getBuff(type:Type, cfg: YamlConfiguration):HashMap<Int,MutableList<PotionEffect>>{

        val path = when(type){

            Type.NORMAL->"buff"
            Type.RANDOM->"buffRandom"
            Type.SYMPTOMS->"buffSymptoms"

        }

        val retMap = HashMap<Int,MutableList<PotionEffect>>()

        for (map in getHMList(path,cfg)){

            val r = mutableListOf<PotionEffect>()

            for (data in map.value){

                val s = data.split(",")

                val effect = PotionEffect(PotionEffectType.getByName(s[0])!!,s[1].toInt(),s[2].toInt(),false,false)

                r.add(effect)
            }

            retMap[map.key] = r
        }

        return retMap

    }

    private fun getSound(type: Type, cfg: YamlConfiguration):HashMap<Int,MutableList<SoundData>>{

        val path = when(type){

            Type.NORMAL->"sound"
            Type.RANDOM->"soundRandom"
            Type.SYMPTOMS->"soundSymptoms"

        }

        val retMap = HashMap<Int,MutableList<SoundData>>()

        for (map in getHMList(path,cfg)){

            val r = mutableListOf<SoundData>()

            for (data in map.value){
                val s = data.split(",")

                val sound = SoundData()
                sound.sound = s[0]
                sound.volume = s[1].toFloat()
                sound.pitch = s[2].toFloat()

                r.add(sound)
            }

            retMap[map.key] = r
        }

        return retMap
    }

    private fun getParticle(type: Type, cfg:YamlConfiguration): HashMap<Int, MutableList<ParticleData>> {

        val path = when(type){

            Type.NORMAL->"particle"
            Type.RANDOM->"particleRandom"
            Type.SYMPTOMS->"particleSymptoms"

        }
        val retMap = HashMap<Int,MutableList<ParticleData>>()

        for (map in getHMList(path,cfg)){

            val r = mutableListOf<ParticleData>()

            for (data in map.value){
                val s = data.split(",")

                val particle = ParticleData()
                particle.particle = Particle.valueOf(s[0])
                particle.size = s[1].toInt()

                r.add(particle)
            }

            retMap[map.key] = r

        }

        return retMap
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
        var material = "DIAMOND_HOE"
        var modelData = 0
        var type = 0


        //クールダウン
        var cooldown : Long = 0

        //任意
        var changeItem = "none" //使用後、アイテムを変更する場合
        var isChange = false //違う変えるか

        //HashMap key...Level,value...mutableList<String>
        //type0 以外はレベルがないので、Level0のみをかく

        //全レベル使用可
        //lore
        var lore = mutableListOf<String>()
        //message
        var useMsg = mutableListOf<String>()

        var hasEnchantEffect = false

        var parameter = mutableListOf<DrugParameter>()

        //type1(治療薬など)
        var weakDrug = "drug" //type2
        var weakProb  = mutableListOf<Double>()//飲むときに、確率で治る
        var stopDepend = false

        //type2(マスクなど)
        var defenseProb :Double = 0.0

    }

    class DrugParameter{

        var msg = ""

        var func = ""

        var buff = mutableListOf<PotionEffect>()
        var buffRandom = mutableListOf<PotionEffect>()

        var particle = mutableListOf<ParticleData>()
        var particleRandom = mutableListOf<ParticleData>()

        var sound = mutableListOf<SoundData>()
        var soundRandom = mutableListOf<SoundData>()

        var cmd = mutableListOf<String>()
        var cmdRandom = mutableListOf<String>()

        var serverCmd = mutableListOf<String>()
        var serverCmdRandom = mutableListOf<String>()

        var isRemoveBuff = false
        var isRemoveItem = true

        var dependLvUp : Double = 0.5
        var dependLvDown : Double = 0.5

        var symptomsFirstTime = 0 //使用時に最初の禁断症状が来る時間(秒)
        var symptomsTime = 0 //2回目以降に禁断症状が来る時間(秒)

        var symptomsMsg = ""
        var dependMsg = ""

        var buffSymptoms = mutableListOf<PotionEffect>()

        var particleSymptoms = mutableListOf<ParticleData>()

        var soundSymptoms = mutableListOf<SoundData>()

        var cmdSymptoms = mutableListOf<String>()

        var serverCmdSymptoms = mutableListOf<String>()

        var funcSymptoms = ""

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

    enum class Type{
        NORMAL,
        RANDOM,
        SYMPTOMS

    }
}