# Subaru DL — Android

Baixador de vídeos, áudios e galerias para Android. É o irmão do [Subaru DL para PC](../subaru-dl): o mesmo motor
(**yt-dlp** + **gallery-dl** + **ffmpeg** + **aria2**, todos embutidos) numa interface Material 3 escura, baseada no
design "Rally Precision Dark" do Stitch (azul de rally, dourado, Space Grotesk / Manrope).

## O que faz

| Tela | O que tem |
| --- | --- |
| **Início** | Cola/compartilha um link → prévia (capa, título, canal, duração, qualidade) → vídeo ou áudio, qualidades reais com tamanho estimado, formato, **faixa de áudio** (a original sempre é a padrão), **legendas** (as do vídeo e as automáticas; embutidas ou em arquivo), pasta de destino, organização em subpastas, opções avançadas (trecho, capítulos, codec, formato exato, metadados, capa). Playlists abrem com a lista de vídeos (com capas) para marcar. |
| **Fila** | Telemetria (velocidade ao vivo, pico, gráfico), abas Baixando / Concluídos / Falhas, filtro e atalhos MP4/MP3/4K, pausar, retomar, cancelar, tentar de novo, abrir, compartilhar, apagar arquivos, armazenamento. |
| **Playlists** | Download em lote: uma playlist ou vários links, escolha de faixas, formato (MP3 320k, M4A, OPUS, FLAC, vídeo 1080p…), pasta com o nome da playlist, numeração, capas. |
| **Galeria** | gallery-dl: perfis, posts e álbuns (Instagram, X, Pixiv, Bluesky, Reddit, mangás…), prévia com miniaturas para marcar o que baixar, pasta só do perfil / por site / personalizada, pular o que já baixou. |
| **Ajustes** | Qualidade e formato padrão, Wi-Fi apenas, downloads simultâneos, Turbo (aria2), notificações, legendas, nomes e pastas, rede (proxy, limite, argumentos), **contas** (login dentro do app, `cookies.txt` ou cookies enviados do PC pelo Wi-Fi), dependências e atualização do yt-dlp/gallery-dl, logs. |

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
  ui/        theme, components, home, queue, batch, gallery, settings
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

O APK pronto para instalar fica em `dist/SubaruDL-1.0.0.apk` (arm64 + x86_64, ~170 MB). A chave de assinatura
(`subaru-dl.jks` e `keystore.properties`) **não** vai para o git: guarde uma cópia, porque é ela que permite atualizar o app
instalado depois.

## Receber cookies do PC (como funciona)

Enquanto a tela "Receber do PC" está aberta, o celular escuta na porta 38475 e mostra um código de 8 letras. O PC varre a
rede local procurando essa porta (`GET /subaru`, que devolve um "sal"), deriva uma chave do código (PBKDF2-SHA256, 120 mil
voltas), cifra o `cookies.txt` com AES-256-GCM e envia (`POST /cookies`). Sem o código certo nada é aceito; depois de 5
tentativas erradas o celular fecha o recebimento; os cookies não viajam em texto puro. O receptor só existe com a tela
aberta. O código está em `data/CookieReceiver.kt` (celular) e `src/main/celular.ts` (PC).

Teste no emulador: `adb forward tcp:38475 tcp:38475`; o PC acha o emulador em 127.0.0.1.

## Turbo (aria2c)

O aria2c do Android não acha os certificados do sistema e recusava todo site https ("aria2c exited with code 1").
Agora o app aponta o arquivo de certificados embutido (`--ca-certificate`), mostra o progresso do aria2c na barra
e, se o Turbo falhar por qualquer motivo, refaz o download sem ele em vez de dar erro.

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

Celular arm64 de verdade (só o emulador x86_64), login em sites que pedem verificação extra, Wi-Fi apenas com troca de rede
no meio do download, Turbo (aria2) em downloads longos, Instagram (precisa de conta).
