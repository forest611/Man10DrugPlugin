package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

class Man10DrugPlugin : JavaPlugin() {

    //////////////////////////
    //起動時の処理
    //////////////////////////
    override fun onEnable() { // Plugin startup logic

        plugin = this

        Config.loadPluginConfig()

        getCommand("mdp")!!.setExecutor(Command)

        Bukkit.getServer().pluginManager.registerEvents(Events,this)
        Database.mysql = MySQLManager(this,"man10drug")

        Database.executeDBQueue()
        DependThread.dependThread()

        Config.loadDrugs()
        MDPFunction.loadFunction()

    }

    override fun onDisable() { // Plugin shutdown logic
        for (p in Bukkit.getOnlinePlayers()){
            Database.save(p)
        }
    }

    companion object{
        fun rep(str:String,p:Player,d:String):String{
            return str.replace("<player>",p.name).replace("<uuid>",p.uniqueId.toString()).
            replace("<drug>",d).replace("<x>",p.location.blockX.toString()).
            replace("<y>",p.location.blockY.toString()).replace("<z>",p.location.blockZ.toString()).replace("<world>",p.location.world.name)
        }

        fun random(list : MutableList<String>):String{
            return list[Random.nextInt(list.size-1)]
        }

        var pluginEnable = true
        var useMilk = false
        var debugMode = false
        var disableWorld = mutableListOf<String>()

        var isReload = false//リロード中かどうか

        val drugName = mutableListOf<String>()
        val drugData = HashMap<String,Config.Drug>()

        lateinit var plugin : Man10DrugPlugin
    }

}