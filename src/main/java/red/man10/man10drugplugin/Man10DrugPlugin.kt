package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.*

class Man10DrugPlugin : JavaPlugin() {

    var drugName = ArrayList<String>()  //command name
    var drugItemStack = HashMap<String,ItemStack>()//key drugName
    var playerLog = HashMap<Player,MutableList<String>>()//log

    lateinit var mysql : MySQLManager
    lateinit var db : MDPDataBase


    private val mdpConfig = MDPConfig(this)
    var event: MDPEvent? = null

    val mdpfunc = MDPFunction(this)
    var vault : VaultManager? = null

    var canMilk = true // milkを使えるか
    var stop = false //止まっているか

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

        if (data.damage.toInt() != 0){
            drug.durability = data.damage
            meta.isUnbreakable = true
            meta.itemFlags.add(ItemFlag.HIDE_UNBREAKABLE)
        }
        if(data.enchantEffect){ meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS,1,true)}

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

        config.set("CanUseMilk",canMilk)
        config.set("Stop",stop)

        mysql = MySQLManager(this,"man10DrugPlugin")

        saveConfig()

        //drug load
        load()

        db = MDPDataBase(this,mysql,mdpConfig)

        event = MDPEvent(this,mysql,db,mdpConfig)
        Bukkit.getServer().pluginManager.registerEvents(event,this)
        getCommand("mdp").executor = MDPCommand(this,db)

        vault = VaultManager(this)

        //再起動時にオンラインプレイヤーがいた場合
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getServer().onlinePlayers){
                    db.loadDataBase(player)
                }
            }
        }.run()

    }

    ////////////////////////
    //シャットダウン、ストップ時
    override fun onDisable() {

        //鯖落ち時にオンラインプレイヤーがいた場合
        Bukkit.getScheduler().cancelTasks(this)

        object : BukkitRunnable(){
            override fun run() {
                for (player in Bukkit.getServer().onlinePlayers){
                    db.saveDataBase(player,true)
                }
            }
        }.run()
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
                .replace("<count>",pd.count.toString()).replace("<times>",pd.times.toString())
                .replace("<stock>",d.stock.toString())
    }

    fun getNearByPlayers(loc: Location,distance:Int):ArrayList<Player>{
        val ds = distance*distance
        val players = ArrayList<Player>()
        for(player in Bukkit.getOnlinePlayers()){
            if (player.location.distanceSquared(loc) < ds){
                players.add(player)
            }
        }
        return players
    }


}
