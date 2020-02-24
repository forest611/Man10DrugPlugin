package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class DependThread (private val plugin: Man10DrugPlugin){

    //////////////////////
    //依存処理に関するスレッド プラグイン起動時にスレッドスタート
    //////////////////////
    fun dependThread(){
        Thread(Runnable {

            while (true){

                if (plugin.isReload || !plugin.pluginEnable)break

                for (p in Bukkit.getOnlinePlayers()){

                    for (drug in plugin.drugName){

                        val data = plugin.drugData[drug]!!
                        val pd = plugin.db.playerData[Pair(p,drug)]?:continue


                        if (!pd.isDepend)continue
                        if (data.type !=0)continue

                        val now = Date().time
                        val time = pd.finalUseTime

                        val difference = (now-time)/1000 //時間差(second)

                        //デバッグモード
                        if (plugin.debugMode && difference>60){

                            symptoms(p,data,pd)

                            pd.totalSymptoms ++
                            plugin.db.playerData[Pair(p,drug)] = pd
                            continue
                        }

                        //最初の禁断症状
                        if (pd.totalSymptoms == 0 && data.symptomsFirstTime[pd.level]<difference){

                            symptoms(p,data,pd)

                            pd.totalSymptoms ++
                            plugin.db.playerData[Pair(p,drug)] = pd
                            continue
                        }
                        //2回目以降の禁断症状
                        if (data.symptomsTime[pd.level]<difference){

                            symptoms(p,data,pd)

                            pd.totalSymptoms ++
                            //依存レベルダウン
                            if (Math.random()<data.symptomsStopProb[pd.level]){
                                pd.level --
                                if (pd.level == -1){
                                    pd.level = 0
//                                    pd.usedCount =0
                                    pd.isDepend = false
                                    pd.totalSymptoms = 0
                                    p.sendMessage("§e§l依存が完全に治ったようだ")
                                }
                            }

                            plugin.db.playerData[Pair(p,drug)] = pd
                            continue
                        }
                    }
                }

                Thread.sleep(100000)

            }
        }).start()
    }

    fun symptoms(p:Player,data:Configs.DrugData,pd:DataBase.PlayerData){
        if (!data.buffSymptoms[pd.level].isNullOrEmpty()){
            for (b in data.buffSymptoms[pd.level]!!){
                val s = b.split(",")
                p.addPotionEffect(PotionEffect(
                        PotionEffectType.getByName(s[0]),
                        s[1].toInt(),s[2].toInt(),false,false))
            }
        }

        if (!data.soundSymptoms[pd.level].isNullOrEmpty()){
            for (so in data.soundSymptoms[pd.level]!!){
                val s = so.split(",")
                p.location.world.playSound(p.location, Sound.valueOf(s[0]),
                        s[1].toFloat(),s[2].toFloat())
            }

        }

        if (!data.particleSymptoms[pd.level].isNullOrEmpty()){
            for (par in data.particleSymptoms[pd.level]!!){
                val s = par.split(",")
                p.location.world.spawnParticle(Particle.valueOf(s[0]),p.location,s[1].toInt())
            }

        }

        if (!data.cmdSymptoms[pd.level].isNullOrEmpty()){
            for (c in data.cmdSymptoms[pd.level]!!){
                p.isOp = true
                p.performCommand(c)
                p.isOp = false
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

    }
}