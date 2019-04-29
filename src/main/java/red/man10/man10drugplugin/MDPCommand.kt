package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class MDPCommand (val plugin: Man10DrugPlugin) : CommandExecutor {


    val permissionError = "§4§lYou don't have permission."
    val chatMessage = "§5[Man10DrugPlugin]"


    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {

        ///////////////////
        //console command
        if (sender !is Player) {

            if (args == null || args.isEmpty()){
                return true
            }

            val cmd = args[0]

            //使用状態をコマンドで
            //vale using [player] [drug]
            if (cmd == "using" && args.size  == 3){

                try{ plugin.event!!.using(plugin.db.get(args[1]+args[2]),plugin.mdpConfig.get(args[2]),Bukkit.getPlayer(args[1]),args[2])
                }catch (e:Exception){
                    Bukkit.getLogger().info("error:${e.message}")
                }
            }


            return true
        }

        //////////////
        //help
        if (args != null&& args.isEmpty()){
            helpChat(sender)
            return true
        }

        val cmd = args!![0]

        ////////////
        //hasPermission
        if(!sender.hasPermission("man10drug.$cmd")){
            sender.sendMessage(permissionError)
            return true
        }

        if (cmd == "show"){

            if (args.size == 1){

                if (plugin.db.online.indexOf(sender) == -1){
                    sender.sendMessage("§e現在データの読み込み中です.....")
                    return true
                }

                sender.sendMessage("$chatMessage §e現在の依存状況")

                for (drug in plugin.drugName){
                    val c = plugin.mdpConfig.drugData[drug]?:continue

                    if (!c.isDependence){
                        continue
                    }

                    val pd = plugin.db.get(sender.name+drug)

                    if (pd.usedLevel == 0 && pd.level == 0){
                        continue
                    }
                    if(c.dependenceMsg != null){ sender.sendMessage("$chatMessage§e§l${c.displayName}:${c.dependenceMsg!![pd.level]}") }

                }

                return true
            }

            ///////////////////////////////
            //運営用show コマンド
            if (sender.hasPermission("man10drug.showop") && args.size == 2){
                try {
                    sender.sendMessage("$chatMessage§e${args[1]}の依存状況(カウント、レベル)")


                    for (drug in plugin.drugName){

                        val pd = plugin.db.playerMap[args[1]+drug]

                        if (pd!!.usedCount == 0 && pd.level == 0){
                            continue
                        }

                        sender.sendMessage(
                                "$chatMessage§e§l$drug" +
                                        ":${pd.usedCount}" +
                                        ",${pd.level}"
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

            plugin.cancelTask()

            plugin.reload = true

            Thread(Runnable {

                val mysql = MySQLManagerV2(plugin, "man10drugPlugin")

                for (p in Bukkit.getServer().onlinePlayers){
                    plugin.db.saveDataBase(p,mysql)
                }

                sender.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを保存しました")
                Bukkit.getLogger().info("オンラインプレイヤーのドラッグデータを保存しました")

                plugin.load()
                sender.sendMessage("$chatMessage§eドラッグのデータを読み込みました")
                Bukkit.getLogger().info("ドラッグのデータを読み込みました")

                for (p in Bukkit.getServer().onlinePlayers){
                    plugin.db.loadDataBase(p,mysql)
                }

                sender.sendMessage("$chatMessage§eオンラインプレイヤーのドラッグデータを読み込みました")
                Bukkit.getLogger().info("オンラインプレイヤーのドラッグデータを読み込みました")

                plugin.mdpfunc.reloadAllFile()
                sender.sendMessage("$chatMessage§e全関数を再読み込みしました")
                plugin.event!!.clearCooldown()
                sender.sendMessage("$chatMessage§e全クールダウンをリセットしました")

                Bukkit.getLogger().info("関数、クールダウン終了")

                plugin.isTask = true
                plugin.startDependenceTask()

                Bukkit.broadcastMessage("${chatMessage}§eドラッグプラグインのリロード完了 みんな使いまくってね！")

                plugin.reload = false
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

        if(cmd == "stopDependence"){
            if (!plugin.isTask){
                sender.sendMessage("$chatMessage§e依存スレッドは止まっています")
                return true

            }
            plugin.cancelTask()
            sender.sendMessage("$chatMessage§e依存スレッドを止めました")
            return true
        }
        if(cmd == "startDependence"){
            if (plugin.isTask){
                sender.sendMessage("$chatMessage§e依存スレッドは動いています")
                return true

            }
            plugin.isTask = true
            plugin.startDependenceTask()
            sender.sendMessage("$chatMessage§e依存スレッドをスタートしました")
            return true
        }

        if (cmd =="on"){
            plugin.stop = false
            plugin.startDependenceTask()
            sender.sendMessage("$chatMessage§eプラグインをスタートしました")
            return true
        }

        if (cmd == "off"){
            plugin.stop = true
            plugin.cancelTask()
            sender.sendMessage("$chatMessage§eプラグインをストップしました")
            return true
        }

        if (cmd == "clear"){
            if (args.size !=3)return true
            val pd = plugin.db.get(args[1]+args[2])

            pd.usedLevel = 0
            pd.level = 0
            pd.symptomsTotal = 0
            pd.isDependence = false
            pd.usedCount = 0

            plugin.db.playerMap[args[0]+args[1]] = pd

            sender.sendMessage("$chatMessage§e§l${args[1]}§rの§l${args[2]}のドラッグデータを削除しました")

            return true
        }

        if (cmd == "stat" && args.size == 2){
            sender.sendMessage("$chatMessage§e${args[1]}の利用統計")

            Thread(Runnable {

                val list = plugin.db.getDrugServerLevel(args[1])


                sender.sendMessage("$chatMessage§e累計使用回数:§l${plugin.db.getDrugServerTotal(args[1])}")
                sender.sendMessage("$chatMessage§e各依存レベルの依存人数")
                for (i in 0 until list.size){
                    sender.sendMessage("$chatMessage§e§lLv.$i:${list[i]}")
                }

            }).start()

            return true

        }

        if (cmd == "highspeed"){

            when(plugin.debug){
                true -> {
                    sender.sendMessage("$chatMessage§ehighspeedモードを切りました")
                    plugin.debug = false
                }
                false -> {
                    sender.sendMessage("$chatMessage§eデバッグ機能：禁断症状が3分ごとに発生するようになりました")
                    plugin.debug = true
                }
            }




            return true
        }

        if(cmd == "restore"){
            plugin.reload = false
            return true
        }


        return true
    }

    fun helpChat(player: Player) {

        player.sendMessage("$chatMessage§e§lMan10DrugPlugin HELP")

        if (player.hasPermission("man10drug.help")){ player.sendMessage("$chatMessage§e/mdp show 薬の使用情報を見ることができます") }

        if (!player.hasPermission("man10drug.helpop"))return
        player.sendMessage("$chatMessage§e/mdp get [drugName] 薬を手に入れる drugNameはDataNameに書いた値を入力してください")
        player.sendMessage("$chatMessage§e/mdp reload 薬の設定ファイルを再読込みします")
        player.sendMessage("$chatMessage§e/mdp show [player名] 薬の使用情報を見ることができます")
        player.sendMessage("$chatMessage§e/mdp list 読み込まれている薬の名前を表示します")
        player.sendMessage("$chatMessage§e/mdp log [player名] [回数]プレイヤーの使用ログを見ることができます")
        player.sendMessage("$chatMessage§e/mdp startDependence オンラインプレイヤーのタスクをスタートします")
        player.sendMessage("$chatMessage§e/mdp stopDependence オンラインプレイヤーのタスクを止めます")
        player.sendMessage("$chatMessage§e/mdp on/off プラグインの on off を切り替えます")
        player.sendMessage("$chatMessage§e/mdp clear [player] [drug] 指定プレイヤー、ドラッグの依存データをリセットします")
        player.sendMessage("$chatMessage§e/mdp using [player] [drug] ドラッグを消費せずにドラッグの使用状態を再現します(console用)")
        player.sendMessage("$chatMessage§e/mdp stat [drug] 指定ドラッグの利用統計を表示します")
        player.sendMessage("$chatMessage§e/mdp highspeed 禁断症状が3分毎に発生するようになります(デバッグ用)")
        player.sendMessage("---------------------------------------------------------")

        when(plugin.stop){
            false -> player.sendMessage("$chatMessage§e§l現在プラグインは可動しています")
            true -> player.sendMessage("$chatMessage§e§l現在プラグインはストップしています")

        }
        player.sendMessage("")
        when(plugin.db.canConnect){
            false -> player.sendMessage("$chatMessage§e§lMySQLの接続ができてません")
            true -> player.sendMessage("$chatMessage§e§lMySQLに接続できています")

        }
        player.sendMessage("")

        when(plugin.isTask){
            false -> player.sendMessage("$chatMessage§e§l依存タスクは停止しています")
            true -> player.sendMessage("$chatMessage§e§l依存タスクは動いています")

        }
        player.sendMessage("---------------------------------------------------------")


    }

}



