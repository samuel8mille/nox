# Voz (entrada e saída por fala)

Permite conversar com a Nox por voz: o microfone é transcrito offline (Whisper) e as respostas podem
ser faladas em voz alta pela síntese nativa do sistema.

## Requisitos

### Requirement: Transcrição offline do microfone
A Nox DEVE transcrever a fala do usuário localmente, sem enviar áudio para a nuvem, usando o Whisper
quando houver um modelo GGML disponível.

#### Scenario: Botão de microfone disponível
- **WHEN** há um modelo Whisper acessível (ex.: `ggml-base.bin`)
- **THEN** o compositor exibe o botão de microfone habilitado

#### Scenario: Gravar e transcrever
- **WHEN** o usuário ativa o microfone, fala e desativa
- **THEN** o áudio é transcrito offline e o texto resultante é colocado no campo de mensagem

### Requirement: Microfone indisponível sem modelo
Quando não houver modelo Whisper, o botão de microfone DEVE ficar indisponível, sem quebrar o chat
por texto.

#### Scenario: Sem modelo Whisper
- **WHEN** nenhum modelo GGML é encontrado
- **THEN** a entrada de voz fica desligada e o chat por texto continua funcionando normalmente

### Requirement: Microfone bloqueado durante processamento
O microfone DEVE ficar bloqueado enquanto a Nox está pensando ou falando, para evitar capturar áudio
no momento errado.

#### Scenario: Nox está respondendo
- **WHEN** o estado é Thinking ou Speaking
- **THEN** o botão de microfone fica desabilitado até a Nox voltar ao repouso

### Requirement: Síntese de voz da resposta
A Nox PODE falar a resposta em voz alta usando o TTS do sistema (`say` no macOS), de forma offline.
Em sistemas sem `say`, a fala é silenciosamente ignorada, sem erro.
