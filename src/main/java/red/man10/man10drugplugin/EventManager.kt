package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class EventManager(val plugin: Man10DrugPlugin,val player: Player,val pd:playerData,drug: String) {

    val data = plugin.mdpConfig.get(drug)

    fun removeBuffs(){
        if (data.removeBuffs){
            for (effect in player.activePotionEffects){
                player.removePotionEffect(effect.type)
            }
        }

    }

    fun buff(){
        if (data.buff[pd.level] != null) {

            for (b in data.buff[pd.level]!!) {
                val buff = b.split(",")
                buff(player,buff,data)
            }
        }

    }

    fun buffRandom(){
        if (data.buffRandom[pd.level] != null) {

            val buff = data.buffRandom[pd.level]!![Random()
                    .nextInt(data.buffRandom[pd.level]!!.size - 1)].split(",")

            buff(player,buff,data)
        }

    }

    fun buffDelay(){
        if (data.buffDelay[pd.level] != null) {

            for (b in data.buffDelay[pd.level]!!) {
                val time = b.split(";")
                val buff = time[0].split(",")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {buff(player, buff,data)}, time[1].toLong())
            }
        }

    }

    fun buffDelayRandom(){
        if (data.buffRandomDelay[pd.level] != null) {
            val time = data.buffRandomDelay[pd.level]!![Random()
                    .nextInt(data.buffRandomDelay[pd.level]!!.size - 1)].split(";")
            val buff = time[0].split(",")

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {buff(player,buff,data)}, time[1].toLong())

        }

    }

    fun cmd(){
        if (data.command[pd.level] != null){
            for (c in data.command[pd.level]!!) {
                val cmd = plugin.repStr(c, player, pd)

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

            }
        }

    }

    fun cmdRandom(){
        if (data.commandRandom[pd.level] != null) {

            val cmd = plugin.repStr(data.commandRandom[pd.level]!![Random().nextInt(
                    data.commandRandom[pd.level]!!.size
            )], player, pd)

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

        }

    }

    fun cmdDelay(){
        if (data.commandDelay[pd.level] != null) {

            for (c in data.commandDelay[pd.level]!!) {
                val command = c.split(";")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.repStr(command[0], player, pd))

                }, command[1].toLong())
            }

        }

    }

    fun cmdDelayRandom(){
        if (data.commandRandomDelay[pd.level] != null) {

            val command = data.commandRandomDelay[pd.level]!![
                    data.commandRandomDelay[pd.level]!!.size - 1].split(";")

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.repStr(command[0], player, pd))

            }, command[1].toLong())
        }

    }

    fun pCmd(){
        if (data.playerCmd[pd.level] != null){
            for (c in data.playerCmd[pd.level]!!) {
                val cmd = plugin.repStr(c, player, pd)

                Bukkit.dispatchCommand(player, cmd)

            }
        }

    }

    fun pCmdRandom(){
        if (data.playerCmdRandom[pd.level] != null) {

            val cmd = plugin.repStr(data.playerCmdRandom[pd.level]!![Random().nextInt(
                    data.playerCmdRandom[pd.level]!!.size
            )], player, pd)

            Bukkit.dispatchCommand(player, cmd)

        }

    }

    fun pCmdDelay(){
        if (data.playerCmdDelay[pd.level] != null) {

            for (c in data.playerCmdDelay[pd.level]!!) {
                val command = c.split(";")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

                    Bukkit.dispatchCommand(player, plugin.repStr(command[0], player, pd))

                }, command[1].toLong())
            }
        }

    }

    fun pCmdDelayRandom(){
        if (data.playerCmdRandomDelay[pd.level] != null) {

            val command = data.playerCmdRandomDelay[pd.level]!![
                    data.playerCmdRandomDelay[pd.level]!!.size - 1].split(";")

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {

                Bukkit.dispatchCommand(player, plugin.repStr(command[0], player, pd))

            }, command[1].toLong())
        }

    }

    fun particle(){
        if (data.particle[pd.level] != null) {
            for (p in data.particle[pd.level]!!) {
                val par = p.split(",")
                player.world.spawnParticle(Particle.valueOf(par[0]), player.location, par[1].toInt())

            }
        }

    }

    fun particleRandom(){
        if (data.particleRandom[pd.level] != null) {

            val particle = data.particleRandom[pd.level]!![Random()
                    .nextInt(data.particleRandom[pd.level]!!.size - 1)].split(",")
            player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())

        }

    }

    fun particleDelay(){
        if (data.particleDelay[pd.level] != null) {
            for (p in data.particleDelay[pd.level]!!) {
                val times = p.split(";")

                val par = times[0].split(",")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.world.spawnParticle(Particle.valueOf(par[0]), player.location, par[1].toInt())
                }, times[1].toLong())

            }
        }

    }

    fun particleDelayRandom(){
        if (data.particleRandomDelay[pd.level] != null) {
            val time = data.particleRandomDelay[pd.level]!![Random()
                    .nextInt(data.particleRandomDelay[pd.level]!!.size - 1)].split(";")
            val particle = time[0].split(",")

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())
            }, time[1].toLong())

        }

    }

    fun sound(){
        if (data.sound[pd.level] != null) {
            for (s in data.sound[pd.level]!!) {
                val sound = s.split(",")
                player.world.playSound(player.location, sound[0], sound[1].toFloat(), sound[2].toFloat())
            }
        }

    }

    fun soundRandom(){
        if (data.soundRandom[pd.level] != null) {

            val sound = data.soundRandom[pd.level]!![Random()
                    .nextInt(data.soundRandom[pd.level]!!.size - 1)].split(",")

            player.world.playSound(player.location, sound[0], sound[1].toFloat(), sound[2].toFloat())
        }

    }

    fun soundDelay(){
        if (data.soundDelay[pd.level] != null) {

            for (s in data.soundDelay[pd.level]!!) {
                val time = s.split(";")
                val sound = time[0].split(",")

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                    player.world.playSound(player.location, sound[0], sound[1].toFloat()
                            , sound[2].toFloat())
                }, time[1].toLong())
            }
        }

    }

    fun soundDelauRandom(){
        if (data.soundRandomDelay[pd.level] != null) {
            val time = data.soundRandomDelay[pd.level]!![Random()
                    .nextInt(data.soundRandomDelay[pd.level]!!.size - 1)].split(";")
            val sound = time[0].split(",")

            player.world.playSound(player.location, sound[0], sound[1].toFloat(), sound[2].toFloat())
        }

    }

    fun buff(player:Player,buff:List<String>,c:Data){
        if (c.hideBuff){
            player.addPotionEffect(PotionEffect(
                    PotionEffectType.getByName(buff[0]),
                    buff[1].toInt(),
                    buff[2].toInt(),
                    false,false
            ))
            return
        }
        player.addPotionEffect(PotionEffect(
                PotionEffectType.getByName(buff[0]),
                buff[1].toInt(),
                buff[2].toInt()
        ))

    }


    fun runAll(){
        buff()
        buffRandom()
        buffDelay()
        buffDelayRandom()

        sound()
        soundRandom()
        soundDelay()
        soundDelauRandom()

        cmd()
        cmdRandom()
        cmdDelay()
        cmdDelayRandom()

        particle()
        particleRandom()
        particleDelay()
        particleDelayRandom()

        pCmd()
        pCmdRandom()
        pCmdDelay()
        pCmdDelayRandom()


    }
}