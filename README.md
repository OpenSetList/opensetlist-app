# OpenSetlist

[![Licença](https://img.shields.io/badge/licen%C3%A7a-MIT-blue.svg)](LICENSE)

Aplicativo multiplataforma de **setlists e cifras** para músicos, construído com **Kotlin Multiplatform** + **Compose Multiplatform** + **SQLDelight**.

Organize suas músicas, monte setlists para shows/gigs, transponha cifras na hora e acompanhe o repertório — tudo offline, no Android, iOS e desktop.

## Intuito

O OpenSetlist nasce da necessidade de um músico que quer:

- **Ter o repertório em mãos** — letra + cifra (ChordPro) de todas as músicas, num só lugar e sem depender de internet;
- **Montar setlists por evento** — ordem das músicas, local, data e horário da gig;
- **Praticar sem perder o lugar** — rolagem automática, transposição de tom, busca dentro do texto e suporte a pedal Bluetooth;
- **Não ficar preso a um ecossistema** — funciona em Android, iOS e desktop, com backup e importação/exportação do seu conteúdo.

O projeto é livre e aberto: contribuições são bem-vindas.

## Funcionalidades

### Músicas
- Lista de todas as músicas com busca (título/artista) e ordenação (nome, artista, criação);
- Editor de cifras **ChordPro** completo (título, artista, tom, BPM, capo, duração, link do YouTube, tags);
- Excluir música pela lista, pelo visualizador ou pelo editor.

### Visualizador de cifras
- Transposição de tom (+/-);
- Tamanho da letra e ocultar acordes (para cantar junto);
- Rolagem automática com controle de velocidade;
- Busca dentro do texto com navegação entre ocorrências;
- Pinch-to-zoom em dispositivos touch;
- Suporte a **pedal Bluetooth** (próximo/anterior/play-pause);
- Alternar entre músicas com swipe horizontal (pager).

### Setlists
- Criar, renomear, editar dados da gig (data, local, horário) e excluir;
- Adicionar/remover músicas (com busca no modal) e **reordenar por arrastar**;
- Duração total calculada a partir da duração das músicas;
- Compartilhar/exportar setlist como arquivo JSON;
- Lista de setlists com busca, ordenação e ações por linha.

### Organização
- **Artistas** — renomear, excluir (com ou sem as músicas);
- **Tags** — criar, renomear, excluir e associar a músicas.

### Dados e backup
- **Exportar/importar backup completo** do banco (`.db`);
- **Exportar/importar músicas** em lote (JSON);
- **Importar setlist compartilhada** (JSON);
- **Importar backup do SetList Helper** (`.db`);
- **Importar/exportar `.pro`** (ChordPro);
- **Nuvem** — exportar/importar backup via seletor do sistema (Google Drive/Dropbox, SAF no Android, picker no desktop).

### Configurações
- **Modo escuro/claro** com persistência da escolha;
- Todas as ações de backup/importação/exportação centralizadas.

## Roadmap

### ✅ Já implementado
- [x] CRUD de músicas com busca e ordenação
- [x] Editor ChordPro (título, artista, tom, BPM, capo, duração, YouTube, tags)
- [x] Visualizador: transposição, rolagem automática, busca, ocultar acordes, pinch-to-zoom, pedal Bluetooth
- [x] CRUD de setlists com reordenação por arrastar e dados da gig
- [x] Artistas e tags
- [x] Backup completo (`.db`), exportação/importação de músicas e setlists (JSON)
- [x] Importação de backup do SetList Helper
- [x] Modo escuro persistente
- [x] Nuvem via seletor do sistema (export/import)

### 🚀 Previsto
- [ ] **Sincronização por conta** — login em conta cloud e sincronização automática entre dispositivos (OAuth real com Google Drive/Dropbox)
- [ ] **Obter cifras da internet** — busca e importação de cifras de APIs públicas de música
- [ ] Keystore de release própria + publicação na Play Store
- [ ] Publicação na App Store (iOS)
- [ ] Minificação com `proguard-rules.pro`
- [ ] Testes automatizados (parser ChordPro, transposer)

## Plataformas

| Plataforma | Status |
|---|---|
| Android | ✅ Suportada (minSdk 24, targetSdk 36) |
| Desktop (Windows/macOS/Linux) | ✅ Suportada (JVM) |
| iOS | ✅ Compilável (framework `ComposeApp`) |

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin 2.0.21 |
| UI | Compose Multiplatform 1.6.11 (Material 3) |
| Persistência | SQLDelight 2.0.2 |
| Android | AGP 8.2.2, compileSdk 34, minSdk 24, targetSdk 36 |
| Build | Gradle 8.5, JDK 17 |
| Entrada iOS | `iosApp/` (Xcode) |

## Estrutura do projeto

```
composeApp/
├── src/
│   ├── commonMain/kotlin/com/opensetlist/app/
│   │   ├── App.kt                 # Estado global e roteamento
│   │   ├── AppStrings.kt          # Todas as strings de UI (pt-BR)
│   │   ├── data/                  # SongRepository, parser ChordPro, Transposer, etc.
│   │   ├── model/                 # Song, ChordProLine, ...
│   │   └── ui/
│   │       ├── components/        # ChordProView, SideDrawer, SortMenu, ...
│   │       ├── screens/           # SongList, Setlist, Editor, ChordViewer, Artists, Tags, Settings
│   │       └── theme/             # Tema Material 3 (claro/escuro)
│   ├── commonMain/sqldelight/com/opensetlist/app/data/db/
│   │   ├── AppDatabase.sq         # Schema + queries
│   │   └── migrations/            # Migrações 2.sqm / 3.sqm / 4.sqm
│   ├── androidMain/               # actuals (SharedPreferences, SAF, Bluetooth, ...)
│   ├── iosMain/                   # actuals (NSUserDefaults, SQLDelight native, ...)
│   └── desktopMain/               # actuals (java.util.prefs, JDBC SQLite, JFileChooser, ...)
├── build.gradle.kts
└── ...
iosApp/                            # Projeto Xcode
```

### Padrões importantes

- Código 100% em `commonMain`; cada plataforma fornece apenas implementações `actual` (`expect`/`actual`);
- `SongRepository` é a **única via de acesso ao banco**;
- **Todas** as strings de UI ficam em `AppStrings.kt` e são usadas como `AppStrings.nome`;
- Listas ordenáveis usam `SortMenu` + enums com labels vindos de `AppStrings`;
- Banco do desktop em `~/.opensetlist/setlist.db` (gerenciado pelo driver JDBC do SQLDelight — não criar o schema manualmente).

## Como rodar

### Requisitos
- JDK 17+
- Android Studio (para Android) ou Xcode (para iOS)

### Android (debug)
```bash
./gradlew :composeApp:assembleDebug
# ou, com o dispositivo/emulador conectado:
./gradlew :composeApp:installDebug
```

### Desktop
```bash
./gradlew :composeApp:packageUberJarForCurrentOS
java -jar composeApp/build/compose/jars/composeApp-linux-x64-*.jar
```

### iOS
Abra `iosApp/` no Xcode e rode, ou compile o framework:
```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

### Release Android
```bash
./gradlew :composeApp:assembleRelease
# APK: composeApp/build/outputs/apk/release/composeApp-release.apk
```

> ⚠️ A assinatura de release atual usa a keystore **debug** do Android SDK (`~/.android/debug.keystore`) — serve para testes, mas **não** para publicação na Play Store. Use uma keystore própria de release.

## Guia de contribuições

Obrigado pelo interesse em contribuir! Qualquer ajuda — correção de bug, nova funcionalidade, melhoria de UI, testes, documentação — é bem-vinda.

Veja o guia completo em **[CONTRIBUTING.md](CONTRIBUTING.md)**: setup do ambiente, fluxo de trabalho com branches, validação antes do PR, regras de código, estilo de commits e ideias para evoluir o projeto.

## Licença

Distribuído sob a licença [MIT](LICENSE).
