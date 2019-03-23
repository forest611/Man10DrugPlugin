package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable


class MDPCommand (val plugin: Man10DrugPlugin,val db:MDPDataBase) : CommandExecutor {


    val permissionError = "§4§lYou don't have permission."
    val permission = "man10drug.cmd"
    val chatMessage = "§5[Man10DrugPlugin]"


    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {

        if (!sender!!.hasPermission(permission)){
            sender.sendMessage(permissionError)
            return true
        }

        val player = sender as Player

        //help
        if (args == null|| args.isEmpty()){
            helpChat(player)
            return true
        }
        val cmd = args[0]

        if (cmd == "show" && args.size == 2){

            try {
                player.sendMessage("$chatMessage§e${args[1]}の使用情報(カウント、レベル)")
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

        if (cmd == "get" && args.size == 2){

            if (plugin.drugItemStack[args[1]] == null){
                player.sendMessage("$chatMessage§4${args[1]}§aという名前の薬は見つかりませんでした。")
                player.sendMessage("$chatMessage§a設定ファイルの名前を入力してください(拡張子を含まない)")
                return true
            }

            player.inventory.addItem(plugin.drugItemStack[args[1]])
            return true

        }

        if (cmd == "reload"){
            object : BukkitRunnable() {
                override fun run() {
                    Bukkit.getScheduler().cancelTasks(plugin)
                    for (p in Bukkit.getServer().onlinePlayers){
                        db.saveDataBase(p,true)
                    }

                    player.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを保存しました")
                    plugin.load()
                    player.sendMessage("$chatMessage§eドラッグのデータを読み込みました")

                    for (p in Bukkit.getServer().onlinePlayers){
                        db.loadDataBase(p)
                    }
                    player.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを読み込みました")
                    plugin.mdpfunc.reloadAllFile()
                    player.sendMessage("$chatMessage§e全関数を再読み込みしました")
                }

            }.run()

        }

        if (cmd == "list"){
            player.sendMessage("${chatMessage}§e読み込まれているドラッグ一覧")

            object : BukkitRunnable() {
                override fun run() {
                    for (i in 0 until plugin.drugName.size){
                        player.sendMessage("${chatMessage}§e${plugin.drugName[i]}")
                    }
                }
            }.run()
        }

        if (cmd == "log"){
            if (args.size == 1)return false

            if (args[1] == "save"){
                for (p in Bukkit.getServer().onlinePlayers){
                    db.saveLog(p)
                }
                player.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグ使用ログを保存しました")
                return true
            }

                val data = plugin.playerLog[Bukkit.getPlayer(args[1])]

                if (data == null){
                    player.sendMessage("$chatMessage§e指定したプレイヤーはオフラインの可能性があります")
                    return false
                }

                player.sendMessage("$chatMessage§e${args[1]}の直近10回のドラッグ使用ログ")
                for(i in data.size - 10 until data.size ){
                    player.sendMessage("$chatMessage§e${data[i]}")
                }


        }

        return true
    }

    fun helpChat(player: Player) {

        player.sendMessage("$chatMessage§e§lMan10DrugPlugin HELP")
        player.sendMessage("$chatMessage§e/mdp get [drugName] 薬を手に入れる drugNameは設定ファイルの名前を入力してください(拡張子を含まない)")
        player.sendMessage("$chatMessage§e/mdp reload 薬の設定ファイルを再読込みします")
        player.sendMessage("$chatMessage§e/mdp show [player名] 薬の使用情報を見ることができます")
        player.sendMessage("$chatMessage§e/mdp list 読み込まれている薬の名前を表示します")
        player.sendMessage("$chatMessage§e/mdp log [player名]プレイヤーの使用ログを見ることができます \n" +
                "プレイヤー名を[save]にすると、オンラインプレイヤーのドラッグデータをDBに保存することができます")

    }

}



