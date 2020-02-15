package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import sun.security.krb5.Config

class Man10DrugPlugin : JavaPlugin() {

    var pluginEnable = true
    var useMilk = false
    var debugMode = false
    var disableWorld = mutableListOf<String>()

    var isReload = false//リロード中かどうか

    val drugName = mutableListOf<String>()
    val drugData = HashMap<String,Configs.DrugData>()

    lateinit var cmds : Commands
    lateinit var events : Events
    lateinit var db :DataBase
    lateinit var configs : Configs

    //////////////////////////
    //起動時の処理
    //////////////////////////
    override fun onEnable() { // Plugin startup logic

        saveDefaultConfig()

        pluginEnable = config.getBoolean("enableplugin",true)
        useMilk = config.getBoolean("usemilk",false)
        debugMode = config.getBoolean("debugmode",false)
        disableWorld = config.getStringList("disableworld")

        cmds = Commands(this)
        events = Events(this)
        configs = Configs(this)
        db = DataBase(this)

        getCommand("mdp").executor = cmds
        Bukkit.getServer().pluginManager.registerEvents(events,this)
        db.loginConnection = MySQLManager(this,"DrugPluginLogin")
        db.executeDBQueue()


    }

    override fun onDisable() { // Plugin shutdown logic
    }
}