package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class MDPDataBase(val plugin: Man10DrugPlugin,val mysql:MySQLManager,val config:MDPConfig){

    var playerMap = HashMap<String,playerData>()
    var drugDB = HashMap<String,Any>() //key drug,type
    var canConnect = true

    ////////////////////////
    //DBのデータを読み込む
    ////////////////////////
    fun loadDataBase(player: Player){

        if (!canConnect){
            Bukkit.getLogger().info("MySQLに接続できません")
            Bukkit.getLogger().info("/mdp reloadしてください")
            return
        }

        for (i in 0 until plugin.drugName.size){

            val drugData = config.get(plugin.drugName[i])
            val key = player.name+plugin.drugName[i]

            val data = get(key)


            if (drugData.type != 0 && drugData.type != 1){
                data.level = 0
                data.count = 0
                data.times = 0
                playerMap[key] = data

                continue
            }


            var sql = "SELECT " +
                    "count," +
                    "level," +
                    "times " +
                    "FROM drug " +
                    "WHERE uuid='${player.uniqueId}' "
            "and drug_name='${plugin.drugName[i]}';"

            var rs : ResultSet

            rs = mysql.query(sql)

            if (!rs.next()){
                sql = "INSERT INTO drug " +
                        "VALUES('${player.uniqueId}'," +
                        "'${player.name}'," +
                        "'${plugin.drugName[i]}',0,0,0);"

                mysql.execute(sql)

                sql = "SELECT " +
                        "count," +
                        "level," +
                        "times " +
                        "FROM drug " +
                        "WHERE uuid='${player.uniqueId}' "
                        "and drug_name='${plugin.drugName[i]}';"
                rs = mysql.query(sql)
                Bukkit.getLogger().info("${player.name}...insert ${plugin.drugName[i]}")

            }

            try{
                rs.next()
                data.count = rs.getInt("count")
                data.level = rs.getInt("level")
                data.times= rs.getInt("times")


                rs.close()

                //禁断症状
                if (drugData.isDependence){
                    data.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(player
                            ,drugData,data)
                            ,drugData.symptomsTime!![data.level],drugData.symptomsNextTime!![1])
                    data.isDependence = true
                }

                playerMap[key] = data

            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
                Bukkit.getLogger().info("/mdp reload もしくは再起動してください")
                canConnect = false
                Bukkit.getScheduler().cancelTasks(plugin)
                return
            }
        }

        Bukkit.getLogger().info(player.name+"...Loaded DB")
    }

    /////////////////////////////
    //データをDBに保存
    //////////////////////////
    fun saveDataBase(player: Player,remove:Boolean){
        if (!canConnect){
            Bukkit.getLogger().info("MySQLに接続できません")
            Bukkit.getLogger().info("/mdp reloadしてください")
            return
        }



        for (i in 0 until plugin.drugName.size){

            val drugData = config.get(plugin.drugName[i])


            if (drugData.type != 0 && drugData.type != 1){
                continue
            }

            val key = player.name+plugin.drugName[i]

            val data = get(key)

            val sql = "UPDATE drug " +
                    "SET count=${data.count}"+
                    ",level=${data.level}"+
                    ",times=${data.times}" +
                    " WHERE uuid='${player.uniqueId}' and drug_name='${plugin.drugName[i]}';"

            mysql.execute(sql)

            if(remove){
                playerMap.remove(key)
            }
        }
        Bukkit.getLogger().info("${player.name}...save DB")
        saveLog(player)
        Bukkit.getLogger().info("${player.name}...save Logs")


    }

    /////////////////////////
    //DBにログを保存
    fun saveLog(player: Player){

        val log = plugin.playerLog[player] ?: return

        for (i in 0 until log.size){
            val logs = log[i].split(",")

            mysql.execute("INSERT INTO log " +
                    "VALUES('${player.uniqueId}', " +
                    "'${player.name}', " +
                    "'${logs[0]}'," +
                    "'${logs[1]}');")
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

    ////////////////////
    //ログをメモリに保存
    fun addLog(player: Player,drug:String){
        val date = Date()
        val format = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())

        if (plugin.playerLog[player] == null){
            plugin.playerLog[player] = mutableListOf("$drug,${format.format(date)}")
            return
        }
        plugin.playerLog[player]!!.add("$drug,${format.format(date)}")
    }

    //////////////////////
    //drugDataをdbに保存
    fun saveDrugDB(){
        val keys = drugDB.keys.toMutableList()

        for(i in 0 until keys.size){
            val drugtype = keys[i].split(",")
            val sql = "UPDATE data " +
                    "SET ${drugtype[1]}='${drugDB[keys[i]]}' " +
                    "WHERE drug_name='${drugtype[0]}';"

            mysql.execute(sql)
        }
    }

    /////////////////////////
    //drugDataをdbから読み込む
    fun loadDrugDB(){
        for (i in 0 until plugin.drugName.size){
            val d = config.get(plugin.drugName[i])
            if (d.saveData == null || d.saveData!!.isEmpty()){
                continue
            }

            var sql = "SELECT ${d.saveData!![0]} " +
                    "FROM data " +
                    "WHERE drug='${plugin.drugName[i]}';"

            var rs = mysql.query(sql)

            if (!rs.next()||rs == null){
                sql = "INSERT INTO data " +
                       "VALUES('${plugin.drugName[i]}'," +
                       "'none'," +
                       "false," +
                       "0);"

                mysql.execute(sql)

                sql = "SELECT ${d.saveData!![0]} " +
                        "FROM data " +
                        "WHERE drug='${plugin.drugName[i]}';"
                rs = mysql.query(sql)
            }

            try {
                rs.next()
                when(d.saveData!![0]){
                    "text" ->drugDB[d.saveData!![0]] = rs.getString("text")
                    "bool" ->drugDB[d.saveData!![0]] = rs.getBoolean("bool")
                    "int" ->drugDB[d.saveData!![0]] = rs.getString("value")
                }


            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
            }
        }
    }

}

class playerData{
    var count = 0
    var level = 0
    var times = 0
    var taskId = 0
    var isDependence = false
}