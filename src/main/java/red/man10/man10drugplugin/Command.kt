package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.debugMode
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugData
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugName
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.isReload
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.pluginEnable

object Command: CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if(sender is Player && !sender.hasPermission("drug.op"))return false

        //help message
        if (args.isEmpty()){
            //help
            if (sender is Player){
                help(sender)
                return true
            }
            return true
        }

        when(args[0]){

            "help" -> {
                if (sender !is Player)return false

                help(sender)
            }

            "use" ->{
                if (!drugName.contains(args[1]))return true

                val p = Bukkit.getPlayer(args[1])?:return true
                val drug = args[1]
                Event.useDrug(p,args[2],drugData[drug]!!)
                return true

            }


            "get" ->{
                if (sender !is Player)return true
                if (!drugName.contains(args[1]))return true
                sender.inventory.addItem(drugData[args[1]]!!.itemStack!!)
                return true

            }

            "reload"->{
                Bukkit.broadcastMessage("§e§lドラッグプラグインのリロードを開始します!")
                isReload = true


                Thread{
                    Config.loadPluginConfig()

                    Config.loadDrugs()
                    MDPFunction.loadFunction()

                    sender.sendMessage("§e§lドラッグデータ、プラグインのコンフィグ読み込み完了")

                    Bukkit.broadcastMessage("§e§lドラッグプラグインのリロードが完了しました！")
                    isReload = false
                }.start()

            }

            "list" ->{
                sender.sendMessage("§e§l読み込まれているドラッグ、アイテム一覧")

                for (d in drugName){
                    sender.sendMessage("${drugData[d]!!.displayName}§f§l:$d")
                }
                return true

            }

            "on" ->{
                pluginEnable = true
                sender.sendMessage("§lプラグインをonにしました")
                return true

            }

            "off" ->{
                pluginEnable = false
                sender.sendMessage("§lプラグインをoffにしました")
                return true

            }

            "debug" ->{
                debugMode = !debugMode
                sender.sendMessage("$debugMode")
            }

        }


        return false
    }


    private fun help(p: Player) {

        p.sendMessage("§e§lMan10DrugPlugin HELP")
        p.sendMessage("§e/mdp get [drugName] 薬を手に入れる drugNameはDataNameに書いた値を入力してください")
        p.sendMessage("§e/mdp reload 薬の設定ファイルを再読込みします")
        p.sendMessage("§e/mdp list 読み込まれている薬の名前を表示します")
        p.sendMessage("§e/mdp on/off プラグインの on off を切り替えます")
        p.sendMessage("§e/mdp clear [player] 指定プレイヤーの依存データをリセットします")
        p.sendMessage("§e/mdp use [player] [drug] ドラッグを消費せずにドラッグの使用状態を再現します(console用)")
    }
}