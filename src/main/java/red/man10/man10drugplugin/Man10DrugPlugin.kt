package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class Man10DrugPlugin : JavaPlugin() {

    lateinit var drugName : MutableList<String> //command name
    lateinit var drugData : MutableList<String> //lore §data
    lateinit var drugItemStack : HashMap<String,ItemStack>//key drugName
    lateinit var mysql : MySQLManager
    lateinit var db : MDPDataBase


    private val mdpconfig = MDPConfig(this  )

    var canMilk = true // milkを使えるか


    //config load
    fun load(){
        drugData.clear()
        drugName.clear()
        drugItemStack.clear()


        val drugfolder = File(Bukkit.getServer()
                .pluginManager.getPlugin("Man10DrugPlugin")
                .dataFolder,File.separator)

        if (!drugfolder.exists()){
            Bukkit.getLogger().info("フォルダが見つかりません")
            return
        }

        val drugFiles = drugfolder.listFiles().toMutableList()


        //file
        for (i in 0 until drugFiles.size){
            if (drugFiles[i].name == "config.yml" ||
                    drugFiles[i].name.indexOf("yml") == -1){

                drugFiles.removeAt(i)
                continue
            }

            Bukkit.getLogger().info("loading..." + drugFiles[i].name)
            mdpconfig.loadConfig(drugFiles[i])

            addDrug(drugName[i])

        }

    }


    /**
     * @name command name
     */
    fun addDrug(name:String):ItemStack{
        val data = mdpconfig.get(name)


        val drug = ItemStack(Material.valueOf(data.material),1)

        val meta = drug.itemMeta

        meta.displayName = data.displayName



        //loreに識別コードを書き込む
        val nameData = name.toCharArray()

        val loreData = StringBuffer()

        for (i in 0 until  nameData.size){
            loreData.append("§").append(nameData[i])
        }

        data.lore.add(loreData.toString())

        meta.lore = data.lore


        drugData.add(loreData.toString())

        if (data.damage.toInt() != 0){
            drug.durability = data.damage
            meta.isUnbreakable = true
            meta.itemFlags.add(ItemFlag.HIDE_UNBREAKABLE)
        }


        drug.itemMeta = meta


        return drug
    }


    override fun onEnable() {
        // Plugin startup logic

        //config load
        saveDefaultConfig()
        val config = config
        canMilk = config.getBoolean("CanUseMilk",false)
        config.set("CanUseMilk",canMilk)
        saveConfig()

        //drug load
        load()

        mysql = MySQLManager(this,"man10DrugPlugin")

        db = MDPDataBase(this,mysql)


        Bukkit.getServer().pluginManager.registerEvents(MDPEvent(this,mysql),this)
        getCommand("mdp").executor = MDPCommand(this,mysql)



        //起動時にオンラインプレイヤーがいた場合
        for (player in Bukkit.getServer().onlinePlayers){
            db.loadDataBase(player)
        }


    }

    override fun onDisable() {
        // Plugin shutdown logic


        //鯖落ち時にオンラインプレイヤーがいた場合
        for (player in Bukkit.getServer().onlinePlayers){
            db.saveDataBase(player)
        }
        Bukkit.getScheduler().cancelTasks(this)

    }


}
