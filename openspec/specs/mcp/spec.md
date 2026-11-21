# Suporte a MCP (Model Context Protocol)

## Requisitos

### Requirement: Conexão a servidores MCP via stdio
O Nox SHALL conectar a servidores MCP locais pelo transporte stdio, lançando-os como processos e
trocando mensagens JSON-RPC 2.0 por stdin/stdout, incluindo o handshake `initialize`.

#### Scenario: Servidor configurado inicia com sucesso
- **WHEN** há um servidor MCP configurado e seu comando é válido
- **THEN** o Nox lança o processo, completa o handshake e o servidor fica disponível

#### Scenario: Servidor falha ao iniciar
- **WHEN** um servidor configurado não pode ser iniciado (comando inexistente, erro)
- **THEN** o Nox registra o erro e continua funcionando sem aquele servidor, sem quebrar o chat

### Requirement: Descoberta dinâmica de ferramentas
O Nox SHALL descobrir as ferramentas expostas por cada servidor MCP (via `tools/list`) e adaptá-las
para que o modelo possa chamá-las junto das ferramentas nativas.

#### Scenario: Ferramentas de um servidor aparecem para o modelo
- **WHEN** um servidor MCP expõe ferramentas
- **THEN** essas ferramentas ficam disponíveis ao modelo, com nome, descrição e parâmetros do servidor

### Requirement: Execução de ferramentas MCP
O Nox SHALL executar uma ferramenta MCP chamada pelo modelo via `tools/call`, devolvendo o resultado
ao modelo como texto.

#### Scenario: Modelo chama uma ferramenta MCP
- **WHEN** o modelo decide usar uma ferramenta vinda de um servidor MCP
- **THEN** o Nox envia `tools/call` ao servidor e devolve a resposta ao modelo

### Requirement: Confirmação de ações MCP
Toda ferramenta vinda de um servidor MCP SHALL exigir confirmação do usuário antes de executar,
exibindo o card de aprovação com um preview da ação (nome da ferramenta e argumentos).

#### Scenario: Ação MCP aprovada
- **WHEN** o modelo chama uma ferramenta MCP e o usuário aprova a confirmação
- **THEN** a ferramenta é executada e o resultado volta à conversa

#### Scenario: Ação MCP recusada
- **WHEN** o usuário recusa a confirmação
- **THEN** a ferramenta NÃO é executada e o modelo é informado da recusa

### Requirement: Configuração por arquivo
Os servidores MCP SHALL ser configurados num arquivo local (`~/.nox/mcp.json`) no formato
`{ "mcpServers": { "<nome>": { "command": "...", "args": [...] } } }`. Na ausência do arquivo, o Nox
funciona normalmente sem servidores MCP.

#### Scenario: Sem arquivo de configuração
- **WHEN** não existe `~/.nox/mcp.json`
- **THEN** nenhum servidor MCP é conectado e o resto do Nox funciona normalmente
