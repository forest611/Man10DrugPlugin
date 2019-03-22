package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class MDPEvent(val plugin: Man10DrugPlugin, val mysql :MySQLManager,val db:MDPDataBase,val config:MDPConfig) : Listener {



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


            val item = event.item ?: return

            if (item.itemMeta.lore == null)return
            if (item.itemMeta.lore.isEmpty())return
            if (item.type == Material.AIR)return

            event.isCancelled = true

            useDrug(event.player,item)
        }
    }

    @EventHandler
    fun foodEvent(event: PlayerItemConsumeEvent){

        //can milk
        if (event.item.type == Material.MILK_BUCKET&&!plugin.canMilk){
            event.isCancelled = true
            return
        }

        //lore check1
        if (event.item.itemMeta.lore.isEmpty())return

        useDrug(event.player,event.item)

    }

    ////////////////////////
    ///ドラッグ使用時
    ///pd = ドラッグを使用したプレイヤーのデータ
    ///drugData = プレイヤーが使用したドラッグのデータ
    fun useDrug(player: Player,item: ItemStack){

        object : BukkitRunnable() {
            override fun run() {

                val drug = item.itemMeta.lore[item.itemMeta.lore.size -1].replace("§","")

                if(plugin.drugName.indexOf(drug) == -1){
                    return
                }

                val key = player.name + drug

                val pd = db.get(key)
                val drugData = config.get(drug)


                ////////////////////////
                //command
                ///////////////////////
                if (drugData.command[pd.level] != null){

                    for (i in 0 until drugData.command[pd.level]!!.size){

                        val cmd = drugData.command[pd.level]!![i].replace("<player>",player.name)

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                    }

                }

                ////////////////////////
                //random command
                ///////////////////////
                if (drugData.commandRandom[pd.level] != null){

                    val cmd = drugData.commandRandom[pd.level]!![Random().nextInt(
                            drugData.commandRandom[pd.level]!!.size
                    )].replace("<player>",player.name)

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

                }

                ////////////////////////
                //command delay
                ///////////////////////
                if (drugData.commandDelay[pd.level] != null){


                    for (i in 0 until drugData.commandDelay[pd.level]!!.size){

                        val command = drugData.commandDelay[pd.level]!![i].split(";")

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{

                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command[0].replace("<player>",player.name))

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

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command[0].replace("<player>",player.name))

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

                    val buff = drugData.buff[pd.level]!![Random()
                            .nextInt(drugData.buff[pd.level]!!.size -1)].split(",")

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
                //buff delay random
                ///////////////////////
                if (drugData.buffRandomDelay[pd.level] != null){
                    val time = drugData.buffDelay[pd.level]!![Random()
                            .nextInt(drugData.buff[pd.level]!!.size -1)].split(";")
                    val buff = time[0].split(",")

                    player.addPotionEffect(PotionEffect(
                            PotionEffectType.getByName(buff[0]),
                            buff[1].toInt(),
                            buff[2].toInt()
                    ))
                }

                ////////////////////////
                //particle
                ///////////////////////
                if(drugData.particle != null && size(drugData.particle!!,pd)){
                    val particle = drugData.particle!![pd.level].split(",")

                    player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())

                }

                ////////////////////////
                //particle random
                ///////////////////////
                if(drugData.particleRandom != null && size(drugData.particleRandom!!,pd)) {

                    val particle = drugData.particleRandom!![Random()
                            .nextInt(drugData.particleRandom!!.size - 1)].split(",")

                    player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())
                }

                ////////////////////////
                //particle delay
                ///////////////////////
                if (drugData.particleDelay != null  && size(drugData.particleDelay!!,pd)){
                    val times = drugData.particleDelay!![pd.level].split(";")

                    val particle = times[0].split(",")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())
                    },times[1].toLong())

                }

                ////////////////////////
                //particle random delay
                ///////////////////////
                if(drugData.particleRandomDelay != null && size(drugData.particleRandomDelay!!,pd)){
                    val times = drugData.particleRandomDelay!![Random()
                            .nextInt(drugData.particleRandomDelay!!.size -1)].split(";")

                    val particle = times[0].split(",")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())
                    },times[1].toLong())

                }

                ////////////////////////
                //sound
                ///////////////////////
                if (drugData.sound != null && size(drugData.sound!!,pd)){
                    val sound = drugData.sound!![pd.level].split(",")

                    player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())

                }

                ////////////////////////
                //sound delay
                ///////////////////////
                if (drugData.soundDelay != null && size(drugData.soundDelay!!,pd)){
                    val times = drugData.soundDelay!![pd.level].split(";")

                    val sound = times[0].split(",")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())
                    },times[1].toLong())

                }

                ////////////////////////
                // type 0 only
                ///////////////////////
                if (drugData.type == 0){

                    pd.count ++

                    if (pd.count >= drugData.nextLevelCount){
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
                        pd.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(player,drugData,pd)
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

                    pd.count ++

                    if (pd.count >= drugData.medicineCount){
                        pd.count = 0
                        pd2.count -= drugData.weakCount

                        //countがゼロになったら
                        if (pd2.count <=0){
                            pd2.level -= 1
                            pd2.count +=  drug2.nextLevelCount
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
                            pd2.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,SymptomsTask(player,drug2,pd2)
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

                ////////////////////////
                //message
                ///////////////////////
                if (drugData.useMsg != null && size(drugData.useMsg!!,pd)){
                    player.sendMessage(drugData.useMsg!![pd.level])
                }

                //Delay message
                if (drugData.useMsgDelay != null && size(drugData.useMsgDelay!!,pd)){
                    val times = drugData.useMsgDelay!![pd.level].split(";")

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,{
                        player.sendMessage(times[0])
                    },times[1].toLong())

                }

                //remove 1 item
                item.amount = item.amount - 1
                player.inventory.itemInMainHand = item

                ////////////////
                //アイテムを変える
                if (drugData.isChange){ player.inventory.addItem(plugin.drugItemStack[drugData.changeItem]) }

                db.addLog(player,drug)

                //player data save memory
                db.playerMap[key] = pd

            }
        }.run()

    }

    fun size(list:MutableList<String>,pd:playerData):Boolean{
        if (list.size>pd.level){
            return true
        }
        return false
    }
}