# Subaru DL — Android

Baixador de vídeos, áudios e galerias para Android. É o irmão do [Subaru DL para PC](../subaru-dl): o mesmo motor
(**yt-dlp** + **gallery-dl** + **ffmpeg** + **aria2**, todos embutidos) numa interface Material 3 escura, no padrão dos
apps do Android (a régua é o Seal e os apps do Google), nas três cores Subaru: azul-marinho, amarelo e dourado.
Desde a 1.3.0 tudo acontece numa tela só: colar ou compartilhar o link e acompanhar o download no mesmo lugar.

## Baixar

A versão mais nova fica em [Releases](https://github.com/ailtoncja/subaru-dl-mobile/releases/latest): baixe o
`SubaruDL-<versão>.apk` no celular e abra. Na primeira vez, o Android pede para permitir a instalação por essa fonte.
Cada versão instala por cima da anterior sem perder os ajustes. Precisa de Android 10 ou mais novo, num celular de 64 bits.

## O que faz

| Onde | O que tem |
| --- | --- |
| **Tela principal** | Campo do link (colar, ou cola sozinho quando o link chega pelo teclado), a escolha **Vídeo / Áudio / Imagens**, avisos (lendo o link, erro em português com o caminho, sem internet, esperando o Wi-Fi) e a lista dividida em **Baixando** (velocidade total, Pausar tudo), **Na fila**, **Falhas** e **Concluídos** (Limpar, com **Desfazer** no aviso). Cada falha diz a causa numa linha e traz o caminho num botão: **Baixar como vídeo** (pediu MP3 de um vídeo sem som), **Entrar na conta** (o site pediu login), **Baixar como imagens** (o link não tem vídeo), **Tentar sem o Turbo** ou **Tentar de novo**. Cada cartão tem capa, título, barra de progresso e uma linha com %, tempo que falta e velocidade (ou formato, qualidade e tamanho). Busca por título, site, formato (mp3, mp4) e qualidade (1080p, 4k). Botão **Colar link** embaixo, ao alcance do polegar, e o espaço do celular no pé da lista. |
| **Folha de um link** | Sobe de baixo quando o link é lido: capa (toque para ver no site), título, canal, vídeo ou áudio, qualidades reais em chips com o tamanho estimado, formato, **faixa de áudio** (a original sempre é a padrão), **legendas** (as do vídeo e as automáticas; embutidas ou em arquivo), pasta, organização em subpastas e opções avançadas (trecho, capítulos, codec, formato exato, metadados, capa). Playlists e canais mostram os vídeos com capa para marcar. Botões **Pôr na fila** e **Baixar**. |
| **Vários links** | Colar ou compartilhar mais de um link abre o lote: vídeo ou áudio, qualidade, formato e legenda para todos, cada link com seus itens para marcar, uma pasta por playlist, numeração e capas. |
| **Imagens** | Com **Imagens** escolhido, o link vai para o gallery-dl: perfis, posts e álbuns (Instagram, X, Pixiv, Bluesky, Reddit, mangás…), grade de miniaturas para marcar no jeito do Google Fotos (a marcada encolhe com o círculo amarelo), carregar mais, pasta e organização, pular o que já baixou. Links que só podem ser vídeo (YouTube, TikTok, arquivo .mp4…) vão para o vídeo mesmo com Imagens escolhido. |
| **Detalhes de um download** | Toque no cartão (ou nos três pontos dos concluídos): andamento ao vivo, pausar, retomar, cancelar, tentar de novo, entrar na conta quando o site pede login, abrir, compartilhar, copiar o link, abrir no site, baixar de novo, tirar da lista e apagar os arquivos (com confirmação). |
| **Tablet** | A lista e os Ajustes ficam numa coluna central de até 720 dp, com a barra de cima alinhada a ela; as folhas usam a largura padrão do Material (640 dp). |
| **Ajustes** | Qualidade e formatos padrão, ler o link copiado ao abrir, pastas de vídeos, músicas e imagens, só no Wi-Fi, downloads ao mesmo tempo, Turbo (aria2), avisos e som do Boxer; e as páginas de legendas, nomes e pastas, imagens (gallery-dl), rede avançada (proxy, limite, argumentos), **contas** (login dentro do app, `cookies.txt` ou cookies enviados do PC pelo Wi-Fi), ferramentas e atualização do yt-dlp/gallery-dl e registro. |

Outras coisas do Android: **Compartilhar → Subaru DL** a partir de qualquer app, links do YouTube abrem direto no app,
link copiado é analisado ao abrir (opcional), download em segundo plano com notificação de progresso (serviço em
primeiro plano), retomada depois que o app fecha.

## Onde ficam os arquivos

Sem pedir nenhuma permissão: vídeos em `Filmes/Subaru DL`, áudios em `Música/Subaru DL`, imagens em
`Imagens/Subaru DL` (MediaStore). Também dá para escolher qualquer pasta pelo seletor do Android (por download).

## Como funciona por dentro

- `io.github.junkfood02.youtubedl-android` (Maven Central) traz Python 3.12, yt-dlp, ffmpeg, aria2 e o **QuickJS**
  (que resolve o JavaScript do YouTube no lugar do Deno).
- O **gallery-dl** vai num zip em `assets/gallerydl.zip` (Python puro + `requests`) e roda no mesmo Python.
- O yt-dlp se atualiza sozinho (a cada 12 h, ao abrir) e pelo botão em Ajustes. Uma trava impede atualizar enquanto algo
  está sendo baixado ou lido.
- O download acontece numa pasta de trabalho do app e cada arquivo é entregue ao destino quando termina.

```
app/src/main/java/com/subarudl/app/
  SubaruApp.kt, MainActivity.kt, MainViewModel.kt
  data/      modelos, configurações, armazenamento (MediaStore/SAF), cookies, logs
  engine/    Args (monta o comando do yt-dlp), Ytdlp, GalleryDl, InfoParser, Engine
  service/   DownloadManager (a fila), DownloadService (primeiro plano), Notifier
  ui/        AppRoot (telas e voltar), theme, components,
             home (tela principal, lista, folhas de link e de detalhes), settings (Ajustes, login, receber cookies)
app/src/main/assets/gallerydl.zip
```

## Como testar (Windows, igual ao PitStopHub)

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=C:/Users/Administrator/jt -Djdk.net.unixdomain.tmpdir=C:/Users/Administrator/jt"
./gradlew.bat :app:assembleDebug          # APK de teste
./gradlew.bat :app:assembleRelease        # APK assinado (usa keystore.properties + subaru-dl.jks)
emulator -avd Medium_Phone_API_37.0 -no-snapshot-save
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "https://youtu.be/..." -n com.subarudl.app/.MainActivity
adb logcat -s SUBARU:I
```

O APK pronto para instalar fica em `dist/SubaruDL-<versão>.apk` (arm64 + x86_64, ~170 MB). Ele passa do limite de
100 MB por arquivo do GitHub, então não vai no git: cada versão é publicada como Release (tag `v<versão>`). A chave de assinatura
(`subaru-dl.jks` e `keystore.properties`) **não** vai para o git: guarde uma cópia, porque é ela que permite atualizar o app
instalado depois.

## Receber cookies do PC (como funciona)

Enquanto a tela "Receber do PC" está aberta, o celular escuta na porta 38475 e mostra um código de 8 letras. O PC varre a
rede local procurando essa porta (`GET /subaru`, que devolve um "sal"), deriva uma chave do código (PBKDF2-SHA256, 120 mil
voltas), cifra o `cookies.txt` com AES-256-GCM e envia (`POST /cookies`). Sem o código certo nada é aceito; depois de 5
tentativas erradas o celular fecha o recebimento; os cookies não viajam em texto puro. O receptor só existe com a tela
aberta. O código está em `data/CookieReceiver.kt` (celular) e `src/main/celular.ts` (PC).

Teste no emulador: `adb forward tcp:38475 tcp:38475`; o PC acha o emulador em 127.0.0.1.

## Som do Boxer

Ajustes → Avisos → Som do motor Boxer: ao terminar um download com sucesso, a notificação toca o ronco do motor
(`res/raw/boxer_wrc.mp3`, um áudio de 4,5 s fornecido pelo autor). Ligar a opção toca o som para ouvir.
Falhas avisam em silêncio. O som segue o volume de notificações e o modo "Não perturbe" do aparelho.

## Turbo (aria2c)

O aria2c do Android não acha os certificados do sistema e recusava todo site https ("aria2c exited with code 1").
Agora o app aponta o arquivo de certificados embutido (`--ca-certificate`) e, se o Turbo falhar por qualquer motivo,
refaz o download sem ele em vez de dar erro. O aria2c reescreve o progresso na mesma linha (`\r`), que nunca chegava ao
app; desde a 1.3.0 ele manda um resumo por linha a cada segundo (`--summary-interval=1 --show-console-readout=false`),
e o cartão mostra porcentagem, velocidade e tempo que falta também com o Turbo. O limite de velocidade da Rede
avançada também vale com o Turbo: vai para o aria2 pelo próprio parâmetro dele (`--max-overall-download-limit`).

## Diferenças em relação ao PC

- Sem bandeja, atalhos globais e monitor de área de transferência em segundo plano (o Android não deixa); no lugar,
  "Compartilhar" e a leitura do link copiado ao abrir o app.
- Sem cookies direto do navegador (o Android não deixa um app ler os cookies do Chrome). Os cookies entram de três jeitos:
  1. login numa janela dentro do app;
  2. **Receber do PC pelo Wi-Fi**: o Subaru DL do computador gera o `cookies.txt` a partir do navegador e envia ao celular
     (Ajustes → Contas e cookies → Receber do PC; no PC, Configurações → Rede e login → Enviar para o celular);
  3. importar um `cookies.txt`, ou abrir/compartilhar o arquivo com o Subaru DL (WhatsApp, Telegram, Drive, USB).
- Baixar com o app fechado exige a notificação fixa de progresso.
- Não vai para a Play Store (política do Google contra baixadores do YouTube): instalação por APK.

## Ainda não testado

Celular arm64 de verdade (só o emulador x86_64), navegação de 3 botões (o emulador usado não desenha a barra), login em
sites que pedem verificação extra, Wi-Fi apenas com troca de rede no meio do download, Turbo (aria2) em downloads longos,
Instagram (precisa de conta). No emulador o YouTube pediu verificação de robô ("Sign in to confirm you're not a bot"):
com a conta conectada em Ajustes → Contas e cookies isso costuma passar.
