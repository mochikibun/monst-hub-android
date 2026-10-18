# 構成と拡張
- MainActivity: 日本時間のカレンダーとフィルタ、選択日の表示。
- Campaign: 日付・カテゴリ・日別属性のモデル。終了日を含む。
- FeedParser: schema_version=1のJSONを検証。壊れたJSONは成功扱いにしない。
- FeedRepository: 指定された公開URLをGETするだけ。成功したcampaignsのみ端末内キャッシュ。status取得失敗は必ず注意。
- UIはネイティブAndroid、外部UIライブラリなし。Android 8.0以上。

## 12アカウント管理の拡張案（未実装）
アカウントID・表示名・並び順を持つAccount、accountIdと日付をキーとするDailyProgress、専用AccountRepositoryを別パッケージに追加する。
Campaignとフィード取得にはアカウント状態を混在させず、将来Room等で端末内保存する。今回ログイン情報やゲームアカウント情報は収集しない。

## 配信仕様
campaigns.json: schema_version/generated_at/campaigns。categoryはarchive/temple/normal/other。
各要素のname/start_date/end_date/daily_attributes[{date,attribute}]/source.urlを使用。
status.json: schema_version/status。ok以外は注意表示。degradedでは不完全の可能性を明示。
日単位のフィードのため開始終了時刻は表示しない。データがない日は「イベントなし」と断定しない。
新しい形式に変わった場合はFeedParserを更新する。

キャッシュは有効期限で消去せず、通信失敗時に前回データとして明示する。generated_atは常時表示。
既存Discord BOT/monst-hub-feedへの書き込み処理・トークン・秘密情報は含まない。
