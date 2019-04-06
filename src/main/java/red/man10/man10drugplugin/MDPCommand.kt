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
                for (drug in plugin.drugName){
                    player.sendMessage(
                            "$chatMessage§e$drug" +
                            ",${db.playerMap[args[1]+drug]!!.count}" +
                            ",${db.playerMap[args[1]+drug]!!.level}"
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
                player.sendMessage("$chatMessage§adrugNameはDataNameに書いた値を入力してください")
                return true
            }

            player.inventory.addItem(plugin.drugItemStack[args[1]])
            return true

        }

        if (cmd == "reload"){

            Bukkit.getScheduler().cancelTasks(plugin)

            Thread(Runnable {
                for (p in Bukkit.getServer().onlinePlayers){
                    db.saveDataBase(p,true)
                }

                db.saveStock()
                player.sendMessage("$chatMessage§eドラッグの情報を保存しました")
                db.loadStock()

                player.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを保存しました")

                plugin.load()
                player.sendMessage("$chatMessage§eドラッグのデータを読み込みました")

                for (p in Bukkit.getServer().onlinePlayers){
                    db.loadDataBase(p)
                }
                player.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを読み込みました")

                plugin.mdpfunc.reloadAllFile()
                player.sendMessage("$chatMessage§e全関数を再読み込みしました")
                plugin.event!!.clearCooldown()
                player.sendMessage("$chatMessage§e全クールダウンをリセットしました")

            }).start()

        }


        if (cmd == "list"){
            player.sendMessage("${chatMessage}§e読み込まれているドラッグ一覧")
            for (d in plugin.drugName){
                player.sendMessage("${chatMessage}§e$d")
                }
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

            if (args.size != 3){
                player.sendMessage("$chatMessage§e/mdp log player名 回数 で入力してください")
                return true
            }

            val data = plugin.playerLog[Bukkit.getPlayer(args[1])]

            if (data == null){
                player.sendMessage("$chatMessage§e指定したプレイヤーはオフラインの可能性があります")
                return false
            }

            if (args[2].toInt() > data.size){
                player.sendMessage("$chatMessage§e指定回数以上ドラッグを使用していません")
                return  true
            }


            player.sendMessage("$chatMessage§e${args[1]}の直近${args[2]}回のドラッグ使用ログ")
            for(i in data.size - args[2].toInt() until data.size ){
                player.sendMessage("$chatMessage§e${data[i]}")
            }


        }

        if(cmd == "cancel"){
            Bukkit.getScheduler().cancelTasks(plugin)
            player.sendMessage("$chatMessage§eオンラインプレイヤーのタスクを止めました")
            return true
        }

        if (cmd =="on"){
            plugin.stop = false
            player.sendMessage("$chatMessage§eプラグインをスタートしました")
            return true
        }

        if (cmd == "off"){
            plugin.stop = true
            player.sendMessage("$chatMessage§eプラグインをストップしました")
            return true
        }

        if (cmd == "clear"){
            if (args.size !=3)return true
            val pd = db.get(args[1]+args[2])

            pd.count = 0
            pd.level = 0
            pd.times = 0
            pd.taskId = 0
            pd.isDependence = false

            player.sendMessage("削除しました")

        }

        return true
    }

    fun helpChat(player: Player) {

        player.sendMessage("$chatMessage§e§lMan10DrugPlugin HELP")
        player.sendMessage("$chatMessage§e/mdp get [drugName] 薬を手に入れる drugNameはDataNameに書いた値を入力してください")
        player.sendMessage("$chatMessage§e/mdp reload 薬の設定ファイルを再読込みします")
        player.sendMessage("$chatMessage§e/mdp show [player名] 薬の使用情報を見ることができます")
        player.sendMessage("$chatMessage§e/mdp list 読み込まれている薬の名前を表示します")
        player.sendMessage("$chatMessage§e/mdp log [player名] [回数]プレイヤーの使用ログを見ることができます \n" +
                "プレイヤー名を[save]にすると、オンラインプレイヤーのログをDBに保存することができます")
        player.sendMessage("$chatMessage§e/mdp cancel オンラインプレイヤーのタスクを止めます（デバッグ用)")
        player.sendMessage("$chatMessage§e/mdp on/off プラグインの on off を切り替えます")
        player.sendMessage("$chatMessage§e/mdp clear [player] [drug] 指定プレイヤー、ドラッグの依存データをリセットします")
        when(plugin.stop){
            false -> player.sendMessage("$chatMessage§e§l現在プラグインは可動しています")
            true -> player.sendMessage("$chatMessage§e§l現在プラグインはストップしています")

        }

    }

}



