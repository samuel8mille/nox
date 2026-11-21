# Adicionar memória persistente

## Por que

Hoje a Nox esquece tudo entre conversas: "Nova conversa" zera o histórico e, ao reabrir o app, ela
não lembra nada sobre o usuário. Isso a mantém como um "chatbot" em vez de um "assistente que te
conhece". Dar memória de longo prazo — fatos sobre o usuário (nome, preferências, projetos, contexto)
que sobrevivem a reinícios — é o maior salto de utilidade depois da busca web.

## O que muda

A Nox ganha uma memória persistente de fatos sobre o usuário, exposta ao modelo como ferramentas:

- **`lembrar`** — salva um fato (a Nox decide chamar quando o usuário conta algo sobre si).
- **`listar_memorias`** — lista o que a Nox já sabe (para "o que você sabe sobre mim?").
- **`esquecer`** — remove um fato (para "esqueça que...").

Os fatos lembrados são injetados no contexto do sistema a cada mensagem (como já acontece com a
data/hora), para que a Nox os use naturalmente. A memória persiste em arquivo local, sobrevive a
"Nova conversa" e a reinícios do app.

## Fora de escopo

- Tela dedicada de gerenciamento de memória (o controle é feito via conversa).
- Memória de conversas inteiras / resumo de histórico longo (isto é só fatos pontuais).
- Sincronização entre dispositivos.

## Decisões

- A Nox **decide sozinha** quando lembrar (via ferramenta), mas o usuário também pode pedir
  explicitamente ("lembre que...").
- Gerenciamento é **via conversa** (listar/esquecer), sem tela nova.
- Persistência em **arquivo local** simples (ex.: JSON em um diretório de dados do app), sem banco.
