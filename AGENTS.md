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
  - `data/` — `SongRepository.kt` (toda a lógica de banco), `ChordProParser.kt`, `Transposer.kt`, `DurationUtils.kt` (parse/format de duração "3:45"), `DataTransfer.kt`, `SampleSongs.kt`, `Timestamps.kt` (`currentTimestampIso`/`currentTimestampCompact` via expect/actual), `JcArchive.kt`/`OslArchive.kt`/`ZipData.kt` (formatos `.jcarchive`/`.osl` como ZIP com `data.json`)
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
- APK release: `composeApp/build/outputs/apk/release/opensetlist-release.apk` (~2 MB com R8); `mapping.txt` em `build/outputs/mapping/release/` p/ deobfuscar crashes
- Backup `.db` exportado com timestamp no nome: `setlist_backup_<aaaa-MM-dd_HH-mm-ss>.db`; JSON de backup tem `"createdAt"` ISO e `"version":4` (ids numéricos e `creationDate`/`lastEdit` epoch ms; `transpose` INT na música; `date` INT epoch ms na setlist; tabelas cadastrais `song`/`artist`/`tag`/`setlist` usam `id` INTEGER AUTOINCREMENT + `creation_date`/`last_edit`)
- Ao alterar dependências/kotlin: rodar `:composeApp:assembleRelease` para validar o R8 (e não só `assembleDebug`)

## Lançamento de versão (sempre que lançar)

Checklist completo para cada release — seguir na ordem:

1. **Bump no código**: `appVersionName` e `versionCode` em `composeApp/build.gradle.kts` (Play exige `versionCode` crescente; tag segue `vX.Y.Z`)
2. **Validar**: `./gradlew :composeApp:assembleRelease` (gera `opensetlist-release.apk`) e rodar a suíte de testes (`desktopTest`/`testDebugUnitTest`)
3. **Commit de versão** no padrão `🔖 Versão X.Y.Z` na `main`; push para `upstream` (OpenSetList) e `origin` (fork)
4. **Tag + push**: `git tag vX.Y.Z` e push nos dois remotes
5. **GitHub Release** no upstream com o **"O que há de novo"** em pt-BR (título `OpenSetList X.Y.Z`; body com bullet list das mudanças para o usuário). Use `gh` — ex.: `gh api -X POST repos/OpenSetList/opensetlist-app/releases -f tag_name=vX.Y.Z ...` (ou `gh release edit`)
6. **README**: atualizar badge de versão (topo), seção de recursos alterados e tabela de Plataformas/Distribuição
7. **F-Droid metadata** (`fdroiddata` branch `com.opensetlist.app`): rodar `fdroid checkupdates --auto com.opensetlist.app` (geraria a nova entrada no `Builds` + `CurrentVersion{Code}`) e `fdroid rewritemeta` — sempre reproduzir com o ambiente da CI (fdroidserver master + `ruamel.yaml==0.18.0`; ver notas abaixo); commit + push → pipeline verde
8. **Play Store**: publicar o APK com o campo "O que há de novo" correspondente

### Notas do F-Droid (armadilhas já mapeadas)

- A CI roda `fdroid rewritemeta` com o **código master do fdroidserver** + `ruamel.yaml` ≥ 0.18, que faz *wrap* de scalars longos (ex.: `UpdateCheckData` vira 2 linhas). Não confiar em suposição de versão — o jeito seguro é reproduzir: clonar `gitlab.com/fdroid/fdroidserver` (master) e rodar com venv com `ruamel.yaml==0.18.0`
- `fdroid checkupdates --auto` exige repo git **limpo** (sem untracked) — mover artefatos fora antes de rodar
- `UpdateCheckData` aponta para `composeApp/build.gradle.kts` → atualizar os dois campos (`appVersionName` string e `versionCode` int) na mesma tag; a regex é `versionCode\s*=\s*(\d+)|.|[Vv]ersionName\s*=\s*"([^"]+)"`

### Notas do F-Droid: build reproduzível (`Binaries`/`AllowedAPKSigningKeys`)

- O `Binaries` do metadata aponta para `https://github.com/OpenSetList/opensetlist-app/releases/download/v%v/opensetlist-release.apk` — **cada versão listada em `Builds` precisa ter o APK binário publicado na release correspondente** (senão o `fdroid build` falha com 404 no download). Na primeira inclusão, manter só a versão mais recente no `Builds`.
- A CI builda com **JDK 21** (`openjdk-21-jdk-headless` + `update-alternatives`) e Gradle do wrapper (8.5). O `kotlin-tooling-metadata.json` que entra no APK grava o `jvmTarget` do **target desktop** do KMP, que herda a JDK do runtime (o android está fixado em 17) → **o binário da release deve ser compilado com JDK 21**, senão o `sameApk` da CI diverge. Confirmado: JDK17 vs JDK21 diferem só nesse arquivo; build com JDK21 é idêntico entre raízes diferentes.
- Passos para gerar/subir o binário de uma versão: worktree no commit da tag → renomear `~/.android/debug.keystore` (senão AGP assina e o output não é `*-unsigned.apk`) → `./gradlew :composeApp:assembleRelease --no-daemon --no-build-cache` com `JAVA_HOME` JDK 21 → assinar com **v2/v3 apenas** (nunca v1): `apksigner sign --v1-signing-enabled false --ks ~/.keystores/opensetlist-release.keystore --ks-key-alias opensetlist --ks-pass stdin` (senha via stdin) → **salvar com o nome exato `opensetlist-release.apk` antes do upload** (`gh release upload` usa o basename do arquivo como nome da asset) → `gh release upload <tag> --repo OpenSetList/opensetlist-app opensetlist-release.apk --clobber`
- **Importante — não assinar referência F-Droid com v1 (JAR)**: o `apksigcopier` copia a assinatura do binário de referência para o APK buildado e **recomprime** os arquivos v1 (`META-INF/MANIFEST.MF`, `*.SF`) no nível correto — mas esses arquivos vêm do **Java Deflater** do apksigner, cujo deflate não é reproduzível pelo `zlib` do Python (nem `diff -r` acusa: conteúdo é igual, só os bytes comprimidos diferem) → job falha com `signature copying failed: Unsupported compresslevel` (local) / `verification of APK with copied signature failed` + `Unexpected diff output:` vazio (CI). Sintoma clássico: o "Unexpected diff output" não exibe nenhuma linha. Teste local reproduzível: `common.verify_apks(reference, built, tmp)` com o fdroidserver + `apksigcopier` (pip) — referência v2/v3-only → `successfully verified`; com v1 → falha. `apksigner` por padrão adiciona v1 se o minSdk não for detectado; forçar `--v1-signing-enabled false`.
- Keystore própria em `~/.keystores/opensetlist-release.keystore` (alias `opensetlist`, senha em `opensetlist-release.password`, cert PEM em `opensetlist-release-cert.pem`) → **fazer backup offline**; `AllowedAPKSigningKeys` = `e7dbb3740f412252758ac7368028959d4744738de4f6b193613b27a9404047c5` (SHA-256 do cert via `apksigner verify --print-certs`)
- Job quebrado do CI com "codequality.json: no matching files / No files to upload" = **sintoma** de falha interna no `fdroid build` (o script aborta antes de gerar o `codequality.json`). Olhar o log do job, não o erro de upload.

## Regras de código

- Não adicionar comentários ao código
- Seguir padrões existentes (Material3, Column/Row com `Modifier.padding(16.dp)`, `LazyColumn` com `items(key=...)`)
- Ao adicionar string de UI: criar em `AppStrings.kt` e usar `AppStrings.nome`
- Alterações no schema SQLDelight: nova `migration/N.sqm` + `schemaVersion` no `.sq`; nunca editar migração já publicada
- Mantém listas ordenáveis com `SortMenu` + enums (SongListSort, SetlistSort, ArtistSort, TagSort) cujos labels vêm de `AppStrings`
- `SongRepository` é a única via de acesso ao banco
