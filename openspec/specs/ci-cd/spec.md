# Integração e entrega contínuas (CI/CD)

Pipeline automatizado que valida cada mudança antes de integrá-la e automatiza a geração de
artefatos de distribuição. Inspirado no pipeline do projeto Recorda, adaptado à stack do Nox
(Kotlin Multiplatform; alvo principal desktop JVM/Compose, com Android e iOS na base).

## Requisitos

### Requirement: Pipeline de CI em cada mudança
O projeto DEVE ter um workflow de CI (GitHub Actions) disparado em push nos branches de release e
em pull requests para o branch principal, além de disparo manual (`workflow_dispatch`).

#### Scenario: Pull request aberto
- **WHEN** um pull request é aberto ou atualizado contra o branch principal
- **THEN** o workflow de CI roda os quality gates, os testes e o build

### Requirement: Cancelamento de execuções obsoletas
O CI DEVE cancelar execuções anteriores em andamento no mesmo branch/PR (concurrency com
`cancel-in-progress`), para não gastar tempo de runner numa versão já obsoleta do código.

#### Scenario: Novo push durante CI em andamento
- **WHEN** um novo push chega enquanto o CI do push anterior ainda roda no mesmo branch
- **THEN** a execução anterior é cancelada e só a mais recente continua

### Requirement: Pré-verificação de mudanças relevantes
O CI DEVE ter um job de pré-verificação (pre-flight) que detecta se a mudança toca em código,
build ou workflows. Mudanças que só alteram arquivos irrelevantes (ex.: documentação) DEVEM
pular os quality gates e o build.

#### Scenario: PR só de documentação
- **WHEN** um PR altera apenas arquivos não relevantes (ex.: README)
- **THEN** o pre-flight decide não continuar e os quality gates/build são pulados

### Requirement: Quality gates em paralelo
O CI DEVE executar os quality gates em jobs paralelos que falham rápido: formatação (ktlint),
análise estática (detekt), lint da plataforma e verificação do grafo de módulos (garantir que
`:core`/`:shared` não dependem de `:app`/`:desktopApp`).

#### Scenario: Violação de camada de módulos
- **WHEN** um módulo de base passa a depender de um módulo de aplicação
- **THEN** o job de grafo de módulos falha

### Requirement: Testes com cobertura reportada no PR
O CI DEVE rodar os testes de unidade, gerar relatório de cobertura e publicá-lo como comentário
no PR, com limites mínimos de cobertura geral e para arquivos modificados.

#### Scenario: Cobertura abaixo do mínimo
- **WHEN** a cobertura dos arquivos modificados fica abaixo do limite exigido
- **THEN** o report de cobertura no PR sinaliza a reprovação

### Requirement: Build condicionado aos gates
O job de build (gerar o artefato distribuível — ex.: binário/instalador do desktop) DEVE rodar
somente quando todos os quality gates e testes passam.

#### Scenario: Gate reprovado
- **WHEN** qualquer quality gate ou o conjunto de testes falha
- **THEN** o build não roda e o CI termina reprovado

### Requirement: Verificação de dependências (segurança)
O CI DEVE revisar dependências quanto a vulnerabilidades (CVE) em pull requests, falhando em
severidade alta, e DEVE detectar mudanças não intencionais no grafo de dependências.

#### Scenario: Dependência vulnerável introduzida num PR
- **WHEN** um PR adiciona uma dependência com CVE de severidade alta
- **THEN** o job de revisão de dependências falha

### Requirement: Entrega contínua de builds
Após o CI passar no branch principal, um workflow de CD DEVE gerar o artefato de distribuição e
publicá-lo num canal de distribuição de testes, derivando a versão a partir do CHANGELOG.

#### Scenario: CI verde no branch principal
- **WHEN** o workflow de CI conclui com sucesso no branch principal
- **THEN** o CD gera o artefato e o publica no canal de distribuição

### Requirement: Varredura de segurança agendada
O projeto DEVE ter análise de segurança de código (CodeQL) em push/PR e uma varredura completa
agendada periodicamente (ex.: semanal).

#### Scenario: Varredura semanal
- **WHEN** chega o horário agendado da varredura
- **THEN** o CodeQL analisa o código e reporta alertas de segurança

### Requirement: Análise de qualidade e segurança de dependências (meta)
O projeto DEVERIA, como meta futura (já em uso no projeto irmão Recorda, ainda não implementado
aqui), integrar: análise de qualidade contínua (SonarCloud), varredura de CVEs em dependências
(OWASP dependency-check), e detecção de mudanças não intencionais no grafo de dependências
(dependency-guard). A regressão visual (screenshot tests) está especificada em `specs/testes`.

#### Scenario: Qualidade contínua
- **WHEN** um pull request é aberto
- **THEN** (meta) a análise SonarCloud reporta duplicação, code smells e cobertura no PR
