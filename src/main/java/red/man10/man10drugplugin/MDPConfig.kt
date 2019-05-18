package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MDPConfig(val plugin: Man10DrugPlugin) {

    val drugData = ConcurrentHashMap<String,Data>()

    /////////////////
    //config 読み込み
    ///////////////
    fun loadConfig(drug:File){


        val config = YamlConfiguration.loadConfiguration(drug)

        val data = get(config.getString("DataName"))
        //コマンド用の名前を登録
        plugin.drugName.add(config.getString("DataName"))

        data.displayName = config.getString("DisplayName")
        data.material = config.getString("Material")
        data.damage = config.getInt("Damage").toShort()
        data.type =  config.getInt("Type")
        data.isChange = config.getBoolean("IsChange")
        if(data.isChange)data.changeItem = config.getString("ChangeItem")

        data.cooldown = config.getLong("cooldown")

        data.lore = config.getStringList("Lore")
        //use message
        data.useMsg = config.getStringList("UseMsg")
        data.useMsgDelay = config.getStringList("UseMsgDelay")
        data.enchantEffect = config.getBoolean("EnchantEffect")

        data.nearPlayer = config.getStringList("NearPlayer")

        //func lists
        data.func = config.getStringList("func")
        data.funcRandom = config.getStringList("funcrandom")
        data.funcDelay = config.getStringList("funcdelay")
        data.funcRandomDelay = config.getStringList("funcrandomdelay")


        //command
        getHashMap("Command",config,data.command)
        getHashMap("CommandRandom",config,data.commandRandom)
        getHashMap("CommandDelay",config,data.commandDelay)
        getHashMap("CommandRandomDelay",config,data.commandRandomDelay)
        //command player
        getHashMap("PlayerCmd",config,data.playerCmd)
        getHashMap("PlayerCmdRandom",config,data.playerCmdRandom)
        getHashMap("PlayerCmdDelay",config,data.playerCmdDelay)
        getHashMap("PlayerCmdRandomDelay",config,data.playerCmdRandomDelay)
        //buff
        getHashMap("Buff",config,data.buff)
        getHashMap("BuffRandom",config,data.buffRandom)
        getHashMap("BuffDelay",config,data.buffDelay)
        getHashMap("BuffRandomDelay",config,data.buffRandomDelay)

        //particle
        getHashMap("Particle",config,data.particle)
        getHashMap("ParticleRandom",config,data.particleRandom)
        getHashMap("ParticleDelay",config,data.particleDelay)
        getHashMap("ParticleRandomDelay",config,data.particleRandomDelay)
        //sound
        getHashMap("Sound",config,data.sound)
        getHashMap("SoundRandom",config,data.soundRandom)
        getHashMap("SoundDelay",config,data.soundDelay)
        getHashMap("SoundRandomDelay",config,data.soundRandomDelay)


        data.isCrashChat = config.getBoolean("IsCrashChat")
        data.crashChance = config.getStringList("CrashChance")

        data.addStock = config.getInt("AddStock")
        data.stockMode = config.getBoolean("IsStock")

        data.removeBuffs = config.getBoolean("RemoveBuffs")

        data.removeItem = config.getBoolean("RemoveItem",true)
        data.hideBuff = config.getBoolean("HideBuff",true)
        data.disableWorld = config.getStringList("DisableWorld")

        data.isAttack = config.getBoolean("IsAttack",false)

        //type 0 only
        if (data.type == 0){
            data.isDependence = config.getBoolean("IsDependence") //禁断症状が出るか
            data.dependenceLevel = config.getInt("DependenceLevel")
            data.nextLevelCount = config.getIntegerList("NextLevelCount")

            data.weakDrug = config.getString("WeakDrug","none")

            data.symptomsTime = config.getLongList("SymptomsTime")
            data.symptomsNextTime = config.getLongList("SymptomsNextTime")
            data.symptomsCount = config.getIntegerList("SymptomsCount")

            data.weakenProbability = config.getIntegerList("WeakenProb")

            getHashMap("BuffSymptoms",config,data.buffSymptoms)
            getHashMap("BuffSymptomsRandom",config,data.buffSymptomsRandom)

            getHashMap("CommandSymptoms",config,data.commandSymptoms)
            getHashMap("CommandSymptomsRandom",config,data.commandSymptomsRandom)

            getHashMap("ParticleSymptoms",config,data.particleSymptoms)
            getHashMap("ParticleSymptomsRandom",config,data.particleSymptomsRandom)

            getHashMap("SoundSymptoms",config,data.soundSymptoms)
            getHashMap("SoundSymptomsRandom",config,data.soundSymptomsRandom)

            getHashMap("CommandLvUp",config,data.commandLvUp)
            getHashMap("CommandRandomLvUp",config,data.commandRandomLvUp)

            data.funcLvUp = config.getStringList("funcLvUp")
            data.funcRandomLvUp = config.getStringList("funcrandomLvUp")
            data.funcDelayLvUp = config.getStringList("funcdelayLvUp")
            data.funcRandomDelayLvUp = config.getStringList("funcrandomdelayLvUp")

            data.msgSymptoms = config.getStringList("MsgSymptoms")

            data.dependenceMsg = config.getStringList("DependenceMsg")

            data.symptomsNearPlayer = config.getStringList("SymptomsNearPlayer")


        }

        //type1 2
        if (data.type == 1 || data.type == 2){
            data.weakDrug = config.getString("WeakDrug") //DataName
            data.weakUsing = config.getIntegerList("WeakUsing")
            data.stopTask = config.getBoolean("StopTask")
        }

        //type3 ... free item

        if(data.type == 4){
            data.defenseNear = config.getInt("DefenseNear")
            data.defenseTouch = config.getInt("DefenseTouch")
            data.usePlace = config.getInt("UsePlace")

            //0 頭装備 1 オンハンド 2オフハンド

        }


        drugData[config.getString("DataName")] = data
    }

    fun get(key:String):Data{
        var data = drugData[key]
        if (data == null){
            data = Data()
        }
        return data
    }

    /////////////////////////////////
    //list in list をhashmapに保存する
    fun getHashMap(path:String,config:FileConfiguration,map:HashMap<Int, MutableList<String>>) {

        if (config.getConfigurationSection(path) == null)return

        for (i in config.getConfigurationSection(path).getKeys(false)) {
            map[i.toInt()] = config.getStringList("$path.$i")
        }
    }
}

//////////////
//ドラッグデータ用クラス
////////////////
class Data{

    //必須
    var displayName = "drug"
    var material = "DIAMOND_HOE"
    var damage : Short = 0
    var type = 0


    //クールダウン
    var cooldown : Long = 0

    //任意
    var changeItem = "none" //使用後、アイテムを変更する場合
    var isChange = false //変えるか

    //HashMap key...Level,value...mutableList<String>
    //type0 以外はレベルがないので、Level0のみをかく

    //全レベル使用可
    //lore
    var lore : MutableList<String>? = null
    //message
    var useMsg : MutableList<String>? = null
    var useMsgDelay : MutableList<String>? = null

    var enchantEffect = false

    /*
    Func: 機能をまとめられる機能
    あらゆるコマンド実行やメッセージ、サウンドプレイを一括にまとめる機能。
    */

    //func
    var func : MutableList<String>? = null
    //funcDelay
    var funcDelay : MutableList<String>? = null
    //funcRandom
    var funcRandom : MutableList<String>? = null
    //funcRandomDelay
    var funcRandomDelay : MutableList<String>? = null


    //command
    val command = HashMap<Int,MutableList<String>>()
    val commandRandom = HashMap<Int,MutableList<String>>()
    val commandDelay = HashMap<Int,MutableList<String>>()
    val commandRandomDelay = HashMap<Int,MutableList<String>>()
    //player dispatch
    val playerCmd = HashMap<Int,MutableList<String>>()
    val playerCmdRandom = HashMap<Int,MutableList<String>>()
    val playerCmdDelay = HashMap<Int,MutableList<String>>()
    val playerCmdRandomDelay = HashMap<Int,MutableList<String>>()
    //buff
    val buff = HashMap<Int,MutableList<String>>()
    val buffRandom = HashMap<Int,MutableList<String>>()
    val buffDelay = HashMap<Int,MutableList<String>>()
    val buffRandomDelay = HashMap<Int,MutableList<String>>()
    //particle
    var particle = HashMap<Int,MutableList<String>>()
    var particleRandom = HashMap<Int,MutableList<String>>()
    var particleDelay = HashMap<Int,MutableList<String>>()
    var particleRandomDelay = HashMap<Int,MutableList<String>>()
    //sound
    var sound = HashMap<Int,MutableList<String>>()
    var soundRandom = HashMap<Int,MutableList<String>>()
    var soundDelay = HashMap<Int,MutableList<String>>()
    var soundRandomDelay = HashMap<Int,MutableList<String>>()

    var isCrashChat = false

    var crashChance : MutableList<String>? = null

    var stockMode = false
    var addStock = 0

    var removeBuffs = false
    var removeItem = true
    var hideBuff = true

    var isAttack = false

    //type0
    var isDependence = false
    var dependenceLevel = 0  //依存レベル
    var nextLevelCount : MutableList<Int>? = null //次のレベルに上がるまでの回数

    //type0 HM
    var symptomsTime : MutableList<Long>? = null
    var symptomsNextTime : MutableList<Long>? = null
    var symptomsCount : MutableList<Int>? = null //何回禁断症状が出るか (終わるまでの回数)

    val buffSymptoms = HashMap<Int,MutableList<String>>()
    val buffSymptomsRandom = HashMap<Int,MutableList<String>>()

    val commandSymptoms = HashMap<Int,MutableList<String>>()
    val commandSymptomsRandom = HashMap<Int,MutableList<String>>()

    var msgSymptoms : MutableList<String>? = null

    var particleSymptoms = HashMap<Int,MutableList<String>>()
    var particleSymptomsRandom = HashMap<Int,MutableList<String>>()

    var soundSymptoms = HashMap<Int,MutableList<String>>()
    var soundSymptomsRandom = HashMap<Int,MutableList<String>>()

    val commandLvUp = HashMap<Int,MutableList<String>>()
    val commandRandomLvUp = HashMap<Int,MutableList<String>>()

    var nearPlayer : MutableList<String>? = null
    var symptomsNearPlayer : MutableList<String>? = null
    var dependenceMsg : MutableList<String>? = null
    var weakenProbability : MutableList<Int>? = null
    var disableWorld : MutableList<String>? = null

    //func
    var funcLvUp : MutableList<String>? = null
    //funcDelay
    var funcDelayLvUp : MutableList<String>? = null
    //funcRandom
    var funcRandomLvUp : MutableList<String>? = null
    //funcRandomDelay
    var funcRandomDelayLvUp : MutableList<String>? = null


    //type1
    var weakDrug = "drug" //type2
    var weakUsing : MutableList<Int>? = null//弱めるのに必要な量
    var stopTask = false //薬で依存を止めるか

    //type4 defense
    var defenseNear = 0
    var defenseTouch = 0
    var usePlace = 0

}