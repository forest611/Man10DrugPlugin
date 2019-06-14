package red.man10.man10drugplugin

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import java.security.SecureRandom
import java.util.*


class MDPEvent(val plugin: Man10DrugPlugin) : Listener {

    var cooldownMap: MutableList<String> = ArrayList()

    @EventHandler
    fun joinEvent(event: PlayerJoinEvent) {
        Thread(Runnable {
            Thread.sleep(10000)
            if (plugin.reload) {
                return@Runnable
            }
            val mysql = MySQLManagerV2(plugin, "man10drugplugin")
            plugin.db.loadDataBase(event.player, mysql)
        }).start()
    }

    @EventHandler
    fun leftEvent(event: PlayerQuitEvent) {
        if (plugin.reload) {
            return
        }
        Thread(Runnable {
            val mysql = MySQLManagerV2(plugin, "man10drugplugin")
            plugin.db.saveDataBase(event.player, mysql)
        }).start()
    }

    //////////////
    //ドラッグ使用イベント
    @EventHandler
    fun useEvent(event: PlayerInteractEvent) {


        if (event.action == Action.RIGHT_CLICK_AIR ||
                event.action == Action.RIGHT_CLICK_BLOCK) {

            val item = event.player.inventory.itemInMainHand ?: return

            if (item.itemMeta == null) return
            if (!item.itemMeta.hasDisplayName())return
            if (plugin.reload) return

            val display = item.itemMeta.displayName.replace("§","")

            for(d in plugin.drugName){

                if (plugin.mdpConfig.get(d).type == 4) {
                    continue
                }

                if (display.indexOf(d) == -1){
                    continue
                }

                if (display.indexOf(plugin.mdpConfig.get(d).displayName.replace("§","")) >=0){

                    event.isCancelled = true

                    useDrug(event.player, item, d)

                    return

                }
            }
        }
    }


    /////////////////////////
    //milk対策
    @EventHandler
    fun foodEvent(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET && !plugin.canMilk && !event.player.hasPermission("man10drug.milk")) {
            event.isCancelled = true
            return
        }
    }


    fun clearCooldown() {
        cooldownMap.clear()
    }

    ////////////////////////
    ///ドラッグ使用時
    ///pd = ドラッグを使用したプレイヤーのデータ
    ///drugData = プレイヤーが使用したドラッグのデータ
    fun useDrug(player: Player, item: ItemStack, drug: String) {


        if (plugin.disableWorld.indexOf(player.world.name) >= 1) {
            player.sendMessage("§eここではドラッグは使えません")
            return
        }


        if (cooldownMap.contains(player.uniqueId.toString() + " " + drug)) {
            return
        }

        if (plugin.stop || !plugin.db.canConnect || plugin.db.playerMap[player.name + drug] == null) {
            player.sendMessage("§e今は使う気分ではないようだ")
            return
        }
////////////////////////////////////////////////////////////
        val key = player.name + drug

        val pd = plugin.db.get(key)
        val drugData = plugin.mdpConfig.get(drug)

        if (drugData.disableWorld.isNotEmpty() && drugData.disableWorld.indexOf(player.world.name) >= 1) {
            player.sendMessage("§eここではドラッグは使えません")
            return
        }

        ////////////////////////
        //cooldown
        ///////////////////////

        cooldownMap.add(player.uniqueId.toString() + " " + drug)

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

            cooldownMap.remove(player.uniqueId.toString() + " " + drug)

        }, drugData.cooldown)

        using(pd, drugData, player, drug)

        ////////////////////
        //remove 1 item
        if (drugData.removeItem) {
            item.amount = item.amount - 1
            player.inventory.itemInMainHand = item
        }


        ////////////////////////
        // Func
        ///////////////////////
        for (funcname in drugData.func) {
            plugin.mdpfunc.runFunc(player, funcname)
        }
        ////////////////////////
        // FuncDelay
        ///////////////////////
        for (funcname in drugData.funcDelay) {
            val times = funcname.split(";")
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                plugin.mdpfunc.runFunc(player, times[0])
            }, times[1].toLong())
        }
        ////////////////////////
        // FuncRandom
        ///////////////////////
        if (drugData.funcRandom.size > 0) {
            val rnd = SecureRandom()
            val r = rnd.nextInt(drugData.funcRandom.size)
            val s = drugData.funcRandom[r]
            plugin.mdpfunc.runFunc(player, s)
        }
        ////////////////////////
        // FuncRandomDelay
        ///////////////////////
        if (drugData.funcRandomDelay.size > 0) {
            val rnd = SecureRandom()
            val r = rnd.nextInt(drugData.funcRandomDelay.size)
            val funcname = drugData.funcRandomDelay[r]
            val times = funcname.split(";")
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                plugin.mdpfunc.runFunc(player, times[0])
            }, times[1].toLong())
        }

    }


    ///////////////////////////////////
    //      サブスレッドで動かす処理
    ///////////////////////////////
    fun using(pd: playerData, drugData: Data, player: Player, drug: String) {

        val em = EventManager(plugin, player, pd, drug)

        em.removeBuffs()

        Bukkit.getScheduler().runTask(plugin, Runnable {

            em.runAll()

            /////////////////////////
            //周囲に迷惑
            ///////////////////////
            if (plugin.size(drugData.nearPlayer, pd)) {
                val data = drugData.nearPlayer[pd.level].split(";")
                val list = getNearByPlayers(player, data[0].toInt(),drug)
                for (p in list) {
                    plugin.mdpfunc.runFunc(p, data[1])
                }
            }

            //usedLevel increment
            pd.usedLevel++  //levelごと
            pd.usedCount++  //total
            pd.countOnline++

            //免疫上げる
            if (drugData.immunity !=""){
                val i = plugin.db.get(player.name+drugData.immunity)
                i.immunity++
                if (i.immunity>10){
                    player.sendMessage("§2もうこれ吸う必要ないんじゃないかな...")
                    i.immunity = 10
                }
                plugin.db.playerMap[player.name+drugData.immunity] = i
            }

            ////////////////////////
            // type 0 only
            ///////////////////////
            if (drugData.type == 0) {

                var isUp = false

                if (pd.level < drugData.dependenceLevel && pd.usedLevel >= drugData.nextLevelCount[pd.level]
                        && drugData.nextLevelCount.isNotEmpty()){
                    isUp = true
                }

                //レベルアップ
                //dependenceLevelを5にした場合、level5まで上がる
                //0も含めるので、6段階となる
                if (drugData.symptomsLeUpProb.isNotEmpty()){

                    if (drugData.symptomsLeUpProb[pd.level]>Math.random())isUp = true
                }

                if (isUp) {

                    ////////////////////////
                    //command
                    ///////////////////////
                    if (drugData.commandLvUp[pd.level] != null) {

                        for (c in drugData.commandLvUp[pd.level]!!) {
                            val cmd = plugin.repStr(c, player, pd)

                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                        }
                    }

                    ////////////////////////
                    //command random
                    ///////////////////////
                    if (drugData.commandRandomLvUp[pd.level] != null) {

                        val cmd = plugin.repStr(drugData.commandRandomLvUp[pd.level]!![Random().nextInt(
                                drugData.commandRandomLvUp[pd.level]!!.size
                        )], player, pd)

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                    }
                    ////////////////////////
                    // Func
                    ///////////////////////
                    for (funcname in drugData.func) {
                        plugin.mdpfunc.runFunc(player, funcname)
                    }
                    ////////////////////////
                    // FuncDelay
                    ///////////////////////
                    for (funcname in drugData.funcDelayLvUp) {
                        val times = funcname.split(";")
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                            plugin.mdpfunc.runFunc(player, times[0])
                        }, times[1].toLong())
                    }
                    ////////////////////////
                    // FuncRandom
                    ///////////////////////
                    if (drugData.funcRandomLvUp.size > 0) {
                        val rnd = SecureRandom()
                        val r = rnd.nextInt(drugData.funcRandomLvUp.size)
                        val s = drugData.funcRandomLvUp[r]
                        plugin.mdpfunc.runFunc(player, s)
                    }
                    ////////////////////////
                    // FuncRandomDelay
                    ///////////////////////
                    if (drugData.funcRandomDelayLvUp.size > 0) {
                        val rnd = SecureRandom()
                        val r = rnd.nextInt(drugData.funcRandomDelayLvUp.size)
                        val funcname = drugData.funcRandomDelayLvUp[r]
                        val times = funcname.split(";")
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                            plugin.mdpfunc.runFunc(player, times[0])
                        }, times[1].toLong())
                    }

                    //usedLevel reset レベルアップ
                    pd.usedLevel = 0
                    pd.level++
                }

                ///////////////////////
                //治療薬の効果削除
                if (drugData.weakDrug != "none") {
                    val weak = plugin.db.get(player.name + drugData.weakDrug)

                    weak.usedLevel = 0

                    plugin.db.playerMap[player.name + drugData.weakDrug] = weak

                }

                //最終利用時刻更新
                pd.time = Date()
                pd.isDependence = true
                pd.symptomsTotal = 0

            }

            ////////////////////////
            // type 1 only
            ///////////////////////
            if (drugData.type == 1) {

                val key2 = player.name + drugData.weakDrug
                val pd2 = plugin.db.get(key2)

                if (pd2.level <= 0 && pd2.usedLevel <= 0) {
                    player.sendMessage("§aあれ？.....解毒薬を飲む必要ってあるのかな...")
                    return@Runnable
                }

                pd.usedLevel++

                if (pd.usedCount > drugData.weakUsing[pd2.level]) {
                    pd2.usedLevel = 0
                    pd2.level--
                    pd.usedLevel = 0
                    if (pd2.level < 0) {
                        pd2.level = 0
                        pd2.isDependence = false
                    }
                }

                plugin.db.playerMap[key2] = pd2

            }

            ////////////////////
            //ログをメモリに保存、最後に使った時間を保存
            plugin.db.addLog(player, drug)


            ////////////////////////
            //message
            ///////////////////////
            if (plugin.size(drugData.useMsg, pd)) {
                player.sendMessage(plugin.repStr(drugData.useMsg[pd.level], player, pd))
            }

            //Delay message
            if (plugin.size(drugData.useMsgDelay, pd)) {
                val times = drugData.useMsgDelay[pd.level].split(";")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.sendMessage(plugin.repStr(times[0], player, pd))
                }, times[1].toLong())

            }

            ////////////////
            //アイテムを変える
            ////////////////
            if (drugData.isChange) {
                player.inventory.addItem(plugin.drugItemStack[drugData.changeItem])
            }

            //player data save memory
            plugin.db.playerMap[player.name + drug] = pd

        })

    }


    ///////////////////////////
    //周囲のプレイヤーを検知
    fun getNearByPlayers(centerPlayer: Player, distance: Int,drug:String): ArrayList<Player> {
        val ds = distance * distance
        val players = ArrayList<Player>()
        val loc = centerPlayer.location
        val world = centerPlayer.world
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.world == world && player.location.distanceSquared(loc) < ds) {

                if (defenseCheck(0,player,drug))continue

                players.add(player)

            }
        }
        players.remove(centerPlayer)
        return players
    }


    fun defenseCheck(place: Int, player: Player,drug: String): Boolean {

        val pd = plugin.db.get(player.name+drug)

        if(Random().nextInt(9) +1 <= pd.immunity){
            pd.immunity --
            plugin.db.playerMap[player.name+drug] = pd
            return false
        }

        val item: ItemStack = when (place) {
            0 -> {
                player.inventory.helmet ?: return false
            }

            else -> return false
        }

        if (item.itemMeta == null) return false
        if (item.itemMeta.lore == null) return false
        if (item.itemMeta.lore.isEmpty()) return false

        val protecter = item.itemMeta.displayName.replace("§", "")

        ////////////////////////
        /// 副流煙をうけないかどうか
        for(d in plugin.drugName){

            if (plugin.mdpConfig.get(d).type != 4) {
                continue
            }

            if (protecter.indexOf(d) == -1){
                continue
            }

            if (protecter.indexOf(plugin.mdpConfig.get(d).displayName.replace("§","")) >=0){

                if (plugin.mdpConfig.get(protecter).defenseNear>Math.random()) { return true}

            }
        }

        return false

    }
}
