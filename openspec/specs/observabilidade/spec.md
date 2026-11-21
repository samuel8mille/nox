# Observabilidade

Instrumentação que torna o comportamento do Nox diagnosticável: logging estruturado, relato de
falhas, métricas de uso e ferramentas de diagnóstico em desenvolvimento. Inspirado na
observabilidade do projeto Recorda (Timber + Crashlytics + Analytics + LeakCanary + Chucker),
adaptado à stack do Nox, onde serviços pagos/Android são substituíveis por equivalentes locais.

## Requisitos

### Requirement: Logging estruturado
O Nox DEVE usar uma fachada de logging estruturado em vez de `println`, com níveis (debug, info,
warn, error) e contexto por componente (chat, MCP, voz, memória).

#### Scenario: Evento relevante registrado
- **WHEN** uma ferramenta MCP falha ao iniciar ou uma requisição ao LLM erra
- **THEN** o evento é registrado pela fachada de logging com nível e contexto, não por `println`

### Requirement: Relato de falhas (crash reporting)
Falhas não tratadas DEVEM ser capturadas e relatadas. A implementação DEVE ser trocável por
variante: em desenvolvimento registra localmente (log); em distribuição envia a um serviço de
crash reporting.

#### Scenario: Exceção não tratada
- **WHEN** uma exceção não tratada ocorre em runtime
- **THEN** ela é capturada pelo reporter configurado e registrada com stack trace

### Requirement: Swap Pattern para integrações externas
Integrações externas e de telemetria (LLM, crash reporter, analytics, TTS) DEVEM ser expostas
como interfaces, com implementações distintas por variante de build: desenvolvimento usa dublês
que só registram em log; distribuição usa as implementações reais.

#### Scenario: Build de desenvolvimento sem serviços pagos
- **WHEN** o Nox roda em modo desenvolvimento
- **THEN** analytics e crash reporting usam implementações que apenas logam, sem chamar serviços externos

### Requirement: Métricas de uso (analytics)
O Nox DEVE registrar métricas de uso relevantes (ex.: mensagens enviadas, ferramentas invocadas,
ações confirmadas/recusadas) por meio de uma interface de analytics trocável por variante.

#### Scenario: Ação do usuário rastreada
- **WHEN** o usuário aprova ou recusa uma ação sensível
- **THEN** o evento correspondente é registrado pelo tracker de analytics configurado

### Requirement: Ferramentas de diagnóstico em desenvolvimento
O build de desenvolvimento DEVE incluir ferramentas de diagnóstico: inspeção das requisições de
rede (LLM e MCP) e detecção de vazamentos de memória/recursos.

#### Scenario: Depurar uma chamada ao LLM
- **WHEN** o desenvolvedor precisa inspecionar o corpo de uma requisição/resposta do LLM
- **THEN** a ferramenta de inspeção de rede do build de desenvolvimento mostra a chamada

### Requirement: Segredos fora de logs e do versionamento
Chaves de API e tokens NÃO DEVEM aparecer em logs nem ser versionados. Logs de execução que
possam capturar a linha de comando (com segredos) DEVEM ser ignorados pelo versionamento.

#### Scenario: Log de execução com segredo
- **WHEN** um log de execução captura a linha de comando contendo uma chave de API
- **THEN** esse log está no `.gitignore` e não é commitado
