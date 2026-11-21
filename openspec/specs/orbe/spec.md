# Orbe (rosto animado da Nox)

A interface primária da Nox: um orbe animado desenhado em Compose Canvas que reage ao estado da
conversa com cor, ritmo e expressão. Substitui a ideia de "caixa de texto" por presença.

## Requisitos

### Requirement: Estados expressivos
O orbe DEVE refletir o estado da conversa com uma combinação distinta de cor, movimento e expressão
para cada um dos estados: Idle (ocioso), Listening (ouvindo), Thinking (pensando), Speaking
(respondendo) e Error (erro).

#### Scenario: Transição durante uma resposta
- **WHEN** o usuário envia uma mensagem
- **THEN** o orbe vai de Idle → Thinking (enquanto consulta o modelo) → Speaking (ao exibir a
  resposta) → de volta a Idle

#### Scenario: Erro na conversa
- **WHEN** ocorre uma falha ao responder
- **THEN** o orbe assume o estado Error (cor âmbar, tremida e expressão de desânimo)

### Requirement: Transições suaves
As mudanças entre estados DEVEM ser animadas suavemente (cores e dimensões interpoladas), sem saltos
bruscos.

#### Scenario: Troca de estado
- **WHEN** o orbe muda de um estado para outro
- **THEN** as cores e as dimensões dos olhos/boca transicionam gradualmente (~350–400ms)

### Requirement: Reação ao cursor
O rosto do orbe DEVE acompanhar o cursor do mouse, com deslocamento limitado, dando a sensação de
que a Nox olha para o usuário.

#### Scenario: Mouse se move sobre a janela
- **WHEN** o cursor se move
- **THEN** os olhos/rosto e as partículas se deslocam na direção do cursor, dentro de um limite

### Requirement: Cutucada (poke)
Clicar no orbe DEVE disparar uma reação lúdica temporária ("cutucada") sobreposta ao estado atual,
sem alterar o estado da conversa.

#### Scenario: Usuário clica no orbe
- **WHEN** o usuário clica sobre o orbe
- **THEN** o orbe reage com squash & stretch e um sorriso por ~700ms, voltando depois ao estado anterior

### Requirement: Mini-orbe
As bolhas de mensagem da Nox DEVEM exibir uma versão reduzida do orbe (sem rosto, anéis nem
partículas) como avatar.

### Requirement: Modo debug de estados
A barra superior DEVE oferecer um menu de debug que permita forçar qualquer estado do orbe, para
inspeção visual, sem depender do fluxo real da conversa.

#### Scenario: Forçar um estado
- **WHEN** o usuário escolhe um estado no menu Debug → Estados
- **THEN** o orbe fica fixo nesse estado até o usuário voltar ao modo automático
