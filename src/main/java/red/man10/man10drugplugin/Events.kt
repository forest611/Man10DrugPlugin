package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
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

            val dataName : String

            //NBTTagからドラッグを識別
            try {
               dataName = CraftItemStack.asNMSCopy(item).tag!!.getString("name")
            }catch (e:Exception){
                Bukkit.getLogger().info(e.message)
                return
            }

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

        if (data.type == 2)return //マスクなど
        if (data.disableWorld.indexOf(p.world.name) != -1)return

        val pd = plugin.db.playerData[Pair(p,dataName)]?:return

        //cooldown
        val difference = (Date().time - pd.finalUseTime)/1000
        if (data.cooldown > difference && data.cooldown != 0L)return


        ///////////////////////
        //remove an item
        if (data.isRemoveItem){
            val item = p.inventory.itemInMainHand
            item.amount = item.amount -1
            p.inventory.itemInMainHand = item
        }else if(data.crashChance[pd.level] !=0.0 && Math.random()<data.crashChance[pd.level]){
            val item = p.inventory.itemInMainHand
            item.amount = item.amount -1
            p.inventory.itemInMainHand = item
            p.sendMessage(data.crashMsg)
        }

        if (data.useMsg.size > pd.level){
            p.sendMessage(data.useMsg[pd.level])
        }

        //add logs
        plugin.db.executeQueue.add("INSERT INTO `log` " +
                "(`uuid`, `player`, `drug_name`,`date`)" +
                " VALUES ('${p.uniqueId}', '${p.name}', '$dataName',now());")


        if (!data.buff[pd.level].isNullOrEmpty()){
            for (b in data.buff[pd.level]!!){
                val s = b.split(",")
                p.addPotionEffect(PotionEffect(
                        PotionEffectType.getByName(s[0]),
                        s[1].toInt(),s[2].toInt()))
            }
        }

        if (!data.buffRandom[pd.level].isNullOrEmpty()){
            val s = plugin.random(data.buffRandom[pd.level]!!).split(",")
            p.addPotionEffect(PotionEffect(
                    PotionEffectType.getByName(s[0]),
                    s[1].toInt(),s[2].toInt()))

        }

        if (!data.sound[pd.level].isNullOrEmpty()){
            for (so in data.sound[pd.level]!!){
                val s = so.split(",")
                p.location.world.playSound(p.location, Sound.valueOf(s[0]),
                        s[1].toFloat(),s[2].toFloat())
            }

        }

        if (!data.soundRandom[pd.level].isNullOrEmpty()){
            val s = plugin.random(data.soundRandom[pd.level]!!).split(",")
            p.location.world.playSound(p.location,Sound.valueOf(s[0]),
                    s[1].toFloat(),s[2].toFloat())

        }

        if (!data.particle[pd.level].isNullOrEmpty()){
            for (par in data.particle[pd.level]!!){
                val s = par.split(",")
                p.location.world.spawnParticle(Particle.valueOf(s[0]),p.location,s[1].toInt())
            }

        }

        if (!data.particleRandom[pd.level].isNullOrEmpty()){
            val s = plugin.random(data.particleRandom[pd.level]!!).split(",")
            p.location.world.spawnParticle(Particle.valueOf(s[0]),p.location,s[1].toInt())

        }

        if (!data.cmd[pd.level].isNullOrEmpty()){
            for (c in data.cmd[pd.level]!!){
                p.isOp = true
                p.performCommand(c)
                p.isOp = false
            }

        }

        if (!data.cmdRandom[pd.level].isNullOrEmpty()){
            p.isOp = true
            p.performCommand(plugin.random(data.cmdRandom[pd.level]!!))
            p.isOp = false

        }

        if (!data.sCmd[pd.level].isNullOrEmpty()){
            for (c in data.sCmd[pd.level]!!){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),c)
            }

        }

        if (!data.sCmdRandom[pd.level].isNullOrEmpty()){
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),plugin.random(data.cmdRandom[pd.level]!!))
        }

        if (data.func.size>pd.level){
            plugin.func.runFunc(data.func[pd.level],p)
        }

        if (data.removeBuffs){
            for (e in p.activePotionEffects){
                p.removePotionEffect(e.type)
            }
        }

        if (data.nearPlayer.size>pd.level){
            val s = data.nearPlayer[pd.level].split(";")

            for (pla in getNearPlayer(p,s[1].toInt())){
                plugin.func.runFunc(s[0],p)
            }
        }

        pd.usedCount ++ //使用回数更新
        pd.finalUseTime = Date().time  //最終使用時刻更新

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

            if (defenseCheck(p))continue //弾けた場合
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

        if (data.type !=2)return false
        if (Math.random()<data.defenseProb)return true

        return false
    }


}