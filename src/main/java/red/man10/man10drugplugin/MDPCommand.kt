package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable


class MDPCommand (val plugin: Man10DrugPlugin,val mysql :MySQLManager) : CommandExecutor {


    val permissionError = "§4§lYou don't have permission."
    val permission = "man10drug.cmd"
    val chatMessage = "§5[Man10DrugPlugin]"
    val db = MDPDataBase(plugin,mysql)
    val config = MDPConfig(plugin)


    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {



        if (!sender!!.hasPermission(permission)){
            sender.sendMessage(permissionError)
            return true
        }

        val player = sender as Player


        //help
        if (args == null){
            helpChat(player)
            return true
        }



        if (args[0] == "show" && args.size == 2){

            try {
                player.sendMessage("$chatMessage§e+${args[1]}の使用情報(カウント、レベル)")
                for (i in 0 until plugin.drugName.size){
                    player.sendMessage(
                            "$chatMessage${plugin.drugName[i]}" +
                            ",${db.playerMap[player.name+plugin.drugName[i]]!!.count}" +
                            ",${db.playerMap[player.name+plugin.drugName[i]]!!.level}"
                    )
                }
            }catch (e:Exception){
                player.sendMessage(chatMessage+"§e${args[1]}の使用情報を取得できませんでした")
            }
            return true

        }


        if (args[0] == "get" && args.size == 2){

            if (plugin.drugItemStack[args[1]] == null){
                player.sendMessage("$chatMessage§4${args[1]}§aという名前の薬は見つかりませんでした。")
                player.sendMessage("$chatMessage§a設定ファイルの名前を入力してください(拡張子を含まない)")
                return true
            }

            player.inventory.addItem(plugin.drugItemStack[args[1]])
            return true

        }



        if (args[0] == "reload"){
            val th = object : BukkitRunnable() {
                override fun run() {
                    Bukkit.getScheduler().cancelTasks(plugin)
                    for (p in Bukkit.getServer().onlinePlayers){
                        db.saveDataBase(p)
                    }

                    plugin.load()

                    for (p in Bukkit.getServer().onlinePlayers){
                        db.loadDataBase(p)
                    }
                }

            }
            th.run()

        }


        if (args[0] == "list"){
            player.sendMessage("${chatMessage}§e読み込まれているドラッグ一覧")

            for (i in 0 until plugin.drugName.size){
                player.sendMessage("${chatMessage}§e${plugin.drugName[i]}")
            }
        }


//        if (args[0] == "data" && args.size == 2){
//
//            if (config.drugData[args[1]] == null){
//                player.sendMessage("${chatMessage}指定したドラッグが見つかりませんでした")
//                return true
//            }
//
//
//
//        }


        return true
    }


    fun helpChat(player: Player) {
        player.sendMessage("$chatMessage§e§lMan10DrugPlugin HELP")
        player.sendMessage("$chatMessage§e/mdp get [drugName] 薬を手に入れる drugNameは設定ファイルの名前を入力してください(拡張子を含まない)")
        player.sendMessage("$chatMessage§e/mdp cancel ドラッグの依存を止めます")
        player.sendMessage("$chatMessage§e/mdp reload 薬の設定ファイルを再読込みします")
        player.sendMessage("$chatMessage§e/mdp show [player名] 薬の使用情報を見ることができます")
        player.sendMessage("$chatMessage§e/mdp list 読み込まれている薬の名前を表示します")
        player.sendMessage("$chatMessage§e/mdp data [drug名] ドラッグの情報を表示します")
    }

}



/*
    DrugData drug = drugMap.get(args[1]);
    player.sendMessage("§e表示名 "+drug.name);
    player.sendMessage("§eMaterial "+drug.material);
    player.sendMessage("§eダメージ値"+drug.damage);
    player.sendMessage("§eドラッグタイプ "+drug.type);
    player.sendMessage("§eドラッグが食べ物かどうか"+drug.isFood);
    switch (drug.type){
        case 0:{
            player.sendMessage("§e依存段階 "+drug.level);
            player.sendMessage("§e"+drug.power+"回使用で依存レベルが上がります");
            player.sendMessage("§e禁断症状があるかどうか "+drug.symptoms);
            player.sendMessage("§e最初に服用してから禁断症状が出るまでの時間(tick)"+drug.time);
            player.sendMessage("§e禁断症状の繰り返し時間(tick)"+drug.sympTime);
            player.sendMessage("§e禁断症状が切れる時間"+drug.stopTime);
            player.sendMessage("§e死んだら依存が切れるか"+drug.deathCancel);
            break;
        }
        case 1:{
            player.sendMessage("§e依存を弱める強さ"+drug.downCount);
            player.sendMessage("§e"+"回使用で依存を弱める");
            player.sendMessage("§e最初に服用してから禁断症状が出るまでの時間(tick)"+drug.time);
            player.sendMessage("§e禁断症状の繰り返し時間(tick)"+drug.sympTime);
            break;
        }

 */