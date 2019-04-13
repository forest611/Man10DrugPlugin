package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class MDPCommand (val plugin: Man10DrugPlugin,val db:MDPDataBase) : CommandExecutor {


    val permissionError = "§4§lYou don't have permission."
    val chatMessage = "§5[Man10DrugPlugin]"
    val config = MDPConfig(plugin)


    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {

        if (sender !is Player) {

            if (args == null || args.isEmpty()){
                return true
            }

            val cmd = args[0]

            //使用状態をコマンドで
            if (cmd == "using" && args.size  == 3){

                val e = MDPEvent(plugin,db,config)
                try{
                    e.useDrug(Bukkit.getPlayer(args[1]), plugin.drugItemStack[args[2]]!!,args[2])

                }catch (e:Exception){
                    Bukkit.getLogger().info("error:${e.message}")
                }
            }


            return true
        }



        if (args != null&& args.isEmpty()){
            helpChat(sender)
            return true
        }

        val cmd = args!![0]

        if(sender.hasPermission("man10drug.$cmd")){
            sender.sendMessage(permissionError)
            return true
        }

        if (cmd == "show"){

            if (args.size == 1){

                sender.sendMessage("$chatMessage §e現在の依存状況")

                for (drug in plugin.drugName){
                    sender.sendMessage(
                            "$chatMessage§e§l$drug" +
                                    ":${db.playerMap[sender.name+drug]!!.count}" +
                                    ",${db.playerMap[sender.name+drug]!!.level}"
                    )
                }
            }

            ///////////////////////////////
            //運営用show コマンド
            if (sender.hasPermission("man10drug.showop") && args.size == 2){
                try {
                    sender.sendMessage("$chatMessage§e${args[1]}の依存状況(カウント、レベル)")
                    for (drug in plugin.drugName){
                        sender.sendMessage(
                                "$chatMessage§e§l$drug" +
                                        ":${db.playerMap[args[1]+drug]!!.count}" +
                                        ",${db.playerMap[args[1]+drug]!!.level}"
                        )
                    }
                }catch (e:Exception){
                    sender.sendMessage(chatMessage+"§e${args[1]}の使用情報を取得できませんでした")
                    sender.sendMessage(chatMessage+"§e${e.message}")
                }
            }

            return true

        }

        if (cmd == "get" && args.size == 2){

            if (plugin.drugItemStack[args[1]] == null){
                sender.sendMessage("$chatMessage§4${args[1]}§aという名前の薬は見つかりませんでした。")
                sender.sendMessage("$chatMessage§adrugNameはDataNameに書いた値を入力してください")
                return true
            }

            sender.inventory.addItem(plugin.drugItemStack[args[1]])
            return true

        }

        if (cmd == "reload"){

            Bukkit.broadcastMessage("${chatMessage}§eドラッグプラグインのリロードを始めます")

            Bukkit.getScheduler().cancelTasks(plugin)

            Thread(Runnable {
                for (p in Bukkit.getServer().onlinePlayers){
                    db.saveDataBase(p,true)
                }

                db.saveStat()
                sender.sendMessage("$chatMessage§eドラッグの情報を保存しました")
                db.loadStat()

                sender.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを保存しました")

                plugin.load()
                sender.sendMessage("$chatMessage§eドラッグのデータを読み込みました")

                for (p in Bukkit.getServer().onlinePlayers){
                    db.loadDataBase(p)
                }
                sender.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを読み込みました")

                plugin.mdpfunc.reloadAllFile()
                sender.sendMessage("$chatMessage§e全関数を再読み込みしました")
                plugin.event!!.clearCooldown()
                sender.sendMessage("$chatMessage§e全クールダウンをリセットしました")

                Bukkit.broadcastMessage("${chatMessage}§eドラッグプラグインのリロード完了 みんな使いまくってね！")

            }).start()

        }

        if (cmd == "list"){
            sender.sendMessage("${chatMessage}§e読み込まれているドラッグ一覧")
            for (d in plugin.drugName){
                sender.sendMessage("${chatMessage}§e$d")
            }
        }

        if (cmd == "log"){
            if (args.size == 1)return false

            if (args.size != 3){
                sender.sendMessage("$chatMessage§e/mdp log player名 回数 で入力してください")
                return true
            }

            val data = plugin.playerLog[Bukkit.getPlayer(args[1])]

            if (data == null){
                sender.sendMessage("$chatMessage§e指定したプレイヤーはオフラインの可能性があります")
                return false
            }

            if (args[2].toInt() > data.size){
                sender.sendMessage("$chatMessage§e指定回数以上ドラッグを使用していません")
                return  true
            }


            sender.sendMessage("$chatMessage§e${args[1]}の直近${args[2]}回のドラッグ使用ログ")
            for(i in data.size - args[2].toInt() until data.size ){
                sender.sendMessage("$chatMessage§e${data[i]}")
            }


        }

        if(cmd == "cancel"){
            Bukkit.getScheduler().cancelTasks(plugin)
            sender.sendMessage("$chatMessage§eオンラインプレイヤーのタスクを止めました")
            return true
        }

        if (cmd =="on"){
            plugin.stop = false
            sender.sendMessage("$chatMessage§eプラグインをスタートしました")
            return true
        }

        if (cmd == "off"){
            plugin.stop = true
            sender.sendMessage("$chatMessage§eプラグインをストップしました")
            Bukkit.getScheduler().cancelTasks(plugin)
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
            Bukkit.getScheduler().cancelTask(pd.taskId)

            db.playerMap[args[0]+args[1]] = pd

            sender.sendMessage("$chatMessage§e§l${args[1]}§rの§l${args[2]}のドラッグデータを削除しました")

            return true
        }

        if (cmd == "usedTimes" &&args.size == 2){
            try{
                sender.sendMessage("$chatMessage§eサーバー起動後の${args[1]}の使用回数:${config.get(args[1]).used}")

            }catch (e:Exception){
                sender.sendMessage("$chatMessage§eerror:${e.message}")
            }
        }

        if (cmd == "stat" && args.size == 2){
            sender.sendMessage("$chatMessage§e${args[1]}の利用統計")

            val stat = db.getStat(args[1])

            sender.sendMessage("$chatMessage§e累計使用回数:§l${stat.count}")
            sender.sendMessage("$chatMessage§e各依存レベルの依存人数")
            for (i in 0 until stat.level.size){
                sender.sendMessage("$chatMessage§e§lLv.$i:${stat.level[i]}")
            }
        }

        return true
    }

    fun helpChat(player: Player) {

        if (player.hasPermission("man10drug.help")){ player.sendMessage("$chatMessage§e/mdp show 薬の使用情報を見ることができます") }


        if (!player.hasPermission("man10drug.helpop"))return
        player.sendMessage("$chatMessage§e§lMan10DrugPlugin HELP")
        player.sendMessage("$chatMessage§e/mdp get [drugName] 薬を手に入れる drugNameはDataNameに書いた値を入力してください")
        player.sendMessage("$chatMessage§e/mdp reload 薬の設定ファイルを再読込みします")
        player.sendMessage("$chatMessage§e/mdp show [player名] 薬の使用情報を見ることができます")
        player.sendMessage("$chatMessage§e/mdp list 読み込まれている薬の名前を表示します")
        player.sendMessage("$chatMessage§e/mdp log [player名] [回数]プレイヤーの使用ログを見ることができます")
        player.sendMessage("$chatMessage§e/mdp cancel オンラインプレイヤーのタスクを止めます（デバッグ、修正用)")
        player.sendMessage("$chatMessage§e/mdp on/off プラグインの on off を切り替えます")
        player.sendMessage("$chatMessage§e/mdp clear [player] [drug] 指定プレイヤー、ドラッグの依存データをリセットします")
        player.sendMessage("$chatMessage§e/mdp using [player] [drug] ドラッグを消費せずにドラッグの使用状態を再現します(console用)")
        player.sendMessage("$chatMessage§e/mdp usedTimes [drug] サーバー起動後に何回ドラッグを使用されたか確認できます")
        player.sendMessage("$chatMessage§e/mdp stat [drug] 指定ドラッグの利用統計を表示します")
        when(plugin.stop){
            false -> player.sendMessage("$chatMessage§e§l現在プラグインは可動しています")
            true -> player.sendMessage("$chatMessage§e§l現在プラグインはストップしています")

        }
        when(db.canConnect){
            false -> player.sendMessage("$chatMessage§e§lMySQLの接続ができてません")
            true -> player.sendMessage("$chatMessage§e§lMySQLに接続できています")

        }

    }

}



