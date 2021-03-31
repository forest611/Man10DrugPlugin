package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import red.man10.man10drugplugin.Database.executeQueue
import red.man10.man10drugplugin.Database.load
import red.man10.man10drugplugin.Database.save
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.disableWorld
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugData
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugName
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.isReload
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.plugin
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.pluginEnable
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.random
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.rep
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.useMilk
import java.util.*

object Event:Listener{

    @EventHandler
    fun useDrugEvent(e:PlayerInteractEvent){


        if (e.action!=Action.RIGHT_CLICK_AIR && e.action!=Action.RIGHT_CLICK_BLOCK)return

        val p = e.player

        if (isReload || !pluginEnable){
            p.sendMessage("§e§l今は使う気分ではないようだ...")
            return
        }

        if (disableWorld.contains(p.world.name))return

        val item = e.item?:return

//        if (item.type == Material.POTION)return

        val meta = item.itemMeta?:return

        val drug =  meta.persistentDataContainer[NamespacedKey(plugin,"name"), PersistentDataType.STRING]?:return

        if (!drugName.contains(drug))return

        e.isCancelled = true

        val data = drugData[drug]!!

        if (data.disableWorld.contains(p.world.name))return

        val pd = Database.get(p, drug)!!

        //cooldown
        val difference = (Date().time - pd.finalUseTime.time)/1000
        if (data.cooldown > difference && data.cooldown != 0L)return

        //buffなどの処理
        useDrug(p,drug,pd,data)

        //remove an item
        if (data.parameter[pd.level].isRemoveItem) {
            item.amount = item.amount - 1
        }
//        //remove prob
//        if (!data.isRemoveItem && data.crashChance.size >pd.level){
//            if(data.crashChance[pd.level] !=0.0 && Math.random()<data.crashChance[pd.level]){
//                item.amount = item.amount -1
//                p.sendMessage(data.crashMsg)
//            }
//        }

    }

    /////////////////////////////
    //ミルク対策
    //////////////////////////////
    @EventHandler
    fun milkEvent(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET && !useMilk && !event.player.hasPermission("man10drug.milk")) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    fun loginEvent(e : PlayerJoinEvent){
        Thread {
            Thread.sleep(5000)
            load(e.player)
        }.start()
    }

    @EventHandler
    fun logoutEvent(e:PlayerQuitEvent){
        if (isReload)return
        save(e.player)
    }

    /////////////////////////////////
    //ドラッグ使用時の処理
    /////////////////////////////////
    fun useDrug(p: Player, dataName:String, pd:Database.PlayerData, drug:Config.Drug){

        val parameter = drug.parameter[pd.level]

        p.sendMessage(rep(parameter.msg,p,dataName))

        //add logs
        executeQueue.add("INSERT INTO `log` " +
                "(`uuid`, `player`, `drug_name`,`date`)" +
                " VALUES ('${p.uniqueId}', '${p.name}', '$dataName',now());")

        if (parameter.isRemoveBuff){
            for (e in p.activePotionEffects){
                p.removePotionEffect(e.type)
            }
        }

        for (b in parameter.buff){
            p.addPotionEffect(b)
        }

        p.addPotionEffect(parameter.buffRandom[Random().nextInt(parameter.buffRandom.size-1)])

        for (s in parameter.sound){
            p.location.world.playSound(p.location, s.sound, s.volume,s.pitch)
        }

        val s = parameter.soundRandom[Random().nextInt(parameter.soundRandom.size -1)]
        p.location.world.playSound(p.location, s.sound, s.volume,s.pitch)


        for (par in parameter.particle){
            p.location.world.spawnParticle(par.particle,p.location,par.size)
        }

        val par = parameter.particleRandom[Random().nextInt(parameter.particleRandom.size-1)]
        p.location.world.spawnParticle(par.particle,p.location,par.size)

        for (c in parameter.cmd){

            if (p.isOp){
                p.performCommand(rep(c,p,dataName))
                continue
            }
            p.isOp = true
            p.performCommand(rep(c,p,dataName))
            p.isOp = false
        }

        if (p.isOp){
            p.performCommand(rep(random(parameter.cmdRandom),p,dataName))
        }else{
            p.isOp = true
            p.performCommand(rep(random(parameter.cmdRandom),p,dataName))
            p.isOp = false
        }

        for (c in parameter.serverCmd){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),rep(c,p,dataName))
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),rep(random(parameter.serverCmdRandom),p,dataName))

        MDPFunction.runFunc(parameter.func,p)

//        if (data.nearPlayer.size>pd.level){
//            val s = data.nearPlayer[pd.level].split(";")
//
//            for (pla in getNearPlayer(p,s[1].toInt())){
//                MDPFunction.runFunc(s[0],pla)
//            }
//        }

        pd.usedCount ++ //使用回数更新
        pd.finalUseTime = Date()  //最終使用時刻更新

        if (drug.type == 0){

            pd.isDepend = true
            pd.totalSymptoms = 0

            //確率で依存レベルアップ
            if (pd.level < drug.level &&Math.random()<parameter.dependLvUp){
                pd.level ++
            }
        }

//        if (data.type == 1){
//            val pd2 = Database.get(p,data.weakDrug)!!
//
//            if (pd2.level == 0 && pd2.usedCount == 0){
//                p.sendMessage("§a§lどうやら使う必要はなかったようだ...")
//                return
//            }
//            if (Math.random()<data.weakProb[pd2.level]){
//                pd2.level --
//                if (pd2.level == -1){
//                    pd2.level = 0
//                    pd2.usedCount = 0
//                    pd2.isDepend = false
//                    pd2.totalSymptoms = 0
//                    p.sendMessage("§a§l症状が完全に治ったようだ")
//                }
//                Database.set(p,data.weakDrug,pd2)
//            }
//        }


        //save player data
        Database.set(p,dataName,pd)

    }

    //周囲のプレイヤーを検知
//    fun getNearPlayer(centerPlayer:Player,distance:Int):MutableList<Player>{
//        val ds = distance * distance
//        val players = mutableListOf<Player>()
//        val loc = centerPlayer.location
//        val world =centerPlayer.world
//        for (p in Bukkit.getOnlinePlayers()){
//            if (p == centerPlayer)continue
//            if (p.world != world)continue
//            if (p.location.distanceSquared(loc)>=ds)continue
//
//            if (defenseCheck(p))continue //弾けた場合
//            players.add(p)
//        }
//          return players
//
//    }
//
//    //周囲からの影響を受ける確率
//    private fun defenseCheck(p:Player):Boolean{
//        val helmet = p.inventory.helmet?:return false
//
//        val meta = helmet.itemMeta?:return false
//
//        if (meta.persistentDataContainer.isEmpty)return false
//
//        val dataName = meta.persistentDataContainer[NamespacedKey(plugin,"name"), PersistentDataType.STRING]?:return false
//
//        if (drugName.contains(dataName))return false
//
//        val data = drugData[dataName]!!
//
//        if (data.type !=2)return false
//        if (Math.random()<data.defenseProb)return true
//
//        return false
//    }


}