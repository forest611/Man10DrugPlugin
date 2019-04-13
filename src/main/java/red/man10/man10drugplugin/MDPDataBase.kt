package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class MDPDataBase(val plugin: Man10DrugPlugin,val config:MDPConfig){

    var playerMap = HashMap<String,playerData>()
    var drugStat = HashMap<String,DrugStat>()
    var canConnect = true

    ////////////////////////
    //DBのデータを読み込む
    ////////////////////////
    fun loadDataBase(player: Player){

        val mysql = MySQLManager(plugin,"man10drugPlugin")

        if (!canConnect){
            Bukkit.getLogger().info("MySQLに接続できません")
            Bukkit.getLogger().info("/mdp reloadしてください")
            return
        }

        for (name in plugin.drugName){

            val drugData = config.get(name)
            val key = player.name+name

            val data = get(key)

            Bukkit.getLogger().info(key)

            if (drugData.type != 0 && drugData.type != 1){
                data.level = 0
                data.usedLevel = 0
                data.symptomsTotal = 0
                playerMap[key] = data

                continue
            }

            var rs : ResultSet = selectRecord(mysql,player,name)

            if (!rs.next()){

                addRecord(mysql,player,name)

                rs = selectRecord(mysql,player,name)

                Bukkit.getLogger().info("${player.name}...insert $name")

                rs.next()
            }

            try{

                data.usedLevel = rs.getInt("usedLevel")
                data.level = rs.getInt("level")
                data.symptomsTotal = rs.getInt("symptoms_total")
                data.time = rs.getString("used_time")
                data.usedCount = rs.getInt("used_count")

                rs.close()

                //禁断症状
                if (drugData.isDependence&&data.symptomsTotal!=0){




                    data.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(player
                            ,drugData,data,plugin)
                            ,drugData.symptomsTime!![data.level],drugData.symptomsNextTime!![1])
                    data.isDependence = true
                }

                playerMap[key] = data


            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
                Bukkit.getLogger().info("/mdp reload もしくは再起動してください")
                canConnect = false
                Bukkit.getScheduler().cancelTasks(plugin)
            }

        }
        Bukkit.getLogger().info(player.name+"...Loaded DB")

        mysql.close()

    }

    /////////////////////////////
    //データをDBに保存
    //////////////////////////
    fun saveDataBase(player: Player){

        val mysql = MySQLManager(plugin,"man10drugPlugin")

        if (!canConnect){
            Bukkit.getLogger().info("MySQLに接続できません")
            Bukkit.getLogger().info("/mdp reloadしてください")
            return
        }

        canConnect = true

        for (i in 0 until plugin.drugName.size){

            val drugData = config.get(plugin.drugName[i])


            if (drugData.type != 0 && drugData.type != 1){
                continue
            }

            val key = player.name+plugin.drugName[i]

            val data = get(key)

            val sql = "UPDATE drug " +
                    "SET used_count='${data.usedCount}'"+
                    ",usedLevel='${data.usedLevel}'"+
                    ",used_time='${data.time}'"+
                    ",level='${data.level}'"+
                    ",symptoms_total='${data.symptomsTotal}' " +
                    " WHERE uuid='${player.uniqueId}' and drug_name='${plugin.drugName[i]}';"

            mysql.execute(sql)

            Bukkit.getScheduler().cancelTask(data.taskId)

            playerMap.remove(key)

        }
        Bukkit.getLogger().info("${player.name}...save DB")
        saveLog(player,mysql)
        Bukkit.getLogger().info("${player.name}...save Logs")

        mysql.close()

    }

    /////////////////////////
    //DBにログを保存
    fun saveLog(player: Player,mysql: MySQLManager){

        val log = plugin.playerLog[player] ?: return

        for (i in 0 until log.size){
            val logs = log[i].split(",")

            mysql.execute(
                    "INSERT INTO log " +
                    "VALUES('${player.uniqueId}', " +
                    "'${player.name}', " +
                    "'${logs[0]}'," +
                    "'${logs[1]}');"
            )
        }
        plugin.playerLog.remove(player)

    }

    fun get(key:String):playerData{
        var data = playerMap[key]
        if (data == null){
            data = playerData()
        }
        return data
    }

    fun getStat(key:String):DrugStat{
        var data = drugStat[key]
        if (data == null){
            data = DrugStat()
        }
        return data
    }

    ////////////////////
    //ログをメモリに保存 最後に使用した時間を保存
    fun addLog(player: Player,drug:String){
        val date = Date()
        val format = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())

        if (plugin.playerLog[player] == null){
            plugin.playerLog[player] = mutableListOf("$drug,${format.format(date)}")
            return
        }
        plugin.playerLog[player]!!.add("$drug,${format.format(date)}")
    }

    ////////////////////////
    //プレイヤーレコード追加
    private fun addRecord(mysql:MySQLManager,player:Player,drug:String){
        val sql = "INSERT INTO drug " +
                "VALUES('${player.uniqueId}'," +
                "'${player.name}'," +
                "'$drug',0,0,0,0,0);"

        mysql.execute(sql)

    }

    //////////////////
    //select
    private fun selectRecord(mysql:MySQLManager,player: Player,drug:String):ResultSet{
        val sql = "SELECT " +
                "used_count," +
                "usedLevel," +
                "used_time," +
                "level," +
                "symptoms_total " +
                "FROM drug " +
                "WHERE uuid='${player.uniqueId}' "+
                "and drug_name='$drug';"

        return mysql.query(sql)
    }

    ///////////////////////
    //DBとメモリから累計使用回数取得
    fun getDrugServerTotal(drug:String):Int{

        val data = getStat(drug)

        val mysql = MySQLManager(plugin,"man10drugplugin")

        val rs = mysql.query("SELECT SUM(used_count) FROM drug WHERE drug_name='$drug';")

        rs.next()

        val total = data.count + rs.getInt(1)

        rs.close()
        mysql.close()

        return total

    }

    ////////////////////
    //DBからレベルごとの人数を取得
    fun getDrugServerLevel(drug:String):ArrayList<Int>{

        val list = ArrayList<Int>()

        val mysql = MySQLManager(plugin,"man10drugplugin")

        val rs = mysql.query("SELECT level , COUNT(level) FROM drug WHERE drug_name='$drug' AND used_count!='0' group BY LEVEL;")

        while (rs.next()){
            list.add(rs.getInt(1))
        }

        rs.close()
        mysql.close()

        return list
    }

}

class playerData{
    var usedLevel = 0
    var level = 0
    var symptomsTotal = 0
    var usedCount = 0
    var time = ""
    var taskId = 0
    var isDependence = false
}

class DrugStat{
    var count = 0
    var stock = 0
}