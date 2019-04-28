package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Man10DrugPlugin : JavaPlugin() {

    var drugName = ArrayList<String>()  //command name
    var drugItemStack = HashMap<String,ItemStack>()//key drugName
    var playerLog = ConcurrentHashMap<Player,MutableList<String>>()//log

    lateinit var db : MDPDataBase
    lateinit var disableWorld : MutableList<String>


    val mdpConfig = MDPConfig(this)

    val mdpfunc = MDPFunction(this)
    var vault : VaultManager? = null
    var event : MDPEvent? =null

    var canMilk = true // milkを使えるか
    var stop = false //止まっているか

    var isTask = true //task が動いてるか

    var debug = false

    var reload = false

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

            if (drugFiles[i].name == "config.yml"||drugFiles[i].isDirectory){
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

        meta.displayName = data.displayName

        //loreに識別コードを書き込む
        val nameData = name.toCharArray()

        val loreData = StringBuffer()

        for (i in 0 until  nameData.size){
            loreData.append("§").append(nameData[i])
        }

        if (data.lore == null){
            data.lore = mutableListOf(loreData.toString())
        }else{
            data.lore!!.add(loreData.toString())
        }

        meta.lore = data.lore

        if(data.enchantEffect){ meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS,1,true)}

        drug.durability = data.damage
        meta.isUnbreakable = true
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)

        drug.itemMeta = meta

        return drug
    }

    //////////////////
    //起動時
    override fun onEnable() {
        // Plugin startup logic

        //config load
        saveDefaultConfig()

        val config:FileConfiguration = config

        canMilk = config.getBoolean("CanUseMilk",false)
        stop = config.getBoolean("Stop",false)
        disableWorld = config.getStringList("DisableWorld")

        config.set("CanUseMilk",canMilk)
        config.set("Stop",stop)
        config.set("DisableWorld",disableWorld)

        saveConfig()

        //drug load
        load()

        db = MDPDataBase(this)

        val mysql = MySQLManager(this,"man10drugplugin")
        for (p in Bukkit.getServer().onlinePlayers){
            db.loadDataBase(p,mysql)
        }

        event = MDPEvent(this)
        Bukkit.getServer().pluginManager.registerEvents(event,this)
        getCommand("mdp").executor = MDPCommand(this)

        vault = VaultManager(this)


        startDependenceTask()

    }

    ////////////////////////
    //シャットダウン、ストップ時
    ///////////////////////
    override fun onDisable() {

        cancelTask()

        val mysql = MySQLManager(this,"say man10drugplugin")
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

                        if (!pd.isDependence || (pd.symptomsTotal > c.symptomsCount!![pd.level] &&c.symptomsCount!![pd.level] !=0)){
                            continue
                        }

                        val now  = Date().time
                        val time = pd.time.time

                        val differenceTick = (now - time) / 1000

                        /*
                            依存レベルチェック、debugモードの場合は3分ごとに発生

                         */
                        if ((pd.symptomsTotal >0&&c.symptomsNextTime!![pd.level] <= differenceTick.toInt() )
                                || c.symptomsTime!![pd.level] <= differenceTick.toInt()
                                || (debug && differenceTick > 180)){

                            SymptomsTask(p,c,pd,this@Man10DrugPlugin,db,drug).run()

                            //確率で依存レベルを下げる
                            if (c.weakenProbability != null && c.weakenProbability!!.isNotEmpty()){
                                val r = Random().nextInt(c.weakenProbability!![pd.level])+1

                                if (r==1){

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

                }
            }
        },200,200)

    }

    fun cancelTask(){
        Bukkit.getScheduler().cancelTasks(this)
        isTask = false
    }


    ///////////////////////////////
    //複数のクラスで使うメソッド
    fun size(list:MutableList<String>,pd:playerData):Boolean{
        if (list.size>pd.level){
            return true
        }
        return false
    }

    fun repStr(str:String,player: Player,pd:playerData,d:Data):String{
        return str.replace("<player>",player.name).replace("<level>",pd.level.toString())
                .replace("<usedLevel>",pd.usedLevel.toString()).replace("<symptomsTotal>",pd.symptomsTotal.toString())
                //.replace("<stock>",d.stock.toString())
    }
}
