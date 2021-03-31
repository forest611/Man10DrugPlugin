package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import red.man10.man10drugplugin.Database.playerData
import red.man10.man10drugplugin.Events.getNearPlayer
import red.man10.man10drugplugin.MDPFunction.runFunc
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.debugMode
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugData
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.drugName
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.isReload
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.plugin
import red.man10.man10drugplugin.Man10DrugPlugin.Companion.pluginEnable
import java.util.*

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
                    val pd = playerData[Pair(p, drug)] ?: continue

                    if (!pd.isDepend) continue
                    if (data.type != 0) continue

                    val now = Date().time
                    val time = pd.finalUseTime.time

                    val difference = (now - time) / 1000 //時間差(second)

                    //デバッグモード
                    if (debugMode && difference > 60) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, data, pd)
                        })

                        pd.totalSymptoms++
                        playerData[Pair(p, drug)] = pd
                        continue
                    }

                    //最初の禁断症状
                    if (pd.totalSymptoms == 0 && data.symptomsFirstTime[pd.level] < difference) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, data, pd)
                        })

                        pd.totalSymptoms++
                        pd.finalUseTime = Date()
                        playerData[Pair(p, drug)] = pd
                        continue
                    }
                    //2回目以降の禁断症状
                    if (data.symptomsTime[pd.level] < difference) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, data, pd)
                        })

                        pd.totalSymptoms++
                        pd.finalUseTime = Date()
                        //依存レベルダウン
                        if (Math.random() < data.symptomsStopProb[pd.level]) {
                            pd.level--
                            if (pd.level == -1) {
                                pd.level = 0
                                pd.usedCount = 0
                                pd.isDepend = false
                                pd.totalSymptoms = 0
                                p.sendMessage("§e§l依存が完全に治ったようだ")
                            }
                        }

                        playerData[Pair(p, drug)] = pd
                        continue
                    }
                }
            }

            Thread.sleep(10000)
        }

    }

    fun symptoms(p:Player, data:Config.Drug, pd:Database.PlayerData){
        if (!data.buffSymptoms[pd.level].isNullOrEmpty()){
            for (b in data.buffSymptoms[pd.level]!!){
                p.addPotionEffect(b)
            }
        }

        if (!data.soundSymptoms[pd.level].isNullOrEmpty()){
            for (so in data.soundSymptoms[pd.level]!!){
                p.location.world.playSound(p.location, so.sound,so.volume,so.pitch)
            }
        }

        if (!data.particleSymptoms[pd.level].isNullOrEmpty()){
            for (par in data.particleSymptoms[pd.level]!!){
                p.location.world.spawnParticle(par.particle,p.location,par.size)
            }
        }

        if (!data.cmdSymptoms[pd.level].isNullOrEmpty()){
            for (c in data.cmdSymptoms[pd.level]!!){

                if (p.isOp){
                    p.performCommand(c)
                }else{
                    p.isOp = true
                    p.performCommand(c)
                    p.isOp = false
                }
            }
        }

        if (data.funcSymptoms.size>pd.level){
            runFunc(data.func[pd.level],p)
        }

        if (data.symptomsNearPlayer.size>pd.level){
            val s = data.symptomsNearPlayer[pd.level].split(";")

            for (pla in getNearPlayer(p,s[1].toInt())){
                runFunc(s[0],p)
            }
        }

        if (data.msgSymptoms.size > pd.level){
            p.sendMessage(data.msgSymptoms[pd.level])
        }

    }
}