# Ferramentas (tool calling)

A Nox pode invocar ferramentas para agir além de conversar: consultar data/hora, calcular, pesquisar
na web, agir no GitHub e operar no sistema do usuário. Ações que alteram algo externo exigem
confirmação.

## Requisitos

### Requirement: Mecanismo de ferramentas
A Nox DEVE expor ferramentas ao modelo com nome, descrição e parâmetros (JSON Schema), executar as
que o modelo pedir e devolver o resultado ao modelo até produzir uma resposta final em texto.

#### Scenario: Modelo decide usar uma ferramenta
- **WHEN** a pergunta do usuário requer uma ferramenta (ex.: "quanto é 23 × 17?")
- **THEN** o modelo chama a ferramenta, o resultado volta ao modelo, e a Nox responde com o valor

### Requirement: Ferramentas internas de data e cálculo
A Nox DEVE dispor de uma ferramenta que retorna a data e a hora atuais reais e de uma calculadora
para operações aritméticas simples.

#### Scenario: Pergunta sobre a hora
- **WHEN** o usuário pergunta "que horas são?"
- **THEN** a Nox usa a ferramenta de hora e responde com o horário real do sistema

### Requirement: Busca web (opcional)
Quando houver `TAVILY_API_KEY`, a Nox DEVE dispor de uma ferramenta de busca na web que traz
informações atuais (notícias, eventos, preços) resumidas, e usá-la para perguntas sobre fatos que
podem mudar com o tempo.

#### Scenario: Pergunta sobre evento atual
- **WHEN** o usuário pergunta sobre um fato atual (ex.: "qual o próximo jogo do Cruzeiro?")
- **THEN** a Nox pesquisa na web e responde com a informação encontrada, em vez de dizer que não sabe

#### Scenario: Sem chave de busca
- **WHEN** não há `TAVILY_API_KEY`
- **THEN** a ferramenta de busca fica desligada e as demais capacidades continuam funcionando

### Requirement: Confirmação de ações sensíveis
Toda ferramenta que altera algo externo (arquivos, sistema ou GitHub) DEVE ser marcada como
`requiresConfirmation` e, antes de executar, exibir ao usuário um card com um preview legível da ação
e os botões Aprovar/Recusar. A ação só executa se o usuário aprovar.

#### Scenario: Ação aprovada
- **WHEN** o modelo pede uma ação sensível e o usuário clica em Aprovar
- **THEN** a ação é executada e o resultado volta à conversa

#### Scenario: Ação recusada
- **WHEN** o usuário clica em Recusar
- **THEN** a ação NÃO é executada e a Nox é informada da recusa para perguntar como prosseguir

### Requirement: Ações no GitHub (opcional)
Quando houver `GITHUB_TOKEN`, a Nox DEVE poder criar repositório, abrir pull request e criar issue
via API REST do GitHub. Todas exigem confirmação.

#### Scenario: Criar repositório
- **WHEN** o usuário pede para criar um repositório e aprova a confirmação
- **THEN** o repositório é criado na conta do dono do token e a Nox responde com o link

### Requirement: Ações no sistema (desktop)
No desktop, a Nox DEVE poder definir uma pasta de trabalho, executar comandos no terminal, criar e
ler arquivos e listar pastas. Terminal e criação de arquivo exigem confirmação; as de leitura, não.

#### Scenario: Definir a pasta de trabalho
- **WHEN** o usuário indica em qual pasta trabalhar
- **THEN** as ações de arquivo e terminal passam a operar a partir dessa pasta

#### Scenario: Commit e push de um projeto
- **WHEN** o usuário pede para fazer commit e push, informando a pasta
- **THEN** a Nox propõe cada comando de terminal (git add/commit/push) com confirmação antes de executar
