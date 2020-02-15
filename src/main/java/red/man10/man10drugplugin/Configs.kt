package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.lang.Exception

class Configs(private val plugin: Man10DrugPlugin){



    /////////////////////////////
    //ドラッグ読み込み(起動時)
    /////////////////////////
    fun loadDrugs(){
        plugin.drugName.clear()
        plugin.drugData.clear()


        val drugFolder = File(Bukkit.getServer()
                .pluginManager.getPlugin("Man10DrugPlugin")
                .dataFolder, File.separator)

        if (!drugFolder.exists()){
            Bukkit.getLogger().info("プラグインフォルダが見つかりません")
            return
        }

        val drugFiles = drugFolder.listFiles().toMutableList()

        for (file in drugFiles){
            if (file.name == "config.yml" || !file.path.endsWith(".yml") || file.isDirectory){
                Bukkit.getLogger().info("${file.name} Was not drug file.")
                continue
            }

            val cfg = YamlConfiguration.loadConfiguration(file)

            if (cfg.getString("dataName") == null){
                Bukkit.getLogger().info("”dataname”が設定されていません")
                continue
            }

            plugin.drugName.add(cfg.getString("dataname"))

            val data = HashMap<String,DrugData>()[cfg.getString("dataname")]!!

            plugin.drugName.add(cfg.getString("DataName"))

            data.displayName = cfg.getString("displayName")
            data.material = cfg.getString(",material","DIAMOND_HOE")
            data.damage = cfg.getInt("damage",0).toShort()
            data.type =  cfg.getInt("type",0)
            data.isChange = cfg.getBoolean("isChange")
            if(data.isChange)data.changeItem = cfg.getString("changeItem")

            data.lore = cfg.getStringList("Lore")
            //use message
            data.useMsg = cfg.getStringList("useMsg")
            data.useMsgDelay = cfg.getStringList("useMsgDelay")
            data.hasEnchantEffect = cfg.getBoolean("hasEnchantEffect")

            data.cooldown = cfg.getLong("cooldown")

            data.nearPlayer = cfg.getStringList("nearPlayer")

            //func lists
            data.func = cfg.getStringList("func")
            data.funcRandom = cfg.getStringList("funcrandom")
            data.funcDelay = cfg.getStringList("funcdelay")
            data.funcRandomDelay = cfg.getStringList("funcrandomdelay")

            data.cmd = getHMList("cmd",cfg)
            data.cmdRandom = getHMList("cmdRandom",cfg)
            data.cmdDelay = getHMList("cmdDelay",cfg)// - commandName;time(tick)
            data.cmdRandomDelay = getHMList("cmdDelayRandom",cfg)

            data.playerCmd = getHMList("pCmd",cfg)
            data.playerCmdRandom = getHMList("pCmdRandom",cfg)
            data.playerCmdDelay = getHMList("pCmdDelay",cfg)
            data.playerCmdRandomDelay = getHMList("pCmdDelayRandom",cfg)

            data.buff = getHMList("buff",cfg)
            data.buffRandom = getHMList("buffRandom",cfg)
            data.buffDelay = getHMList("buffDelay",cfg)
            data.buffRandomDelay = getHMList("buffDelayRandom",cfg)

            data.particle = getHMList("particle",cfg)
            data.particleRandom = getHMList("particleRandom",cfg)
            data.particleDelay = getHMList("particleDelay",cfg)
            data.particleRandomDelay = getHMList("particleDelayRandom",cfg)

            data.sound = getHMList("sound",cfg)
            data.soundRandom = getHMList("soundRandom",cfg)
            data.soundDelay = getHMList("soundDelay",cfg)
            data.soundRandomDelay = getHMList("soundDelayRandom",cfg)

            data.crashChance = cfg.getDoubleList("crashChance")//無記名で壊れなくなる

            data.removeBuffs = cfg.getBoolean("removeBuff")//使ったときに今のバフを消す

            data.isRemoveItem = cfg.getBoolean("RemoveItem",true)
            data.disableWorld = cfg.getStringList("DisableWorld")

            if (data.type == 0){
                data.isdepend = cfg.getBoolean("isDepend")
                data.dependLevel = cfg.getInt("dependLevel")//最大レベルを入力
                data.dependLvUp = cfg.getDoubleList("dependLvUp")//依存レベルが下る確率
                data.dependLvDown = cfg.getDoubleList("dependLvDown")//依存レベルが下る確率

                data.symptomsFirstTime = cfg.getLongList("symptomsFirstTime")//最初の一回目
                data.symptomsTime = cfg.getLongList("symptomsTime")//それ以降

                data.symptomsCount = cfg.getIntegerList("symptomsCount")//禁断症状が止まる確率

                data.buffSymptoms = getHMList("buffSymptoms",cfg)
                data.buffSymptomsRandom = getHMList("buffSymptomsRandom",cfg)

                data.particleSymptoms = getHMList("particleSymptoms",cfg)
                data.particleSymptomsRandom = getHMList("particleSymptomsRandom",cfg)

                data.soundSymptoms = getHMList("soundSymptoms",cfg)
                data.soundSymptomsRandom = getHMList("soundSymptomsRandom",cfg)

                data.cmdSymptoms = getHMList("cmdSymptoms",cfg)
                data.cmdSymptomsRandom = getHMList("cmdSymptomsRandom",cfg)

                data.msgSymptoms = cfg.getStringList("msgSymptoms")
                data.dependMsg = cfg.getStringList("dependMsg")

                data.symptomsNearPlayer = cfg.getStringList("nearPlayerSymptoms")

                data.cmdLvUp = getHMList("cmdLvUp",cfg)

            }

            if (data.type == 1){
                data.weakDrug = cfg.getString("weakDrug")//治療する対象のドラッグの名前を入力
                data.weakProb = cfg.getDoubleList("weakProb")//確率で1レベルダウン
                data.stopDepend = cfg.getBoolean("stopDepend")
            }

        }
    }

    fun getHMList(path:String,cfg:YamlConfiguration): HashMap<Int, MutableList<String>> {

        val listInHashMap = HashMap<Int,MutableList<String>>()

        for (i in 0 .. 100){
            try {
                listInHashMap[i] = cfg.getStringList("$path.$i")

            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
                break
            }

        }

        return listInHashMap

    }

    class DrugData{

        //必須
        var displayName = "drug"
        var material = "DIAMOND_HOE"
        var damage : Short = 0
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
        var useMsgDelay = mutableListOf<String>()

        var hasEnchantEffect = false

        /*
        Func: 機能をまとめられる機能
        あらゆるコマンド実行やメッセージ、サウンドプレイを一括にまとめる機能。
        */

        //func
        var func = mutableListOf<String>()
        //funcDelay
        var funcDelay = mutableListOf<String>()
        //funcRandom
        var funcRandom = mutableListOf<String>()
        //funcRandomDelay
        var funcRandomDelay = mutableListOf<String>()


        //cmd
        var cmd = HashMap<Int,MutableList<String>>()
        var cmdRandom = HashMap<Int,MutableList<String>>()
        var cmdDelay = HashMap<Int,MutableList<String>>()
        var cmdRandomDelay = HashMap<Int,MutableList<String>>()
        //player dispatch
        var playerCmd = HashMap<Int,MutableList<String>>()
        var playerCmdRandom = HashMap<Int,MutableList<String>>()
        var playerCmdDelay = HashMap<Int,MutableList<String>>()
        var playerCmdRandomDelay = HashMap<Int,MutableList<String>>()
        //buff
        var buff = HashMap<Int,MutableList<String>>()
        var buffRandom = HashMap<Int,MutableList<String>>()
        var buffDelay = HashMap<Int,MutableList<String>>()
        var buffRandomDelay = HashMap<Int,MutableList<String>>()
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

        //アイテムが一定確率で消える
        var crashChance = mutableListOf<Double>()

        var removeBuffs = false //使用時 バフが消えるかどうか
        var isRemoveItem = true //使用時 アイテムを消去するか

        //type0
        var isdepend = false    //依存するかどうか
        var dependLevel = 0  //依存レベル
        var dependLvUp = mutableListOf<Double>() //レベルアップする確率
        var dependLvDown = mutableListOf<Double>()//レベルダウンする確率

        //type0 HM
        var symptomsFirstTime = mutableListOf<Long>()
        var symptomsTime  = mutableListOf<Long>()
        var symptomsCount  = mutableListOf<Int>() //何回禁断症状が出るか (終わるまでの回数)

        var buffSymptoms = HashMap<Int,MutableList<String>>()
        var buffSymptomsRandom = HashMap<Int,MutableList<String>>()

        var cmdSymptoms = HashMap<Int,MutableList<String>>()
        var cmdSymptomsRandom = HashMap<Int,MutableList<String>>()

        var msgSymptoms = mutableListOf<String>()

        var particleSymptoms = HashMap<Int,MutableList<String>>()
        var particleSymptomsRandom = HashMap<Int,MutableList<String>>()

        var soundSymptoms = HashMap<Int,MutableList<String>>()
        var soundSymptomsRandom = HashMap<Int,MutableList<String>>()

        var cmdLvUp = HashMap<Int,MutableList<String>>()

        var nearPlayer = mutableListOf<String>()//周囲のプレイヤーに干渉
        var symptomsNearPlayer = mutableListOf<String>()//禁断症状が出たときに、周囲のプレイヤーに
        var dependMsg = mutableListOf<String>()//チェッカーで表示する依存度
        var disableWorld = mutableListOf<String>()//使えなくするワールド

        //type1
        var weakDrug = "drug" //type2
        var weakProb  = mutableListOf<Double>()//飲むときに、確率で治る
        var stopDepend = false



    }

}