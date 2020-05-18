package red.man10.man10drugplugin

import org.bukkit.entity.Player
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class DataBase (private val plugin: Man10DrugPlugin){

    val playerData = ConcurrentHashMap<Pair<Player,String>,PlayerData>()
    val executeQueue = LinkedBlockingQueue<String>()//query
    lateinit var mysql : MySQLManager

    @Synchronized
    fun loginDB(p:Player){

        if (!p.isOnline)return

        for (drug in plugin.drugName){
            val data = PlayerData()

            val rs = mysql.query("SELECT * FROM drug_dependence" +
                    " WHERE uuid='${p.uniqueId}' and drug_name='$drug';")

            if (rs==null){
                p.sendMessage("§e§lデータベースのエラーです。お近くの運営に報告してください:Drug。")
                return
            }

            if (!rs.next()){
                executeQueue.add("INSERT INTO `drug_dependence` " +
                        "(`uuid`, `player`, `drug_name`, `used_count`, `used_time`, `level`, `symptoms_total`)" +
                        " VALUES ('${p.uniqueId}', '${p.name}', '$drug', '0', now(), '0', '0');")

                playerData[Pair(p,drug)] = data
                mysql.close()
                continue
            }

            data.usedCount = rs.getInt("used_count")
            data.finalUseTime = rs.getDate("used_time")
            data.totalSymptoms = rs.getInt("symptoms_total")
            data.level = rs.getInt("level")
            if (data.usedCount !=0 || data.level >0){
                data.isDepend = true
            }

            playerData[Pair(p,drug)] = data
            mysql.close()
            rs.close()

        }
    }

    fun logoutDB(p:Player){
        for (drug in plugin.drugName){
            val data = playerData[Pair(p,drug)]?:continue

            executeQueue.add(
            "UPDATE drug_dependence " +
                    "SET used_count='${data.usedCount}'"+
                    ",used_time='${Timestamp(data.finalUseTime.time)}'"+
                    ",level='${data.level}'" +
                    ",symptoms_total='${data.totalSymptoms}' " +
                    " WHERE uuid='${p.uniqueId}' and" +
                    " drug_name='${drug}';"
            )
            playerData.remove(Pair(p,drug))
        }
    }


    fun getServerTotal(drug:String):Int{
        val mysql = MySQLManager(plugin,"DrugStat")

        val rs = mysql.query("SELECT COUNT(drug_name) FROM log WHERE drug_name='$drug';") ?: return 0

        rs.next()

        val total = rs.getInt(1)

        rs.close()
        mysql.close()

        return total

    }



    ////////////////////
    //DBに保存するためのキュー
    //executeQueueにデータを保存する
    /////////////////////
    fun executeDBQueue(){
        Thread(Runnable {
            try{
                val sql = MySQLManager(plugin,"DrugPluginExecute")
                while (true){
                    val take = executeQueue.take()
                    sql.execute(take)
                }
            }catch (e:InterruptedException){

            }
        }).start()
    }

    class PlayerData{
        var usedCount = 0//トータル
        var level = 0
        var totalSymptoms = 0
        var finalUseTime = Date()//最終使用時刻(cooldownのでも使用)
        var isDepend = false
    }
}