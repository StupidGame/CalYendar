# CalYendar コード完全ガイド

## 1. このアプリがしていること

CalYendar は、次の 4 系統の情報を 1 つのカレンダー体験にまとめる Android アプリです。

- 家計の取引: 収入と支出
- 金融目標: いつまでにいくら必要か
- 手入力イベント: 通知付きの予定
- 外部カレンダーイベント: `.ics` / `webcal` / 日本の祝日

画面は大きく 3 つあります。

- 月カレンダー画面
- 日別の詳細画面
- 設定画面

内部構造は、`Compose UI -> ViewModel -> Repository -> Room Database / SharedPreferences / AlarmManager / AppWidget` という流れです。

---

## 2. 先に知っておくと読みやすい設計ルール

### 日付と金額の表現

- `month` は 0 始まりです。
- 例: 1 月は `0`、12 月は `11`
- これは `java.util.Calendar` と `LocalDate.of(year, month + 1, day)` の変換に合わせるためです。
- イベント時刻は `Long` の epoch millis で保存します。
- 収支や目標額は `Long` 円です。

### データの置き場所

- 取引、目標、手入力イベント、外部イベントは Room に保存します。
- 設定は `SharedPreferences` に保存します。
- 通知の実行は `AlarmManager` を使います。
- ウィジェットの更新は Room の変更監視から起動します。

### 重要な実装上の約束

- `Event.notificationMinutesBefore` は旧形式の通知列です。
- 新形式は `Event.notifications` のカンマ区切り文字列です。例: `"60,1440"`
- 外部カレンダーイベントは `ImportedEvent` として `VEvent` 丸ごと保存します。
- バックアップ対象は設定、手入力イベント、取引、目標です。
- `ImportedEvent` はバックアップの置換対象に含めていません。

---

## 3. 実行フローの全体像

### 起動時

1. `MainActivity` が `CalYendarTheme` でアプリ全体を描画します。
2. `CalYendarApp()` が Navigation、Drawer、ViewModel を初期化します。
3. `CalYendarApplication` が Database / Repository / SettingsStore を遅延生成します。
4. `CalYendarApplication.onCreate()` が通知チャンネルを作り、ウィジェット同期監視を開始します。
5. `CalendarViewModel.init` が当月を読み込み、日本の祝日取得を非同期開始します。

### 月カレンダー表示

1. `MainActivity` が `monthSelection` を持ちます。
2. `LaunchedEffect(monthSelection.year, monthSelection.month)` で `CalendarViewModel.loadMonth()` を呼びます。
3. `CalendarViewModel` が複数の `Flow` を `combine()` します。
4. `CalendarMonthUiStateFactory.create()` が各日の `DayState` を計算します。
5. `CalendarScreen` が `LazyVerticalGrid` で日セルを表示します。

### 日別詳細表示

1. カレンダーのセルを押すと `detail/{year}/{month}/{day}` に遷移します。
2. `RealDetailScreen()` が `DetailViewModel` を生成します。
3. `DetailViewModel.uiState` が取引、目標、イベント、外部イベントを結合します。
4. `DetailScreen` が所持金カード、目標サマリー、イベント一覧、取引一覧を表示します。

### 予定通知

1. `DetailScreen` から `AddEventDialog` で予定を保存します。
2. `DetailViewModel.upsertAndSchedule()` が DB 保存後に通知予約します。
3. `EventNotificationManager` が通知ごとに `PendingIntent` を作ります。
4. 指定時刻になると `EventNotificationReceiver` が通知を表示します。

### ウィジェット更新

1. `BalanceGoalWidgetSyncManager` が Room テーブルの invalidation を監視します。
2. `transactions` または `financial_goals` が変わると `refreshAll()` を呼びます。
3. `BalanceGoalWidgetProvider` が Repository から当日のスナップショットを作ります。
4. `RemoteViews` を使ってホーム画面ウィジェットを更新します。

---

## 4. このアプリで使っている Kotlin / Compose / Room 文法

### `data class`

用途: 値の入れ物です。`equals`、`copy`、`toString` が自動生成されます。

このアプリでの例:

- `Transaction`
- `Event`
- `FinancialGoal`
- `CalendarUiState`

よくある使い方:

```kotlin
val updated = oldEvent.copy(title = "新しいタイトル")
```

### `object`

用途: シングルトンです。インスタンスを 1 つだけ持ちます。

このアプリでの例:

- `FinancialCalculator`
- `RecurringEventGenerator`
- `CalendarMonthUiStateFactory`
- `CsvBackupCodec`

### `enum class`

用途: 決まった候補の集合です。

このアプリでの例:

- `TransactionType`
- `EventRepeatType`

### `sealed interface`

用途: 分岐可能な状態を限定できます。

このアプリでの例:

- `DetailDialogState`
- 追加ダイアログ、編集ダイアログの種類を型安全に切り替えています。

### 拡張関数

用途: 既存クラスに後付けで関数を生やします。

このアプリでの例:

- `Event.toLocalDate()`
- `FinancialGoal.toLocalDate()`
- `List<ImportedEvent>.groupByStartLocalDate()`

### `Flow` と `StateFlow`

用途: 値の変化を継続的に流します。

このアプリでの例:

- `CalYendarDao` は Room の問い合わせ結果を `Flow` で返します。
- ViewModel は `MutableStateFlow` と `asStateFlow()` で UI 状態を公開します。
- `collectAsState()` で Compose から購読します。

### `combine(...)`

用途: 複数の `Flow` をまとめて 1 つの状態にします。

このアプリでの例:

- `CalendarViewModel.loadMonth()`
- `DetailViewModel.uiState`

### コルーチン

用途: 非同期処理です。

このアプリでの例:

- `viewModelScope.launch { ... }`
- `withContext(Dispatchers.IO) { ... }`
- ネットワーク、DB、ファイル入出力をメインスレッド外で処理します。

### Compose の基本文法

- `@Composable`: UI 関数
- `remember { mutableStateOf(...) }`: 画面内ローカル状態
- `by`: 委譲構文。`state.value` を省略しやすくします。
- `LaunchedEffect(key)`: key 変化時の副作用
- `Scaffold`: 画面の基本骨格
- `NavHost` / `composable`: 画面遷移定義
- `LazyColumn` / `LazyVerticalGrid`: 遅延描画リスト

### Room の注釈

- `@Entity`: テーブル
- `@Dao`: SQL 窓口
- `@Query`: SQL 文
- `@Upsert`: Insert or Update
- `@Delete`: Delete
- `@Database`: DB 定義
- `@TypeConverter`: 非プリミティブ型の変換

### Kotlin の頻出イディオム

- Elvis 演算子: `a ?: b`
- スコープ関数: `let`, `apply`, `runCatching`
- コレクション操作: `filter`, `map`, `groupBy`, `sumOf`, `sortedBy`
- `buildList`, `buildString`: 可読性の高い構築
- `generateSequence`: 繰り返しイベント生成

---

## 5. ファイル構成と役割

### 5.1 ビルドとプロジェクト設定

#### `settings.gradle.kts`

- Gradle plugin と依存ライブラリの取得先を定義します。
- `google()`, `mavenCentral()`, `jitpack` を登録しています。
- `include(":app")` で Android アプリモジュールを読み込みます。

#### `build.gradle.kts`

- ルートプロジェクトの plugin 宣言だけを持ちます。
- `apply false` で各モジュールから使えるようにしています。

#### `gradle/libs.versions.toml`

- 依存バージョン管理の中枢です。
- AGP、Kotlin、Compose BOM、Room、Activity Compose などのバージョンをここで統一しています。

#### `app/build.gradle.kts`

- Android アプリ本体のビルド設定です。
- `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
- Java / Kotlin は 17
- Compose を有効化
- Room 用に `ksp` を使用
- 主な依存:
  - Compose Material3
  - Navigation Compose
  - Room
  - biweekly / ical4j
  - OkHttp
  - lifecycle-viewmodel-compose
- 署名設定は環境変数から読みます。
- `archivesName` で APK/AAB の出力名を整えます。

---

### 5.2 Android エントリポイントと宣言ファイル

#### `app/src/main/AndroidManifest.xml`

- 必要な権限を宣言します。
  - `POST_NOTIFICATIONS`
  - `SCHEDULE_EXACT_ALARM`
  - `USE_EXACT_ALARM`
  - `INTERNET`
- `CalYendarApplication` をアプリケーションクラスに設定しています。
- `EventNotificationReceiver` を登録しています。
- `BalanceGoalWidgetProvider` をウィジェット受信先として登録しています。
- `MainActivity` をランチャー Activity にしています。

#### `CalYendarApplication.kt`

- アプリ全体で共有する依存を遅延生成します。
  - `database`
  - `repository`
  - `appSettingsStore`
- `onCreate()` で通知チャンネル作成とウィジェット同期開始を行います。
- 依存注入フレームワークは使わず、`Application` 経由で取り回しています。

#### `MainActivity.kt`

- 画面 UI の最上位です。
- `AppRoute` で画面ルートを管理します。
- `CalendarMonthSelection` と `DetailDateSelection` が日付移動ロジックを担当します。
- `CalYendarApp()` の主な役割:
  - `NavController` の作成
  - Drawer の管理
  - 通知権限要求
  - `CalendarViewModel` / `SettingsViewModel` の生成
  - 月変更時に `calendarViewModel.loadMonth()` を発火
  - TopAppBar タイトルと左右移動ボタン制御
  - `NavHost` に 3 画面を登録
- 文法上の読みどころ:
  - `rememberNavController()`
  - `currentBackStackEntryAsState()`
  - `LaunchedEffect`
  - `viewModel(factory = ...)`
  - `when` による画面別アクション分岐

---

### 5.3 画面コンポーザブル

#### `CalendarScreen.kt`

- 月表示画面です。
- `CalendarViewModel.uiState` を `collectAsState()` で監視します。
- `WeekdaysHeader()` が曜日見出しを描画します。
- `Calendar.getInstance()` で月初の曜日を計算し、先頭の空セル数を決めます。
- `LazyVerticalGrid(columns = GridCells.Fixed(7))` で 7 列カレンダーを作ります。
- 各日セルは `DayCell` に委譲します。
- グリッドの下に `MonthlyGoalCard` を表示します。

#### `DetailScreen.kt`

- 指定日の詳細画面です。
- `DetailDialogState` が現在開いているダイアログ種類を保持します。
- `deleteTarget` によって削除確認ダイアログの対象を切り替えます。
- `saveEvent` ラムダで Event の組み立てと繰り返し保存呼び出しを共通化しています。
- 表示内容:
  - 日付見出し
  - `CurrentBalanceCard`
  - `SummaryCard`
  - 祝日一覧
  - 通常イベント一覧
  - 取り込みイベント一覧
  - 取引一覧
- FAB から BottomSheet を開き、目標、収入、支出、イベントの追加を選びます。
- `RealDetailScreen()` は `Application` から依存を取り、`DetailViewModel` を作るラッパーです。

#### `AddEntryDialog.kt`

- 目標追加と取引追加のダイアログです。
- `remember { mutableStateOf(...) }` で入力状態を保持します。
- 数値入力は `filter { it.isDigit() }` で数字だけに制限します。
- `amount.toLongOrNull() != null` と `name.isNotBlank()` で保存ボタン有効化を制御します。

#### `AddEventDialog.kt`

- もっとも機能が多い入力ダイアログです。
- 扱う状態:
  - タイトル
  - 開始日と終了日
  - 開始時刻と終了時刻
  - タイムゾーン
  - 複数通知
  - 休日フラグ
  - 繰り返し種別
  - 繰り返し終了日
  - 曜日指定
- `formatNotificationLabel()` は分単位の通知設定を表示文字列へ変換します。
- `ZoneId.toJapaneseLabel()` はタイムゾーン名と時差を日本語表示します。
- 日付選択は `DatePickerDialog`、時刻選択は `TimePicker` を使います。
- 通知は標準候補かカスタム値から `List<Long>` に積みます。
- 繰り返しは `EventRepeatType` を選び、必要なら `repeatDays` と `repeatUntil` を使います。
- 保存時は `onConfirm(...)` に全入力値をまとめて渡します。

#### `SettingsScreen.kt`

- 設定とデータ入出力の画面です。
- `rememberLauncherForActivityResult(...)` を 3 つ持ちます。
  - `.ics` 取り込み
  - CSV バックアップ書き出し
  - CSV バックアップ読み込み
- WebCal URL の編集は `settingsViewModel.updateWebCalUrl()` に接続されています。
- デフォルト通知は `Switch` で ON/OFF します。
- 結果表示は `Toast` です。

---

### 5.4 UI 部品

#### `ui/components/DayCell.kt`

- 月カレンダーの各日セルです。
- `DayState` を受け取り、残高、イベント有無、祝日、目標進捗を 1 枚に圧縮表示します。
- 背景色は `predictionDiff` と `goal.amount` から `getGradientColor()` で決めます。
- 当日は枠線を太くします。
- 祝日または日曜は赤、土曜は青です。
- 取引は上段に `収+` / `支-` 形式で表示します。
- 目標との差分は下段に `余` または `不` で表示します。
- `AutoSizeAnnotatedText()` と `AutoSizeText()` は文字が溢れる間だけフォントサイズを縮める自前処理です。

#### `ui/components/CommonCards.kt`

- 詳細画面と月画面のカード UI を集めたファイルです。
- `SummaryCard`
  - 次の目標までの達成率と差額を表示
  - `combinedClickable` で通常タップと長押しを分けます
- `TransactionCard`
  - 収入と支出でアイコン、色、符号を変えます
- `EventCard`
  - 手入力イベントを表示
- `IcalEventCard`
  - 取り込みイベントを表示
- `MonthlyGoalCard`
  - 月内目標の有無で表示内容を大きく切り替えます
  - 目標がなければ現在残高カード
  - 全目標達成済みなら達成済みカード
  - 進行中目標があれば合計額と差額カード
- `CurrentBalanceCard`
  - その日時点の所持金を強調表示します

---

### 5.5 テーマ

#### `ui/theme/Color.kt`

- テンプレート由来の基本色定義です。
- 実際の UI はこれ以外の直接色指定も多く使っています。

#### `ui/theme/Type.kt`

- Material3 の Typography を 1 つだけ上書きしています。
- `bodyLarge` を `FontFamily.Default` で定義しています。

#### `ui/theme/Theme.kt`

- `CalYendarTheme()` がアプリ全体の MaterialTheme を提供します。
- Android 12 以上では dynamic color を使います。
- それ未満では `DarkColorScheme` / `LightColorScheme` を使います。
- `SideEffect` でステータスバー色を更新します。

---

### 5.6 データモデル

#### `data/Transaction.kt`

- `transactions` テーブルの Entity
- 主な列:
  - `id`
  - `year`, `month`, `day`
  - `type`
  - `name`
  - `amount`
  - `details`
- `TransactionType` は `GOAL`, `EXPENSE`, `INCOME`
- 実運用では主に `EXPENSE`, `INCOME` を使っています。

#### `data/Event.kt`

- `events` テーブルの Entity
- `startTime` と `endTime` は epoch millis
- `seriesId` は繰り返しイベントのまとまり識別子
- `notifications` は複数通知の保存文字列

#### `data/FinancialGoal.kt`

- `financial_goals` テーブルの Entity
- その日までに必要な金額を保存します。

#### `data/ImportedEvent.kt`

- `imported_events` テーブルの Entity
- `VEvent` を `TypeConverter` で文字列化して保存します。
- `isHoliday` で祝日か通常購読イベントかを区別します。

#### `data/AppSettings.kt`

- 共有設定の値オブジェクトです。
- `defaultNotificationMinutes` は保存値から通知分数リストを導出する計算プロパティです。

#### `data/CuryendarItem.kt`

- `Goal`, `Expense`, `Income`, `CalYendarItem` を定義しています。
- ただし現在のメインロジックでは使われていません。
- 旧設計の名残、または将来用の型として見てよいファイルです。

---

### 5.7 データアクセス

#### `data/CalYendarDao.kt`

- Room の SQL インターフェースです。
- 主な責務:
  - 取引の CRUD
  - 目標の CRUD
  - イベントの CRUD
  - 外部イベントの CRUD
  - 各種一覧取得
- 戻り値に `Flow<List<...>>` を使うことで、DB 変更が UI に伝播します。
- クエリの読みどころ:
  - `getTransactionsUpTo(...)` は指定月の前まで
  - `getTransactionsUpToDate(...)` は指定日まで
  - `getLatestGoalUpToDate(...)` はその日までの最新目標
  - `clearImportedEvents()` は `isHoliday = 0` のみ消す
  - `deleteHolidays()` は `isHoliday = 1` のみ消す

#### `data/CalYendarDatabase.kt`

- RoomDatabase 本体です。
- `@Database(... version = 1)` で対象 Entity を登録します。
- `getDatabase(context)` はダブルチェック風の singleton 初期化です。
- `fallbackToDestructiveMigration()` を使っているため、将来 schema 変更時に既存データを消して再作成します。

#### `VEventConverter`

- `VEvent -> String`
- `String -> VEvent`
- `biweekly` を使って iCalendar 形式文字列へ変換します。

---

### 5.8 Repository 層

#### `data/CalYendarRepository.kt`

- ViewModel とデータソースの橋渡し層です。
- DAO への単純委譲だけでなく、次の処理もまとめています。
  - 祝日 API 取得
  - `.ics` 解析
  - `webcal` 読み込み
  - バックアップ復元時の全置換

主な関数:

- `getTransactionsForDate(...)`
- `getTransactionsUpToDate(...)`
- `getTransactionsForMonth(...)`
- `getEventsForDate(...)`
- `getEventsForMonth(...)`
- `getImportedEvents()`
- `replaceUserData(...)`
- `fetchJapaneseHolidays()`
- `importIcs(...)`
- `importWebcal(...)`

実装の読みどころ:

- `withContext(Dispatchers.IO)` で重い処理を IO スレッドに寄せています。
- 祝日取得は `https://holidays-jp.github.io/api/v1/date.json`
- `importWebcal()` は URL の `webcal` を `https` に置換してから OkHttp で取得します。
- 解析した予定はすべて `ImportedEvent(event = event, isHoliday = false)` に変換します。

---

### 5.9 ViewModel 層

#### `data/CalendarViewModel.kt`

- 月画面の状態を持ちます。
- `CalendarUiState` が月画面全体の表示用データです。
- `DayState` が 1 日セル用の表示データです。
- `init` で当月読み込みと祝日取得を行います。
- `loadMonth(year, month)` の流れ:
  1. 以前の読み込み Job をキャンセル
  2. 6 本の Flow を `combine`
  3. `CalendarMonthUiStateFactory.create()` に渡す
  4. `_uiState.value` を更新
- `importIcs()` と `importWebcal()` は設定画面から呼ばれる窓口です。

#### `data/DetailViewModel.kt`

- 日別詳細画面の状態を持ちます。
- `DetailUiState` はその日を表示するための集約データです。
- `uiState` は 5 本の Flow を `combine` して作られます。
- 計算内容:
  - 当日までの残高
  - 達成済み目標を引いた残高
  - 次の目標との差分
  - 当日の手入力イベント
  - 当日の取引
  - 当日の外部イベント
- `upsertEventWithRepeat(...)` は `RecurringEventGenerator.generate()` を呼びます。
- `upsertAndSchedule(...)` は保存後に通知予約する共通内部関数です。

#### `data/SettingsViewModel.kt`

- 設定画面の状態を持ちます。
- `SettingsUiState` と `AppSettings` の相互変換があります。
- `settingsStore.settingsFlow.collect` で設定永続値を UI 状態へ反映します。
- `persist { ... }` が設定更新の共通口です。
- `exportCsv(...)`
  - 現在の設定、イベント、取引、目標を取得
  - `CsvBackupCodec.encode()` で 1 本の CSV 文字列へ変換
  - ContentResolver の出力先へ書き込み
- `importCsv(...)`
  - CSV を読み込み
  - `CsvBackupCodec.decode()` で復元
  - 既存イベント通知を一旦キャンセル
  - DB を置換
  - 設定を保存
  - 復元イベント通知を再登録
  - 失敗時は元の通知を戻す

---

### 5.10 計算ロジックと補助関数

#### `data/FinancialCalculator.kt`

- 金額計算の純粋関数群です。
- `calculateDailyBalance(transactions)`
  - 収入を足し、支出を引きます
- `calculateBalanceAfterCompletedGoals(...)`
  - 指定日以前の目標金額を現在残高から差し引きます
- `calculatePrediction(...)`
  - 今後最初の目標を探します
  - その目標より前の目標合計を求めます
  - `currentBalance - totalPriorGoalCost` を比較用残高として返します
- `PredictionResult` には差額、次目標、累積目標コストが入ります。

#### `data/GoalComparisonSnapshot.kt`

- ウィジェット用に使う簡易スナップショットです。
- `GoalComparisonSnapshotCalculator.calculate(...)` が純粋計算を担当します。
- `CalYendarRepository.loadGoalComparisonSnapshot(...)` は Repository 拡張関数です。

#### `data/CalendarMonthUiStateFactory.kt`

- 月画面の表示状態をまとめて構築する純粋関数ファクトリです。
- 主な仕事:
  - 取引を日ごとにグループ化
  - イベントを日ごとにグループ化
  - 外部イベントを `LocalDate` 単位でグループ化
  - 月初以前の残高から日ごとの running balance を作る
  - 各日について目標予測差分を出す
  - 月末までの総残高、当月目標、使用可能額を算出する
- `shouldDisplayGoal()` は「今日以降かつ目標日以前」の日にだけ目標を見せる制御です。

#### `data/RecurringEventGenerator.kt`

- 繰り返しイベント生成器です。
- `EventRepeatType`
  - `NONE`
  - `DAILY`
  - `WEEKLY`
  - `WEEKDAY_SELECTION`
- `generate(...)` の流れ:
  1. 繰り返しなしなら元イベントだけ返す
  2. 終了日が開始日前なら元イベントだけ返す
  3. `generateSequence(startDate)` で日付列を作る
  4. repeat 種別に応じて filter
  5. 各日へ `copy(...)` で Event を複製
  6. 同一 `seriesId` を付与
- 先頭イベントだけ既存 `id` を保ち、追加分は `id = 0` です。

#### `data/CalendarDateExtensions.kt`

- 日付変換の拡張関数群です。
- `ImportedEvent.toStartLocalDate()` は `VEvent` の開始日を `LocalDate` 化します。
- `filterByStartLocalDate()` と `groupByStartLocalDate()` は外部イベントの紐付けに使います。

#### `data/CsvBackupCodec.kt`

- CSV バックアップのエンコード / デコードを担当します。
- 1 つの CSV に複数レコード種別を混在させています。
  - `META`
  - `SETTING`
  - `EVENT`
  - `TRANSACTION`
  - `GOAL`
- `encode(...)`
  - UTF-8 BOM を先頭に付与
  - ヘッダ行を書き出し
  - 設定、イベント、取引、目標を順に書き出し
- `decode(...)`
  - CSV を自前パーサで分解
  - ヘッダ検証
  - 各行をレコード種別で分岐して復元
- 文字列エスケープ、引用符、改行入りフィールドに対応しています。

---

### 5.11 通知

#### `utils/EventNotificationManager.kt`

- Event から Android 通知予約を行う管理クラスです。
- `AlarmManager` を使います。
- 複数通知に対応しています。
- 通知時刻ごとに `requestCode = event.id * 100 + index` を作り、同一イベントの複数通知を区別します。
- Android 12 以上では `canScheduleExactAlarms()` を確認し、不可なら予約しません。
- `cancelEventNotification(...)` は旧形式と新形式の両方を打ち消せるようにしています。

#### `EventNotificationReceiver.kt`

- Alarm 発火時に通知を組み立てて表示する `BroadcastReceiver`
- Intent extra:
  - `event_title`
  - `event_id`
- 通知チャンネルは `"EVENT_REMINDERS"` を使います。

---

### 5.12 ウィジェット

#### `widget/BalanceGoalWidgetProvider.kt`

- ホーム画面ウィジェットの本体です。
- `onUpdate()` と `onReceive()` から非同期更新をキックします。
- `refreshActions` に日付変更やタイムゾーン変更も含めています。
- `updateWidgets(...)`
  - Repository から `GoalComparisonSnapshot` を取得
  - `RemoteViews` を生成
  - 全ウィジェットへ反映
- クリックで `MainActivity` を開く `PendingIntent` を設定しています。

#### `widget/BalanceGoalWidgetSyncManager.kt`

- Room の更新監視役です。
- `InvalidationTracker.Observer("transactions", "financial_goals")` を登録します。
- 対象テーブルが変わったとき、ウィジェットが存在すれば `refreshAll()` を呼びます。

---

### 5.13 リソース

#### `res/values/strings.xml`

- UI 表示文言、通知文言、ウィジェット文言が入っています。

#### `res/values/colors.xml`

- テンプレート由来の旧色定義です。
- Compose 側では `ui/theme/Color.kt` や直接 `Color(...)` を使うことが多いです。

#### `res/values/themes.xml`

- XML テーマ定義です。
- 親は `Theme.Material3.DayNight.NoActionBar`

#### `res/layout/widget_balance_goal.xml`

- ウィジェットの `RemoteViews` レイアウトです。
- ルートは `LinearLayout`
- 残高と次目標を表示します。

#### `res/xml/balance_goal_widget_info.xml`

- ウィジェットのサイズや更新間隔を定義します。
- `updatePeriodMillis = 1800000` で 30 分間隔更新です。

#### `res/drawable/widget_balance_goal_background.xml`

- ウィジェット全体背景の shape

#### `res/drawable/widget_balance_goal_surface.xml`

- ウィジェット内カード面の shape

#### `res/xml/backup_rules.xml`

- Android Auto Backup の雛形です。
- 今はサンプル状態で、明示的 include/exclude は未設定です。

#### `res/xml/data_extraction_rules.xml`

- Android 12 以降のバックアップ / 移行制御 XML の雛形です。
- 今はサンプル状態で、明示的制御は未設定です。

#### そのほかの画像系リソース

- `calyendar_logo.svg`
- `drawable/calyendar_logo.xml`
- `drawable/ic_launcher*.xml`
- `mipmap-*`

これらは主にアイコンやランチャー表示用で、アプリロジックは持ちません。

---

### 5.14 テスト

#### `FinancialCalculatorTest.kt`

- 達成済み目標を残高から引く計算が正しいか確認します。

#### `RecurringEventGeneratorTest.kt`

- 曜日指定の繰り返し生成
- `seriesId` 共有
- 時刻保持
- 通知文字列保持

#### `CsvBackupCodecTest.kt`

- カンマ、引用符、改行を含むデータでも round trip できることを確認します。

#### `GoalComparisonSnapshotCalculatorTest.kt`

- 次目標がない場合とある場合で比較残高が期待通りかを確認します。

#### `CalendarMonthUiStateFactoryTest.kt`

- 日ごとの running balance
- 祝日フラグ
- 目標表示
- prediction 差分

#### `DatabaseTest.kt`

- Room の in-memory DB に取引を入れて取り出せるかを確認します。

#### `ExampleUnitTest.kt` / `ExampleInstrumentedTest.kt`

- Android Studio テンプレート由来の基本テストです。

---

## 6. よく使うデータフローを具体的に追う

### 6.1 月画面の 1 セルが表示されるまで

1. `MainActivity` が対象年月を `monthSelection` に保持
2. `CalendarViewModel.loadMonth(year, month)` 実行
3. Repository から次を Flow 取得
   - 今日までの取引
   - 月初以前の取引
   - 当月取引
   - 当月手入力イベント
   - 全目標
   - 全外部イベント
4. `CalendarMonthUiStateFactory.create()` が `DayState` を作成
5. `CalendarScreen` が `DayCell` を並べる

### 6.2 予定保存から通知予約まで

1. `AddEventDialog` で入力
2. `DetailScreen.saveEvent` が epoch millis の `Event` を構築
3. `DetailViewModel.upsertEventWithRepeat()` を呼ぶ
4. `RecurringEventGenerator.generate()` が複数 Event を返す
5. `repository.upsertEvent()` で保存
6. `EventNotificationManager.scheduleEventNotification()` で Alarm 登録

### 6.3 バックアップ復元

1. `SettingsScreen` でファイル選択
2. `SettingsViewModel.importCsv()` 実行
3. `CsvBackupCodec.decode()` で `CsvBackupData` へ復元
4. 既存イベント通知をキャンセル
5. `repository.replaceUserData()` で transactions / events / goals を全置換
6. `AppSettingsStore.updateSettings()` で設定反映
7. 復元イベント通知を再登録

### 6.4 WebCal 取り込み

1. `SettingsScreen` から URL 入力
2. `CalendarViewModel.importWebcal(url)` 実行
3. Repository が `webcal` を `https` に置換
4. OkHttp で取得
5. `Biweekly.parse(...)` で iCalendar 解析
6. `ImportedEvent` 一覧として保存

---

## 7. 読む順番のおすすめ

### 初見で全体を掴みたいとき

1. `MainActivity.kt`
2. `CalendarViewModel.kt`
3. `CalendarMonthUiStateFactory.kt`
4. `DetailViewModel.kt`
5. `CalYendarRepository.kt`
6. `CalYendarDao.kt`
7. `CalYendarDatabase.kt`

### UI だけ追いたいとき

1. `CalendarScreen.kt`
2. `DetailScreen.kt`
3. `AddEventDialog.kt`
4. `AddEntryDialog.kt`
5. `ui/components/*.kt`

### データ保存だけ追いたいとき

1. `Transaction.kt`, `Event.kt`, `FinancialGoal.kt`, `ImportedEvent.kt`
2. `CalYendarDao.kt`
3. `CalYendarDatabase.kt`
4. `CalYendarRepository.kt`

### 通知とウィジェットを追いたいとき

1. `EventNotificationManager.kt`
2. `EventNotificationReceiver.kt`
3. `BalanceGoalWidgetProvider.kt`
4. `BalanceGoalWidgetSyncManager.kt`

---

## 8. このコードベースの特徴と注意点

- 月が 0 始まりなので、`LocalDate` と相互変換する箇所は `month + 1` を必ず確認する必要があります。
- 外部イベントは `ImportedEvent` として手入力イベントと別管理です。
- 祝日も `ImportedEvent` として保持され、`isHoliday` で見分けます。
- バックアップは imported events を置換しません。購読カレンダーは残る設計です。
- `fallbackToDestructiveMigration()` を使っているため、DB version を上げると schema migration 未実装時にデータ消去が起こります。
- `CuryendarItem.kt` は現行ロジックから外れています。
- テーマは Compose Material3 を使いつつ、UI 部品側で色を直接指定している部分も多いです。

---

## 9. 1 行まとめ

CalYendar は、Compose で作った月次家計カレンダー UI に、Room の永続化、Flow による状態反映、biweekly による iCalendar 取り込み、AlarmManager の通知、AppWidget の残高表示を組み合わせたアプリです。
