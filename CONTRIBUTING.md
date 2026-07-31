# Guia de contribuições

Obrigado pelo interesse em contribuir com o OpenSetlist! Qualquer ajuda — correção de bug, nova funcionalidade, melhoria de UI, testes, documentação — é bem-vinda.

## 1. Setup do ambiente

1. Faça um fork do repositório e clone o seu fork;
2. Instale o JDK 17+;
3. Rode o projeto em desktop (`packageUberJarForCurrentOS` + `java -jar`) — é o caminho mais rápido para testar;
4. Confirme que o app abre e o banco é criado em `~/.opensetlist/setlist.db`.

## 2. Fluxo de trabalho

1. Crie uma branch a partir de `main` com nome descritivo:
   ```bash
   git checkout -b feat/nova-funcionalidade
   # ou: fix/correcao-de-bug
   ```
2. Implemente a mudança;
3. Valide (veja "Validação" abaixo);
4. Abra um Pull Request para `main` descrevendo o que foi feito e como testar.

## 3. Validação antes do PR

Sempre rode pelo menos Android e desktop:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:packageUberJarForCurrentOS
```

Quando a mudança afetar código comum, confira também:

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Testes manuais devem cobrir o fluxo afetado (ex.: criar/excluir música, montar/reordenar setlist, importar/exportar backup).

## 4. Regras de código

- **Sem comentários no código** — prefira nomes descritivos;
- Siga os padrões existentes (Material 3, `LazyColumn` com `items(key=...)`, paddings de `16.dp`);
- **Strings de UI**: adicione em `AppStrings.kt` e use `AppStrings.nome` — nunca use literais no código;
- **Banco**: todo acesso passa por `SongRepository`. Mudanças de schema exigem uma nova `migration/N.sqm` + incremento do `schemaVersion` no `.sq` — **nunca** edite uma migração já publicada;
- **Expect/actual**: novas capacidades de plataforma devem declarar o `expect` em `commonMain` e implementar os `actual`s em cada plataforma;
- Não mude versões de stack (Kotlin, Compose, SQLDelight, Gradle, SDKs) sem motivo e sem discussão prévia.

## 5. Commits

Use mensagens em inglês, no estilo convencional, com emoji temático (padrão do projeto):

- `✨ Add: ...` — nova funcionalidade
- `🐛 Fix: ...` — correção de bug
- `♻️ Refactor: ...` — refatoração
- `💄 Style/UI: ...` — ajuste visual
- `📝 Docs: ...` — documentação

Exemplos:

```
✨ Add: dark mode toggle with persistence
🐛 Fix: crash on deleting last song of a setlist
```

Faça commits pequenos e focados, e não inclua arquivos de build, `.idea/`, `.gradle/` ou `local.properties` (ver `.gitignore`).

## 6. Ideias para evoluir

Veja também o [roadmap no README](README.md#roadmap).

- Sincronização por conta: login em conta cloud e sync automático entre dispositivos (OAuth real para Google Drive/Dropbox — hoje usa o seletor do sistema);
- Busca e importação de cifras de APIs públicas da internet;
- Keystore de release própria + publicação na Play Store;
- Publicação na App Store (iOS);
- Minificação com `proguard-rules.pro` no release;
- Testes automatizados (unitários para o parser ChordPro e o transposer).
