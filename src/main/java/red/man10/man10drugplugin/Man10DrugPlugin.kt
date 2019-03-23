package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
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

    var canMilk = true // milkを使えるか

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

            if (drugFiles[i].name == "config.yml"){
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
        config.set("CanUseMilk",canMilk)

        mysql = MySQLManager(this,"man10DrugPlugin")

        saveConfig()

        //drug load
        load()

        db = MDPDataBase(this,mysql,mdpConfig)

        Bukkit.getServer().pluginManager.registerEvents(MDPEvent(this,mysql,db,mdpConfig),this)
        getCommand("mdp").executor = MDPCommand(this,db)

        //再起動時にオンラインプレイヤーがいた場合
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getServer().onlinePlayers){
                    db.loadDataBase(player)
                }
            }
        }

    }

    ////////////////////////
    //シャットダウン、ストップ時
    override fun onDisable() {
        //鯖落ち時にオンラインプレイヤーがいた場合
        for (player in Bukkit.getServer().onlinePlayers){
            db.saveDataBase(player,true)
        }
        Bukkit.getScheduler().cancelTasks(this)
    }
}
