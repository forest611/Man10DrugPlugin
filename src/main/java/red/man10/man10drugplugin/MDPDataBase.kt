package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import red.man10.man10drugplugin.MySQLManagerV2.Query as Query

class MDPDataBase(val plugin: Man10DrugPlugin){

    var playerMap = ConcurrentHashMap<String,playerData>()
    var canConnect = true

    ////////////////////////
    //DBのデータを読み込む
    ////////////////////////
    fun loadDataBase(player: Player,mysql: MySQLManagerV2){

        if (!player.isOnline){
            return
        }


        if (!canConnect){
            Bukkit.getLogger().info("MySQLに接続できません")
            Bukkit.getLogger().info("/mdp reloadしてください")
            return
        }

        for (name in plugin.drugName){

            val drugData = plugin.mdpConfig.get(name)
            val key = player.name+name

            val data = get(key)

            if (drugData.type != 0 && drugData.type != 1){
                data.level = 0
                data.usedLevel = 0
                data.symptomsTotal = 0
                playerMap[key] = data

                continue
            }

            var sql = "SELECT " +
                    "used_count," +
                    "used_level," +
                    "used_time," +
                    "level," +
                    "symptoms_total " +
                    "FROM drug_dependence " +
                    "WHERE uuid='${player.uniqueId}' "+
                    "and drug_name='$name';"

            val query = mysql.query(sql)

            var rs : ResultSet = query.rs

            if (!rs.next()){

                addRecord(mysql,player,name)

                sql = "SELECT " +
                        "used_count," +
                        "used_level," +
                        "used_time," +
                        "level," +
                        "symptoms_total " +
                        "FROM drug_dependence " +
                        "WHERE uuid='${player.uniqueId}' "+
                        "and drug_name='$name';"

                rs = mysql.query(sql).rs

                Bukkit.getLogger().info("${player.name}...insert $name")

                rs.next()

            }

            try{

                data.usedLevel = rs.getInt("used_level")
                data.level = rs.getInt("level")
                data.symptomsTotal = rs.getInt("symptoms_total")
                data.time = Date()
                data.time.time = rs.getLong("used_time")
                data.usedCount = rs.getInt("used_count")

                if (data.usedLevel > 0 || data.level > 0){
                    data.isDependence = true
                }


                playerMap[key] = data
                query.close()
                query.close()


            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
                Bukkit.getLogger().info("/mdp reload もしくは再起動してください")
                canConnect = false
                Bukkit.getScheduler().cancelTasks(plugin)
            }

        }

        Bukkit.getLogger().info(player.name+"...Loaded DB")

    }

    /////////////////////////////
    //データをDBに保存
    //////////////////////////
    fun saveDataBase(player: Player,mysql: MySQLManagerV2){



        if (!canConnect){
            Bukkit.getLogger().info("MySQLに接続できません")
            Bukkit.getLogger().info("/mdp reloadしてください")
            return
        }

        canConnect = true

        for (i in 0 until plugin.drugName.size){

            val drugData = plugin.mdpConfig.get(plugin.drugName[i])


            if (drugData.type != 0 && drugData.type != 1){
                continue
            }

            val key = player.name+plugin.drugName[i]

            if (playerMap[key] == null){
                return
            }


            val data = get(key)

            val sql = "UPDATE drug_dependence " +
                    "SET used_count='${data.usedCount}'"+
                    ",used_level='${data.usedLevel}'"+
                    ",used_time='${data.time.time}'"+
                    ",level='${data.level}'"+
                    ",symptoms_total='${data.symptomsTotal}' " +
                    " WHERE uuid='${player.uniqueId}' and drug_name='${plugin.drugName[i]}';"

            mysql.execute(sql)


            playerMap.remove(key)

        }
        Bukkit.getLogger().info("${player.name}...save DB")
        saveLog(player,mysql)
        Bukkit.getLogger().info("${player.name}...save Logs")


    }

    /////////////////////////
    //DBにログを保存
    fun saveLog(player: Player,mysql: MySQLManagerV2){

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
    private fun addRecord(mysql: MySQLManagerV2, player:Player, drug:String){
        val sql = "INSERT INTO drug_dependence " +
                "VALUES('${player.uniqueId}'," +
                "'${player.name}'," +
                "'$drug',0,0,${Date().time},0,0);"

        mysql.execute(sql)

    }

    ///////////////////////
    //DBとメモリから累計使用回数取得
    fun getDrugServerTotal(drug:String,mysql:MySQLManagerV2):Int{

        val query = mysql.query("SELECT SUM(used_count) FROM drug_dependence WHERE drug_name='$drug';")

        val rs = query.rs

        rs.next()

        var total = rs.getInt(1)

        for(p in Bukkit.getOnlinePlayers()){
            val pd = get(p.name+drug)

            total += pd.countOnline
        }

        query.close()

        return total

    }

    ////////////////////
    //DBからレベルごとの人数を取得
    fun getDrugServerLevel(drug:String,mysql: MySQLManagerV2):ArrayList<Int>{

        val list = ArrayList<Int>()

        val query = mysql.query("SELECT level,COUNT(player) FROM drug_dependence WHERE drug_name='$drug' AND used_level !=0 GROUP BY level ORDER BY level;")

        val rs = query.rs

        while (rs.next()){
            list.add(rs.getInt("COUNT(player)"))
        }

        for(player in Bukkit.getOnlinePlayers()){
            val pd = get(player.name+drug)

            if (pd.usedLevel==0&&pd.level==0){ continue}

            list[pd.level]++
        }
        return list

    }

}

class playerData{
    var countOnline = 0
    var usedLevel = 0
    var level = 0
    var symptomsTotal = 0
    var usedCount = 0
    var time = Date()
    var isDependence = false
}

