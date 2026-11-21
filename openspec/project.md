# Nox

> Assistente pessoal multiplataforma (Kotlin Multiplatform) com um rosto animado — o "orbe" —
> que reage à conversa, fala por voz e executa ações no computador do usuário por meio de
> ferramentas (busca web, GitHub, git, terminal e arquivos).

---

## O que é o Nox

Nox é um assistente de IA de desktop (com base multiplataforma) cujo diferencial é a **presença**:
em vez de uma caixa de texto, o usuário conversa com um orbe animado que muda de cor, ritmo e
expressão conforme o estado da conversa (ocioso, ouvindo, pensando, respondendo, erro). O cérebro
é um LLM na nuvem (via endpoint compatível com OpenAI), e o Nox pode **agir** no mundo real através
de ferramentas: pesquisar na web, criar repositórios no GitHub, rodar comandos no terminal, criar e
editar arquivos — sempre com confirmação do usuário antes de qualquer ação que altere algo externo.

O foco inicial é o **desktop (JVM/Compose)**, com entrada de voz offline (Whisper) e síntese de voz
nativa do sistema. A base em Kotlin Multiplatform mantém a porta aberta para Android e iOS.

---

## Princípios de produto

| Princípio | O que significa no Nox |
|---|---|
| **Presença, não caixa de texto** | O orbe é a interface primária; o estado da conversa é sempre visível no rosto |
| **Capaz de agir, não só responder** | Ferramentas executam ações reais (GitHub, git, arquivos, terminal) |
| **Seguro por padrão** | Toda ação que altera algo externo pede confirmação explícita do usuário |
| **Sem custo recorrente** | LLM e busca usam tiers gratuitos; voz é on-device |
| **Offline onde dá** | Transcrição (Whisper) e TTS (`say`) rodam localmente, sem depender de rede |

---

## Capacidades atuais

- **Chat com LLM** — conversa via Groq (endpoint compatível com OpenAI, modelo `openai/gpt-oss-120b`),
  com fallback automático entre modelos e mensagens de erro claras (limite, instabilidade, etc.)
- **Orbe animado** — rosto reativo com 5 estados (Idle, Listening, Thinking, Speaking, Error) + cutucada,
  desenhado em Compose Canvas; segue o cursor, pisca, tem partículas, halo e bocas por estado
- **Voz** — entrada por microfone transcrita offline com Whisper (whisper-jni); saída por TTS do sistema (`say`)
- **Ferramentas internas** — data/hora atual e calculadora
- **Busca web** — via Tavily (resultados atuais já resumidos), ligada quando há `TAVILY_API_KEY`
- **GitHub** — criar repositório, abrir pull request e criar issue, ligado quando há `GITHUB_TOKEN`
- **Sistema** — executar comandos no terminal, criar/ler arquivos e listar pastas, numa pasta de trabalho
  definida pelo usuário na conversa
- **Confirmação de ações** — card "Aprovar/Recusar" na UI antes de qualquer ação que altere algo externo
- **Qualidade de vida** — foco automático no campo, histórico de comandos (setas ↑/↓), limpeza de Markdown

---

## Arquitetura

```
:shared        — código multiplataforma (commonMain) + implementações por plataforma (jvm/android/ios)
:desktopApp    — app de desktop (Compose for Desktop), ponto de entrada atual
:androidApp    — app Android (esqueleto)
```

O código segue, de forma pragmática, a separação **ui / presentation / data**:

```
shared/commonMain/
  chat/          → OpenRouterClient (LLM), Tool, ferramentas (BuiltInTools, WebSearchTool, GithubTools)
  ui/orb/        → NoxOrb (Canvas), NoxMood (estados + animações)
  ui/theme/      → NoxTheme, tokens de cor
  speech/        → SpeechRecognizer (expect/actual; Whisper no JVM)

desktopApp/
  data/          → ChatRepository (monta o LLM + ferramentas, resolve chaves)
  presentation/  → ChatViewModel (StateFlow de UiState, deriva o NoxMood, confirmação, histórico)
  ui/            → ChatScreen, ChatPanel, OrbPanel (telas Compose)
  SystemTools.kt → ferramentas de terminal/arquivos (JVM)
  DesktopNox.kt  → pipeline de voz (microfone → Whisper → LLM → TTS)
```

### Padrão de ferramentas (tool calling)

Uma `Tool` tem `name`, `description`, `parameters` (JSON Schema), `requiresConfirmation`, `preview`
e `execute`. O `OpenRouterClient` expõe as ferramentas ao modelo, executa as que ele pedir e, quando
`requiresConfirmation` é true, chama um callback `confirm(preview)` que a UI resolve mostrando o card
de aprovação. Ferramentas novas entram pelo `ChatRepository.buildTools`.

---

## Stack técnico

| Camada | Tecnologia |
|---|---|
| Base | Kotlin Multiplatform (commonMain + jvm/android/ios) |
| UI | Jetpack Compose (Compose for Desktop / Material 3) |
| LLM | Groq (endpoint compatível com OpenAI), `openai/gpt-oss-120b` + fallback |
| Busca web | Tavily (opcional) |
| Ações externas | API REST do GitHub; `ProcessBuilder` (terminal); `java.io.File` (arquivos) |
| Voz (entrada) | Whisper via whisper-jni (offline, JVM) |
| Voz (saída) | `say` do macOS (offline) |
| Rede | Ktor Client (OkHttp no JVM, Darwin no iOS) + kotlinx.serialization |
| Datas | kotlinx-datetime |

Escolhas feitas para manter **custo zero** em uso pessoal: Groq e Tavily têm tier gratuito, a voz é
toda on-device, e nenhuma dependência paga é obrigatória.

---

## Configuração local

Chaves lidas de variáveis de ambiente (ou propriedades `-D`), nunca commitadas:

| Variável | Propriedade | Obrigatória? | Para quê |
|---|---|---|---|
| `GROQ_API_KEY` | `-Dgroq.key` | Sim | Chat (LLM). Crie em https://console.groq.com/keys |
| `TAVILY_API_KEY` | `-Dtavily.key` | Não | Busca web. Crie em https://tavily.com |
| `GITHUB_TOKEN` | `-Dgithub.token` | Não | Ações no GitHub. PAT com escopo `repo` |

Além disso, a entrada de voz requer o modelo GGML do Whisper (`ggml-base.bin`) acessível no diretório
do projeto ou via `-Dnox.model`.

```bash
# Rodar o desktop (as chaves do ambiente são repassadas automaticamente)
./gradlew :desktopApp:run
```

---

## Convenções

- **Idioma** — o produto e o código falam português do Brasil (comentários, nomes de ferramentas, UI).
- **Segurança** — qualquer ação que altere algo externo (arquivos, sistema, GitHub) deve ter
  `requiresConfirmation = true` e um `preview` legível.
- **Chaves** — sempre por variável de ambiente; nunca hardcoded nem commitadas.
- **Ferramentas comuns vs. de plataforma** — ferramentas sem dependência de SO ficam no
  `shared/commonMain`; as que usam JVM (terminal, arquivos) ficam no `desktopApp`.

---

## Engenharia e qualidade (meta)

O Nox adota como meta de engenharia as práticas maduras do projeto irmão **Recorda**, adaptadas à
sua stack (Kotlin Multiplatform / Compose Desktop). Estas práticas estão especificadas em
`openspec/specs/` como requisitos a atingir — algumas ainda não implementadas:

| Dimensão | Spec | O que cobre |
|---|---|---|
| Arquitetura | `specs/arquitetura` | Modularização com dependências direcionadas, camadas, MVI, DI, base KMP |
| Qualidade | `specs/qualidade` | ktlint, detekt, KDoc de API pública, git hooks (lefthook) |
| Testes | `specs/testes` | Unidade + cobertura mínima, screenshot tests, E2E, benchmarks |
| CI/CD | `specs/ci-cd` | Pipeline com pre-flight, quality gates paralelos, cobertura no PR, CD, CodeQL |
| Observabilidade | `specs/observabilidade` | Logging estruturado, crash reporting, Swap Pattern, analytics, diagnóstico |

Estado atual: o Nox tem testes de unidade incipientes e ainda **não** possui CI, hooks nem análise
estática configurados — estes specs definem o alvo, não o presente.
