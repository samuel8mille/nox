# Nox — handoff de UI para Compose Desktop (v3)

Referência visual: `Main.dc.html` (janela de chat), `OrbStates.dc.html` (estados do orbe) e `MenuBar.dc.html` (popover futuro), nesta pasta. São HTML/CSS e são a fonte da verdade para cores, tamanhos e tempos: todo o CSS do orbe está no bloco `<style>` de cada arquivo (as classes `.orb*` e os `@keyframes nox-*`). Reimplemente em Compose. Não use web view.

Todas as medidas do orbe são frações de `size` (no CSS, `calc(var(--size)*X)`). Tamanhos usados: 240dp (janela), 140dp (folha de estados), 96dp (popover), 28dp (avatar do chat).

---

## 1. Layout da janela (Main)
- Janela de ~1280×800. Title bar transparente (com os traffic lights nativos) e título "Nox".
- Duas colunas:
  - **Painel do orbe** (esquerda, mín. ~340dp, fundo `panel`, borda direita `line`):
    - área clicável de 320×340 com o orbe de 240dp; o clique dispara a "cutucada" (§4.7);
    - "Nox" em Space Grotesk 36sp SemiBold;
    - pill de status: ponto de 8dp na cor `c1` do estado + rótulo;
    - dica em 13sp na cor `muted`;
    - rodapé em 12sp `muted`: "Atalho ⌥ Espaço e ícone na barra de menus chegam em breve".
  - **Chat** (direita, ocupa o resto da largura):
    - cabeçalho: "Conversa", nome do modelo e botão "Nova conversa";
    - lista rolável (padding 28×32, espaço de 16 entre mensagens);
    - compositor fixo no rodapé.
- **Bolhas:**
  - Usuário: à direita, fundo `user`, texto branco, cantos 18/18/6/18, largura máx. 72%.
  - Nox: à esquerda, com mini-orbe de 28dp, fundo `surface`, borda `line`, cantos 18/18/18/6, largura máx. 78%.
  - Erro: fundo `dangerBg`, borda `danger`, título "Não consegui responder" e mini-orbe no estado Error.
- **Digitando:** bolha do Nox com 3 pontos de 6dp pulando (ciclo de 1,2s, 0,15s de atraso entre eles), mini-orbe no estado Thinking.
- **Compositor:**
  - Caixa com fundo `surface`, cantos 16, borda que vira `accent` com foco.
  - TextField multilinha (1 a 6 linhas). Enter envia, Shift+Enter quebra linha.
  - À direita do campo: **botão de microfone** (40dp, cantos 12, transparente, ícone `muted`) e **botão enviar** (40dp, cantos 12, fundo `user`, alpha 0,35 quando desabilitado).
  - **Microfone ligado:** fundo `micBg`, ícone `mic`, um anel que se expande de 0 a 10dp e some em 1,6s, em loop. O placeholder vira "Ouvindo… fale ou digite" e o estado vai para Listening, com a dica "Ouvindo pelo microfone". Desliga ao clicar de novo ou ao enviar. Fica bloqueado durante Thinking e Speaking.
  - Legenda embaixo em 12sp `muted`.
- Fonte: Inter (15sp nas mensagens).

## 1b. Alternância entre janela e popover (v3)
O Nox tem dois modos de apresentação que compartilham o mesmo ViewModel (mesmo histórico, mesmo `NoxMood`):
- **Modo janela** (Main): botão "Modo compacto" no cabeçalho do chat, à esquerda de "Nova conversa". É um botão fantasma de 36dp de altura (borda `line`, cantos 10, texto 13sp Medium), com ícone de 16dp (retângulo com um retângulo menor no canto) e 8dp de espaço até o texto. Ao clicar: fecha a janela principal e abre o popover.
- **Modo popover** (MenuBar): botão "Modo janela" no rodapé, à esquerda de "Sair do Nox", mesmo estilo, com ícone de 16dp de setas se afastando. Ao clicar: fecha o popover e abre a janela principal.
- No protótipo cada tela guarda o próprio estado, então a conversa não passa de uma para a outra. No app real, o histórico vive no ViewModel e os dois modos apenas observam o mesmo `StateFlow`.
- Sugestão: `enum class WindowMode { Window, Popover }` guardado num `StateFlow` do ViewModel, ou persistido nas preferências.

## 1c. Menu de debug (v3)
Botão "Debug" (ghost de 36dp, ícone de inseto de 16dp) no cabeçalho do chat, à esquerda de "Modo compacto". Abre um painel flutuante de 340dp de largura (altura máx. 540dp, com rolagem), ancorado embaixo do botão, com fundo `surface`, borda `line`, cantos 14, sombra e padding 16. Fecha ao clicar fora ou no botão de novo. No app real, mostre só em builds de debug (`BuildConfig.DEBUG` ou uma flag de sistema).
Seções (títulos em 11sp SemiBold, caixa alta, `muted`):
- **Estado do Nox:** Auto (real), Pronto, Dormindo, Ouvindo, Pensando, Respondendo, Erro. "Auto" usa o estado de verdade do ViewModel; qualquer outro força o estado exibido pelo orbe (só a exibição, não o ViewModel real). Cada chip tem um ponto na cor `c1` do estado.
- **Cor do orbe:** Por estado, Roxo, Ciano, Verde, Laranja. Força uma única paleta em todos os estados, no orbe grande, nos mini-orbes, no ponto do status e na aura.
- **Olhos:** Escuro, Dourado, Ciano, Branco (ver §4.1).
- **Tema:** Sistema, Escuro, Claro.
- **Dormir após 20s parado:** interruptor (liga/desliga o `Sleeping` automático).
- **Ações:** "Cutucar o orbe", "Simular erro" (adiciona uma mensagem de erro e vai a Error por 3,2s) e "Limpar conversa".
Chips: pílulas de 32dp, texto 12,5sp Medium, borda `line`; o selecionado fica com fundo `user`, borda `user` e texto branco. Interruptor de 40×24dp, bolinha de 18dp. Sugestão: um `DebugState` (data class) num `StateFlow` do ViewModel, lido pela UI como override das propriedades reais.

## 2. Tokens de tema (seguem o sistema)
| token | escuro | claro |
|---|---|---|
| bg | #0B0C12 | #F6F6FA |
| panel | #10121A | #EEEEF5 |
| surface | #181B27 | #FFFFFF |
| line | branco 8% | #0F111E 9% |
| text | #ECEDF5 | #14151F |
| muted | #9A9EB4 | #5A5E73 |
| user (bolha/botão) | #5B5BD6 | #4B4BC4 |
| accent | #A99BFF | #5B5BD6 |
| danger | #FF8A80 | #B42318 |
| mic | #7FE3F5 | #0E7490 |
| micBg | #7FE3F5 16% | #0E7490 12% |

## 3. Estado
```kotlin
enum class NoxMood { Idle, Sleeping, Listening, Thinking, Speaking, Error }
```
O ViewModel decide o estado assim:
- campo vazio e microfone desligado → Idle;
- texto no campo ou microfone ligado → Listening;
- requisição em andamento → Thinking;
- resposta chegou → Speaking, por 1,2s + 22ms por caractere (máx. 4,2s), depois volta para Idle ou Listening;
- falha → Error;
- Idle por 20s → Sleeping. Passar o mouse sobre o orbe, clicar nele, digitar ou ligar o microfone acorda (§4.12).

A "cutucada" (`poke`) é um booleano separado. Ela dura 900ms e se sobrepõe ao estado atual.

Cores do orbe por estado (`c1` brilho, `c2` meio, `c3` fundo):
| estado | c1 | c2 | c3 |
|---|---|---|---|
| Idle | #A99BFF | #5B6CF0 | #1B1550 |
| Listening | #7FE3F5 | #4F8DF5 | #0E2552 |
| Thinking | #C29BFF | #7A5CF0 | #24104F |
| Speaking | #8CF0D2 | #4FB9E8 | #0D2E45 |
| Sleeping | #8B86C9 | #41478F | #141235 |
| Error | #FFB37A | #E8604F | #4A1414 |

Faça a transição das cores com `animateColorAsState` (~400ms) e das dimensões de olhos e boca com `animateFloatAsState` (~350ms).

## 4. Orbe — camadas, de trás para frente

### 4.1 Base (vale para todos os estados)
1. **Reflexo (pool):** elipse abaixo do orbe (largura 72% do orbe, altura 13%, centro em +0,57·size de y), radial de `c1` até transparente, com blur. Sai de 0,5 para 0,32 de alpha e de 1 para 0,84 de scaleX em 6s, em sincronia inversa com a flutuação. Fica fora do grupo que flutua.
2. **Grupo flutuante:** tudo o que vem a seguir sobe e desce, com translateY de 0 a −3,5% do size em 6s (ease in-out).
3. **Glow:** radial `c1` → transparente a 60%, área de 1,7× o size, blur de 10% do size. Alpha oscila entre 0,45 e 0,72 em 4s.
4. **Anel de marcações (ticks):** anel de raio 0,67·size e 6dp de espessura, formado por traços de 1,6° a cada 7,5° (sweep gradient repetido, recortado em anel). Gira em 30s. Fica invisível no Idle.
5. **Anéis de onda (3):** círculos com contorno de 2dp em `c1`. Ficam invisíveis no Idle.
6. **Satélite + rastro:** ponto branco de 4,5% do size com glow `c1`, em órbita de raio 0,59·size. Logo atrás dele, um arco de rastro de 120° num anel de 2–3dp (sweep de transparente até `c1`). Uma volta leva 1,8s. Fica invisível no Idle.
7. **Partículas (6):** pontos brancos com glow `c1`, em órbitas circulares. Raio, período, sentido, atraso de fase e tamanho de cada um:
   - p1: 0,74·size, 14s, horário, fase 0s, 2,2%
   - p2: 0,65·size, 19s, anti-horário, fase −6s, 1,6%
   - p3: 0,80·size, 23s, horário, fase −11s, 2,2%
   - p4: 0,59·size, 12s, anti-horário, fase −3s, 1,4%
   - p5: 0,70·size, 27s, horário, fase −17s, 1,8%
   - p6: 0,85·size, 17s, anti-horário, fase −9s, 2,2%

   Cada ponto pisca (alpha 0,25↔1 e escala 0,6↔1) num ciclo de 2,6s, com fases diferentes. O grupo inteiro tem alpha base de 0,75.
8. **Corpo:** respira com escala de 1 a 1,035 em 4s. É recortado em círculo e contém:
   - **Núcleo:** radial centrado em (34%, 28%): branco 90% em 0, `c1` em 20%, `c2` em 58%, `c3` em 100%. Sombra interna escura na parte de baixo.
   - **Redemoinho:** sweep gradient (transparente, `c1` em 70°, transparente em 150°, `c2` em 230°, transparente em 320°). Fica numa área de 1,6× o size, com blur de 7%, `BlendMode.Screen`, alpha 0,65 e uma volta a cada 10s.
   - **Plasma (2 manchas):** círculos de 70% do size, com blur de 8% e `BlendMode.Screen`.
     - A: radial de `c1`, alpha 0,75, deriva em 9s pelos pontos (−18%, −10%, escala 1) → (20%, 8%, escala 1,15) → (−4%, 22%, escala 0,9).
     - B: radial de branco → `c2` → transparente, alpha 0,45, deriva em 12s pelos pontos (16%, 14%, escala 1) → (−20%, 4%, escala 0,85) → (8%, −22%, escala 1,2).
   - **Borda de luz (rim):** brilho interno em `c1` com 7% de blur e 1,2% de spread, mais uma linha interna de 1dp em branco 18%.
   - **Brilho especular:** elipse branca 65%, de 36%×20%, na posição (20%, 9%). Desloca-se em −0,6× o offset do olhar, para dar paralaxe.
9. **Rosto:** acompanha o mouse com offset máximo de ±14dp em x e ±10dp em y. É uma coluna centralizada com espaço de 4,5% do size:
   - **Olhos:** duas pílulas **escuras** de 7%×15% do size, com 13% de espaço entre elas, em gradiente vertical de `#2A2D5E` (topo) a `#0B0C22` (a partir de 60%), sem glow. Cada olho tem dois reflexos brancos: um maior (elipse de 46%×24% da pílula, a 16% da esquerda e 10% do topo, alpha 0,9) e um menor (26%×14%, a 18% da direita e 12% da base, alpha 0,5). Os reflexos somem em Sleeping e Error. A cor `ink` (`#0B0C22`) é também a cor de traço e preenchimento da boca e dos arcos do estado Dormindo. **Atenção:** onde as seções 4.2 a 4.12 falam em traço, contorno ou preenchimento "branco" da boca e dos olhos, leia `ink`; os glows brancos de olhos e boca ficam transparentes. Variantes opcionais (tweak `olhos` no protótipo): dourado (`#FFF6D6` → `#FFD36B`, glow `#FFD36B`), ciano (`#E6FDFF` → `#6FEFFF`, glow `#6FEFFF`) e branco. Nas variantes que não são escuras, `ink` volta a ser branco. Piscam duas vezes seguidas a cada 6s (scaleY vai a 0,1 em 86% e em 90% do ciclo).
   - **Boca:** abaixo dos olhos. Varia por estado (§4.2–4.7).

### 4.2 Idle
- Olhos fazem "glance" em 9s: ficam no centro até 38%, olham para a esquerda (−5% x) entre 42% e 52%, para a direita e um pouco para cima (+5% x, −2% y) entre 56% e 66%, e voltam ao centro em 70%.
- **Boca:** sorriso leve. É um arco inferior de 13%×5% com traço de 1,6% (só a borda de baixo de um retângulo de cantos inferiores totalmente arredondados) e glow branco.

### 4.3 Listening
- Respiração e glow em 2,2s, glow com alpha 0,75, redemoinho em 5s.
- Marcações com alpha 0,55.
- Anéis de onda: escala de 1 a 1,6 e alpha de 0,55 a 0, em 2,2s, com atrasos de 0, 0,73 e 1,46s.
- Partículas puxadas para perto (escala do grupo 0,82, alpha 1).
- Olhos ficam parados e mais altos (18%).
- **Boca:** "o" atento. Círculo de 7% com contorno branco de 1,4%, preenchido de `c3`, pulsando de escala 1 a 1,15 em 2,2s.

### 4.4 Thinking
- Redemoinho em 2,4s com alpha 0,85; plasma A acelera para 4s.
- Satélite e rastro ocultos (os nós do giroscópio fazem esse papel, ver §4.10).
- Marcações com alpha 0,3, girando no sentido contrário em 6s.
- O grupo de partículas inteiro gira (uma volta em 4s), além da órbita própria de cada uma, e fica com alpha 1.
- Olhos baixos (8% de altura) e vagando em 3s: (+5%, −7%) → (−5%, −7%) → (+1%, −3%).
- **Boca:** "hmm" de lado. Traço cheio de 7%×2,2%, que oscila em 3s: (+4% x, −8°) → (−3% x, +6°) → (+1% x, −3°).

### 4.5 Speaking
O corpo NÃO pulsa: uma versão anterior inflava e desinflava a cada 0,3s e parecia um baiacu. A fala aparece na boca, nos olhos, na luz interna e no anel de voz.
- Corpo: só a respiração normal, mais calma (escala 1 a 1,035 em 2,4s).
- Glow: alpha irregular entre 0,6 e 1,0 num ciclo de 1,6s (picos em 15%, 48% e 80% do ciclo).
- Luz da voz: mancha radial (branco → `c1` → transparente) de 50% do size, com centro a 60% de altura, blur de 5% e `BlendMode.Screen`, dentro do núcleo (recortada pelo círculo). Alpha irregular de 0 a 0,45 num ciclo de 1,4s (picos em 12%, 38%, 64% e 90%).
- Redemoinho em 4s; plasma A e B em 3s. Partículas com escala 1,12, piscando em 0,9s.
- Ondas suaves: contorno de 1,5dp, escala 1 a 1,4 e alpha 0,35 a 0, em 1,8s, com atrasos de 0, 0,6 e 1,2s.
- Olhos abertos (13% de altura), que "apertam" nas ênfases da fala, num ciclo de 2,8s: escala Y 0,72 em 14%, 0,82 em 46% e 0,74 em 78%, subindo 0,6 a 0,8% do size nesses momentos e voltando a 1 cerca de 6% do ciclo depois.
- **Boca de fala:** preenchida de `c3`, contorno branco de 1,1%, topo quase reto e fundo bem redondo (raios 18/18/60/60% na horizontal e 25/25/75/75% na vertical), com a língua (elipse de `c1`, alpha 0,85, 56% da largura e 42% da altura, na parte de baixo). Ciclo de 1,6s com easing in-out, imitando sílabas. Tamanhos (largura × altura, em % do size): 0% e 100%: 11×2,4; 10%: 15×7,5; 20%: 12×3,5; 30%: 10×8,5; 42%: 16×4; 52%: 11×2,4; 62%: 14×9; 72%: 13×5; 82%: 9×7; 92%: 15×3.

### 4.6 Error
- O corpo treme num ciclo de 2,4s: parado até 70%, depois x = −2,8%, +2,8%, −1,6%, +1,2% e volta a zero em 86%.
- O núcleo pisca no mesmo ciclo:
  - em 90%: brilho ×1,7 e matiz −25°;
  - em 92%: brilho ×0,6;
  - em 94%: brilho ×1,4 e saturação ×1,6.

  No Compose, um `ColorFilter` com `ColorMatrix` faz isso.
- Glitch: uma mancha ciano (#5CE1E6) em (62%, 42%), em `BlendMode.Screen`, aparece deslocada só entre 90% e 96% do ciclo.
- Glow com alpha 0,3 e parado; reflexo com alpha 0,2; flutuação mais lenta (9s); redemoinho em 18s com alpha 0,4.
- Partículas caem e apagam: o grupo desce 10%, encolhe para escala 0,95 e fica com alpha 0,25. Os pontos ficam na cor `c1` e param de piscar.
- Olhos viram traços de 10%×2,5% e não piscam.
- **Boca:** triste. Arco superior de 12%×4,5%, deslocado +1,5% para baixo, balançando ±7° no mesmo ciclo de 2,4s da tremida.

### 4.7 Cutucada (clique no orbe, 900ms, por cima do estado atual)
- O corpo faz squash & stretch em 0,6s: escala (1, 1) → (1,14, 0,86) → (0,92, 1,1) → (1,04, 0,97) → (1, 1).
- Glow vai a alpha 1; partículas se espalham (escala 1,3).
- Olhos fecham, ficando com 3,5% de altura (um sorriso de olhos).
- **Extras (v3):** bochechas coradas, onda de choque e faíscas (ver §4.13).
- **Boca:** sorrisão aberto de 18%×8,5%, em forma de "D" (cantos 12/12/999/999), preenchido de `c3`, com contorno branco e a língua visível.

### 4.8 Mini-orbe de 28dp
Só glow, núcleo, redemoinho, plasma A, borda de luz e brilho especular. Não tem rosto, anéis, satélite, partículas, reflexo nem flutuação.

### 4.9 Acessibilidade
Com "reduzir movimento" ligado no sistema, desligue todas as animações infinitas e mantenha só as transições de cor e forma.

### 4.10 Halo de energia (v3)
Três anéis finos em volta do orbe, desenhados **acima** do corpo (z-order), com contorno em `c1` e glow externo e interno de 4% do size. Ficam invisíveis em Idle, Sleeping e Error (transição de alpha em 450ms). Geometria base: círculos com insets de −9%, −14% e −19% do size e espessuras de 0,8%, 0,5% e 0,4% do size.
- **Speaking (líquido):** alpha 0,95 / 0,7 / 0,45. Cada anel gira (10s; 13s no sentido contrário; 17s), muda de forma entre elipses irregulares (5s, 6,5s e 8s; use formas de 4 cantos com raios entre 42% e 58%) e pulsa de escala no ritmo da fala, num ciclo irregular de 1,6s: escala 1 → 1+0,05k → 1+0,01k → 1+0,075k → 1+0,02k → 1+0,06k → 1+0,015k → 1 (pontos em 14%, 28%, 42%, 58%, 72% e 86% do ciclo). `k` vale 1, 1,6 e 2,2 do anel de dentro para fora, e a fase de cada anel fica deslocada em 0, −0,5s e −1s.
- **Listening:** mesma ideia, mais calma. Alpha 0,55 / 0,38 / 0,22; giro em 16s, 20s (contrário) e 24s; morph em 8s, 10s e 12s; amplitude do pulso em 35% da de Speaking, ciclo de 2,2s, fases em 0, −0,7s e −1,4s.
- **Thinking (giroscópio):** os anéis viram círculos inclinados em 3D (perspectiva de 4× o size) que precessionam. Anel 1: inclinado 58° no eixo X, uma volta em 7s. Anel 2: inclinado 58° no eixo Y, 9s, sentido contrário. Anel 3: inclinado 50° em X e 35° em Y, 11s. Insets de −12%, −15% e −18%, espessura de 0,6%, alpha 0,7 / 0,6 / 0,5. Cada anel carrega um nó de luz (ponto branco de 4% do size com halo `c1`) que corre ao longo dele: voltas de 2,6s, 3,4s (contrário) e 4,2s. O satélite e o rastro do estado ficam ocultos, porque os nós fazem esse papel. No Compose, desenhe cada anel com `drawOval` dentro de um `graphicsLayer` com `rotationX/rotationY/rotationZ` e `cameraDistance`.

### 4.11 Dormindo (v3)
Cores `#8B86C9 / #41478F / #141235`, bem apagadas.
- Flutuação e reflexo em 9s; corpo respira de escala 0,99 a 1,025 em 7s.
- Glow parado com alpha 0,22; redemoinho em 40s com alpha 0,3; plasma em 26s.
- Partículas com alpha 0,3 e escala 0,88, piscando num ciclo de 6s.
- Olhos viram arcos: o contorno de baixo de uma pílula de 8,5%×4%, com traço de 1,4%, sem preenchimento, na cor `ink`. Não piscam.
- Boca: círculo de 4,5% com contorno de 1,2%, sem preenchimento, que oscila de escala 0,55 a 1 em 7s (sincronizado com a respiração).
- "z", "z", "Z" sobem a partir do canto superior direito, em ciclos de 4,5s com atrasos de 0, 1,5 e 3s. Fontes: 10%, 13% e 16% do size. Cada letra parte de (68%, 6%), (72%, 6%) e (76%, 6%), sobe 20% do size, desloca 14% para a direita, gira de −8° a +10° e cresce de escala 0,6 a 1,2, aparecendo (alpha 0,9 em 20%) e sumindo no fim. Texto branco bold (Space Grotesk) com glow `c1`.
- A aura do painel cai para alpha 0,05.

### 4.12 Despertar (v3)
Ao sair de Sleeping o estado vira Idle e dispara um "susto" de 800ms (por cima do Idle, como a cutucada):
- corpo: escala 0,99 → 1,13 → 0,97 → 1,03 → 1;
- glow com alpha 0,95;
- olhos arregalados de 9%×24%;
- boca "o" de 8%×9%, preenchida de `c3`;
- um "!" branco bold de 20% do size aparece acima do orbe (a −16% do topo), sobe 6%, cresce a 1,25× e some em 850ms.

### 4.13 Cutucada v3, extras
- **Bochechas:** duas elipses de 15%×8,5%, uma em x = 17% da esquerda e outra em x = 17% da direita, a 56% de altura. Radial de rosa (#FF82A5 a 85%) para transparente, com blur de 1,2%. Alpha de 0 a 1 em 250ms.
- **Onda de choque:** círculo do tamanho do orbe com contorno de 1,2%, que vai de escala 1 a 2,2 e de alpha 0,8 a 0 em 750ms. A cor do contorno vai de branco a `c1`.
- **Faíscas (10):** pontos brancos de 3,2% com glow `c1`, um a cada 36°. Cada um nasce na borda do orbe e voa para fora, com alcance de 26% a 44% do size (varie entre eles: 0,3 / 0,42 / 0,26 / 0,38 / 0,33 / 0,44 / 0,28 / 0,4 / 0,31 / 0,36), encolhendo até escala 0,2 e sumindo em 850ms, com easing de desaceleração.

### 4.14 Paralaxe e aura (v3)
- **Paralaxe do cursor:** o offset normalizado do mouse (o mesmo do olhar, ±14 em x e ±10 em y) é aplicado em camadas com pesos diferentes, para dar profundidade: rosto ×1,0 (já existia), partículas ×1,3 (seguem o mouse), glow ×0,5, brilho especular ×−0,6 (vai para o lado oposto). Transição de 500ms com ease-out nas partículas e no glow.
- **Aura do painel:** círculo de 460dp atrás do orbe, na cor `c1` do estado, com blur de 80dp. Alpha por estado: Idle 0,16, Sleeping 0,05, Listening 0,22, Thinking 0,24, Speaking 0,32, Error 0,20. No tema claro, multiplique por 0,55. Faça a transição da cor e do alpha em 800ms. Corte o que passar do painel.

## 5. Sugestão de implementação
- `ui/orb/NoxOrb.kt`: `@Composable fun NoxOrb(mood: NoxMood, size: Dp, look: Offset = Offset.Zero, poked: Boolean = false, compact: Boolean = false)`.
- Desenho em `Canvas`/`drawBehind`, com um único `rememberInfiniteTransition` e várias `animateFloat`, uma para cada ritmo.
- Blur: no Compose Desktop (Skia), `Modifier.blur()` e `graphicsLayer { renderEffect = BlurEffect(...) }` funcionam. Ponha cada camada com blur num `Box` próprio.
- Blend Screen: `drawCircle(brush, blendMode = BlendMode.Screen)` dentro de `graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }`.
- Recorte do corpo: `Modifier.clip(CircleShape)`.
- Anéis de marcações e rastro: em vez de máscara, desenhe com `drawArc` (traços curtos em loop para as marcações e um arco com sweep gradient para o rastro).
- Olhar acompanhando o mouse: `Modifier.onPointerEvent(PointerEventType.Move)` na janela, com o offset normalizado e limitado.
- Boca: um único `drawPath`/`drawRoundRect` cujos parâmetros (largura, altura, raios, tipo de traço ou preenchimento) são interpolados entre os estados.

## 6. Avatares alternativos e seletor (v4)

O orbe passa a ser **um de seis avatares**. O usuário escolhe pelo botão **"Avatar: <nome>"** no cabeçalho da janela (à esquerda de Debug). Arquivos de referência: `Avatar.dc.html` (componente, aceita `kind` e `mood`), `AvatarEstados.dc.html` (grade 5×6), `Conceitos.dc.html` (os cinco lado a lado) e `Main.dc.html` (seletor).

### 6.1 Modelo
```kotlin
enum class NoxAvatar { Orb, Mercury, Swarm, Terminal, Iris, Spirit }   // padrão: Orb
// NoxMood é o mesmo de antes: Idle, Sleeping, Listening, Thinking, Speaking, Error
@Composable fun NoxAvatarView(avatar: NoxAvatar, mood: NoxMood, size: Dp, look: Offset, poked: Boolean, woke: Boolean)
```
- Persistir a escolha (ex.: `java.util.prefs.Preferences` ou um JSON em `~/Library/Application Support/MinhaAssistente/`). O popover da barra de menus lê a mesma preferência e mostra o avatar escolhido em Idle (96dp).
- `look` é o mesmo offset normalizado do olhar do orbe. No `Avatar.dc.html` ele chega como `lx`, `ly` em −1..1 (= offset ÷ 14 e ÷ 10).
- Palco de cada avatar: quadrado 270 × 270 com cantos de 34, fundo radial `#181B2E → #0A0B12` (centro a 50% × 38%) e contorno de 1dp branco a 7%. Na janela o palco é desenhado a 1,18× (≈ 319dp). Nos outros avatares a aura atrás é da cor de acento e vira `#FF7A6B` em Error.
- Cutucada (`poked`, 750ms): o palco inteiro vai a escala 1,07×0,94 com rotação −2°, depois 0,97×1,04 com +2°, e volta a 1. Despertar (`woke`, 900ms): escala 0,96 → 1,05×1,07 → 1.
- Efeitos comuns por cima de qualquer avatar (exceto Terminal, que tem os próprios): em **Sleeping**, três "z" (18, 14 e 11sp) sobem 44dp à direita do topo em ciclos de 3,6s, defasados em 1,2s; em **Error**, vinheta vermelha `#FF5046` a 55% pulsando entre 25% e 75% em 1,1s, e uma bolinha `#FF5B4F` de 30dp com "!" no canto superior direito.
- Os mini-avatares ao lado das mensagens do chat continuam sendo o mini-orbe (28dp), qualquer que seja o avatar escolhido. É a marca do Nox.

### 6.2 Mercúrio (`Mercury`) — metal líquido
- **Corpo:** círculo de 150 que deforma o `border-radius` num ciclo de 7s, mais 6 gotas (diâmetros 40, 30, 24, 34, 20, 16) em órbita com raios que oscilam entre ~100 e ~66. Tudo isso é desenhado em preto/branco, com blur de 9 e contraste ×22 por cima, o que funde as gotas ao corpo (efeito "gosma"). O resultado é pintado com um gradiente cromado vertical (branco → `#C9D2FF` → `#6F7FE0` → `#161A4A` → `#7D8CF0` → branco) que desliza em 7s e gira o matiz entre −35° e +45° em 12s.
- **Compose:** é o único que pede shader. Use `RuntimeShader` (SkSL) com metaballs: soma de campos `r²/d²` com limiar, ou desenhe as formas num layer offscreen e aplique blur + `ColorFilter` de contraste.
- **Rosto:** olhos de 13×22 (gradiente `#232652 → #0B0C22`, brilho branco de 5×6) e uma boca que só aparece ao falar. Brilho especular de 40×16, a −28°.
- **Idle:** deformação lenta; gotas orbitando.
- **Listening:** corpo estica (1,02 × 1,14) e sobe 4; as gotas deixam de orbitar e são **sugadas** de fora (de 135 a 18 do centro, encolhendo até 0,25, 1,8s em loop, defasadas); olhos de 28 de altura.
- **Thinking:** corpo encolhe a 0,58; gotas giram 4,5× mais rápido num raio de ~0,75; olhos sobem 8, vão 5 à direita e ficam a 0,78×.
- **Speaking:** corpo oscila (0,8s: 1,05×0,97 → 0,97×1,04); gotas pulam para fora e voltam (0,75s); olhos apertam a 0,7 em dois momentos do ciclo; boca de 26 de largura que abre de 5 a 17 de altura.
- **Sleeping:** brilho a 50% e saturação a 55%; corpo a 0,78 e 10 abaixo; gotas coladas ao corpo (raio ~0,5) e 3,5× mais lentas; olhos viram traços de 16×3; boca "o" de 10 respirando (escala 0,7 ↔ 1,1 em 4s).
- **Error:** matiz do cromado girado em +135° (fica vermelho); corpo treme (translate ±2 a cada 120ms) e deforma em 1,2s; gotas disparam rápido (0,32s); olhos de 17×11 inclinados ±16° (preocupados); boca de 24×4 tremendo.

### 6.3 Enxame (`Swarm`) — 90 partículas em 3D
- **Partículas:** 90 pontos (tamanhos 3, 4,5 ou 6; cores alternando `#7FE3F5`, `#A99BFF`, `#FFFFFF`, `#8CF0D2`; glow de 2× o tamanho) com perspectiva de 560. Cada uma flutua ±3 em Y num ciclo de 3,2s defasado pelo índice. Ao mudar de estado cada partícula interpola até a nova posição em 1,1s, com atraso individual de 0 a 0,35s.
- **Formas por estado:**
  - Idle: esfera de raio 92 (espiral de Fibonacci, passo 2,399963 rad) girando em Y a cada 20s.
  - Listening: três anéis de raios 34, 66 e 98 (14, 28 e 48 partículas) girando em Z a cada 30s.
  - Thinking: galáxia de 3 braços (raio de 8 a 108, ângulo `t·7 + braço·2,0944`), inclinada 64° em X e girando em Z a cada 5s.
  - Speaking: linha horizontal de −100 a +100; cada partícula oscila em Y com amplitude 8 + 30·sen(π·i/89)^1,2 por 0,9s (vira uma onda de som). O glow central cai a 25%.
  - Sleeping: a esfera encolhida a 0,42, com brilho a 60%, giro de 70s e "respiração" (escala 1 ↔ 1,08 em 5s).
  - Error: posições aleatórias fixas (±115, ±95, ±80), todas em `#FF6B5E`, tremendo 6dp a cada 0,25s, e o grupo todo sacudindo ±2.
- **Compose:** `Canvas` com projeção feita à mão (rotação em Y/X/Z, divisão por `perspective − z`) e `animateFloatAsState` para interpolar cada coordenada. Ordene por profundidade para pintar as de trás primeiro.

### 6.4 Terminal (`Terminal`) — 8-bit num CRT
- **Monitor:** 212×190 com cantos de 20 (`#1A1C26`), tela com cantos de 12 e fundo radial verde-escuro (`#0C2A1A → #04120A`), pé e base, LED verde `#35FF8A` no canto. O rosto é uma grade de **pixels de 10×10** numa área de 160×100 (`shape-rendering: crispEdges`), cor `#6BFFA8` com glow.
- **Efeitos de tela:** scanlines (linhas de 2dp com alpha 30% a cada 4), faixa de brilho de 50dp descendo em 4,5s, reflexo no canto superior esquerdo, cintilação a cada ~4s (alpha 94–96%).
- **Legenda:** `> ` + texto monoespaçado (JetBrains Mono 13) **digitado caractere a caractere** na troca de estado, com cursor de 8×14 piscando a 1s.
  Textos: "pronto", "zzz…", "ouvindo…", "pensando…", "respondendo", "falhou :(".
- **Idle:** olhos de 2×2 pixels com piscada a cada 5s; sorriso de 8 pixels.
- **Listening:** olhos altos (2×4,5 pixels), duas "antenas" de 1 pixel que piscam nas laterais, boca em "o" retangular.
- **Thinking:** olhos de 1 pixel que olham para a esquerda e a direita a cada 3s (±20), boca reta de 4 pixels, três pontos no canto que acendem em sequência, LED piscando rápido.
- **Speaking:** olhos felizes (arco de 3 pixels) e 8 barras de equalizador de 8 pixels de largura que mudam de altura em passos (0,66s, defasadas em 0,11s).
- **Sleeping:** olhos viram traços de 24×6, boca de 16×10, tela a 55% de brilho, LED âmbar `#FFB347` respirando em 3s, cursor lento (2,4s).
- **Error:** tela e pixels em vermelho (`#3A0D0D → #140404`; pixels `#FF6B5E`), olhos em "X" de 5 pixels, boca de frown, um "!" de pixels piscando, o rosto "glitcha" (desvios de −4/+5 a cada 0,35s), monitor tremendo e LED piscando a 0,25s.
- **Compose:** `Canvas` com `drawRect` por pixel (grade 16×10 de células de 10) e `drawLine` para as scanlines. Troque o conjunto de pixels por estado; o texto digitado é uma `String` que cresce com `LaunchedEffect`.

### 6.5 Íris (`Iris`) — olho mecânico com diafragma
- **Camadas (de fora para dentro):** anel de 226 com marcações (traços de 0,9° a cada 5°, laranja `#FFB564` a 55%, gira em 50s); anel tracejado de 208 (gira ao contrário em 36s); carcaça de 196 (gradiente `#4D5468 → #1B1E2A → #080A10`, glow laranja); "poço" de 168, preto; **íris** de 128 (radial `#FFD27A → #FF9D2E → #D9561A → #7A1F08 → #2A0A02`, fibras de 1,1° a cada 4,6° girando em 90s); **pupila** preta de 44 (`--p`); **diafragma** de 8 lâminas (octógono com raio `--r`); córnea com reflexo.
- **Olhar:** íris ±11/±9, pupila ±7/±6 e reflexo na córnea −6/−6 (oposto), seguindo o mouse.
- **Idle:** pupila respira 44 ↔ 50 (5s); diafragma a raio 80 e **pisca** (fecha a 2 e reabre em ~0,3s a cada 6s).
- **Listening:** pupila dilata a 68; diafragma totalmente aberto (96); anel externo a 100% de alpha e 4× mais rápido; três ondas laranja **convergem** para dentro (2s, defasadas em 0,66s).
- **Thinking:** pupila 26 ↔ 32 (1,3s); diafragma fecha a ~53 e pulsa (1,4s); **laser ciano** `#8FF5FF` de 2dp varre a íris de 6% a 94% de altura (1,5s, vai e volta); anel externo gira ao contrário a 7s.
- **Speaking:** pupila pulsa 38 → 58 → 42 → 62 → 40 → 54 (0,9s); diafragma acompanha (76 → 92); três ondas claras saem da pupila (de 0,8× a 3,4×, 1,2s).
- **Sleeping:** diafragma **fechado** (raio 2, com fresta de 7 respirando em 5s), carcaça a 62% de brilho, anéis quase parados (140s) e a 22–40% de alpha.
- **Error:** íris vermelha (`#FFA08F → #FF4338 → #A3120C → #4D0603`), anel de marcações vermelho, glow vermelho na carcaça, íris tremendo, pupila e diafragma **saltando em passos** (0,5s e 0,45s) e laser vermelho varrendo em 0,6s.
- **Compose:** `Canvas`; o diafragma é um `Path` octogonal com `clipPath` (`ClipOp.Difference`) sobre as lâminas; o círculo da íris usa `Brush.radialGradient`; as fibras são `drawArc` curtos ou um `sweepGradient`.

### 6.6 Espírito (`Spirit`) — fantasminha
- **Corpo:** silhueta de 130×160 (cabeça redonda de raio 53, base com 4 ondas de 26,5), gradiente vertical `#FFFFFF → #CFC6FF` com sombreado radial lilás (`#7864E6` a 38% na borda). Glow `#BEAAFF` a 45%. **Rastro:** duas cópias borradas (alpha 22% e 10%) com atraso de 0,14s e 0,28s na animação. Bracinhos de 16×30 (cápsulas brancas) e sombra elíptica embaixo. Três faíscas de 4 pontas piscando ao redor.
- **Rosto:** olhos de 15×24 (gradiente `#2B1D52 → #150D2E`, brilhos de 6×7 e 3×3), bochechas rosa de 20×11, boca que muda de forma.
- **Idle:** flutua ±10 em 3,6s com balanço de ±2,5°; piscada a cada 5,5s; sorriso de 16×9.
- **Listening:** cabeça inclinada ~10°; olhos maiores (18×30); bochechas mais fortes; boca em "o" de 9; bracinhos abertos a ±40°; três ondas de som saem do lado direito.
- **Thinking:** balança devagar (4,5s, ±5°); olhos 5 à direita e 6 acima, com altura de 19; boca reta que gira ±6°; um braço sobe a 115° (mão no queixo); balão de pensamento branco com 3 pontos pulando no canto.
- **Speaking:** **pula** (0,55s: sobe 14 e alonga 0,96×1,05, desce achatando 1,05×0,94); bracinhos balançam de 10° a 52° (defasados em 0,12s); boca de 16×12 que abre e fecha (0,7s) com língua rosa `#FF7FA5`.
- **Sleeping:** cabeça caída (inclinada 7° e 8 abaixo), respirando (escala até 1,02×1,03 em 5s); olhos viram traços de 17×3; boca "o" de 8 respirando; bracinhos para baixo; brilho a 78%; faíscas quase apagadas; "z"s por cima.
- **Error:** **em pânico**: treme (±3, ±2° a cada 160ms), corpo e rastro avermelhados (matiz +105°), olhos arregalados de 20×30, bochechas vermelhas, boca "O" tremendo, bracinhos balançando rápido entre 55° e 85°, e uma gota de suor azul `#9FD8FF` de 9×13 escorrendo da testa a cada 1s.
- **Compose:** `Path` com `cubicTo` para a cabeça e `arcTo` para as ondas; rastro = o mesmo path desenhado 2× com alpha e deslocamento no tempo; olhos e boca com `drawRoundRect`.

### 6.7 Arquivos de referência dos avatares
- `EstadosMercurio.dc.html`, `EstadosEnxame.dc.html`, `EstadosTerminal.dc.html`, `EstadosIris.dc.html`, `EstadosEspirito.dc.html`: uma folha por avatar, no formato de `OrbStates.dc.html`, com os 6 estados, gatilho, descrição do movimento e cores. Cada uma traz o CSS completo daquele avatar (nomes de classe por avatar: `m-*`, `s-*`, `t-*`, `i-*`, `g-*`), então servem como fonte para medidas, tempos e cores.
- `Avatar.dc.html`: o mesmo avatar como componente (props `kind`, `mood`, `zoom`, `lx`, `ly`, `poked`, `woke`), usado por `Main.dc.html` e `MenuBar.dc.html`.
- `AvatarEstados.dc.html`: grade 5×6 com todos os avatares em todos os estados.
- Ordem sugerida de implementação: Orbe (já especificado), Espírito e Terminal (mais simples), Íris e Enxame, Mercúrio por último (exige shader).
