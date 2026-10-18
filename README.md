# MONST HUB Android
ダークブルー×水色の非公式キャンペーンカレンダー。Android Studioを使わず、GitHub Actionsでdebug APKを作れます。Android 8.0以上。

## 1. ZIPを展開する（パソコンで操作）
1. `MONST-HUB-Android-project.zip`をダウンロード。
2. Windowsで右クリック →「すべて展開」。ZIPを開くだけでなく展開してください。
3. 展開先でREADME.md、app、gradle、.github、gradlewなどが見えることを確認します。

## 2. GitHubにアップロード
1. GitHubにログインし、`https://github.com/mochikibun/monst-hub-android` を開きます。
2. **monst-hub-android**であることを確認します。BOTやmonst-hub-feedは開かなくて大丈夫です。
3. 「Code」タブ →「Add file」→「Upload files」。画面が狭い場合は「＋」の中です。
4. 展開したフォルダーの**中身すべて**をドラッグします。外側のフォルダーやZIP自体はアップロードしません。
5. アップロード一覧に `app/build.gradle`、`gradle/wrapper/gradle-wrapper.jar`、`.github/workflows/build-apk.yml` があることを確認。
6. 既存のREADME.mdは今回の説明に置き換えてOK。下の「Commit changes」を押します。mainに直接保存を選びます。

### .githubがアップロードできない／Actionsにワークフローがない場合
GitHubの画面で確実に追加できます。
1. 「Code」→「Add file」→「Create new file」。
2. ファイル名に `.github/workflows/build-apk.yml` と入力（先頭のドットも含める）。
3. 展開フォルダーの `.github/workflows/build-apk.yml` をメモ帳で開き、内容すべてをコピーしてGitHubの本文へ貼り付け。
4. 「Commit changes」でmainに保存。
既に同じファイルがある場合は作り直さず、ファイルを開いて鉛筆ボタンで内容を確認してください。

## 3. ActionsでAPKを作る
1. リポジトリ上部の「Actions」を押します。
2. 初回に有効化確認が出たら「I understand my workflows, go ahead and enable them」を押します。
3. 左の **Build Android APK** を選びます。
4. アップロード時に自動実行されます。手動なら「Run workflow」→ branch: main → 緑の「Run workflow」。
5. 数分待って最新の実行が緑のチェックになるまで待ちます。
6. 実行名をクリックし、下へスクロールして **Artifacts** の **MONST-HUB-debug-apk** を押します（GitHubへのログインが必要）。
7. ダウンロードしたZIPを展開すると **app-debug.apk** があります。`validation-reports`は検証結果で、アプリ本体ではありません。
8. Artifactsの保存期間は30日です。期限切れならRun workflowで再作成します。

## 4. Androidへインストール
1. app-debug.apkを自分のAndroid端末へ転送して開きます。
2. 必要なら、そのファイルを開いたブラウザー／ファイルアプリの「この提供元のアプリを許可」を一時的に有効にします。
3. インストール後「MONST HUB」を開きます。初回はネット接続が必要です。
4. 許可設定はインストール後に元へ戻せます。Playストア公開用APKではなく個人用debug APKです。

### 更新版がインストールできない場合
GitHubの実行環境でdebug署名を作るため、別の実行で署名が変わり、上書きできない場合があります。
今回の版はゲームアカウント情報を保存しません。旧MONST HUBをアンインストールして再インストールしてください（取得済みキャッシュも消えます）。将来12アカウント管理を追加する前に、固定署名とバックアップを導入する設計です。

## 使い方
- 「今日」「前月」「翌月」で移動。「2026年10月」で対象月へ直接移動。
- 日付をタップすると、その日の開催イベントと当日の属性を下に表示します。
- 「すべて／書庫／神殿／ノマクエ／その他」で絞り込み。横にスワイプできます。
- カレンダーの件数も選択カテゴリに連動。終了日を含む日単位表示、日本時間基準。
- 属性が配信されていない日は「指定なし」。属性を推測して補いません。
- 「データを更新」で再取得。最終更新はフィードのgenerated_atで、端末で更新ボタンを押した時刻ではありません。
- degradedなら「データが不完全な可能性」を表示。配信状態だけ取得できない場合も注意を表示します。
- 通信失敗時に前回取得があれば明記して表示。初回失敗なら再試行を案内。架空データを本番画面へ表示しません。
- 0件でも公式イベントがないと断定しません。配信対象外や取得漏れの可能性があります。

## つまずいたとき
- **Actionsが赤い**：赤い実行 → build → 赤い手順を開き、エラー画面か文章を元のチャットに持ってきてください。
- **gradlewがない**：外側フォルダーごとアップロードした可能性があります。Code直下にapp、gradle、gradlewが並ぶようにしてください。
- **Workflow does not exist**：上の「.githubがアップロードできない場合」を確認。
- **APKが見つからない**：緑の実行を開き、ArtifactsのMONST-HUB-debug-apkを展開してください。
- **データ取得エラー**：端末の通信を確認し再更新。公開JSONの停止・形式変更時はアプリの修正が必要になる場合があります。

## 開発・構成
JDK 17 / Gradle 8.11.1 / Android Gradle Plugin 8.9.2 / compileSdk 35 / targetSdk 35 / minSdk 26。
Gradle Wrapper一式を同梱。Actionsはテスト・lint・APK生成を順に実行し、失敗時はAPKを成功成果物として出しません。
ローカルでSDKがある場合：Windowsは `gradlew.bat testDebugUnitTest lintDebug assembleDebug`、他は `bash ./gradlew testDebugUnitTest lintDebug assembleDebug`。
12アカウント管理の拡張方針は `docs/ARCHITECTURE.md` を参照（今回未実装）。

読み取り先（この2つへのHTTPS GETのみ）：
- https://raw.githubusercontent.com/mochikibun/monst-hub-feed/main/public/campaigns.json
- https://raw.githubusercontent.com/mochikibun/monst-hub-feed/main/public/status.json

既存Discord BOTとmonst-hub-feedは変更しません。GitHubトークンや秘密情報は不要です。
実データのテスト用スナップショットは2026-10-18取得。テスト専用で本番の代替データには使いません。

## 参照した公式仕様
- Androidビルド環境：https://developer.android.com/build/releases/agp-8-9-0-release-notes
- GitHub成果物：https://docs.github.com/en/actions/concepts/workflows-and-actions/workflow-artifacts
- Gradle Wrapper：https://docs.gradle.org/8.11.1/userguide/gradle_wrapper.html
