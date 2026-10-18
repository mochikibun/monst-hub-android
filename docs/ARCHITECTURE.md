# MONST HUB v0.3.1 構造
- HubActivity：黒白ホーム、アカウント、日課・絆、オーブ、URL、追加した管理画面、バックアップ、画像添付。
- HubState：アカウント等と日付別記録。schema_version=1を読み込み時に2へ移行。旧ファイル名は維持。
- PlannerData：inventory / custom_characters / teams / missions / goals / formations / orb_plans。UUIDで保持。入力・参照・上限を検証。
- HubStore：全管理データを一つのAtomicFileで保存。保存前に完全な再検証。上限20MB。破損を検出しても勝手に初期化しない。
- CatalogRepository：公開図鑑をJsonReaderでストリーミング解析し、検索に使う項目だけSQLiteへ保存。評価文や画像は保存しない。データ更新はSQLiteトランザクションで実行し、失敗時は前回分に戻す。
- 検索はNFKCとひらがなへの正規化、SQLプレースホルダー、LIKEのワイルドカードのエスケープ。図鑑・個別追加・図鑑から消えた所持記録を合わせて検索する。
- CatalogUpdates / CatalogUpdateService：JobSchedulerの24時間周期と起動時ジョブ。更新日時が24時間以上前か未取得の場合だけ取り込み。既定はUNMETEREDネットワーク、永続ジョブで再起動後も継続。設定のON/OFFを管理データに保持。
- DeadlineScheduler / DeadlineReceiver：通知チャンネルとAndroidの非厳密アラーム。記録の完了・通知済みトークン・削除を再確認し、同じ期限の重複通知を避ける。再起動で再設定。
- MainActivity / Campaign / FeedParser / FeedRepository：既存のキャンペーンカレンダー。degraded案内は従来通り。

## バックアップ
schema_version=2。plannerの追加データ、個別追加キャラ、画像、独立したbond_reset_hourを保存。図鑑キャッシュは対象外。所持記録には名前等のスナップショットを持ち、取得元の削除・変更やキャッシュ未取得でも復元した所持を検索できる。
古いschema_version=1ではplannerを空で追加し、絆の切替は従来のreset_hourを引き継ぐ。未知の将来スキーマは拒否する。

## キャラID
図鑑はgw:ID（GameWithの内部ID）、個別追加はcustom:UUID。
ゲーム内図鑑番号は取得元の数値アイコン項目がある場合のみ使用する。内部IDを番号として表示しない。名称の一致で自動統合せず、個別追加と各進化形態を保持する。

## 範囲
管理の数値・チェック・キャラ所持は手動。ゲームアカウントへのログインやゲーム内データの自動取得はしない。
Google Play公開向けの署名・権利確認・申請は今回の作業対象外。
Discord BOTとmonst-hub-feed、ユーザーのGitHub同期フォルダーへの変更はしない。

## v0.3.1
新規アカウントは0件。既存の登録は保持。図鑑の前にアカウント選択画面、全垢横断の入口も保持。48件ずつのアイコングリッド。PlannerData.adjustOwnedは対象垢のみに増減を適用し、複数の育成記録を勝手に統合しない。
CatalogIconsは表示対象だけ取得し、画像のメモリ・ディスクキャッシュを上限付きで保持する。URLはGameWith CDNの許可したトークン形式のみ。
SQLiteのicon列はALTER TABLEで追加し、既存キャッシュを消さない。
