package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.text.SimpleDateFormat
import java.util.*

class SymptomsTask (val player: Player,val drugData:Data,val pd :playerData,val plugin: Man10DrugPlugin,val db:MDPDataBase,val drug:String) {

    fun run() {

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

                val cmd = plugin.repStr(drugData.commandSymptoms[pd.level]!![i],player,pd)

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

            }
        }

        //////////////////
        //random command
        ////////////////
        if (drugData.commandSymptomsRandom[pd.level] != null){

            val cmd = plugin.repStr(drugData.commandSymptomsRandom[pd.level]!![Random().nextInt(
                    drugData.commandSymptomsRandom[pd.level]!!.size
            )],player,pd)

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)

        }

        ////////////////////////
        //sound
        ///////////////////////
        if (drugData.soundSymptoms[pd.level] != null){

            for (i in 0 until drugData.soundSymptoms[pd.level]!!.size){
                val sound = drugData.soundSymptoms[pd.level]!![i].split(",")

                player.world.playSound(player.location, sound[0],sound[1].toFloat(),sound[2].toFloat())
            }
        }

        ////////////////////////
        //sound random
        ///////////////////////
        if (drugData.soundSymptomsRandom[pd.level] != null){

            val sound = drugData.soundSymptomsRandom[pd.level]!![Random()
                    .nextInt(drugData.soundSymptomsRandom[pd.level]!!.size -1)].split(",")

            player.world.playSound(player.location, sound[0],sound[1].toFloat(),sound[2].toFloat())
        }

        ////////////////////////
        //particle
        ///////////////////////
        if(drugData.particleSymptoms[pd.level] != null){
            for(i in 0 until drugData.particleSymptoms[pd.level]!!.size){
                val particle = drugData.particleSymptoms[pd.level]!![i].split(",")
                player.world.spawnParticle(Particle.valueOf(particle[0]),player.location,particle[1].toInt())

            }


        }

        ////////////////////////
        //particle random
        ///////////////////////
        if (drugData.particleSymptomsRandom[pd.level] != null){

            val particle = drugData.particleSymptomsRandom[pd.level]!![Random()
                    .nextInt(drugData.particleSymptomsRandom[pd.level]!!.size -1)].split(",")
            player.world.spawnParticle(Particle.valueOf(particle[0]), player.location, particle[1].toInt())

        }

        /////////////////////////
        //周囲に迷惑
        ///////////////////////
        if (plugin.size(drugData.symptomsNearPlayer, pd)) {
            val data = drugData.symptomsNearPlayer[pd.level].split(";")
            val list = plugin.event!!.getNearByPlayers(player, data[0].toInt(),drug)
            for (p in list) {
                plugin.mdpfunc.runFunc(p, data[1])
            }
        }


        //send msg
        if (plugin.size(drugData.msgSymptoms,pd)){
            player.sendMessage(plugin.repStr(drugData.msgSymptoms[pd.level],player,pd))
        }

        pd.symptomsTotal ++

        pd.time = Date()

        db.playerMap[player.name+drug] = pd
        //一定回数禁断症状が出た時
        if(drugData.symptomsCount[pd.level] <= pd.symptomsTotal&&drugData.symptomsCount[pd.level] !=0){

            pd.isDependence = false

        }

    }


}