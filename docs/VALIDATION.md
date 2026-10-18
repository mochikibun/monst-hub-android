# 検証結果（2026年10月18日・日本時間）

## 実行済み
- JDK 17 / Gradle 8.11.1 / AGP 8.9.2 / Android SDK 35でクリーンビルド成功。
- `clean testDebugUnitTest lintDebug assembleDebug`：BUILD SUCCESSFUL。
- 自動テスト6件：成功6件、失敗0件、スキップ0件。
- Android lint：No issues found（エラー0、警告0）。
- Android XML構文、APKのパッケージID・SDK情報を確認。
- 配布ZIPのCRC、必須ファイル存在、元ファイルとのバイト一致を検証。
- Gradle Wrapper本体を同梱し、Gradle配布ZIPの公式SHA256を固定。

## データ確認
指定された公開campaigns.json/status.jsonを読み取り、schema_version=1、キャンペーン7件、status=degradedを確認。
テストでは期間の開始日・終了日を含む判定、カテゴリフィルタ、日別属性、不明属性、degraded、正常状態、壊れたJSON、未対応スキーマ、空データ、2026年10月と閏年を確認。

## 未実施・制約
- Android実機／エミュレーターでの表示・操作・通信断試験は未実施。
- GitHubへのアップロードとGitHub Actions上での実行は未実施。ユーザーのリポジトリは変更していません。
- このAPKとGitHubで再生成したAPKはdebug署名が異なる場合があります。上書き不可なら旧版を削除して再インストールしてください。
- 12アカウント管理は拡張方針のみ。今回の版では未実装。
- 配信データは完全性を保証しません。開催時間・詳細は公式情報で確認してください。

## 変更対象
この作業フォルダーでAndroidプロジェクトと配布ファイルのみ作成しました。Discord BOTとmonst-hub-feedへの書き込み・変更はしていません。
