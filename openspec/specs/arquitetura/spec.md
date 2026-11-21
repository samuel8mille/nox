# Arquitetura

Organização estrutural do Nox: separação em módulos com dependências direcionadas, camadas bem
definidas e um framework de UI unidirecional. Inspirado na arquitetura do projeto Recorda
(modularização + MVI + Clean Architecture + Hilt), adaptado à stack Kotlin Multiplatform
(`shared` + `desktopApp`/`androidApp`/`iosApp`).

## Requisitos

### Requirement: Modularização com dependências direcionadas
O código DEVE ser organizado em módulos com papéis claros (base compartilhada vs. aplicação por
plataforma). Os módulos de base (ex.: `shared`) NÃO DEVEM depender dos módulos de aplicação
(ex.: `desktopApp`). Essa direção DEVE ser verificável automaticamente.

#### Scenario: Dependência invertida entre camadas
- **WHEN** um módulo de base passa a depender de um módulo de aplicação
- **THEN** a verificação do grafo de módulos falha

### Requirement: Camadas separadas (presentation / domain / data)
Cada funcionalidade DEVE separar apresentação (ViewModel + Composables), domínio (modelos,
interfaces de repositório, casos de uso) e dados (clientes HTTP, persistência, mappers). O
domínio NÃO DEVE depender de detalhes de apresentação.

#### Scenario: Lógica de negócio na camada de UI
- **WHEN** regra de negócio é colocada diretamente num Composable ou ViewModel em vez do domínio
- **THEN** isso é considerado violação da separação de camadas e deve ser refatorado

### Requirement: Fluxo de estado unidirecional (MVI) por delegação
A camada de apresentação DEVE seguir um fluxo unidirecional composto por **delegação** (`by`) sobre
o framework MVI em `com.sam.nox.mvi` (`shared/src/commonMain/kotlin/com/sam/nox/mvi/Mvi.kt`), em vez
de reimplementar `StateFlow`/`SharedFlow` manualmente. Uma ViewModel DEVE compor os três papéis:
`UiState<T> by UiStateImpl(...)` (estado observável), `UiEvent<E> by UiEventImpl()` (eventos de
entrada) e `UiEffect<F> by UiEffectImpl()` (efeitos de uma vez). O estado de conteúdo DEVE
implementar `ScreenUiState`; a UI observa `stateFlow` e lê `.content`, e despacha eventos tipados
por `onSendEvent(evento)`. A UI não muta estado diretamente.

#### Scenario: Nova tela
- **WHEN** uma nova tela é criada
- **THEN** define `XUiState : ScreenUiState`, um `sealed interface XUiEvent` e um `XUiEffect`, e a
  ViewModel delega `UiState`/`UiEvent`/`UiEffect` às implementações padrão

#### Scenario: Interação do usuário
- **WHEN** o usuário interage com a tela (digita, envia, confirma uma ação)
- **THEN** a UI despacha um `XUiEvent` via `onSendEvent`, a ViewModel atualiza o estado com
  `setState { copy(content = ...) }` e a UI re-renderiza a partir do novo `content`

### Requirement: Injeção de dependências
As dependências DEVEM ser fornecidas por injeção (módulos de DI), não instanciadas diretamente
nos pontos de uso, permitindo troca por variante de build e em testes.

#### Scenario: Troca de implementação em teste
- **WHEN** um teste precisa substituir o cliente de LLM ou de MCP por um dublê
- **THEN** a dependência é fornecida via injeção, sem alterar o código de produção

### Requirement: Base multiplataforma preservada
A lógica compartilhada (chat, ferramentas, seleção de ferramentas, modelos) DEVE residir no
módulo `shared` (commonMain), mantendo os módulos de aplicação finos e específicos de plataforma.

#### Scenario: Nova lógica de negócio compartilhável
- **WHEN** uma nova regra de negócio independente de plataforma é adicionada
- **THEN** ela é implementada em `shared/commonMain`, não dentro de um módulo de aplicação

### Requirement: Princípios SOLID
O código DEVE respeitar os princípios SOLID. Em particular:

- **SRP**: cada classe tem uma responsabilidade clara. O cliente de LLM orquestra o loop de
  conversa; a execução de ferramentas (seleção, confirmação, despacho) e a estratégia de fallback
  entre modelos ficam em colaboradores próprios, não amontoadas no cliente.
- **OCP**: adicionar uma capacidade (ex.: um novo grupo de ferramentas MCP) NÃO DEVE exigir editar
  a lógica central de seleção/registo. `ToolSelector` e o registo de ferramentas DEVEM ser pontos
  de extensão (ver [[tool-limit-dynamic-selection]]).
- **DIP**: colaboradores (cliente HTTP, memória, gestor de MCP, cliente de LLM) DEVEM ser
  fornecidos por injeção no ponto de composição, não instanciados por omissão dentro das classes
  que os usam — permitindo reuso e substituição por dublês em teste.

#### Scenario: Responsabilidade concentrada demais
- **WHEN** uma classe acumula HTTP, histórico, execução de ferramentas e fallback de modelos
- **THEN** isso é uma violação de SRP e as responsabilidades devem ser extraídas para colaboradores

#### Scenario: Colaborador instanciado em vez de injetado
- **WHEN** uma classe cria por omissão um colaborador com estado (ex.: `MemoryStore()`,
  `McpManager()`, `HttpClient`) em vez de recebê-lo por parâmetro
- **THEN** isso é uma violação de DIP e o colaborador deve ser injetado no ponto de composição
