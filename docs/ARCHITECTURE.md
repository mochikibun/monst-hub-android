# v0.2 構成
- HubActivity：ホーム、垢の編集、日課、絆、オーブ、URL、設定、Android標準ファイル選択を使ったバックアップ。
- HubState：Androidに依存しない管理モデル。UUIDのアカウントID・日課IDで記録を保持し、名前変更では記録が動かない。
- HubStore：アプリ専用領域のJSONをAtomicFileで保存。書き込み失敗では旧データへ戻す。壊れた保存データは勝手に初期化しない。
- MainActivity／Campaign／FeedParser／FeedRepository：既存カレンダーとフィードの取得・キャッシュ。既存のdegraded表示を維持。

## 保存形式
format=monst-hub-backup、schema_version=1。accountsはid/name/memo/url/orbs。tasksはid/name。
daysは日本時間の切替時刻を差し引いた日付→accountId→taskId/bondのboolean。
orb_historyはaccount_id/at/delta/balance/memo。reset_hourは0〜23（既定4）。
バックアップを検証し、重複ID、不明な参照、不正なURL・残高・日付・型を拒否。復元は確認後に保存成功してからメモリ上のデータを置き換える。
変更は状態のコピーに適用し、保存に成功した場合だけ画面の状態を更新する。
100垢・30日課・10,000履歴・5MBの上限。履歴表示は最新50件。これらの上限を超えるバックアップは拒否する。

## 切替・リセット
日付はAsia/Tokyoで計算。午前4時を境に新しいキーを参照するため、アプリを閉じていても起動時に正しい当日を表示。
表示中は15秒ごととチェック操作前に再確認。手動リセットは当日のbondフラグのみ。

## 将来の拡張
現在は端末内のみ。ログイン情報・ゲームへの操作・自動データ取得は実装しない。
固定の配布用署名、過去の日課表示、垢の並べ替え、複数URLの登録等は今後の対象。
BOT・フィードリポジトリへの変更処理は一切含まない。
