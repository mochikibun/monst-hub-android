# MONST HUB v0.3.2 検証結果
2026年10月18日（日本時間）

- testDebugUnitTest / lintDebug / assembleDebug：BUILD SUCCESSFUL。
- 自動テスト54件成功、失敗0、スキップ0。Android lint：No issues found。
- 追加確認：図鑑→全垢検索→元の垢→垢選択→ホームの順に戻ること。垢と検索条件を復元すること。
- ホームボタンで履歴をクリアし、ホームで戻ると終了すること。
- Activityの再生成後も戻る履歴を保持すること。API35でも画面遷移を確認。
- API33以上はOSの戻るコールバック、旧AndroidはonBackPressedを使用。
- 既存50件のテストも成功。実図鑑9,282件の取り込み・全件の名称照合を含みます。
- 実機での戻るボタン・ジェスチャー操作は未確認です。
- versionName 0.3.2 / versionCode 5。署名は従来と同じローカルdebug署名。
- 既存Discord BOT、feedリポジトリ、ユーザーのGitHub同期フォルダーは変更していません。
