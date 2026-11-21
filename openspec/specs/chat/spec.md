# Chat (conversa com LLM)

Conversa em linguagem natural com a assistente Nox, alimentada por um LLM na nuvem via endpoint
compatível com OpenAI. Mantém o histórico da conversa e expõe ferramentas ao modelo.

## Requisitos

### Requirement: Conversa em português
A Nox DEVE conversar em português do Brasil, de forma clara e concisa, mantendo o histórico completo
da conversa enviado ao modelo a cada mensagem.

#### Scenario: Usuário envia uma mensagem
- **WHEN** o usuário escreve uma mensagem e envia (Enter)
- **THEN** a mensagem aparece como bolha do usuário e a Nox responde numa bolha própria
- **AND** o orbe passa por Thinking (processando) e Speaking (respondendo)

#### Scenario: Saudação não dispara ferramentas
- **WHEN** o usuário envia uma saudação ou conversa casual (ex.: "bom dia")
- **THEN** a Nox responde normalmente, sem chamar ferramentas

### Requirement: Contexto temporal
A Nox DEVE conhecer a data e a hora atuais do sistema, injetadas no contexto a cada mensagem, para
responder corretamente perguntas sobre "que dia é hoje" sem alucinar.

#### Scenario: Pergunta sobre a data
- **WHEN** o usuário pergunta "que dia é hoje?"
- **THEN** a Nox responde com a data real do sistema

### Requirement: Respostas em texto simples
A Nox DEVE responder em texto simples, sem formatação Markdown. Marcações residuais (negrito,
itálico, títulos, código, referências de citação) DEVEM ser removidas antes de exibir ao usuário.

#### Scenario: Modelo devolve Markdown
- **WHEN** o modelo inclui `**negrito**` ou similar na resposta
- **THEN** a UI exibe o texto limpo, sem os caracteres de marcação

### Requirement: Resiliência de modelo
O cliente DEVE tentar um ou mais modelos de fallback quando o modelo principal falhar (provedor
instável, resposta vazia ou tool call malformada), e só reportar erro se todos falharem.

#### Scenario: Modelo principal indisponível
- **WHEN** o modelo principal retorna erro de provedor ou resposta vazia
- **THEN** o cliente tenta o próximo modelo de fallback automaticamente

### Requirement: Mensagens de erro claras
Falhas do provedor DEVEM ser traduzidas em mensagens legíveis exibidas numa bolha de erro, em vez de
erros técnicos crus.

#### Scenario: Limite de uso atingido
- **WHEN** o provedor responde com erro de limite (429)
- **THEN** a Nox mostra "Limite de uso gratuito atingido. Aguarde um instante e tente de novo."

### Requirement: Configuração por ambiente
A chave do LLM DEVE ser lida de variável de ambiente (`GROQ_API_KEY`) ou propriedade de sistema
(`-Dgroq.key`), nunca hardcoded. Na ausência da chave, o app DEVE falhar com instrução clara de como
obtê-la.
