# OpenSetlist

[![Licença](https://img.shields.io/badge/licen%C3%A7a-MIT-blue.svg)](LICENSE)
[![Versão](https://img.shields.io/badge/vers%C3%A3o-1.2.0-blue.svg)](https://github.com/OpenSetList/opensetlist-app/releases)

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
- **Voltar e retomar** — ao voltar de uma música, a lista lembra o termo de busca, a ordenação e a posição da rolagem;
- Editor de cifras **ChordPro** completo (título, artista, tom, BPM, capo, duração, link do YouTube, tags);
- Excluir música pela lista, pelo visualizador ou pelo editor;
- **Buscar e importar cifras da internet** — busca por título (ou URL colada) em **Ultimate Guitar** e **CifraClub**, com prévia da cifra, conversão automática para ChordPro e importação direta na biblioteca;
- **Importar cifras de link compartilhado** (Android) — "compartilhar" um link de cifra do navegador para o OpenSetlist importa a música direto na biblioteca.

### Visualizador de cifras
- **Visualizador com memória** — a posição da rolagem é lembrada ao sair e voltar para a cifra;
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
- Compartilhar/exportar setlist no formato **OpenSetList** (`.osl` — arquivo compactado que contém os dados da setlist) ou **JustChords** (`.chopro`/`.jcarchive`) — escolha o formato na lista de setlists ou no topo da setlist aberta;
- Lista de setlists com busca, ordenação e ações por linha.

### Organização
- **Artistas** — renomear, excluir (com ou sem as músicas);
- **Tags** — criar, renomear, excluir e associar a músicas.

### Dados e backup
- **Exportar/importar backup completo** do banco (`.db`);
- **Exportar/importar músicas** em lote (JSON);
- **Arquivos `.osl` compactados** — as exportações OpenSetList agora são arquivos ZIP (menores e mais confiáveis ao compartilhar); os arquivos `.osl` antigos, em texto puro (JSON), continuam sendo importados;
- **Importar setlist compartilhada** (`.osl`) — reimportar uma setlist com o mesmo nome **atualiza** a existente, sem duplicar setlists nem músicas;
- **Importar do JustChords** (`.chopro` e `.jcarchive`) — lê os arquivos exportados pelo app JustChords (texto ou backup), usando o nome do arquivo como nome do setlist e cada `{new_song}` como uma música na ordem; cria/atualiza o setlist e as músicas **sem duplicar** as existentes. As exportações do OpenSetlist `.chopro`/`.jcarchive` são **compatíveis com o JustChords** (reimportáveis no app dele). Arquivos recebidos via "compartilhar/abrir com…" também são importados diretamente;
- **Importar backup do SetList Helper** (`.db`) — músicas, setlists, tags/gêneros, youtube, compasso, bpm, duração e observações; atualiza os dados existentes sem duplicar;
- **Importar/exportar `.pro`** (ChordPro);
- **Nuvem** — exportar/importar backup via seletor do sistema (Google Drive/Dropbox, SAF no Android, picker no desktop).

As importações (backup, músicas, setlists, SetList Helper e JustChords) mostram uma tela de **log** com o resultado por item e um resumo final.

### Configurações
- **Modo escuro/claro** com persistência da escolha;
- **Manter tela acesa** por contexto (visualização de música, playlist ou o tempo todo — marcar "o tempo todo" desmarca as outras duas);
- Todas as ações de backup/importação/exportação centralizadas.

## Roadmap

### ✅ Já implementado
- [x] CRUD de músicas com busca e ordenação
- [x] Editor ChordPro (título, artista, tom, compasso, BPM, capo, duração, YouTube, tags)
- [x] Visualizador: transposição, rolagem automática, ocultar acordes, pinch-to-zoom, pedal Bluetooth, menu flutuante (compartilhar/editar), tela cheia no toque central
- [x] CRUD de setlists com reordenação por arrastar e dados da gig
- [x] Artistas e tags
- [x] Backup completo (`.db`), exportação/importação de músicas e setlists (JSON)
- [x] Importação de setlist do JustChords (`.chopro`) e compartilhamento de setlist nos formatos OpenSetList/JustChords, sem duplicar músicas nem setlists
- [x] Exportação de arquivos `.osl` compactados (ZIP) e `.jcarchive` compatíveis com a reimportação no JustChords
- [x] Memória de rolagem, busca e ordenação ao voltar de uma cifra/setlist
- [x] Importação de backup do SetList Helper (músicas, setlists, tags, gêneros, youtube, compasso, bpm, duração, observações) com atualização sem duplicar
- [x] Modo escuro persistente
- [x] Manter tela acesa por contexto (visualização de música, playlist ou o tempo todo)
- [x] Nuvem via seletor do sistema (export/import)
- [x] Minificação/ofuscação Android (R8 + `proguard-rules.pro`) e backup com timestamp no nome
- [x] Obter cifras da internet — busca/importação de cifras de **Ultimate Guitar e CifraClub** (prévia, conversão para ChordPro, importação direta na biblioteca) e importação de cifras a partir de **link compartilhado** (Android)
- [x] Testes automatizados (parser ChordPro, transposer, importação JSON, offsets de acordes)
- [x] Publicação na **Play Store** (v1.2.0) — pipeline do **F-Droid** verde, aguardando publicação

### 🚀 Previsto
- [ ] **Exportar setlist e músicas em PDF** — exportar setlist e músicas (letra/cifra) em PDF para impressão/uso offline
- [ ] **Metrônomo simples** — metrônomo com BPM e compasso para acompanhar durante o ensaio/apresentação
- [ ] **Visualização web via host local da setlist** — servir a setlist atual em rede local (HTTP embutido + página HTML/QR code) para quem não tem o app instalado abrir no navegador/celular
- [ ] **Sincronização por conta** — login em conta cloud e sincronização automática entre dispositivos (OAuth real com Google Drive/Dropbox)
- [ ] Keystore de release própria (assinatura de produção no lugar da keystore de debug) e publicação na App Store (iOS)

## Plataformas

| Plataforma | Status |
|---|---|
| Android | ✅ Suportada (minSdk 24, targetSdk 36) |
| Desktop (Windows/macOS/Linux) | ✅ Suportada (JVM) |
| iOS | ✅ Compilável (framework `ComposeApp`) |
| Distribuição | ✅ Play Store (v1.2.0) · F-Droid (pipeline verde) |

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
│   │   └── migrations/            # Migrações 2.sqm … 7.sqm (schema atual: 8)
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
# APK: composeApp/build/outputs/apk/release/opensetlist-release.apk
```

Fluxo de lançamento: aumentar `appVersionName`/`versionCode` em `composeApp/build.gradle.kts`, validar com `assembleRelease`, criar a tag `vX.Y.Z`, publicar a **GitHub Release** com o "O que há de novo" e atualizar o metadata do **F-Droid** (ver checklist em `AGENTS.md`).

> ⚠️ A assinatura de release atual usa a keystore **debug** do Android SDK (`~/.android/debug.keystore`) — serve para testes, mas **não** é a assinatura de produção da Play Store.

## Guia de contribuições

Obrigado pelo interesse em contribuir! Qualquer ajuda — correção de bug, nova funcionalidade, melhoria de UI, testes, documentação — é bem-vinda.

Veja o guia completo em **[CONTRIBUTING.md](CONTRIBUTING.md)**: setup do ambiente, fluxo de trabalho com branches, validação antes do PR, regras de código, estilo de commits e ideias para evoluir o projeto.

## Licença

Distribuído sob a licença [MIT](LICENSE).
