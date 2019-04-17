# ドラッグ用yml作成方法
## 必須項目
<br/>
- DisplayName ... ドラッグの表示名
- DataName ... コマンド、db、などのデータ名
- Type ... ドラッグのタイプ
- Material ... どのアイテムをドラッグにするか[マテリアルまとめ](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)
- Damage ... アイテムのダメージ値
<br/>
<br/>
## どのタイプでも使える機能
<br/>
- Lore ... アイテムの説明(List)
- cooldown ... 使用してから次に使用できるまでの時間(int)
- UseMsg ... 使用時のメッセージ(list)
- UseMsgDelay ... 使用時の遅延メッセージ(list)`メッセージ;時間(tick)`のように書く
- EnchantEffect ... エンチャントのエフェクトを表示するか(bool)
- RemoveBuffs ... 使用時にステータスエフェクトを消すかどうか(bool)
<br/>
- func ... func機能（後述)
- funcrandom ... func機能（後述)
- funcdelay ... func機能（後述)
- funcdelayrandom ... func機能（後述)
<br/>
- Command ... 使用時のコマンド(list in list)
- CommandRandom ... ランダムコマンド(list in list)
- CommandDelay ... 遅延コマンド(list in list)`コマンド;(tick)`
- CommandRandomDelay ... ランダム遅延コマンド(list in list)`コマンド;(tick)`
- ※<player>,で使ったプレイヤーを指定できます
<br/>
- Buff ... 使用時のステータスエフェクト(list in list) `バフ名,時間(Tick),レベル` [PotionEffectType](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html)
- BuffRandom ... ランダムステータスエフェクト(list in list)
- BuffDelay ... 遅延コマンド(list in list)`コマンド;(tick)`
- BuffRandomDelay ... ランダム遅延ステータスエフェクト(list in list)`バフ;(tick)`
<br/>
- Particle ... 使用時のコマンド(list in list) `パーティクル名,大きさ` [Particle](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html)
- ParticleRandom ... ランダムコマンド(list in list)
- ParticleDelay ... 遅延コマンド(list in list)`コマンド;(tick)`
- ParticleRandomDelay ... ランダム遅延コマンド(list in list)`パーティクル;(tick)`
<br/>
- Sound ... 使用時のコマンド(list in list) `サウンド名,音量,速さ` [Sound](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html)
- SoundRandom ... ランダムコマンド(list in list)
- SoundDelay ... 遅延コマンド(list in list)`コマンド;(tick)`
- SoundRandomDelay ... ランダム遅延コマンド(list in list)`サウンド;(tick)`
<br/>
<br/>
## Type0(依存薬物)のみ使える機能
<br/>
- IsDependence ... 依存するかどうか(bool)
- DependenceLevel ... 依存レベル(int)(4を指定した場合、Lv.0から含めるため、5段階となります)
- NextLevelCount ... 次の依存レベルに上がるまでに必要な摂取回数(int list)
- SymptomsCount ... 禁断症状が発生する回数(int list)0にしたら止まらなくなります
- SymptomsTime ... 使ってから最初に禁断症状が発生するまでの時間(int list) 単位は秒
- SymptomsNextTime ... 最初の禁断症状が発生した後に、禁断症状が発生するまでの時間(int list) 単位は秒
<br/>
- MsgSymptoms ... 禁断症状発生時のメッセージ(list)
<br/>
- BuffSymptoms ... 禁断症状発生時のステータスエフェクト(list in list)
- BuffSymptomsRandom ... 禁断症状発生時のランダムステータスエフェクト(list in list)
<br/>
- CommandSymptoms ... 禁断症状発生時のコマンド(list in list)
- CommandSymptomsRandom ... 禁断症状発生時のランダムコマンド(list in list)
<br/>
- ParticleSymptoms ... 禁断症状発生時のパーティクル(list in list)
- particleSymptomsRandom ... 禁断症状発生時のランダムパーティクル(list in list)
<br/>
- SoundSymptoms ... 禁断症状発生時のサウンド(list in list)
- SoundSymptomsRandom ... 禁断症状発生時のランダムサウンド(list in list)

- CommandLvUp ... 依存レベルが上がったときのコマンド(list in list)
- CommandRandomLvUp ... 依存レベルが上がったときのランダムコマンド(list in list)
<br/>
- funcLvUp ... func機能（後述)
- funcrandomLvUp ... func機能（後述)
- funcdelayLvUp ... func機能（後述)
- funcdelayrandomLvUp ... func機能（後述)

