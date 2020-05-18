# Man10DrugPlugin

## ディレクトリ構成

```
/plugins/Man10DrugPlugin
├── config.yml //データベースの設定などを行うconfigファイル
├── func
│   └── func_example.yml //特定の機能をひとまとめにするためのもの
├── durg.yml //ドラッグのファイル
└── tabacco.yml

```

## YAML記入法

### 必須項目

```
<p class="alert">**警告** 大文字小文字を間違えずに記入してください</p>
```

- dataName 	・・・識別名(他のアイテムと重複しないように)
- displayName 　・・・アイテムの表示名
- material    ・・・使うアイテム[アイテムタイプ一覧](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)
- modelData    ・・・割り当てるCustomModelData  (初期値0)
- type   ・・・アイテムのタイプ[^1]  (初期値0)
- lore　　・・・アイテムの説明(リスト形式)

[^1]:type0...依存するドラッグ type1...依存ドラッグの治療薬 type3...依存しない普通のアイテム(type2は未実装)

### その他の項目

- isChange　・・・使用時に外のドラッグプラグインのアイテムと置き換えるかどうか(boolean)

- changeItem  ・・・isChangeがtrueの場合に入力、置き換えるアイテムのdataNameを入力する

- useMsg   ・・・使用時に表示されるメッセージ(リスト形式)

- hasEnchantEffect  ・・・アイテムにエンチャントエフェクトを付けるかどうか

- cooldown  ・・・一度使用してから次使うまでのクールダウン(単位はTick)

- nearPlayer  ・・・アイテムを使ったときに周囲のプレイヤーに任意の処理をする

  ```yaml
  nearPlayer:
  	- func_name:4 #周囲のプレイヤーに動かす処理:半径
  ```

- func  ・・・アイテム使用時に使用時にfuncの処理をする(リスト形式)
- cmd  ・・・使用時にプレイヤーがコマンドを発行する(**権限無視なので注意**)[^2]
- cmdRandom  ・・・指定したコマンドのうち一つをランダムでプレイヤーが発行する(**権限無視なので注意**)[^2]
- sCmd  ・・・使用時にサーバー側でコマンドを発行する[^2]
- sCmdRandom  ・・・使用時にランダムでサーバーコマンドを発行する[^2]
- buff  ・・・使用時にプレイヤーにバフを付与する[バフ一覧](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html)[^2]
- buffRandom  ・・・使用時にプレイヤーにランダムでバフを付与する[^2]
- particle  ・・・使用時にプレイヤーにパーティクルを付与する[パーティクル一覧](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html)[^2]
- particleRandom  ・・・使用時にランダムでパーティクルを付与[^2]
- sound  ・・・使用者の場所で音を鳴らす(リソースパックの音に対応)[^2]
- soundRandom  ・・・使用者の場所でランダムで音を鳴らす[^2]
- crashChance  ・・・アイテムをランダムで破壊する、無記名で機能しなくなる(リスト形式、1.0が最大値)
- removeBuffs  ・・・使用時についてるバフをすべて消すかどうか
- isRemoveItem  ・・・消費アイテムにするかどうか
- disableWorld  ・・・指定したワールドでは使えなくなる(リスト形式)

### Type0 Only

- isDepend  ・・・依存するかどうか(デフォルトtrue)
- dependLevel  ・・・最大レベル
- dependLvUp  ・・・依存レベルが上がる確率(リスト形式)
- dependLvDown  ・・・依存レベルが下がる確率(リスト形式)
- symptomsFirstTime  ・・・最初に禁断症状が出るまでの秒数(リスト形式)
- symptomsTime  ・・・２回目移行の秒数(リスト形式)
- symptomsStopProb  ・・・禁断症状が止まる確率(リスト形式)
- buffSymptoms  ・・・禁断症状のバフ[^2]
- particleSymptoms  ・・・禁断症状のパーティクル[^2]
- soundSymptoms  ・・・禁断症状のサウンド[^2]
- cmdSymptoms  ・・・禁断症状のプレイヤーコマンド[^2]
- dependMsg  ・・・禁断症状時のメッセージ(リスト形式)
- symptomsNearPlayer  ・・・禁断症状発生時に周囲のプレイヤーに処理をする(リスト形式)

### Type1 Only

- weakDrug  ・・・どのドラッグの依存を治療するか(dataNameの値)
- wakProb  ・・・確率で依存が１レベルダウンする(リスト形式)
- stopDepend  ・・・使ったら次にドラッグを使うまで依存が止まるかどうか

[^2]:記入方法

```yaml
buff:
	0:	#依存レベル
		- BUFF,<tick>,<level>
		- BUFF,<tick>,<level>
	1:...

```

