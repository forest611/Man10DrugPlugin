package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class DependThread (private val plugin: Man10DrugPlugin){

    //////////////////////
    //依存処理に関するスレッド プラグイン起動時にスレッドスタート
    //////////////////////
    fun dependThread(){

        Thread{
            if (plugin.isReload || !plugin.pluginEnable) return@Thread

            for (p in Bukkit.getOnlinePlayers()) {

                for (drug in plugin.drugName) {

                    val data = plugin.drugData[drug]!!
                    val pd = plugin.db.playerData[Pair(p, drug)] ?: continue

                    if (!pd.isDepend) continue
                    if (data.type != 0) continue

                    val now = Date().time
                    val time = pd.finalUseTime.time

                    val difference = (now - time) / 1000 //時間差(second)

                    //デバッグモード
                    if (plugin.debugMode && difference > 60) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, data, pd)
                        })

                        pd.totalSymptoms++
                        plugin.db.playerData[Pair(p, drug)] = pd
                        continue
                    }

                    //最初の禁断症状
                    if (pd.totalSymptoms == 0 && data.symptomsFirstTime[pd.level] < difference) {

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            symptoms(p, data, pd)
                        })

                        pd.totalSymptoms++
                        pd.finalUseTime = Date()
                        plugin.db.playerData[Pair(p, drug)] = pd
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

                        plugin.db.playerData[Pair(p, drug)] = pd
                        continue
                    }
                }
            }

            Thread.sleep(10000)
        }

    }

    fun symptoms(p:Player, data:Configs.Drug, pd:DataBase.PlayerData){
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
            plugin.func.runFunc(data.func[pd.level],p)
        }

        if (data.symptomsNearPlayer.size>pd.level){
            val s = data.symptomsNearPlayer[pd.level].split(";")

            for (pla in plugin.events.getNearPlayer(p,s[1].toInt())){
                plugin.func.runFunc(s[0],p)
            }
        }

        if (data.msgSymptoms.size > pd.level){
            p.sendMessage(data.msgSymptoms[pd.level])
        }

    }
}