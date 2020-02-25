package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
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

            val rs = mysql.query("SELECT * FROM drug_dependence" +
                    " WHERE uuid='${p.uniqueId}' and drug_name='$drug';")

            if (rs==null){
                p.sendMessage("§e§lデータベースのエラーです。お近くの運営に報告してください。")
                return
            }

            if (!rs.next()){
                executeQueue.add("INSERT INTO `drug_dependence` " +
                        "(`uuid`, `player`, `drug_name`, `used_count`, `used_time`, `level`, `symptoms_total`)" +
                        " VALUES ('${p.uniqueId}', '${p.name}', '$drug', '0', '${Date().time}', '0', '0');")

                playerData[Pair(p,drug)] = data
                mysql.close()
                continue
            }

            data.usedCount = rs.getInt("used_count")
            data.finalUseTime = rs.getLong("used_time")
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
                    ",used_time='${data.finalUseTime}'"+
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

    fun createTable(){
        val mysql = MySQLManager(plugin,"drugTable")

        mysql.execute("CREATE TABLE if not exists drug_dependence " +
                "(uuid text," +
                "player text," +
                "drug_name text," +
                "used_count int," +
                "used_time text," +
                "level int," +
                "immunity int," +
                "symptoms_total int);");

        //logger table
        mysql.execute("CREATE TABLE if not exists log " +
                "(uuid text, " +
                "player text, " +
                "drug_name text, " +
                "date text);");

        //drug box
        mysql.execute("CREATE TABLE if not exists box " +
                "(id int,"  +
                "one text,"     +
                "two text,"     +
                "three text,"   +
                "four text,"    +
                "five text,"    +
                "six text,"     +
                "seven text,"   +
                "eight text,"   +
                "nine text);");
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
        var finalUseTime : Long = 0//最終使用時刻(cooldownのでも使用)
        var isDepend = false
    }
}