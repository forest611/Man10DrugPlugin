package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

class Man10DrugPlugin : JavaPlugin() {

    var pluginEnable = true
    var useMilk = false
    var debugMode = false
    var disableWorld = mutableListOf<String>()

    var isReload = false//リロード中かどうか

    val drugName = mutableListOf<String>()
    val drugData = HashMap<String,Configs.Drug>()

    lateinit var cmds : Commands
    lateinit var events : Events
    lateinit var db :DataBase
    lateinit var configs : Configs
    lateinit var thread: DependThread
    lateinit var func : MDPFunction

    //////////////////////////
    //起動時の処理
    //////////////////////////
    override fun onEnable() { // Plugin startup logic

        cmds = Commands(this)
        events = Events(this)
        configs = Configs(this)
        db = DataBase(this)
        thread = DependThread(this)
        func = MDPFunction(this)

        configs.loadPluginConfig()

        getCommand("mdp")!!.setExecutor(cmds)

        Bukkit.getServer().pluginManager.registerEvents(events,this)
        db.mysql = MySQLManager(this,"man10drug")

        db.executeDBQueue()
        thread.dependThread()

        configs.loadDrugs()
        func.loadFunction()

    }

    override fun onDisable() { // Plugin shutdown logic
        for (p in Bukkit.getOnlinePlayers()){
            db.logoutDB(p)
        }
    }

    fun random(list : MutableList<String>):String{
        return list[Random.nextInt(list.size-1)]
    }

    companion object{
        fun rep(str:String,p:Player,d:String):String{
            return str.replace("<player>",p.name).replace("<uuid>",p.uniqueId.toString()).
            replace("<drug>",d).replace("<x>",p.location.blockX.toString()).
            replace("<y>",p.location.blockY.toString()).replace("<z>",p.location.blockZ.toString()).replace("<world>",p.location.world.name)
        }
    }

}