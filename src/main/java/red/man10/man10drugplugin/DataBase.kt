package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.log

class DataBase (private val plugin: Man10DrugPlugin){

    val playerData = ConcurrentHashMap<Pair<Player,String>,PlayerData>()
    val executeQueue = LinkedBlockingQueue<String>()//query
    lateinit var mysql : MySQLManager

    @Synchronized
    fun loginDB(p:Player){

        if (!p.isOnline)return

        for (drug in plugin.drugName){
            val data = PlayerData()
            val drugData = plugin.drugData[drug]!!

            if (drugData.type == 0){
                val rs = mysql.query("SELECT * FROM drug_dependence" +
                        " WHERE uuid=${p.uniqueId} and drug_name=$drug")

                rs?.next()

                if (rs==null){
                    p.sendMessage("§e§lデータベースのエラーです。お近くの運営に報告してください。")
                    return
                }

                if (!rs.next()){
                    executeQueue.add("INSERT INTO `drug_dependence` " +
                            "(`uuid`, `player`, `drug_name`, `used_count`, `used_level`, `used_time`, `level`, `symptoms_total`)" +
                            " VALUES ('${p.uniqueId}', '${p.name}', '$drug', '0', '0', '${Date().time}', '0', '0');")
                    Bukkit.getLogger().info("insert new data ${p.name} $drug")

                    playerData[Pair(p,drug)] = data
                    mysql.close()
                    continue
                }

                data.usedCount = rs.getInt("used_count")
                data.usedLevel = rs.getInt("used_level")
                data.finalUseTime = rs.getLong("used_time")
                data.totalSymptoms = rs.getInt("symptoms_total")
                if (data.usedLevel>0 || data.level >0){
                    data.isDepend = true
                }

                playerData[Pair(p,drug)] = data
                mysql.close()

            }
        }
    }

    fun logoutDB(p:Player){
        for (drug in plugin.drugName){
            val data = playerData[Pair(p,drug)]!!

            executeQueue.add(
            "UPDATE drug_dependence " +
                    "SET used_count='${data.usedCount}'"+
                    ",used_level='${data.usedLevel}'"+
                    ",used_time='${data.finalUseTime}'"+
                    ",level='${data.level}'" +
                    ",symptoms_total='${data.totalSymptoms}' " +
                    " WHERE uuid='${p.uniqueId}' and" +
                    " drug_name='${drug}';"
            )
            playerData.remove(Pair(p,drug))
        }
    }



    ////////////////////
    //DBに保存するためのキュー
    //executeQueueにデータを保存する
    /////////////////////
    fun executeDBQueue(){
        Thread(Runnable {
            val sql = MySQLManager(plugin,"DrugPluginExecute")

            while (true){
                try {
                    val take = executeQueue.take()
                    sql.execute(take)
                    sql.close()
                }catch (e:InterruptedException){
                    Bukkit.getLogger().info(e.message)
                }
            }
        }).start()
    }

    class PlayerData{
        var usedCount = 0//トータル
        var usedLevel = 0//レベルごとにリセット
        var level = 0
        var totalSymptoms = 0
        var finalUseTime : Long = 0//最終使用時刻(cooldownのでも使用)
        var isDepend = false
    }
}