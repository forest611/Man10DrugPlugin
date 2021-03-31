package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
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

            "use" ->{
                if (!drugName.contains(args[1]))return true

                val p = Bukkit.getPlayer(args[1])?:return true
                val drug = args[1]
                Event.useDrug(p,args[2],drugData[drug]!!,Database.get(p,drug)?:return true)
                return true

            }

            "removedepend" ->{

                val drug = args[1]

                if (!drugName.contains(drug))return true

                Thread {

                    for (p in Bukkit.getOnlinePlayers()) {
                        val pd = Database.get(p,drug)?:continue

                        pd.level = 0
                        pd.isDepend = false
                        pd.totalSymptoms = 0

                        Database.set(p,drug,pd)

                        for (e in p.activePotionEffects) {
                            p.removePotionEffect(e.type)
                        }

                    }
                }.start()
            }

            "clear" ->{
                //コマンド発行者の依存データを初期化する
                if (args.size == 1 && sender is Player){

                    for (drug in drugName){
                        val pd = Database.get(sender,drug)?:continue

                        pd.usedCount = 0
                        pd.totalSymptoms = 0
                        pd.isDepend = false
                        pd.level = 0

                        Database.set(sender,drug,pd)

                        for (e in sender.activePotionEffects){
                            sender.removePotionEffect(e.type)
                        }

                    }

                }
                //指定プレイヤーの依存データを初期化する
                if (args.size == 2){
                    val p = Bukkit.getPlayer(args[1])?:return true
                    for (drug in drugName){
                        val pd = Database.get(p,drug)?:continue

                        pd.usedCount = 0
                        pd.totalSymptoms = 0
                        pd.isDepend = false
                        pd.level = 0

                        Database.set(p,drug,pd)

                        for (e in p.activePotionEffects){
                            p.removePotionEffect(e.type)
                        }

                    }
                }
                //指定プレイヤーの指定ドラッグの依存データを初期化する
                if (args.size == 3){

                    val drug = args[2]

                    if (!drugName.contains(drug))return true
                    val p = Bukkit.getPlayer(args[1])?:return true
                    val pd = Database.get(p,drug)?:return true

                    pd.usedCount = 0
                    pd.totalSymptoms = 0
                    pd.isDepend = false
                    pd.level = 0

                    Database.set(p,drug,pd)

                    for (e in p.activePotionEffects){
                        p.removePotionEffect(e.type)
                    }

                }

            }

            "data" ->{
                if (sender is Player && args.size == 1){
                    sender.sendMessage("§e§lあなたの現在のデータ")
                    sender.sendMessage("§e§l============================================")
                    for (drug in drugName){
                        val data = drugData[drug]!!

                        if (!Database.hasData(sender,drug))continue

                        val pd = Database.get(sender,drug)!!
                        if(data.dependMsg.isNotEmpty() && pd.usedCount !=0){
                            sender.sendMessage("${data.displayName}§f§l:${data.dependMsg[pd.level]}")
                        }
                    }
                    sender.sendMessage("§e§l============================================")

                }
                //指定プレイヤーのデータを見る(詳細データも見る)
                if (sender is Player && args.size == 2){
                    val p = Bukkit.getPlayer(args[1])?:return true
                    sender.sendMessage("§e§l${args[1]}の現在のデータ")
                    sender.sendMessage("§e§l============================================")
                    for (drug in drugName){
                        val data = drugData[drug]!!

                        if (!Database.hasData(p,drug))continue

                        val pd = Database.get(p,drug)!!

                        sender.sendMessage(data.displayName)
                        sender.sendMessage("§f§lcount:${pd.usedCount},isDepend:${pd.isDepend}")
                        sender.sendMessage("§f§ltime:${pd.finalUseTime},totalSymptoms:${pd.totalSymptoms}")
                    }
                    sender.sendMessage("§e§l============================================")

                }

            }

            "get" ->{
                if (sender !is Player)return true
                if (!drugName.contains(args[1]))return true
                sender.inventory.addItem(drugData[args[1]]!!.itemStack)
                return true

            }

            "reload"->{
                Bukkit.broadcastMessage("§e§lドラッグプラグインのリロードを開始します!")
                isReload = true

                for (p in Bukkit.getOnlinePlayers()){
                    Database.save(p)
                }

                sender.sendMessage("§e§lオンラインプレイヤーのデータ保存完了！")

                Thread{
                    Config.loadPluginConfig()

                    Config.loadDrugs()
                    MDPFunction.loadFunction()

                    sender.sendMessage("§e§lドラッグデータ、プラグインのコンフィグ読み込み完了")

                    for (p in Bukkit.getOnlinePlayers()){
                        Database.load(p)
                    }

                    sender.sendMessage("§e§lオンラインプレイヤーのデータ読込完了")

                    isReload = false
                    DependThread.dependThread()
                    Bukkit.broadcastMessage("§e§lドラッグプラグインのリロードが完了しました！")

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
                DependThread.dependThread()
                sender.sendMessage("§lプラグインをonにしました")
                return true

            }

            "off" ->{
                pluginEnable = false
                sender.sendMessage("§lプラグインをoffにしました")
                return true

            }

            "stat" ->{
                Thread{
                    sender.sendMessage("§l現在データを取得中です...")
                    val total = Database.getServerTotal(args[1])
                    sender.sendMessage("§l${args[1]}の利用統計")
                    sender.sendMessage("§lトータル:$total")

                }.start()
            }

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
        p.sendMessage("§e/mdp removedepend [drug] オンラインプレイヤーの指定ドラッグの依存を消します")
    }
}