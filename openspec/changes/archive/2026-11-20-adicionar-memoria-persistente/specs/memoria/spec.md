# Memória persistente

## ADDED Requirements

### Requirement: Lembrar fatos sobre o usuário
A Nox SHALL dispor de uma ferramenta para salvar um fato de longo prazo sobre o usuário (ex.: nome,
preferências, projetos, contexto pessoal). A Nox decide chamá-la quando o usuário revela algo sobre
si; o usuário também pode pedir explicitamente para lembrar.

#### Scenario: Usuário revela um fato
- **WHEN** o usuário conta algo sobre si (ex.: "meu nome é Samuel e trabalho com apps Android")
- **THEN** a Nox salva esse fato na memória persistente

#### Scenario: Pedido explícito
- **WHEN** o usuário diz "lembre que eu prefiro respostas curtas"
- **THEN** a Nox salva o fato e confirma

### Requirement: Usar a memória no contexto
Os fatos lembrados SHALL ser injetados no contexto do sistema a cada mensagem, para que a Nox os use
naturalmente nas respostas, sem o usuário precisar repeti-los.

#### Scenario: Nova conversa reutiliza a memória
- **WHEN** o usuário inicia uma "Nova conversa" e pergunta algo que depende de um fato lembrado
- **THEN** a Nox responde usando o fato, mesmo com o histórico da conversa zerado

### Requirement: Persistência entre sessões
A memória SHALL persistir em armazenamento local, sobrevivendo a "Nova conversa" e ao fechamento e
reabertura do app.

#### Scenario: Reabrir o app
- **WHEN** o usuário fecha e reabre a Nox
- **THEN** os fatos lembrados anteriormente continuam disponíveis

### Requirement: Listar e esquecer via conversa
A Nox SHALL dispor de ferramentas para listar os fatos conhecidos e para remover um fato, acionadas
por conversa, sem necessidade de tela dedicada.

#### Scenario: Listar o que a Nox sabe
- **WHEN** o usuário pergunta "o que você sabe sobre mim?"
- **THEN** a Nox lista os fatos atualmente na memória

#### Scenario: Esquecer um fato
- **WHEN** o usuário diz "esqueça que eu prefiro respostas curtas"
- **THEN** a Nox remove o fato correspondente da memória e confirma
