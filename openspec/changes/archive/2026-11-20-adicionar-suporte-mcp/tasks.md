# Tarefas — Adicionar suporte a MCP (stdio)

## Cliente MCP (stdio)
- [x] Criar `McpClient` (desktopApp) que lança um servidor via `ProcessBuilder`, fala JSON-RPC 2.0
      por stdin/stdout e implementa: `initialize`, `tools/list`, `tools/call`.
- [x] Ler respostas de forma assíncrona (uma linha JSON por mensagem) e casar requisições por `id`.
- [x] Tratar falha de inicialização e timeout sem quebrar o chat; permitir `close()` do processo.

## Configuração
- [x] Ler `~/.nox/mcp.json` no formato `{ "mcpServers": { "<nome>": { "command", "args" } } }`.
      Arquivo ausente = nenhum servidor (comportamento normal).

## Adaptação para Tool
- [x] Converter cada ferramenta MCP (`tools/list`) numa `Tool` do Nox: nome prefixado pelo servidor,
      descrição e `parameters` (JSON Schema do servidor), `requiresConfirmation = true`, `preview`
      com nome + argumentos, e `execute` que chama `tools/call`.
- [x] Criar `mcpTools()` que inicia os servidores configurados e devolve todas as `Tool` descobertas.
- [x] Registrar as ferramentas MCP no `ChatRepository.buildTools`.

## Ciclo de vida
- [x] Encerrar os processos dos servidores MCP quando o `ChatRepository` for fechado.

## Verificação
- [x] Testar com um servidor real (ex.: `npx -y @modelcontextprotocol/server-filesystem <pasta>`):
      a ferramenta aparece, o modelo a chama, a confirmação aparece, a execução retorna.
- [x] Testar ausência de `mcp.json` (chat funciona normal).
- [x] Testar servidor que falha ao iniciar (chat não quebra).
- [x] Compilar e rodar o desktop sem erros.

## Finalização
- [x] Mover a mudança para `openspec/changes/archive/AAAA-MM-DD-adicionar-suporte-mcp/`.
- [x] Criar `openspec/specs/mcp/spec.md` oficial com o comportamento implementado.
