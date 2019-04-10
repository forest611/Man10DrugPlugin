package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class MDPDataBase(val plugin: Man10DrugPlugin,val config:MDPConfig){

    var playerMap = HashMap<String,playerData>()
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
                data.count = 0
                data.times = 0
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

                data.count = rs.getInt("count")
                data.level = rs.getInt("level")
                data.times = rs.getInt("times")

                rs.close()

                //禁断症状
                if (drugData.isDependence&&data.times!=0){
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
    fun saveDataBase(player: Player,remove:Boolean){

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

    ////////////////////////
    //プレイヤーレコード追加
    private fun addRecord(mysql:MySQLManager,player:Player,drug:String){
        val sql = "INSERT INTO drug " +
                "VALUES('${player.uniqueId}'," +
                "'${player.name}'," +
                "'$drug',0,0,0);"

        mysql.execute(sql)

    }

    //////////////////
    //select
    private fun selectRecord(mysql:MySQLManager,player: Player,drug:String):ResultSet{
        val sql = "SELECT " +
                "count," +
                "level," +
                "times " +
                "FROM drug " +
                "WHERE uuid='${player.uniqueId}' "+
                "and drug_name='$drug';"

        return mysql.query(sql)
    }

    ////////////////
    //stockを保存
    fun saveStock(){

        val mysql = MySQLManager(plugin,"man10drugPlugin")

        for (drug in plugin.drugName){

            val d= config.get(drug)

            if (!d.stockMode){
                continue
            }

            val data = config.get(drug)
            val sql = "UPDATE data " +
                    "SET value='${data.stock}' " +
                    "WHERE drug='$drug';"
            mysql.execute(sql)
        }

        mysql.close()
    }

    ////////////////
    //stock読み込み
    fun loadStock(){

        val mysql = MySQLManager(plugin,"man10drugPlugin")

        for (drug in plugin.drugName){

            val d= config.get(drug)

            if (!d.stockMode){
                continue
            }

            var sql = "SELECT " +
                    "value " +
                    "FROM data " +
                    "WHERE drug='$drug';"
            var rs = mysql.query(sql)
            if (!rs.next()){
                sql = "INSERT INTO data " +
                        "VALUES('$drug',0,0,0);"
                mysql.execute(sql)
                sql = "SELECT " +
                        "value " +
                        "FROM data" +
                        "WHERE drug='$drug';"
                rs = mysql.query(sql)
            }
            val data = config.get(drug)
            data.stock = rs.getInt("VALUE")
            config.drugData[drug] = data
        }

        mysql.close()
    }

}

class playerData{
    var count = 0
    var level = 0
    var times = 0
    var taskId = 0
    var isDependence = false
}