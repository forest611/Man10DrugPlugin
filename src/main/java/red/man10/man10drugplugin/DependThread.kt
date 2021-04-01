package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.man10drugplugin.MDPFunction.runFunc
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.debugMode
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugData
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugName
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.isReload
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.plugin
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.pluginEnable
import java.util.*
import javax.xml.crypto.Data

object DependThread{

    //////////////////////
    //依存処理に関するスレッド プラグイン起動時にスレッドスタート
    //////////////////////
    fun dependThread(){

        Thread{
            if (isReload || !pluginEnable) return@Thread

            for (p in Bukkit.getOnlinePlayers()) {

                for (drug in drugName) {

                    val data = drugData[drug]!!
                    val pd = Database.get(p,drug)!!

                    if (!pd.isDepend) continue
                    if (data.type != 0) continue

                    val now = Date().time
                    val time = pd.finalUseTime.time

                    val difference = (now - time) / 1000 //時間差(second)

                    val parameter = data.parameter[pd.level]

                    if (parameter.symptomsStopProb>Math.random()){
                        pd.isDepend = false
                        Database.set(p,drug,pd)
                        return@Thread
                    }

                    //デバッグモード
                    if (debugMode && difference > 60) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, parameter)
                        })

                        pd.totalSymptoms++
                        Database.set(p,drug,pd)
                        continue
                    }


                    //最初の禁断症状
                    if (pd.totalSymptoms == 0 && parameter.symptomsFirstTime < difference) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, parameter)
                        })

                        pd.totalSymptoms++
                        pd.finalUseTime = Date()
                        Database.set(p,drug,pd)
                        continue
                    }
                    //2回目以降の禁断症状
                    if (parameter.symptomsTime < difference) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, parameter)
                        })

                        pd.totalSymptoms++
                        pd.finalUseTime = Date()
                        //依存レベルダウン
                        if (parameter.dependLvDown>Math.random()) {
                            pd.level--
                            if (pd.level == -1) {
                                pd.level = 0
                                pd.usedCount = 0
                                pd.isDepend = false
                                pd.totalSymptoms = 0
                                p.sendMessage("§e§l依存が完全に治ったようだ")
                            }
                        }

                        Database.set(p,drug,pd)
                        continue
                    }
                }
            }

            Thread.sleep(10000)
        }

    }

    private fun symptoms(p:Player, data:Config.DrugParameter){

        for (b in data.buffSymptoms){
            p.addPotionEffect(b)
        }

        for (so in data.soundSymptoms){
            p.location.world.playSound(p.location, so.sound,so.volume,so.pitch)
        }

        for (par in data.particleSymptoms){
            p.location.world.spawnParticle(par.particle,p.location,par.size)
        }

        for (c in data.cmdSymptoms){

            if (p.isOp){
                p.performCommand(c)
            }else{
                p.isOp = true
                p.performCommand(c)
                p.isOp = false
            }
        }

        runFunc(data.func,p)

//        if (data.symptomsNearPlayer.size>pd.level){
//            val s = data.symptomsNearPlayer[pd.level].split(";")
//
//            for (pla in getNearPlayer(p,s[1].toInt())){
//                runFunc(s[0],p)
//            }
//        }

        p.sendMessage(data.msgSymptoms)

    }
}