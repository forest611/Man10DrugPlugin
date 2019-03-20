package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.sql.ResultSet
import java.util.*

class MDPDataBase(val plugin: Man10DrugPlugin,val mysql:MySQLManager){

    var playerMap = HashMap<String,playerData>()
    var canConnect = true
    val config = MDPConfig(plugin)

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
            // type0以外はsqlに残さない

            val drugData = config.get(plugin.drugName[i])

            if (drugData.type != 0 || drugData.type != 1){
                continue
            }


            val key = player.name+plugin.drugName[i]

            val data = get(key)

            var sql = "SELECT " +
                    "count," +
                    "level," +
                    "times" +
                    " FROM man10drugPlugin.drug" +
                    " WHERE uuid='"+player.uniqueId +
                    "' and drug_name='"+ plugin.drugName[i]+"';"

            var rs : ResultSet

            rs = mysql.query(sql)

            try{
                if (rs==null||!rs.next()){
                    sql = "INSERT INTO man10drugPlugin.drug " +
                            "VALUES('${player.uniqueId}'," +
                            "'${player.name}'," +
                            "'${plugin.drugName[i]}',0,0,0);"


                    mysql.execute(sql)


                    Bukkit.getLogger().info("${player.name} inserted DB")


                    sql = "SELECT " +
                            "count," +
                            "level," +
                            "times" +
                            " FROM man10drugPlugin.drug" +
                            " WHERE uuid='${player.uniqueId}' " +
                            "and drug_name='${plugin.drugName[i]}';"

                }
                rs = mysql.query(sql)
                rs.next()

                data.count = rs.getInt("count")
                data.level = rs.getInt("level")
                data.times= rs.getInt("times")

                rs.close()

                //禁断症状
                if (drugData.isDependence){
                    data.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(plugin,player,mysql
                            ,plugin.drugName[i])
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


            if (drugData.type != 0){
                continue
            }

            val key = player.name+plugin.drugName[i]

            val data = get(key)

            val sql = "UPDATE man10drugPlugin.drug " +
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
    }

    fun get(key:String):playerData{
        var data = playerMap[key]
        if (data == null){
            data = playerData()
        }
        return data
    }
}

class playerData{
    var count = 0
    var level = 0
    var times = 0
    var taskId = 0
    var isDependence = false
}