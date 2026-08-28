# AGENT.md

App de setlists/cifras multiplataforma (Kotlin Multiplatform + Compose Multiplatform + SQLDelight).

## Stack e versões (não mudar sem motivo)

- Kotlin 2.0.21, Compose Multiplatform 1.6.11, AGP 8.2.2, SQLDelight 2.0.2
- Gradle 8.5, JDK 17, compileSdk 34, minSdk 24, targetSdk 36
- Código 100% em `commonMain`; UI com Material 3

## Estrutura

- `composeApp/src/commonMain/kotlin/com/opensetlist/app/`
  - `App.kt` — estado global e roteamento (Scaffold + ModalNavigationDrawer), controla todo o fluxo
  - `AppStrings.kt` — TODAS as strings de UI centralizadas aqui (pt-BR); use `AppStrings.*` em vez de literais
  - `data/` — `SongRepository.kt` (toda a lógica de banco), `ChordProParser.kt`, `Transposer.kt`, `DurationUtils.kt` (parse/format de duração "3:45"), `DataTransfer.kt`, `SampleSongs.kt`, `Timestamps.kt` (`currentTimestampIso`/`currentTimestampCompact` via expect/actual)
  - `model/` — `Song.kt`, `ChordProLine.kt` (modelos)
  - `ui/screens/` — telas: SongList, SetlistList, Setlist, Editor, ChordViewer, Artists, Tags, FilteredSongList, Settings
  - `ui/components/` — `ChordProView.kt`, `SideDrawer.kt`, `SortMenu.kt`, `BackHandler.kt`
  - `ui/theme/Theme.kt` — Material 3 (dark/light)
- `composeApp/src/commonMain/sqldelight/com/opensetlist/app/data/db/` — `AppDatabase.sq` + `migrations/` (atual: 8 via 2.sqm/3.sqm/4.sqm/5.sqm/6.sqm/7.sqm)
- `androidMain/` / `iosMain/` / `desktopMain/` — apenas `actual`s e entry points (`MainActivity.kt`, `MainViewController.kt`, `Main.kt`)

## Plataforma desktop

- Target `jvm("desktop")` no `composeApp/build.gradle.kts`; entrada em `desktopMain/.../Main.kt` (`Window { App(...) }`)
- Banco em `~/.opensetlist/setlist.db`, criado/migrado via `JdbcSqliteDriver(url, schema = AppDatabase.Schema)` (factory de `app.cash.sqldelight:sqlite-driver`, que gerencia `PRAGMA user_version` automaticamente; não criar schema manualmente)
- actuals desktop: `BackHandler` no-op, `PedalEvents` no-op, `FileActions`/`BackupActions`/`SetlistHelperActions` com `JFileChooser`/JDBC
- Build desktop: `./gradlew :composeApp:packageUberJarForCurrentOS` (jar em `composeApp/build/compose/jars/`)
- Ajustes de UI desktop: dimensões de janela e `Window` em `Main.kt`

## Padrões expect/actual

`commonMain` declara `expect`; cada plataforma implementa `actual`:

- `data/DatabaseDriverFactory.kt`
- `data/Timestamps.kt` (data/hora atual — `SimpleDateFormat` no Android/desktop, `NSDateFormatter` no iOS)
- `data/FileActions.kt`, `data/BackupActions.kt`, `data/SetlistHelperActions.kt` (operam via `expect fun rememberXxx()`)
- `data/pedal/PedalEvents.kt` (bluetooth)
- `ui/components/BackHandler.kt` (botão voltar do Android)

## Build e validação

```bash
./gradlew :composeApp:assembleDebug        # Android (principal validação)
./gradlew :composeApp:packageUberJarForCurrentOS  # Desktop (jar em composeApp/build/compose/jars/)
./gradlew :composeApp:compileKotlinIosSimulatorArm64  # iOS (quando aplicável)
```

- Build Android com recompilação completa p/ confirmar mudanças: `./gradlew :composeApp:assembleDebug --rerun-tasks`
- Validação runtime: emulador/dispositivo Android via adb (instalar `:composeApp:installDebug`); desktop via `java -jar composeApp/build/compose/jars/composeApp-linux-x64-*.jar` (cria/usa `~/.opensetlist/setlist.db`)
- Sempre rodar o build após editar código (string errada/quebra de import quebra o build)

## Testes

- Testes unitários em `composeApp/src/commonTest/` (Kotlin Multiplatform, `kotlin.test`)
- Cobertura: `TransposerTest`, `DurationUtilsTest`, `ChordProParserTest`, `ChordProDirectivesTest`, `JsonParserTest`, `DataTransferTest` (parsing)
- Rodar: `./gradlew :composeApp:desktopTest` (JVM, rápido), `:composeApp:testDebugUnitTest` (Android), `:composeApp:iosSimulatorArm64Test` (iOS)
- `DataTransfer` usa `expect/actual` de Timestamps → build*/round-trip não testados em commonTest (apenas parsing/detecção)


## Release Android (R8 / ofuscação)

- `buildTypes.release`: `isMinifyEnabled = true` + `isShrinkResources = true` + `proguard-rules.pro` (`composeApp/proguard-rules.pro`)
- Regras mantidas: `MainActivity`, classes SQLDelight geradas (`com.opensetlist.app.data.db.**`), `-keepattributes *Annotation*, Signature, InnerClasses`, campos `volatile` de coroutines, `-dontwarn` p/ classes JVM ausentes
- APK release: `composeApp/build/outputs/apk/release/composeApp-release.apk` (~2 MB com R8); `mapping.txt` em `build/outputs/mapping/release/` p/ deobfuscar crashes
- Backup `.db` exportado com timestamp no nome: `setlist_backup_<aaaa-MM-dd_HH-mm-ss>.db`; JSON de backup tem `"createdAt"` ISO e `"version":4` (ids numéricos e `creationDate`/`lastEdit` epoch ms; `transpose` INT na música; `date` INT epoch ms na setlist; tabelas cadastrais `song`/`artist`/`tag`/`setlist` usam `id` INTEGER AUTOINCREMENT + `creation_date`/`last_edit`)
- Ao alterar dependências/kotlin: rodar `:composeApp:assembleRelease` para validar o R8 (e não só `assembleDebug`)

## Regras de código

- Não adicionar comentários ao código
- Seguir padrões existentes (Material3, Column/Row com `Modifier.padding(16.dp)`, `LazyColumn` com `items(key=...)`)
- Ao adicionar string de UI: criar em `AppStrings.kt` e usar `AppStrings.nome`
- Alterações no schema SQLDelight: nova `migration/N.sqm` + `schemaVersion` no `.sq`; nunca editar migração já publicada
- Mantém listas ordenáveis com `SortMenu` + enums (SongListSort, SetlistSort, ArtistSort, TagSort) cujos labels vêm de `AppStrings`
- `SongRepository` é a única via de acesso ao banco
