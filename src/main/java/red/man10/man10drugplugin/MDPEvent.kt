package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.security.SecureRandom
import java.util.*

class MDPEvent(val plugin: Man10DrugPlugin, val mysql :MySQLManager,val db:MDPDataBase,val config:MDPConfig) : Listener {

    public var cooldownMap : MutableList<String> = ArrayList()

    @EventHandler
    fun joinEvent(event:PlayerJoinEvent){
        object : BukkitRunnable() {
            override fun run() {db.loadDataBase(event.player)}
        }.run()

    }

    @EventHandler
    fun leftEvent(event:PlayerQuitEvent){
        object : BukkitRunnable() {
            override fun run() {db.saveDataBase(event.player,true)}
        }.run()
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

            useDrug(event.player,item,event)
        }
    }

//    @EventHandler
//    fun foodEvent(event: PlayerItemConsumeEvent){
//
//        //can milk
//        if (event.item.type == Material.MILK_BUCKET&&!plugin.canMilk){
//            event.isCancelled = true
//            return
//        }
//        val i = event.player.inventory.itemInMainHand ?: return
//
//        if (i.itemMeta == null)return
//        if (i.itemMeta.lore == null)
//        if (i.itemMeta.lore.isEmpty())return
//
//        useDrug(event.player,event.item)
//
//    }

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

        Bukkit.getLogger().info("msg1")

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

            if (pd.count == 0 && pd.level == 0) {
                continue
            }

            //指定レベルが無い、もしくは壊さない場合
            if (drug.crashChance!![pd.level] == "false") {
                continue
            }

            Bukkit.getLogger().info("msg2")


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

            Bukkit.getLogger().info("msg3")
            Bukkit.getLogger().info(msg.toString())

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
    fun useDrug(player: Player,item: ItemStack,event:PlayerInteractEvent){

        object : BukkitRunnable() {
            override fun run() {

                val drug = item.itemMeta.lore[item.itemMeta.lore.size -1].replace("§","")

                if(plugin.drugName.indexOf(drug) == -1){
                    return
                }

                if(cooldownMap.contains(player.uniqueId.toString()+" "+drug)){
                    return
                }

                if(plugin.stop||!db.canConnect){
                    event.player.sendMessage("§e今は使う気分ではないようだ")
                    return
                }

                val key = player.name + drug

                val pd = db.get(key)
                val drugData = config.get(drug)

                ////////////////////////
                //message
                ///////////////////////
                if (drugData.useMsg != null && plugin.size(drugData.useMsg!!,pd)){
                    player.sendMessage(plugin.repStr(drugData.useMsg!![pd.level],player,pd))
                }

                //Delay message
                if (drugData.useMsgDelay != null && plugin.size(drugData.useMsgDelay!!,pd)){
                    val times = drugData.useMsgDelay!![pd.level].split(";")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.sendMessage(plugin.repStr(times[0],player,pd))
                    },times[1].toLong())

                }

                ////////////////////////
                //cooldown
                ///////////////////////

                cooldownMap.add(player.uniqueId.toString()+" "+drug)

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{

                    cooldownMap.remove(player.uniqueId.toString()+" "+drug)

                },drugData.cooldown)

                ////////////////////////
                //command
                ///////////////////////
                if (drugData.command[pd.level] != null){

                    for (i in 0 until drugData.command[pd.level]!!.size){

                        val cmd = plugin.repStr(drugData.command[pd.level]!![i],player,pd)

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                    }

                }

                ////////////////////////
                //random command
                ///////////////////////
                if (drugData.commandRandom[pd.level] != null){

                    val cmd = plugin.repStr(drugData.commandRandom[pd.level]!![Random().nextInt(
                            drugData.commandRandom[pd.level]!!.size
                    )],player,pd)

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                }

                ////////////////////////
                //command delay
                ///////////////////////
                if (drugData.commandDelay[pd.level] != null){


                    for (i in 0 until drugData.commandDelay[pd.level]!!.size){

                        val command = drugData.commandDelay[pd.level]!![i].split(";")

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{

                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.repStr(command[0],player,pd))

                        },command[1].toLong())
                    }
                }

                ////////////////////////
                //command random delay
                ///////////////////////
                if (drugData.commandRandomDelay[pd.level] != null){

                    val command = drugData.commandRandomDelay[pd.level]!![
                            drugData.commandRandomDelay[pd.level]!!.size - 1].split(";")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.repStr(command[0],player,pd))

                    },command[1].toLong())
                }

                ////////////////////////
                //buff
                ///////////////////////
                if (drugData.buff[pd.level] != null){

                    for (i in 0 until drugData.buff[pd.level]!!.size){
                        val buff = drugData.buff[pd.level]!![i].split(",")

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
                if (drugData.buffRandom[pd.level] != null){

                    val buff = drugData.buffRandom[pd.level]!![Random()
                            .nextInt(drugData.buffRandom[pd.level]!!.size -1)].split(",")

                    player.addPotionEffect(PotionEffect(
                            PotionEffectType.getByName(buff[0]),
                            buff[1].toInt(),
                            buff[2].toInt()
                    ))
                }

                ////////////////////////
                //buff delay
                ///////////////////////
                if (drugData.buffDelay[pd.level] != null){

                    for (i in 0 until drugData.buffDelay[pd.level]!!.size){

                        val time = drugData.buffDelay[pd.level]!![i].split(";")
                        val buff = time[0].split(",")

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                            player.addPotionEffect(PotionEffect(
                                    PotionEffectType.getByName(buff[0]),
                                    buff[1].toInt(),
                                    buff[2].toInt()
                            ))
                        },time[1].toLong())
                    }
                }

                ////////////////////////
                //buff random delay
                ///////////////////////
                if (drugData.buffRandomDelay[pd.level] != null){
                    val time = drugData.buffRandomDelay[pd.level]!![Random()
                            .nextInt(drugData.buffRandomDelay[pd.level]!!.size -1)].split(";")
                    val buff = time[0].split(",")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.addPotionEffect(PotionEffect(
                                PotionEffectType.getByName(buff[0]),
                                buff[1].toInt(),
                                buff[2].toInt()
                        ))

                    },time[1].toLong())

                }

                ////////////////////////
                //particle
                ///////////////////////
                if(drugData.particle[pd.level] != null){
                    for(i in 0 until drugData.particle[pd.level]!!.size){
                        val particle = drugData.particle[pd.level]!![i].split(",")
                        player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())

                    }


                }

                ////////////////////////
                //particle random
                ///////////////////////
                if (drugData.particleRandom[pd.level] != null){

                    val particle = drugData.particleRandom[pd.level]!![Random()
                            .nextInt(drugData.particleRandom[pd.level]!!.size -1)].split(",")
                    player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())

                }


                ////////////////////////
                //particle delay
                ///////////////////////
                if (drugData.particleDelay[pd.level] != null){
                    for(i in 0 until drugData.particleDelay[pd.level]!!.size){
                        val times = drugData.particleDelay[pd.level]!![i].split(";")

                        val particle = times[0].split(",")

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                            player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())
                        },times[1].toLong())

                    }
                }

                ////////////////////////
                //particle random delay
                ///////////////////////
                if(drugData.particleRandomDelay[pd.level] != null){
                    val time = drugData.particleRandomDelay[pd.level]!![Random()
                            .nextInt(drugData.particleRandomDelay[pd.level]!!.size -1)].split(";")
                    val particle = time[0].split(",")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())
                    },time[1].toLong())

                }

                ////////////////////////
                //sound
                ///////////////////////
                if (drugData.sound[pd.level] != null){

                    for (i in 0 until drugData.sound[pd.level]!!.size){
                        val sound = drugData.sound[pd.level]!![i].split(",")

                        player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())
                    }
                }

                ////////////////////////
                //sound random
                ///////////////////////
                if (drugData.soundRandom[pd.level] != null){

                    val sound = drugData.soundRandom[pd.level]!![Random()
                            .nextInt(drugData.soundRandom[pd.level]!!.size -1)].split(",")

                    player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())
                }

                ////////////////////////
                //sound delay
                ///////////////////////
                if (drugData.soundDelay[pd.level] != null){

                    for (i in 0 until drugData.soundDelay[pd.level]!!.size){

                        val time = drugData.soundDelay[pd.level]!![i].split(";")
                        val sound = time[0].split(",")

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                            player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())
                        },time[1].toLong())
                    }
                }

                ////////////////////////
                //sound random delay
                ///////////////////////
                if (drugData.soundRandomDelay[pd.level] != null){
                    val time = drugData.soundRandomDelay[pd.level]!![Random()
                            .nextInt(drugData.soundRandomDelay[pd.level]!!.size -1)].split(";")
                    val sound = time[0].split(",")

                    player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())
                }

                ////////////////////////
                // type 0 only
                ///////////////////////
                if (drugData.type == 0){

                    //レベルアップ
                    if (pd.level<drugData.dependenceLevel&&pd.count >= drugData.nextLevelCount!![pd.level]){

                        ////////////////////////
                        //command
                        ///////////////////////
                        if (drugData.command[pd.level] != null){

                            for (i in 0 until drugData.commandLvUp[pd.level]!!.size){

                                val cmd = plugin.repStr(drugData.commandLvUp[pd.level]!![i],player,pd)

                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                            }

                        }

                        ////////////////////////
                        //command random
                        ///////////////////////
                        if (drugData.commandRandomLvUp[pd.level] != null){

                            val cmd = plugin.repStr(drugData.commandRandomLvUp[pd.level]!![Random().nextInt(
                                    drugData.commandRandomLvUp[pd.level]!!.size
                            )],player,pd)

                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                        }


                        ////////////////////////
                        // FuncDelay
                        ///////////////////////
                        if(drugData.funcDelayLvUp!=null){
                            for(funcname in drugData.funcDelayLvUp!!){
                                val times = funcname.split(";")
                                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                                    plugin.mdpfunc.runFunc(player,times[0])
                                },times[1].toLong())
                            }
                        }
                        ////////////////////////
                        // FuncRandom
                        ///////////////////////
                        if(drugData.funcRandomLvUp!=null&&drugData.funcRandomLvUp!!.size>0){
                            val rnd = SecureRandom()
                            val r = rnd.nextInt(drugData.funcRandomLvUp!!.size)
                            val s = drugData.funcRandomLvUp!![r]
                            plugin.mdpfunc.runFunc(player,s)
                        }
                        ////////////////////////
                        // FuncRandomDelay
                        ///////////////////////
                        if(drugData.funcRandomDelayLvUp!=null&&drugData.funcRandomDelayLvUp!!.size>0){
                            val rnd = SecureRandom()
                            val r = rnd.nextInt(drugData.funcRandomDelayLvUp!!.size)
                            val funcname = drugData.funcRandomDelayLvUp!![r]
                            val times = funcname.split(";")
                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                                plugin.mdpfunc.runFunc(player,times[0])
                            },times[1].toLong())
                        }

                        //count reset レベルアップ
                        pd.count = 0
                        pd.level ++
                    }

                    ////////
                    //依存部分
                    if (drugData.isDependence){

                        //一時タスク終了
                        if (pd.isDependence)
                        {
                            Bukkit.getScheduler().cancelTask(pd.taskId)
                            pd.times = 0
                        }
                        pd.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(player,drugData,pd,plugin)
                        ,drugData.symptomsTime!![pd.level],drugData.symptomsNextTime!![1])

                        pd.isDependence = true
                    }
                }

                ////////////////////////
                // type 1 only
                ///////////////////////
                if (drugData.type == 1){

                    val key2 = player.name+drugData.weakDrug
                    val pd2 = db.get(key2)
                    val drug2 = config.get(drug)

                    if(pd2.level == 0){
                        player.sendMessage("§aあれ？.....解毒薬を飲む必要ってあるのかな...")
                        return
                    }

                    if (pd.count >= drugData.medicineCount){
                        pd.count = 0
                        pd2.count -= drugData.weakCount

                        //countがゼロになったら
                        if (pd2.count <=0){
                            pd2.level -= 1
                            pd2.count +=  drug2.nextLevelCount!![pd.level]
                        }
                    }

                    /////////
                    //禁断症状
                    if (pd2.isDependence&&drugData.stopTask){
                        Bukkit.getScheduler().cancelTask(pd2.taskId)
                        pd.times = 0
                        pd2.isDependence = false

                        //levelがぜろになったらタスクを走らせない
                        if (pd2.level > 0){
                            pd2.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(player,drug2,pd2,plugin)
                                    ,drug2.symptomsTime!![pd.level],drug2.symptomsNextTime!![1])
                            pd2.isDependence = true

                        }
                    }

                    db.playerMap[key2] = pd2

                }

                ////////////////////////
                // type 2 only
                ///////////////////////
                if (drugData.type == 2){
                    val key2 = player.name+drugData.weakDrug
                    val pd2 = db.get(key2)

                    if(pd2.level == 0){
                        player.sendMessage("§aあれ？.....解毒薬を飲む必要ってあるのかな...")
                        return
                    }

                    pd2.count = 0
                    pd2.level = 0
                    if(pd2.isDependence){
                        pd2.isDependence =false
                        Bukkit.getScheduler().cancelTask(pd2.taskId)
                    }

                    db.playerMap[key2] = pd2
                }

                //remove 1 item
                item.amount = item.amount - 1
                player.inventory.itemInMainHand = item

                pd.count ++

                ////////////////
                //アイテムを変える
                ////////////////
                if (drugData.isChange){ player.inventory.addItem(plugin.drugItemStack[drugData.changeItem]) }

                db.addLog(player,drug)

                //player data save memory
                db.playerMap[key] = pd

                ////////////////////////
                // Func
                ///////////////////////
                if(drugData.func!=null){
                    for(funcname in drugData.func!!){
                        plugin.mdpfunc.runFunc(player,funcname)
                    }
                }
                ////////////////////////
                // FuncDelay
                ///////////////////////
                if(drugData.funcDelay!=null){
                    for(funcname in drugData.funcDelay!!){
                        val times = funcname.split(";")
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                            plugin.mdpfunc.runFunc(player,times[0])
                        },times[1].toLong())
                    }
                }
                ////////////////////////
                // FuncRandom
                ///////////////////////
                if(drugData.funcRandom!=null&&drugData.funcRandom!!.size>0){
                    val rnd = SecureRandom()
                    val r = rnd.nextInt(drugData.funcRandom!!.size)
                    val s = drugData.funcRandom!![r]
                    plugin.mdpfunc.runFunc(player,s)
                }
                ////////////////////////
                // FuncRandomDelay
                ///////////////////////
                if(drugData.funcRandomDelay!=null&&drugData.funcRandomDelay!!.size>0){
                    val rnd = SecureRandom()
                    val r = rnd.nextInt(drugData.funcRandomDelay!!.size)
                    val funcname = drugData.funcRandomDelay!![r]
                    val times = funcname.split(";")
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        plugin.mdpfunc.runFunc(player,times[0])
                    },times[1].toLong())
                }
            }
        }.run()

    }

}