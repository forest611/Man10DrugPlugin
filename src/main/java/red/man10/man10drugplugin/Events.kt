package red.man10.man10drugplugin

import net.minecraft.server.v1_12_R1.NBTTagCompound
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import java.lang.Exception

class Events(private val plugin: Man10DrugPlugin):Listener{

    @EventHandler
    fun useDrugEvent(e:PlayerInteractEvent){
        if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK){
            val item = e.item?:return

            val p = e.player

            if (item.itemMeta == null) return
            if (!CraftItemStack.asNMSCopy(item).hasTag())return
            if (plugin.isReload){
                p.sendMessage("§e§l今は使えないようだ....")
                return
            }
            var dataName = ""

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


    fun useDrug(p: Player, dataName:String){
        if (plugin.disableWorld.indexOf(p.world.name) != -1)return

        val data = plugin.drugData[dataName]!!

        if (data.disableWorld.indexOf(p.world.name) != -1)return


    }


}