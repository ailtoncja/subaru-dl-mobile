# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Público geral de língua portuguesa no Android: qualquer pessoa que queira guardar no celular um vídeo, uma música,
uma playlist ou as fotos de um perfil, sem saber o que é yt-dlp (confirmado: o mesmo público do Subaru DL para PC, com a
intenção de divulgar). O autor (Ailton) usa no dia a dia e passa o APK para amigos.

Situação típica: a pessoa está em outro app (YouTube, Instagram, TikTok, X, navegador) e quer aquele conteúdo como
arquivo no celular. O link chega dos dois jeitos, por igual (confirmado): pelo "Compartilhar → Subaru DL" do próprio
app ou copiando e colando.

## Product Purpose

Subaru DL Mobile é o irmão Android do Subaru DL para PC: baixa vídeos, áudios, playlists, canais e galerias de imagens
de mais de 1.800 sites, com yt-dlp, gallery-dl, ffmpeg e aria2 embutidos no APK.

Sucesso: compartilhar ou colar um link e ter o arquivo certo, na qualidade certa, na galeria ou na pasta de música do
celular, sem entender as ferramentas e sem ficar olhando a tela enquanto baixa.

Os quatro trabalhos são de primeira classe, como no PC: vídeo, áudio (MP3), playlists e canais, e imagens de perfis.

## Positioning

- **Tudo embutido no APK:** nada para instalar à parte, nem Termux, nem servidor.
- **Simples e em português do Brasil:** feito para quem não entende de yt-dlp.
- **Mesmas funções do PC (confirmado):** o que o programa do PC faz, o celular também faz.
- **Junto com o PC:** recebe os cookies do PC pelo Wi-Fi (cifrado, com código de 8 letras), para baixar o que pede login.
- **Identidade Subaru:** família visual do programa do PC e das extensões do autor.

## Operating Context

- Android 10 ou mais novo (minSdk 29). Instalado por APK, fora da Play Store (confirmado); a chave de assinatura própria é
  o que permite instalar versões novas por cima.
- O link chega por Compartilhar (intent `SEND` de texto), por links do YouTube abertos no app, ou colado; o app pode
  analisar o link copiado ao abrir (opcional).
- O download continua em segundo plano, com notificação de progresso (serviço em primeiro plano), e volta depois que o
  app fecha. Pode ficar só no Wi-Fi.
- Os arquivos vão, sem pedir permissão, para `Filmes/Subaru DL`, `Música/Subaru DL` e `Imagens/Subaru DL`; dá para
  escolher outra pasta por download.
- Login em sites: dentro do app (WebView), por `cookies.txt` (abrir ou compartilhar o arquivo para o app) ou recebendo
  os cookies do PC pelo Wi-Fi.
- O yt-dlp se atualiza sozinho (ao abrir, a cada 12 h) e por botão nos Ajustes.

## Capabilities and Constraints

Capacidades confirmadas (detalhes no README):

- Prévia do link com capa, título, canal e duração; vídeo ou áudio; qualidades reais com tamanho estimado; formato;
  faixa de áudio (a original como padrão); legendas do vídeo ou automáticas, embutidas ou em arquivo; pasta e subpastas;
  opções avançadas (trecho, capítulos, codec, formato exato, metadados, capa).
- Playlists e lotes: vários links, escolha de itens, qualidade e legenda, pasta com o nome da playlist, numeração.
- Fila: baixando, concluídos e falhas; pausar, retomar, cancelar, tentar de novo, abrir, compartilhar, apagar arquivos;
  velocidade ao vivo e espaço em disco.
- Galeria (gallery-dl): perfis, posts e álbuns com prévia para marcar, organização de pastas, pular o que já baixou.
- Ajustes: padrões de download, só Wi-Fi, downloads simultâneos, Turbo (aria2), notificações, legendas, nomes e pastas,
  rede (proxy, limite, argumentos), contas, ferramentas e atualização do yt-dlp/gallery-dl, registro.
- Som opcional do motor Boxer (áudio fornecido pelo autor) ao terminar um download.

Restrições:

- Kotlin + Jetpack Compose (Material 3); motor youtubedl-android (Python, yt-dlp, ffmpeg, aria2, QuickJS) e gallery-dl em
  `assets/gallerydl.zip`.
- Só português do Brasil e só tema escuro (confirmado).
- APK grande (~170 MB, arm64 + x86_64); o tamanho não é problema.
- A chave `subaru-dl.jks` e o `keystore.properties` nunca vão para o git.

## Brand Commitments

- Nome: **Subaru DL** (o app aparece assim no celular). Ícone adaptativo em `app/src/main/res/mipmap-anydpi/ic_launcher.xml`.
- As três cores Subaru são obrigatórias (confirmado): azul-marinho `#0c1430`, amarelo `#ffd200`, dourado `#d9b44a`;
  só tema escuro.
- Família de produtos: Subaru DL para PC (`ailtoncja/subaru-dl`, visual "Placar Ao Vivo" desde a 0.8.0) e as extensões
  de navegador do autor. O celular não precisa ter a mesma cara do PC (confirmado), só as cores.
- Visual do celular (escolhido em 2026-10-19, entre alternativas): o padrão da categoria em Material 3, feito com
  capricho, à altura do Seal e dos apps do Google (Arquivos, Fotos). Convenção do Android assumida, sem metáfora e sem
  fantasia de painel ou telemetria; a marca entra pelas cores, pela escala de tipo e pelas formas do Material.

## Evidence on Hand

- O app atual (v1.2.0) e o áudio `app/src/main/res/raw/boxer_wrc.mp3`.
- Não existem depoimentos, número de usuários, avaliações ou imprensa: não inventar.

## Product Principles

1. **Compartilhar ou colar e baixar vem primeiro.** O caminho link → arquivo precisa ser óbvio para quem nunca ouviu
   falar de yt-dlp; opções avançadas existem, mas nunca na frente.
2. **Tudo numa tela (confirmado):** colar o link e acompanhar o download no mesmo lugar.
3. **Os quatro trabalhos são de primeira classe.** Vídeo, áudio, playlists e canais, e imagens de perfis.
4. **Nada para instalar além do APK, nada para configurar** antes do primeiro download.
5. **Honesto sobre limites.** Quando um site bloqueia algo, dizer em português simples o que aconteceu e o caminho que
   funciona.
