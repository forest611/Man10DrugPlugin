package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.lang.Exception
import java.util.*

class Events(private val plugin: Man10DrugPlugin):Listener{

    @EventHandler
    fun useDrugEvent(e:PlayerInteractEvent){
        if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK){
            val item = e.item?:return

            val p = e.player

            if (item.itemMeta == null) return
            if (!CraftItemStack.asNMSCopy(item).hasTag())return
            if (plugin.disableWorld.indexOf(p.world.name) != -1){ return }

            if (plugin.isReload || !plugin.pluginEnable){
                p.sendMessage("§e§l今は使う気分ではないようだ...")
                return
            }

            var dataName = ""

            //NBTTagからドラッグを識別
            try {
               dataName = CraftItemStack.asNMSCopy(item).tag!!.getString("name")
            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
                return
            }
            Bukkit.getLogger().info(dataName)

            if (plugin.drugName.indexOf(dataName) == -1)return

            useDrug(p,dataName)
        }
    }

    /////////////////////////////
    //ミルク対策
    //////////////////////////////
    @EventHandler
    fun milkEvent(event: PlayerItemConsumeEvent) {
        if (event.item.type == Material.MILK_BUCKET && !plugin.useMilk && !event.player.hasPermission("man10drug.milk")) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    fun loginEvent(e : PlayerJoinEvent){
        Thread(Runnable {
            Thread.sleep(5000)
            plugin.db.loginDB(e.player)
        }).start()
    }

    @EventHandler
    fun logoutEvent(e:PlayerQuitEvent){
        if (plugin.isReload)return
        plugin.db.logoutDB(e.player)
    }

    /////////////////////////////////
    //ドラッグ使用時の処理
    /////////////////////////////////
    fun useDrug(p: Player, dataName:String){

        val data = plugin.drugData[dataName]!!

        if (data.disableWorld.indexOf(p.world.name) != -1)return

        val pd = plugin.db.playerData[Pair(p,dataName)]!!

        //cooldown
        if (data.cooldown > (Date().time - pd.finalUseTime))return


        /////////////
        //remove an item
        if (data.isRemoveItem && p.inventory.itemInMainHand !=null){
            val item = p.inventory.itemInMainHand
            item.amount = item.amount -1
            p.inventory.itemInMainHand = item
        }

        p.sendMessage(data.useMsg[pd.level])

        //add logs
        plugin.db.executeQueue.add("INSERT INTO `log` " +
                "(`uuid`, `player`, `drug_name`,`date`)" +
                " VALUES ('${p.uniqueId}', '${p.name}', '$dataName',now());")



        pd.usedCount ++
        pd.finalUseTime = Date().time

        if (data.type == 0){

            pd.isDepend = true
            pd.totalSymptoms = 0

            //確率で依存レベルアップ
            if (pd.level < data.dependLevel &&Math.random()<data.dependLvUp[pd.level]){
                pd.level ++
                Bukkit.getLogger().info("level up")
            }
        }

        if (data.type == 1){
            val pd2 = plugin.db.playerData[Pair(p,data.weakDrug)]!!

            if (pd2.level == 0 && pd2.usedCount == 0){
                p.sendMessage("§a§lどうやら使う必要はなかったようだ...")
                return
            }
            if (Math.random()<data.weakProb[pd2.level]){
                pd2.level --
                if (pd2.level == -1){
                    pd2.level = 0
//                    pd2.usedCount = 0
                    pd2.isDepend = false
                    pd2.totalSymptoms = 0
                    p.sendMessage("§a§l症状が完全に治ったようだ")
                }
                plugin.db.playerData[Pair(p,data.weakDrug)] = pd2
            }
        }


        //save player data
        plugin.db.playerData[Pair(p,dataName)] = pd

    }

    //周囲のプレイヤーを検知
    fun getNearPlayer(centerPlayer:Player,distance:Int):MutableList<Player>{
        val ds = distance * distance
        val players = mutableListOf<Player>()
        val loc = centerPlayer.location
        val world =centerPlayer.world
        for (p in Bukkit.getOnlinePlayers()){
            if (p.world != world)continue
            if (p.location.distanceSquared(loc)>=ds)continue

            players.add(p)
        }
        return players

    }

    //周囲からの影響を受ける確率
    fun defenseCheck(p:Player):Boolean{
        val helmet = p.inventory.helmet?:return false

        if (!CraftItemStack.asNMSCopy(helmet).hasTag())return false

        val dataName = CraftItemStack.asNMSCopy(helmet).tag!!.getString("name")

        if (plugin.drugName.indexOf(dataName) == -1)return false

        val data = plugin.drugData[dataName]!!

        if (Math.random()<data.defenseProb)return true

        return false
    }


}