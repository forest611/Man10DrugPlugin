package red.man10.man10drugplugin

import org.bukkit.Bukkit
import org.bukkit.entity.Player
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
                        val pd = plugin.db.playerData[Pair(p,drug)]!!


                        if (!pd.isDepend)continue
                        if (data.type !=0)continue

                        val now = Date().time
                        val time = pd.finalUseTime

                        val difference = (now-time)*1000 //時間差(second)

                        //デバッグモード
                        if (plugin.debugMode && difference>60){

                            symptoms(p,drug)

                            pd.totalSymptoms ++
                            plugin.db.playerData[Pair(p,drug)] = pd
                            continue
                        }

                        //最初の禁断症状
                        if (pd.totalSymptoms == 0 && data.symptomsFirstTime[pd.level]<difference){

                            symptoms(p,drug)

                            pd.totalSymptoms ++
                            plugin.db.playerData[Pair(p,drug)] = pd
                            continue
                        }
                        //2回目以降の禁断症状
                        if (data.symptomsTime[pd.level]<difference){

                            symptoms(p,drug)

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

    fun symptoms(p:Player,drug:String){

    }
}