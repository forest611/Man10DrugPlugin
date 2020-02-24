package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Commands(private val plugin: Man10DrugPlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {

        if(sender is Player && !sender.hasPermission("drug.op"))return false

        //help message
        if (args == null || args.isEmpty()){
            //help
            if (sender is Player){
                help(sender)
                return true
            }
            return true
        }

        val cmd = args[0]

        //指定プレイヤーに指定ドラッグを付与する
        if (cmd == "using"){
            if (plugin.drugName.indexOf(args[1]) == -1)return true

            plugin.events.useDrug(Bukkit.getPlayer(args[1]),args[2])
            return true
        }

        //指定ドラッグの依存をすべて消す
        if (cmd == "removedepend"){
            if (plugin.drugName.indexOf(args[1]) == -1)return true

            Thread(Runnable {
                val drug = args[1]

                for (p in Bukkit.getOnlinePlayers()){
                    val pd = plugin.db.playerData[Pair(p,drug)]!!

                    pd.usedLevel = 0
                    pd.level = 0
                    pd.isDepend = false
                    pd.totalSymptoms = 0

                    plugin.db.playerData[Pair(p,drug)] = pd

                    for (e in p.activePotionEffects){
                        p.removePotionEffect(e.type)
                    }

                }
            }).start()
        }

        if (cmd == "clear"){
            //コマンド発行者の依存データを初期化する
            if (args.size == 1 && sender is Player){

                for (drug in plugin.drugName){
                    val pd = plugin.db.playerData[Pair(sender,drug)]!!

                    pd.usedCount = 0
                    pd.totalSymptoms = 0
                    pd.isDepend = false
                    pd.level = 0

                    plugin.db.playerData[Pair(sender,drug)] = pd

                    for (e in sender.activePotionEffects){
                        sender.removePotionEffect(e.type)
                    }

                }

            }
            //指定プレイヤーの依存データを初期化する
            if (args.size == 2){
                val p = Bukkit.getPlayer(args[1])
                for (drug in plugin.drugName){
                    val pd = plugin.db.playerData[Pair(p,drug)]!!

                    pd.usedCount = 0
                    pd.totalSymptoms = 0
                    pd.isDepend = false
                    pd.level = 0

                    plugin.db.playerData[Pair(p,drug)] = pd

                    for (e in p.activePotionEffects){
                        p.removePotionEffect(e.type)
                    }

                }
            }
            //指定プレイヤーの指定ドラッグの依存データを初期化する
            if (args.size == 3){
                if (plugin.drugName.indexOf(args[2]) == -1)return true
                val p = Bukkit.getPlayer(args[1])
                val pd = plugin.db.playerData[Pair(p,args[2])]!!

                pd.usedCount = 0
                pd.totalSymptoms = 0
                pd.isDepend = false
                pd.level = 0

                plugin.db.playerData[Pair(p,args[2])] = pd

                for (e in p.activePotionEffects){
                    p.removePotionEffect(e.type)
                }

            }
        }

        if (cmd == "data"){
            if (sender is Player && args.size == 1){
                sender.sendMessage("§e§lあなたの現在のデータ")
                sender.sendMessage("§e§l============================================")
                for (drug in plugin.drugName){
                    val data = plugin.drugData[drug]!!
                    val pd = plugin.db.playerData[Pair(sender,drug)]?:return true

                    sender.sendMessage("${data.displayName}§f§l:${data.dependMsg[pd.level]}")
                }
                sender.sendMessage("§e§l============================================")

            }
            //指定プレイヤーのデータを見る(詳細データも見る)
            if (sender is Player && args.size == 2){
                val p = Bukkit.getPlayer(args[1])
                sender.sendMessage("§e§l${args[1]}の現在のデータ")
                sender.sendMessage("§e§l============================================")
                for (drug in plugin.drugName){
                    val data = plugin.drugData[drug]!!
                    val pd = plugin.db.playerData[Pair(p,drug)]?:return true

                    sender.sendMessage("${data.displayName}§f§l:${data.dependMsg[pd.level]}")
                    sender.sendMessage("§f§lcount:${pd.usedCount},isDepend:${pd.isDepend}")
                    sender.sendMessage("§f§ltime:${pd.finalUseTime},totalSympoms:${pd.totalSymptoms}")
                }
                sender.sendMessage("§e§l============================================")

            }
        }
        if (sender !is Player)return true

        if (cmd == "get"){
            if (plugin.drugName.indexOf(args[1]) == -1)return true
            sender.inventory.addItem(plugin.drugData[args[1]]!!.itemStack)
            return true
        }

        if (cmd == "reload"){

            Bukkit.broadcastMessage("§e§lドラッグプラグインのリロードを開始します!")
            plugin.isReload = true

            for (p in Bukkit.getOnlinePlayers()){
                plugin.db.logoutDB(p)
            }

            sender.sendMessage("§e§lオンラインプレイヤーのデータ保存完了！")

            Bukkit.getScheduler().runTask(plugin) {
                plugin.configs.loadPluginConfig()

                plugin.configs.loadDrugs()
                plugin.func.loadFunction()

                sender.sendMessage("§e§lドラッグデータ、プラグインのコンフィグ読み込み完了")

                for (p in Bukkit.getOnlinePlayers()){
                    plugin.db.loginDB(p)
                }

                sender.sendMessage("§e§lオンラインプレイヤーのデータ読込完了")

                plugin.isReload = false
                plugin.thread.dependThread()
                Bukkit.broadcastMessage("§e§lドラッグプラグインのリロードが完了しました！")
            }
            return true
        }

        if (cmd == "list"){
            sender.sendMessage("§e§l読み込まれているドラッグ、アイテム一覧")

            for (d in plugin.drugName){
                sender.sendMessage("${plugin.drugData[d]!!.displayName}§f§l:$d")
            }
            return true
        }

        if (cmd == "on"){
            plugin.pluginEnable = true
            plugin.thread.dependThread()
            sender.sendMessage("§lプラグインをonにしました")
            return true
        }
        if (cmd == "off"){
            plugin.pluginEnable = false
            sender.sendMessage("§lプラグインをoffにしました")
            return true
        }

        if (cmd == "stat"){
            Bukkit.getScheduler().runTask(plugin){
                sender.sendMessage("§l現在データを取得中です...")
                val total = plugin.db.getServerTotal(args[1])
                sender.sendMessage("§l${args[1]}の利用統計")
                sender.sendMessage("§lトータル:$total")
                
            }
        }

        if (cmd == "debug"){
            plugin.debugMode = !plugin.debugMode
        }

        return false
    }


    fun help(p: Player) {

        p.sendMessage("§e§lMan10DrugPlugin HELP")
        p.sendMessage("§e/mdp get [drugName] 薬を手に入れる drugNameはDataNameに書いた値を入力してください")
        p.sendMessage("§e/mdp reload 薬の設定ファイルを再読込みします")
        p.sendMessage("§e/mdp data [player名] 薬の使用情報を見ることができます")
        p.sendMessage("§e/mdp list 読み込まれている薬の名前を表示します")
        p.sendMessage("§e/mdp on/off プラグインの on off を切り替えます")
        p.sendMessage("§e/mdp clear [player] 指定プレイヤーの依存データをリセットします")
        p.sendMessage("§e/mdp using [player] [drug] ドラッグを消費せずにドラッグの使用状態を再現します(console用)")
        p.sendMessage("§e/mdp stat [drug] 指定ドラッグの利用統計を表示します")
        p.sendMessage("§e/mdp removedependence [drug] オンラインプレイヤーの指定ドラッグの依存を消します")
    }
}