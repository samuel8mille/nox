# Adicionar suporte a MCP (Model Context Protocol)

## Por que

Hoje cada capacidade do Nox (hora, cálculo, busca, GitHub, sistema) é uma ferramenta codificada à
mão. O MCP é um padrão aberto em que servidores externos expõem ferramentas que o LLM pode chamar.
Suportar MCP permite ao Nox conectar a servidores prontos (filesystem, git, SQLite, Notion, Slack,
etc.) e ganhar dezenas de integrações sem precisar codar cada uma — o maior multiplicador de
capacidades do roadmap.

## O que muda

O Nox passa a lançar servidores MCP locais (transporte **stdio**) e a descobrir dinamicamente as
ferramentas que eles expõem, somando-as às ferramentas nativas já existentes:

- Um cliente MCP (stdio) que lança o servidor como processo e troca mensagens JSON-RPC 2.0 por
  stdin/stdout: `initialize` → `tools/list` (descoberta) → `tools/call` (execução).
- Cada ferramenta MCP descoberta é adaptada para a interface `Tool` do Nox e exposta ao modelo.
- Os servidores a conectar são configurados num arquivo local (ex.: `~/.nox/mcp.json`), com comando
  e argumentos de cada servidor.
- Toda ferramenta vinda de MCP é marcada como `requiresConfirmation`, usando o card de aprovação já
  existente (não sabemos o que cada ferramenta externa faz).

## Fora de escopo

- Transporte HTTP/SSE (servidores remotos) — fica para uma mudança futura.
- Prompts e resources do MCP (só ferramentas por enquanto).
- UI de gerenciamento de servidores (configuração é por arquivo).
- Autenticação OAuth de servidores MCP.

## Decisões

- **Transporte stdio** primeiro: o Nox lança o servidor localmente (ex.: via `npx`), que cobre a
  maioria dos servidores MCP prontos. Requer Node (ou o runtime do servidor) instalado.
- **Confirmação em todas as ações MCP**: cada chamada a uma ferramenta MCP pede aprovação do usuário.
- **Configuração por arquivo** (`~/.nox/mcp.json`), seguindo o formato comum
  `{ "mcpServers": { "<nome>": { "command": "...", "args": [...] } } }`.
- Se um servidor configurado falhar ao iniciar, o Nox registra o erro e segue sem ele (não quebra o chat).
