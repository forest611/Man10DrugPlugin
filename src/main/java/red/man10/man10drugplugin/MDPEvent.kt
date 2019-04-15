package red.man10.man10drugplugin

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.security.SecureRandom
import java.text.DateFormat
import java.util.*
import java.text.SimpleDateFormat




class MDPEvent(val plugin: Man10DrugPlugin, val db:MDPDataBase,val config:MDPConfig) : Listener {

    public var cooldownMap : MutableList<String> = ArrayList()

    @EventHandler
    fun joinEvent(event:PlayerJoinEvent){
        Thread(Runnable {
            db.loadDataBase(event.player)
        }).start()
    }

    @EventHandler
    fun leftEvent(event:PlayerQuitEvent){
        Thread(Runnable {
            db.saveDataBase(event.player)
        }).start()
    }

    //////////////
    //ドラッグ使用イベント
    @EventHandler
    fun useEvent(event: PlayerInteractEvent){


        if (event.action == Action.RIGHT_CLICK_AIR ||
                event.action == Action.RIGHT_CLICK_BLOCK){

            val item = event.player.inventory.itemInMainHand ?: return

            if (item.itemMeta == null)return
            if (item.itemMeta.lore == null)return
            if (item.itemMeta.lore.isEmpty())return
            if (item.type == Material.AIR)return

            val drug = item.itemMeta.lore[item.itemMeta.lore.size -1].replace("§","")

            if(plugin.drugName.indexOf(drug) == -1){
                return
            }

            event.isCancelled = true

            useDrug(event.player,item,drug)
        }
    }

    //チャット破壊イベント
    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent){

        event.message = crashChat(event)

    }

    /////////////////////////
    //メッセージを壊す
    fun crashChat(event:AsyncPlayerChatEvent):String{

        val msg = StringBuilder()
        msg.append(event.message)


        val player = event.player

        //ドラッグの数だけ実行
        for (i in 0 until plugin.drugName.size) {
            val drug = config.get(plugin.drugName[i])

            val key = player.name + plugin.drugName[i]

            val pd = db.get(key)


            //壊すか
            if (!drug.isCrashChat || drug.crashChance == null) {
                continue
            }

            if (pd.usedLevel == 0 && pd.level == 0) {
                continue
            }

            //指定レベルが無い、もしくは壊さない場合
            if (drug.crashChance!![pd.level] == "false") {
                continue
            }

            val value = drug.crashChance!![pd.level].split(",")

            val size = event.message.length


            //value なん文字以上か,百分率,何箇所壊すか,壊す文字の範囲

            if (size < value[0].toInt() || value[1].toInt() >= Random().nextInt(99) + 1) {
                continue
            }

            //破壊部分
            for (i1 in 0 until value[2].toInt()) {

                val r = Random().nextInt(size - 1)
                msg.insert(r, "&k")

                if (r + value[3].toInt() < size) {
                    msg.insert(r + value[3].toInt(), "&r")
                }
            }

        }
        return msg.toString()
    }

    fun clearCooldown(){
        cooldownMap.clear()
    }

    ////////////////////////
    ///ドラッグ使用時
    ///pd = ドラッグを使用したプレイヤーのデータ
    ///drugData = プレイヤーが使用したドラッグのデータ
    fun useDrug(player: Player,item: ItemStack,drug:String) {



        if (cooldownMap.contains(player.uniqueId.toString() + " " + drug)) {
            return
        }

        if (plugin.stop || !db.canConnect) {
            player.sendMessage("§e今は使う気分ではないようだ")
            return
        }
////////////////////////////////////////////////////////////
        val key = player.name + drug

        val pd = db.get(key)
        val stat = db.getStat(drug)
        val drugData = config.get(drug)

        ////////////////////////////////
        //時間のかかる処理をスレッド化
        Bukkit.getScheduler().runTask(plugin) {
            ////////////////////////
            //message
            ///////////////////////
            if (drugData.useMsg != null && plugin.size(drugData.useMsg!!, pd)) {
                player.sendMessage(plugin.repStr(drugData.useMsg!![pd.level], player, pd, drugData))
            }

            //Delay message
            if (drugData.useMsgDelay != null && plugin.size(drugData.useMsgDelay!!, pd)) {
                val times = drugData.useMsgDelay!![pd.level].split(";")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.sendMessage(plugin.repStr(times[0], player, pd, drugData))
                }, times[1].toLong())

            }

            ////////////////////////
            //command
            ///////////////////////
            if (drugData.command[pd.level] != null) {

                for (c in drugData.command[pd.level]!!) {
                    val cmd = plugin.repStr(c, player, pd, drugData)

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                }

            }
            ////////////////////////
            //random command
            ///////////////////////
            if (drugData.commandRandom[pd.level] != null) {

                val cmd = plugin.repStr(drugData.commandRandom[pd.level]!![Random().nextInt(
                        drugData.commandRandom[pd.level]!!.size
                )], player, pd, drugData)

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

            }

            ////////////////////////
            //command delay
            ///////////////////////
            if (drugData.commandDelay[pd.level] != null) {

                for (c in drugData.commandDelay[pd.level]!!) {
                    val command = c.split(";")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.repStr(command[0], player, pd, drugData))

                    }, command[1].toLong())
                }

            }

            ////////////////////////
            //command random delay
            ///////////////////////
            if (drugData.commandRandomDelay[pd.level] != null) {

                val command = drugData.commandRandomDelay[pd.level]!![
                        drugData.commandRandomDelay[pd.level]!!.size - 1].split(";")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.repStr(command[0], player, pd, drugData))

                }, command[1].toLong())
            }

            /////////////////////////
            //数値ストック
            //////////////////////
            if (drugData.stockMode) {
                stat.stock += drugData.addStock
            }


            ////////////////////
            //ログをメモリに保存、最後に使った時間を保存
            db.addLog(player, drug)
        }

        //////////

        ////////////////////////
        //cooldown
        ///////////////////////

        cooldownMap.add(player.uniqueId.toString() + " " + drug)

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

            cooldownMap.remove(player.uniqueId.toString() + " " + drug)

        }, drugData.cooldown)


        /////////////////////////
        //remove buff
        if (drugData.removeBuffs){
            for (effect in player.activePotionEffects){
                player.removePotionEffect(effect.type)
            }
        }


        ////////////////////////
        //buff
        ///////////////////////
        if (drugData.buff[pd.level] != null) {

            for (b in drugData.buff[pd.level]!!) {
                val buff = b.split(",")
                player.addPotionEffect(PotionEffect(
                        PotionEffectType.getByName(buff[0]),
                        buff[1].toInt(),
                        buff[2].toInt()
                ))
            }
        }


        ////////////////////////
        //buff random
        ///////////////////////
        if (drugData.buffRandom[pd.level] != null) {

            val buff = drugData.buffRandom[pd.level]!![Random()
                    .nextInt(drugData.buffRandom[pd.level]!!.size - 1)].split(",")

            player.addPotionEffect(PotionEffect(
                    PotionEffectType.getByName(buff[0]),
                    buff[1].toInt(),
                    buff[2].toInt()
            ))
        }

        ////////////////////////
        //buff delay
        ///////////////////////
        if (drugData.buffDelay[pd.level] != null) {

            for (b in drugData.buffDelay[pd.level]!!) {
                val time = b.split(";")
                val buff = time[0].split(",")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.addPotionEffect(PotionEffect(
                            PotionEffectType.getByName(buff[0]),
                            buff[1].toInt(),
                            buff[2].toInt()
                    ))
                }, time[1].toLong())
            }
        }

        ////////////////////////
        //buff random delay
        ///////////////////////
        if (drugData.buffRandomDelay[pd.level] != null) {
            val time = drugData.buffRandomDelay[pd.level]!![Random()
                    .nextInt(drugData.buffRandomDelay[pd.level]!!.size - 1)].split(";")
            val buff = time[0].split(",")

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                player.addPotionEffect(PotionEffect(
                        PotionEffectType.getByName(buff[0]),
                        buff[1].toInt(),
                        buff[2].toInt()
                ))

            }, time[1].toLong())

        }

        ////////////////////////
        //particle
        ///////////////////////
        if (drugData.particle[pd.level] != null) {
            for (p in drugData.particle[pd.level]!!) {
                val par = p.split(",")
                player.world.spawnParticle(Particle.valueOf(par[0]), player.location, par[1].toInt())

            }
        }

        ////////////////////////
        //particle random
        ///////////////////////
        if (drugData.particleRandom[pd.level] != null) {

            val particle = drugData.particleRandom[pd.level]!![Random()
                    .nextInt(drugData.particleRandom[pd.level]!!.size - 1)].split(",")
            player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())

        }


        ////////////////////////
        //particle delay
        ///////////////////////
        if (drugData.particleDelay[pd.level] != null) {
            for (p in drugData.particleDelay[pd.level]!!) {
                val times = p.split(";")

                val par = times[0].split(",")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.world.spawnParticle(Particle.valueOf(par[0]), player.location, par[1].toInt())
                }, times[1].toLong())

            }
        }

        ////////////////////////
        //particle random delay
        ///////////////////////
        if (drugData.particleRandomDelay[pd.level] != null) {
            val time = drugData.particleRandomDelay[pd.level]!![Random()
                    .nextInt(drugData.particleRandomDelay[pd.level]!!.size - 1)].split(";")
            val particle = time[0].split(",")

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())
            }, time[1].toLong())

        }

        ////////////////////////
        //sound
        ///////////////////////
        if (drugData.sound[pd.level] != null) {
            for (s in drugData.sound[pd.level]!!) {
                val sound = s.split(",")
                player.world.playSound(player.location, Sound.valueOf(sound[0]), sound[1].toFloat(), sound[2].toFloat())
            }
        }

        ////////////////////////
        //sound random
        ///////////////////////
        if (drugData.soundRandom[pd.level] != null) {

            val sound = drugData.soundRandom[pd.level]!![Random()
                    .nextInt(drugData.soundRandom[pd.level]!!.size - 1)].split(",")

            player.world.playSound(player.location, Sound.valueOf(sound[0]), sound[1].toFloat(), sound[2].toFloat())
        }

        ////////////////////////
        //sound delay
        ///////////////////////
        if (drugData.soundDelay[pd.level] != null) {

            for (s in drugData.soundDelay[pd.level]!!) {
                val time = s.split(";")
                val sound = time[0].split(",")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.world.playSound(player.location, Sound.valueOf(sound[0]), sound[1].toFloat()
                            , sound[2].toFloat())
                }, time[1].toLong())
            }
        }

        ////////////////////////
        //sound random delay
        ///////////////////////
        if (drugData.soundRandomDelay[pd.level] != null) {
            val time = drugData.soundRandomDelay[pd.level]!![Random()
                    .nextInt(drugData.soundRandomDelay[pd.level]!!.size - 1)].split(";")
            val sound = time[0].split(",")

            player.world.playSound(player.location, Sound.valueOf(sound[0]), sound[1].toFloat(), sound[2].toFloat())
        }

        /////////////////////////
        //周囲に迷惑
        ///////////////////////
        if (drugData.nearPlayer != null && plugin.size(drugData.nearPlayer!!, pd)) {
            val data = drugData.nearPlayer!![pd.level].split(";")
            val list = getNearByPlayers(player, data[0].toInt())
            for (p in list) {
                plugin.mdpfunc.runFunc(p, data[1])
                Bukkit.getLogger().info(p.name + "func")
            }
        }

        ////////////////////////
        // type 0 only
        ///////////////////////
        if (drugData.type == 0) {

            //レベルアップ
            //dependenceLevelを5にした場合、level5まで上がる
            //0も含めるので、6段階となる
            if (pd.level < drugData.dependenceLevel && pd.usedLevel >= drugData.nextLevelCount!![pd.level]) {

                ////////////////////////
                //command
                ///////////////////////
                if (drugData.commandLvUp[pd.level] != null) {

                    for (c in drugData.commandLvUp[pd.level]!!) {
                        val cmd = plugin.repStr(c, player, pd, drugData)

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                    }
                }

                ////////////////////////
                //command random
                ///////////////////////
                if (drugData.commandRandomLvUp[pd.level] != null) {

                    val cmd = plugin.repStr(drugData.commandRandomLvUp[pd.level]!![Random().nextInt(
                            drugData.commandRandomLvUp[pd.level]!!.size
                    )], player, pd, drugData)

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                }
                ////////////////////////
                // Func
                ///////////////////////
                if (drugData.func != null) {
                    for (funcname in drugData.func!!) {
                        plugin.mdpfunc.runFunc(player, funcname)
                    }
                }
                ////////////////////////
                // FuncDelay
                ///////////////////////
                if (drugData.funcDelayLvUp != null) {
                    for (funcname in drugData.funcDelayLvUp!!) {
                        val times = funcname.split(";")
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                            plugin.mdpfunc.runFunc(player, times[0])
                        }, times[1].toLong())
                    }
                }
                ////////////////////////
                // FuncRandom
                ///////////////////////
                if (drugData.funcRandomLvUp != null && drugData.funcRandomLvUp!!.size > 0) {
                    val rnd = SecureRandom()
                    val r = rnd.nextInt(drugData.funcRandomLvUp!!.size)
                    val s = drugData.funcRandomLvUp!![r]
                    plugin.mdpfunc.runFunc(player, s)
                }
                ////////////////////////
                // FuncRandomDelay
                ///////////////////////
                if (drugData.funcRandomDelayLvUp != null && drugData.funcRandomDelayLvUp!!.size > 0) {
                    val rnd = SecureRandom()
                    val r = rnd.nextInt(drugData.funcRandomDelayLvUp!!.size)
                    val funcname = drugData.funcRandomDelayLvUp!![r]
                    val times = funcname.split(";")
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                        plugin.mdpfunc.runFunc(player, times[0])
                    }, times[1].toLong())
                }

                //usedLevel reset レベルアップ
                pd.usedLevel = 0
                pd.level++
            }

            //最終利用時刻更新
            pd.time = Date()
            pd.isDependence = true

        }

        ////////////////////////
        // type 1 only
        ///////////////////////
        if (drugData.type == 1) {

            val key2 = player.name + drugData.weakDrug
            val pd2 = db.get(key2)
            val drug2 = config.get(drug)

            if (pd2.level == 0 && pd2.usedLevel == 0) {
                player.sendMessage("§aあれ？.....解毒薬を飲む必要ってあるのかな...")
                return
            }

            if (pd.usedLevel >= drugData.medicineCount) {
                pd.usedLevel = 0
                pd2.usedLevel -= drugData.weakCount

                //countがゼロになったら
                if (pd2.usedLevel <= 0) {
                    pd2.level -= 1
                    pd2.usedLevel += drug2.nextLevelCount!![pd.level]
                }
            }

            /////////
            //禁断症状
            if (pd2.isDependence && drugData.stopTask) {
                pd.symptomsTotal = 0
                pd2.isDependence = false

                //levelがぜろになったらタスクを走らせない
                if (pd2.level > 0) {
                    pd2.isDependence = true

                }
            }

            db.playerMap[key2] = pd2

        }

        ////////////////////////
        // type 2 only
        ///////////////////////
        if (drugData.type == 2) {
            val key2 = player.name + drugData.weakDrug
            val pd2 = db.get(key2)

            if (pd2.level == 0) {
                player.sendMessage("§aあれ？.....解毒薬を飲む必要ってあるのかな...")
                return
            }

            pd2.usedLevel = 0
            pd2.level = 0
            if (pd2.isDependence) {
                pd2.isDependence = false
            }

            db.playerMap[key2] = pd2
        }

        //remove 1 item
        item.amount = item.amount - 1
        player.inventory.itemInMainHand = item

        //usedLevel increment
        pd.usedLevel++  //levelごと
        pd.usedCount++  //total
        stat.count ++

        db.drugStat[drug] = stat

        ////////////////
        //アイテムを変える
        ////////////////
        if (drugData.isChange) {
            player.inventory.addItem(plugin.drugItemStack[drugData.changeItem])
        }

        //player data save memory
        db.playerMap[key] = pd

        ////////////////////////
        // Func
        ///////////////////////
        if (drugData.func != null) {
            for (funcname in drugData.func!!) {
                plugin.mdpfunc.runFunc(player, funcname)
            }
        }
        ////////////////////////
        // FuncDelay
        ///////////////////////
        if (drugData.funcDelay != null) {
            for (funcname in drugData.funcDelay!!) {
                val times = funcname.split(";")
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    plugin.mdpfunc.runFunc(player, times[0])
                }, times[1].toLong())
            }
        }
        ////////////////////////
        // FuncRandom
        ///////////////////////
        if (drugData.funcRandom != null && drugData.funcRandom!!.size > 0) {
            val rnd = SecureRandom()
            val r = rnd.nextInt(drugData.funcRandom!!.size)
            val s = drugData.funcRandom!![r]
            plugin.mdpfunc.runFunc(player, s)
        }
        ////////////////////////
        // FuncRandomDelay
        ///////////////////////
        if (drugData.funcRandomDelay != null && drugData.funcRandomDelay!!.size > 0) {
            val rnd = SecureRandom()
            val r = rnd.nextInt(drugData.funcRandomDelay!!.size)
            val funcname = drugData.funcRandomDelay!![r]
            val times = funcname.split(";")
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                plugin.mdpfunc.runFunc(player, times[0])
            }, times[1].toLong())
        }

    }


    ///////////////////////////
    //周囲のプレイヤーを検知
    private fun getNearByPlayers(centerPlayer:Player, distance:Int):ArrayList<Player>{
        val ds = distance*distance
        val players = ArrayList<Player>()
        val loc = centerPlayer.location
        val world = centerPlayer.world
        for(player in Bukkit.getOnlinePlayers()){
            if (player.world == world && player.location.distanceSquared(loc) < ds){
                players.add(player)
            }
        }
        players.remove(centerPlayer)
        return players
    }
}