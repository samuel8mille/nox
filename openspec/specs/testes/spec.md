# Testes

Estratégia de testes do Nox em camadas: unidade com cobertura mínima, regressão visual dos
componentes de UI, testes de ponta a ponta e benchmarks de desempenho. Inspirado na suíte do
projeto Recorda, adaptado à stack Kotlin Multiplatform (Compose Desktop).

## Requisitos

### Requirement: Testes de unidade com cobertura mínima
A lógica de negócio DEVE ter testes de unidade, e a cobertura de instruções DEVE atingir um
limite mínimo (meta: 90% localmente nos hooks; o CI exige ao menos 70% geral e 80% nos arquivos
modificados). A cobertura DEVE ser medida por relatório (JaCoCo/Kover) e verificável por script.

> Nota de implementação: a medição de cobertura via Kover está bloqueada por uma
> incompatibilidade conhecida do Kover com o plugin `androidMultiplatformLibrary` do AGP 9
> (kotlinx-kover#776). Deve ser habilitada quando o fix do Kover estiver disponível; até lá,
> esta meta permanece como alvo, não gate ativo.

#### Scenario: Cobertura local abaixo da meta
- **WHEN** o desenvolvedor roda os testes e a cobertura de instruções fica abaixo da meta local
- **THEN** o script de verificação de cobertura falha e informa o percentual atual e o exigido

#### Scenario: Nova lógica sem teste
- **WHEN** um novo caso de negócio é adicionado sem teste de unidade
- **THEN** a cobertura dos arquivos modificados cai abaixo do mínimo e o CI sinaliza

### Requirement: Regressão visual dos componentes (screenshot tests)
Os componentes de UI (ex.: estados do orbe, telas de chat) DEVEM ter testes de screenshot que
detectam regressões visuais, comparando a renderização atual com uma imagem de referência.

#### Scenario: Mudança visual não intencional
- **WHEN** uma alteração muda a aparência de um componente coberto por screenshot test
- **THEN** o teste falha mostrando a diferença contra a imagem de referência

### Requirement: Testes de ponta a ponta (E2E)
Os fluxos principais DEVEM ter testes de ponta a ponta executados de forma automatizada e
agendada (ex.: diariamente), além de disparo manual. Em falha, os logs de execução DEVEM ser
coletados para diagnóstico.

#### Scenario: Fluxo principal quebrado
- **WHEN** a execução E2E agendada encontra um fluxo principal quebrado
- **THEN** o teste falha e os logs/relatórios ficam disponíveis como artefato

### Requirement: Benchmarks de desempenho
O projeto DEVE ter benchmarks de desempenho (ex.: tempo de arranque, renderização) cuja
compilação é verificada no CI; a execução real roda sob demanda em ambiente apropriado.

#### Scenario: Benchmark não compila
- **WHEN** uma mudança quebra a compilação dos benchmarks
- **THEN** o job de verificação de benchmark no CI falha

### Requirement: Testes isolados de infraestrutura externa
Os testes de unidade NÃO DEVEM depender de rede, serviços externos ou chaves de API reais. O
acesso a LLM, servidores MCP e TTS externo DEVE ser substituível por implementações de teste.

#### Scenario: Teste roda sem rede
- **WHEN** a suíte de testes de unidade é executada sem acesso à internet
- **THEN** todos os testes passam, usando dublês para dependências externas
