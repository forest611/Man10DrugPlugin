package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Man10DrugPlugin : JavaPlugin() {

    var drugName = mutableListOf<String>()  //command name

    var watchName = mutableListOf<String>()

    var drugItemStack = HashMap<String,ItemStack>()//key drugName
    var playerLog = ConcurrentHashMap<Player,MutableList<String>>()//log

    lateinit var db : MDPDataBase
    lateinit var disableWorld : MutableList<String>

    lateinit var plConfig : FileConfiguration

    val mdpConfig = MDPConfig(this)

    val mdpfunc = MDPFunction(this)
    var vault : VaultManager? = null
    var event : MDPEvent? =null

    var canMilk = true // milkを使えるか
    var stop = false //止まっているか

    var isTask = true //task が動いてるか

    var debug = false

    var reload = false

    var watchTime = 0

    var watchInterval = 0

    //task作成


    ////////////////////////
    //config load
    /////////////////////
    fun load(){

        drugName.clear()
        drugItemStack.clear()

        val drugFolder = File(Bukkit.getServer()
                .pluginManager.getPlugin("Man10DrugPlugin")
                .dataFolder,File.separator)

        if (!drugFolder.exists()){

            Bukkit.getLogger().info("フォルダが見つかりません")
            return

        }

        val drugFiles = drugFolder.listFiles().toMutableList()

        //////////////////////////////////
        //ファイルを読み込んでデータをメモリに保存
        var i = 0
        while (i<drugFiles.size){

            if (drugFiles[i].name == "config.yml"||drugFiles[i].isDirectory||!drugFiles[i].path.endsWith("yml")){
                drugFiles.removeAt(i)
                continue
            }

            Bukkit.getLogger().info("loading..." + drugFiles[i].name)
            mdpConfig.loadConfig(drugFiles[i])

            drugItemStack[drugName[i]] = addDrug(drugName[i])
            i++

        }
    }

    /////////////////////////////////
    //アイテムスタック作成 name = drugData
    ///////////////////////////////
    fun addDrug(name:String):ItemStack{
        val data = mdpConfig.get(name)


        val drug = ItemStack(Material.valueOf(data.material),1)

        val meta = drug.itemMeta


        //loreに識別コードを書き込む

        val loreData = StringBuffer()

        for (i in name.toCharArray()){
            loreData.append("§").append(i)
        }
        meta.displayName = data.displayName + loreData.toString()

        meta.lore = data.lore

        if(data.enchantEffect){ meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS,1,true)}

        drug.durability = data.damage
        meta.isUnbreakable = true
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS)
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON)
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        drug.itemMeta = meta

        return drug
    }

    //////////////////
    //起動時
    override fun onEnable() {
        // Plugin startup logic

        //config load
        saveDefaultConfig()

        plConfig = config

        canMilk = plConfig.getBoolean("CanUseMilk",false)
        stop = plConfig.getBoolean("Stop",false)
        disableWorld = plConfig.getStringList("DisableWorld")
        watchName = plConfig.getStringList("Watches")
        watchInterval = plConfig.getInt("WatchInterval",360)


        saveConfig()

        val mysql = MySQLManagerV2(this, "man10drugplugin")

        //drug load
        getCommand("mdp").executor = MDPCommand(this)

        load()

        event = MDPEvent(this)

        Bukkit.getServer().pluginManager.registerEvents(event,this)


        db = MDPDataBase(this)

        for (p in Bukkit.getServer().onlinePlayers){
            db.loadDataBase(p,mysql)
        }

        vault = VaultManager(this)

        startDependenceTask()

    }

    ////////////////////////
    //シャットダウン、ストップ時
    ///////////////////////
    override fun onDisable() {

        plConfig.set("CanUseMilk",canMilk)
        plConfig.set("Stop",stop)
        plConfig.set("DisableWorld",disableWorld)
        plConfig.set("Watches",watchName)
        plConfig.set("WatchInterval",watchInterval)


        cancelTask()

        val mysql = MySQLManagerV2(this, "man10drugplugin")
        for (player in Bukkit.getServer().onlinePlayers){
            db.saveDataBase(player,mysql)
        }
    }


    //////////////////////////
    //日付差分で禁断症状
    fun startDependenceTask(){


        Bukkit.getScheduler().runTaskTimer(this,object: TimerTask() {
            override fun run() {

                if (stop || !isTask){
                    cancel()
                    return
                }

                isTask = true

                for (p in Bukkit.getOnlinePlayers()){

                    for (drug in drugName){
                        val c = mdpConfig.get(drug)

                        if (!c.isDependence)continue

                        val pd = db.get(p.name + drug)

                        if (!pd.isDependence || (pd.symptomsTotal > c.symptomsCount[pd.level] &&c.symptomsCount[pd.level] !=0)){
                            continue
                        }

                        val now  = Date().time
                        val time = pd.time.time

                        val differenceTick = (now - time) / 1000

                        /*
                            依存レベルチェック、debugモードの場合は3分ごとに発生

                         */
                        if ((pd.symptomsTotal >0&&c.symptomsNextTime[pd.level] <= differenceTick.toInt() )
                                || c.symptomsTime[pd.level] <= differenceTick.toInt()
                                || (debug && differenceTick > 180)){

                            SymptomsTask(p,c,pd,this@Man10DrugPlugin,db,drug).run()

                            //確率で依存レベルを下げる
                            if (c.weakenProbability.isNotEmpty()){

                                if (c.weakenProbability[pd.level]>Math.random()){

                                    pd.level --
                                    pd.usedLevel = 0
                                    if (pd.level <= 0){
                                        pd.level = 0

                                        pd.isDependence = false
                                    }
                                }
                            }
                        }
                    }

                watchTime ++
                if (watchTime >=watchInterval){

                    ///////////////////
                    //watch実行時にデータセーブ
                    val mysql = MySQLManagerV2(this@Man10DrugPlugin, "man10drugPlugin")

                    db.saveDataBase(p,mysql)
                    db.loadDataBase(p,mysql)

                    watch(p)
                    watchTime = 0
                    }
                }
            }
        },200,200)//10秒ごと
    }

    fun cancelTask(){
        Bukkit.getScheduler().cancelTasks(this)
        isTask = false
    }

    fun watch(player: Player){

        val item = player.inventory.itemInOffHand.itemMeta.displayName?:return

        if (watchName.indexOf(item) < 1)return

        player.sendMessage("§b§l[§a§lMan§f§l10§d§lWatch§b§l]§e§lドラッグの依存データ計測中§kX")


        Bukkit.getScheduler().scheduleSyncDelayedTask(this, {
            for (drug in drugName){
                val c = mdpConfig.drugData[drug]?:continue

                if (!c.isDependence){
                    continue
                }

                if (db.playerMap[player.name+drug] == null){
                    player.sendMessage("§e現在データの読み込み中です.....")
                    return@scheduleSyncDelayedTask
                }


                val pd = db.get(player.name+drug)

                if (pd.usedLevel == 0 && pd.level == 0){
                    continue
                }
                if(c.dependenceMsg.isNotEmpty()){
                    player.sendMessage("§b§l[§a§lMan§f§l10§d§lWatch§b§l]§e§l${c.displayName}:${c.dependenceMsg[pd.level]}") }

            }

        },100)
    }

    ///////////////////////////////
    //複数のクラスで使うメソッド
    fun size(list:MutableList<String>,pd:playerData):Boolean{
        if (list.size>pd.level){
            return true
        }
        return false
    }

    fun repStr(str:String,player: Player,pd:playerData):String{
        return str.replace("<player>",player.name).replace("<level>",pd.level.toString())
                .replace("<usedLevel>",pd.usedLevel.toString()).replace("<symptomsTotal>",pd.symptomsTotal.toString())
                //.replace("<stock>",d.stock.toString())
    }
}
