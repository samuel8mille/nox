# Tarefas — Adicionar memória persistente

## Armazenamento
- [x] Criar um `MemoryStore` (desktopApp) que lê/grava fatos num arquivo JSON local
      (ex.: `~/.nox/memory.json`), com operações: listar, adicionar, remover.
- [ ] Garantir leitura/gravação resilientes (arquivo ausente = memória vazia; erro não quebra o chat).

## Ferramentas
- [x] Criar `memoryTools(store)` com três ferramentas: `lembrar`, `listar_memorias`, `esquecer`.
      Nenhuma exige confirmação (não altera nada externo ao app).
- [x] Registrar as ferramentas no `ChatRepository.buildTools`.

## Contexto do sistema
- [x] Injetar os fatos lembrados no system prompt a cada mensagem (ao lado da data/hora atual),
      de forma que a Nox os use naturalmente.
- [x] Atualizar o system prompt para orientar a Nox a lembrar fatos relevantes sobre o usuário.

## Verificação
- [x] Testar o fluxo ponta a ponta: contar um fato → nova conversa → a Nox usa o fato.
- [x] Testar persistência: fechar e reabrir o app → o fato continua.
- [x] Testar listar e esquecer via conversa.
- [x] Compilar e rodar o desktop sem erros.

## Finalização
- [x] Mover a mudança para `openspec/changes/archive/AAAA-MM-DD-adicionar-memoria-persistente/`.
- [x] Criar `openspec/specs/memoria/spec.md` oficial com o comportamento implementado.
