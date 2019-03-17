package red.man10.man10drugplugin

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

class MDPConfig(val plugin: Man10DrugPlugin) {


    var drugData = HashMap<String,drugData>()

    fun loadConfig(drug:File){


        val config = YamlConfiguration.loadConfiguration(drug)

        val data = get(config.getString("DataName"))


        //コマンド用の名前を登録
        plugin.drugName.add(config.getString("DataName"))

        data.displayName = config.getString("DisplayName")
        data.material = config.getString("Material")
        data.damage = config.getInt("Damage").toShort()
        data.type =  config.getInt("Type")
        data.isEnchantParticle = config.getBoolean("IsEnchantParticle")
        data.isChange = config.getBoolean("IsChange")
        data.changeItem = config.getItemStack("ChangeItem")


        data.lore = config.getStringList("Lore")

        //use message
        data.useMsg = config.getStringList("UseMsg")
        data.useMsgDelay = config.getStringList("UseMsgDelay")



        //command
        for (i in 0 until config.getStringList("Command").size){
            data.command[i] = config.getStringList("Command.Level$i")
        }
        for (i in 0 until config.getStringList("CommandRandom").size){
            data.commandRandom[i] = config.getStringList("CommandRandom.Level$i")
        }
        for (i in 0 until config.getStringList("CommandDelay").size){
            data.commandDelay[i] = config.getStringList("CommandDelay.Level$i")
        }
        for (i in 0 until config.getStringList("CommandDelayRandom").size){
            data.commandRandomDelay[i] = config.getStringList("CommandDelayRandom.Level$i")
        }



        //buff
        for (i in 0 until config.getStringList("Buff").size){
            data.buff[i] = config.getStringList("Buff.Level$i")
        }
        for (i in 0 until config.getStringList("BuffRandom").size){
            data.buffRandom[i] = config.getStringList("BuffRandom.Level$i")
        }
        for (i in 0 until config.getStringList("BuffDelay").size){
            data.buffDelay[i] = config.getStringList("BuffDelay.Level$i")
        }
        for (i in 0 until config.getStringList("BuffDelayRandom").size){
            data.buffRandomDelay[i] = config.getStringList("BuffDelayRandom.Level$i")
        }


        //particle
        data.particle = config.getStringList("Particle")
        data.particleRandom = config.getStringList("ParticleRandom")
        data.particleDelay = config.getStringList("ParticleDelay")
        data.particleRandomDelay = config.getStringList("ParticleDelayRandom")


        //sound
        data.sound = config.getStringList("Sound")
        data.soundDelay = config.getStringList("SoundDelay")




        if (data.type == 0){
            data.isDependence = config.getBoolean("IsDependence")
            data.dependenceLevel = config.getInt("DependenceLevel")
            data.isDieCancel = config.getBoolean("IsDieCancel")
            data.isLeftServerStopDep = config.getBoolean("IsLeftServerStopDep")
            data.nextLevelCount = config.getInt("NextLevelCount")

            data.symptomsTime = config.getLongList("SymptomsTime")
            data.symptomsNextTime = config.getLongList("SymptomsNextTime")
            data.symptomsCount = config.getLongList("SymptomsCount")


            for (i in 0 until config.getStringList("BuffSymptoms").size){
                data.buffSymptoms[i] = config.getStringList("BuffSymptoms.Level$i")
            }
            for (i in 0 until config.getStringList("BuffSymptomsRandom").size){
                data.buffSymptomsRandom[i] = config.getStringList("BuffSymptomsRandom.Level$i")
            }



            for (i in 0 until config.getStringList("CommandSymptoms").size){
                data.commandSymptoms[i] = config.getStringList("CommandSymptoms.Level$i")
            }
            for (i in 0 until config.getStringList("CommandSymptomsRandom").size){
                data.commandSymptomsRandom[i] = config.getStringList("CommandSymptomsRandom.Level$i")
            }


            data.particleSymptoms = config.getStringList("ParticleSymptoms")
            data.particleSymptomsRandom = config.getStringList("ParticleSymptomsRandom")


            data.msgSymptoms = config.getStringList("MsgSymptoms")


            data.soundSymptoms = config.getStringList("SoundSymptoms")





        }
        //type1
        if (data.type == 1 || data.type == 2){
            data.weakDrug = config.getString("WeakDrug") //DataName
            data.weakCount = config.getInt("WeakCount")
            data.medicineCount = config.getInt("MedicineCount")
            data.stopTask = config.getBoolean("StopTask")
        }


        drugData[config.getString("DataName")] = data



    }

    fun get(key:String):drugData{
        var data = drugData[key]
        if (data == null){
            data = drugData()
        }
        return data
    }


}

class drugData{

    //必須
    var displayName = "drug"
//    var dataName = "drug"// コマンドで呼び出すときの名前 (アイテム識別に使う) §禁止
    var material = "DIAMOND_HOE"
    var damage : Short = 0
    var type = 0

    //任意
    var isEnchantParticle = false
    lateinit var changeItem : ItemStack //使用後、アイテムを変更する場合
    var isChange = false //変えるか


    //HashMap key...Level,value...mutableList<String>
    //type0 以外はレベルがないので、Level0のみをかく

    //全レベル使用可
    //lore
    lateinit var lore : MutableList<String>
    //message
    lateinit var useMsg : MutableList<String>
    lateinit var useMsgDelay : MutableList<String>
    //command
    val command = HashMap<Int,MutableList<String>>()
    val commandRandom = HashMap<Int,MutableList<String>>()
    val commandDelay = HashMap<Int,MutableList<String>>()
    val commandRandomDelay = HashMap<Int,MutableList<String>>()
    //buff
    val buff = HashMap<Int,MutableList<String>>()
    val buffRandom = HashMap<Int,MutableList<String>>()
    val buffDelay = HashMap<Int,MutableList<String>>()
    val buffRandomDelay = HashMap<Int,MutableList<String>>()
    //particle
    lateinit var particle : MutableList<String>
    lateinit var particleRandom : MutableList<String>
    lateinit var particleDelay : MutableList<String>
    lateinit var particleRandomDelay : MutableList<String>
    //sound
    lateinit var sound : MutableList<String>
    lateinit var soundDelay : MutableList<String>


    //type0
    var isDependence = false
    var dependenceLevel = 0  //依存レベル
    var isDieCancel = false
    var isLeftServerStopDep = false
    var nextLevelCount = 100 //次のレベルに上がるまでの回数

    //type0 HM
    lateinit var symptomsTime : MutableList<Long>
    lateinit var symptomsNextTime : MutableList<Long>
    lateinit var symptomsCount : MutableList<Long> //何回禁断症状が出るか (終わるまでの回数)

    val buffSymptoms = HashMap<Int,MutableList<String>>()
    val buffSymptomsRandom = HashMap<Int,MutableList<String>>()

    val commandSymptoms = HashMap<Int,MutableList<String>>()
    val commandSymptomsRandom = HashMap<Int,MutableList<String>>()

    lateinit var msgSymptoms : MutableList<String>

    lateinit var particleSymptoms : MutableList<String>
    lateinit var particleSymptomsRandom : MutableList<String>

    lateinit var soundSymptoms : MutableList<String>


    //type1
    var weakDrug = "drug" //type2
    var weakCount = 10  //指定値カウントを減らす
    var medicineCount = 0 //弱めるのに必要な量
    var stopTask = false //薬で依存を止めるか




    //random の設定
    //バフの数
    //

}