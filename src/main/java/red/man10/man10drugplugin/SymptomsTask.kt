package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class SymptomsTask (val plugin: Man10DrugPlugin,val player: Player,mysql:MySQLManager,val drug:String): BukkitRunnable() {

    val config = MDPConfig(plugin)
    val playerData = MDPDataBase(plugin,mysql)

    override fun run() {

        val pd = playerData.get(player.name+drug)
        val drugData = config.get(drug)

        ////////////////
        //buff
        ///////////////
        if (drugData.buffSymptoms[pd.level] != null){

            for (i in 0 until drugData.buffSymptoms[pd.level]!!.size){
                val buff = drugData.buffSymptoms[pd.level]!![i].split(",")

                player.addPotionEffect(PotionEffect(
                        PotionEffectType.getByName(buff[0]),
                        buff[1].toInt(),
                        buff[2].toInt()
                ))
            }
        }

        /////////////////
        //random buff
        ///////////////
        if (drugData.buffSymptomsRandom[pd.level] != null){

            val buff = drugData.buffSymptoms[pd.level]!![Random()
                    .nextInt(drugData.buffSymptoms[pd.level]!!.size -1)].split(",")

            player.addPotionEffect(PotionEffect(
                    PotionEffectType.getByName(buff[0]),
                    buff[1].toInt(),
                    buff[2].toInt()
            ))

        }

        //////////////////
        //command
        /////////////////
        if (drugData.commandSymptoms[pd.level] != null){

            for (i in 0 until drugData.commandSymptoms[pd.level]!!.size){

                val cmd = drugData.commandSymptoms[pd.level]!![i].replace("<player>",player.name)

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

            }
        }

        //////////////////
        //random command
        ////////////////
        if (drugData.commandSymptomsRandom[pd.level] != null){

            val cmd = drugData.commandSymptomsRandom[pd.level]!![Random().nextInt(
                    drugData.commandSymptomsRandom[pd.level]!!.size
            )].replace("<player>",player.name)

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

        }

        ////////////////////
        //sound
        ///////////////////
        if (drugData.soundSymptoms != null && !drugData.soundSymptoms!![pd.level].isEmpty()){
            val sound = drugData.soundSymptoms!![pd.level].split(",")

            player.world.playSound(player.location, Sound.valueOf(sound[0]),sound[1].toFloat(),sound[2].toFloat())
        }

        /////////////////////
        //particle
        ////////////////////
        if(drugData.particleSymptoms != null && !drugData.particleSymptoms!![pd.level].isEmpty()){
            val particle = drugData.particleSymptoms!![pd.level].split(",")

            player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())
        }

        ///////////////////
        //particle random
        ///////////////////
        if(drugData.particleSymptomsRandom != null && !drugData.particleSymptomsRandom!![pd.level].isEmpty()) {
            val particle = drugData.particleSymptomsRandom!![Random()
                    .nextInt(drugData.particleSymptomsRandom!!.size - 1)].split(",")

            player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())
        }

        //send msg
        if (drugData.msgSymptoms != null && !drugData.msgSymptoms!![pd.level].isEmpty()){
            player.sendMessage(drugData.msgSymptoms!![pd.level])
        }

        pd.times ++

        //一定回数禁断症状が出た時
        if(drugData.symptomsCount!![pd.level] <= pd.times&&drugData.symptomsCount!![pd.level] !=0.toLong()){

            Bukkit.getScheduler().cancelTask(pd.taskId)
            pd.isDependence = false

        }

    }
}