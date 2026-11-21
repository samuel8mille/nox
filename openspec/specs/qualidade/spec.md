# Qualidade de código

Padrões de qualidade que todo código do Nox DEVE respeitar antes de ser integrado: formatação
consistente, análise estática, documentação de API pública e verificação local automática antes
de cada commit. Inspirado nas práticas do projeto Recorda, adaptado à stack Kotlin Multiplatform
(shared JVM/Compose Desktop).

## Requisitos

### Requirement: Formatação consistente (ktlint)
O código Kotlin DEVE seguir um estilo único, verificado por ktlint. A formatação DEVE ser
verificável por uma task Gradle (`ktlintCheck`) e corrigível automaticamente (`ktlintFormat`).

#### Scenario: Código mal formatado
- **WHEN** um arquivo `.kt` não segue o estilo (indentação, imports, espaçamento)
- **THEN** `ktlintCheck` falha e aponta o arquivo e a linha
- **AND** `ktlintFormat` corrige automaticamente o que for possível

### Requirement: Análise estática (detekt)
O código DEVE passar por análise estática com detekt, configurada por um `detekt.yml` versionado.
As regras DEVEM incluir limites de complexidade (métodos longos, muitos parâmetros, complexidade
ciclomática), proibição de wildcard imports e detecção de membros privados não usados.

#### Scenario: Violação de regra de análise estática
- **WHEN** um método excede o limite de complexidade ou há um wildcard import
- **THEN** `detekt` falha e gera um relatório em `build/reports/detekt`

#### Scenario: Composable isento de regras de nomenclatura
- **WHEN** uma função é anotada com `@Composable` (PascalCase por convenção)
- **THEN** detekt NÃO a reporta como violação de nomenclatura de função

### Requirement: Documentação de API pública
Classes e funções públicas DEVEM ter KDoc, verificado por detekt (`UndocumentedPublicClass`,
`UndocumentedPublicFunction`). Código de teste, `@Composable` e `@Preview` ficam isentos.
Comentários de documentação DEVEM usar KDoc (`/** */`), não comentários de linha (`//`).

#### Scenario: Função pública sem documentação
- **WHEN** uma função ou classe pública (fora de teste) não tem KDoc
- **THEN** a análise estática falha apontando o símbolo não documentado

### Requirement: Verificação local antes do commit (git hooks)
O repositório DEVE ter git hooks (lefthook) que, antes de cada commit, verificam em paralelo:
formatação (ktlint), análise estática (detekt) e os testes de unidade com cobertura mínima.
Antes de cada push, a verificação DEVE incluir o lint da plataforma.

#### Scenario: Commit com problema de qualidade
- **WHEN** o usuário tenta commitar código mal formatado, com violação de detekt ou que quebra testes
- **THEN** o hook de pre-commit falha e bloqueia o commit, com mensagem indicando como corrigir

#### Scenario: Commit limpo
- **WHEN** o código passa em ktlint, detekt e nos testes com cobertura suficiente
- **THEN** o hook de pre-commit passa e o commit prossegue
