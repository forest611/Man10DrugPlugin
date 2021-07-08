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
import org.bukkit.persistence.PersistentDataType
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.disableWorld
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugData
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugName
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.isReload
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.plugin
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.pluginEnable
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.rep
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.useMilk
import java.util.*
import kotlin.collections.HashMap

object Event:Listener{

    private val random = Random()

    private val coolDown = HashMap<Pair<UUID,String>,Long>()

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

        //cooldown
        val difference = (Date().time - (coolDown[Pair(p.uniqueId,drug)]?:0))/1000
        if (data.cooldown > difference && data.cooldown != 0L)return

        //buffなどの処理
        useDrug(p,drug,data)

        //remove an item
        if (data.parameter.isRemoveItem) {
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

    /////////////////////////////////
    //ドラッグ使用時の処理
    /////////////////////////////////
    fun useDrug(p: Player, dataName:String, drug:Config.Drug){

        val parameter = drug.parameter

        p.sendMessage(rep(parameter.msg,p,dataName))

        Bukkit.getLogger().info("[DRUG]${p.name} used ${drug.displayName}")

        if (parameter.isRemoveBuff){
            for (e in p.activePotionEffects){
                p.removePotionEffect(e.type)
            }
        }

        for (b in parameter.buff){
            p.addPotionEffect(b)
        }

        for (b in parameter.buffRandom){
            if (b.second<= random.nextDouble())continue
            p.addPotionEffect(b.first)
        }

        for (s in parameter.sound){
            p.location.world.playSound(p.location, s.sound, s.volume,s.pitch)
        }

        for (s in parameter.soundRandom){
            if (s.second<= random.nextDouble())continue

            p.location.world.playSound(p.location, s.first.sound, s.first.volume,s.first.pitch)
        }

        for (par in parameter.particle){
            p.location.world.spawnParticle(par.particle,p.location,par.size)
        }

        for (par in parameter.particleRandom){
            if (par.second<= random.nextDouble())continue
            p.location.world.spawnParticle(par.first.particle,p.location,par.first.size)
        }

        for (c in parameter.cmd){

            if (p.isOp){
                p.performCommand(rep(c,p,dataName))
                continue
            }
            p.isOp = true
            p.performCommand(rep(c,p,dataName))
            p.isOp = false
        }

        for (c in parameter.cmdRandom){
            if (c.second<= random.nextDouble())continue

            if (p.isOp){
                p.performCommand(rep(c.first,p,dataName))
                continue
            }
            p.isOp = true
            p.performCommand(rep(c.first,p,dataName))
            p.isOp = false
        }

        for (c in parameter.serverCmd){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),rep(c,p,dataName))
        }

        for (c in parameter.serverCmdRandom){
            if (c.second<= random.nextDouble())continue
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),rep(c.first,p,dataName))
        }

        MDPFunction.runFunc(parameter.func,p)

//        if (data.nearPlayer.size>pd.level){
//            val s = data.nearPlayer[pd.level].split(";")
//
//            for (pla in getNearPlayer(p,s[1].toInt())){
//                MDPFunction.runFunc(s[0],pla)
//            }
//        }

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