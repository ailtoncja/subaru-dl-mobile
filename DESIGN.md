---
name: Subaru DL
description: Baixador de vídeos, músicas, playlists e fotos para Android, no padrão Material 3 com as cores Subaru.
colors:
  primary: "#ffd200"
  on-primary: "#3a2f00"
  primary-container: "#574600"
  on-primary-container: "#ffe37f"
  inverse-primary: "#725d00"
  secondary: "#bcc5eb"
  on-secondary: "#262f4e"
  secondary-container: "#2d3960"
  on-secondary-container: "#dce1ff"
  tertiary: "#d9b44a"
  on-tertiary: "#3e2e00"
  tertiary-container: "#574400"
  on-tertiary-container: "#ffe08e"
  error: "#ffb4ab"
  on-error: "#690005"
  error-container: "#93000a"
  on-error-container: "#ffdad6"
  surface: "#0c1430"
  on-surface: "#e2e6f4"
  surface-variant: "#283358"
  on-surface-variant: "#c3c9de"
  surface-container-lowest: "#070d22"
  surface-container-low: "#111a39"
  surface-container: "#162042"
  surface-container-high: "#1d284d"
  surface-container-highest: "#253159"
  surface-bright: "#2d3860"
  surface-tint: "#b4c3ff"
  inverse-surface: "#e2e6f4"
  inverse-on-surface: "#283050"
  outline: "#8d94ae"
  outline-variant: "#3a4468"
  scrim: "#000000"
typography:
  display-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "36sp"
    fontWeight: 400
    lineHeight: "44sp"
    letterSpacing: "0sp"
  title-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "22sp"
    fontWeight: 400
    lineHeight: "28sp"
    letterSpacing: "0sp"
  title-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0.15sp"
  title-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
  body-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  body-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0.25sp"
  body-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
    letterSpacing: "0.4sp"
  body-small-tabular:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
    letterSpacing: "0.4sp"
    fontFeature: "tnum"
  label-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
  label-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.5sp"
rounded:
  badge: "4dp"
  thumb: "8dp"
  chip: "8dp"
  card: "12dp"
  cover: "12dp"
  fab: "16dp"
  field: "28dp"
  sheet: "28dp"
  full: "9999dp"
spacing:
  xxs: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  gutter: "16dp"
  lg: "20dp"
  xl: "32dp"
  xxl: "48dp"
  fab-clearance: "88dp"
  content-max: "720dp"
components:
  top-app-bar:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.title-large}"
    height: "64dp"
  top-app-bar-scrolled:
    backgroundColor: "{colors.surface-container}"
  link-field:
    backgroundColor: "{colors.surface-container-high}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-large}"
    rounded: "{rounded.field}"
    height: "56dp"
  chip-filter:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface-variant}"
    typography: "{typography.label-large}"
    rounded: "{rounded.chip}"
    height: "32dp"
  chip-filter-selected:
    backgroundColor: "{colors.secondary-container}"
    textColor: "{colors.on-secondary-container}"
    rounded: "{rounded.chip}"
  fab-extended:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.fab}"
    height: "56dp"
  button-filled:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "0 24dp"
    height: "40dp"
  button-filled-sheet:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
    height: "48dp"
  button-tonal:
    backgroundColor: "{colors.secondary-container}"
    textColor: "{colors.on-secondary-container}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "40dp"
  button-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "40dp"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.primary}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "40dp"
  button-text-on-error:
    backgroundColor: "transparent"
    textColor: "{colors.on-error-container}"
  section-title:
    textColor: "{colors.primary}"
    typography: "{typography.title-small}"
    height: "48dp"
    padding: "0 4dp 0 16dp"
  card-download:
    backgroundColor: "{colors.surface-container-low}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.card}"
    padding: "12dp 4dp 12dp 12dp"
  card-notice:
    backgroundColor: "{colors.surface-container-high}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.card}"
    padding: "14dp 16dp"
  card-notice-error:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.on-error-container}"
    rounded: "{rounded.card}"
    padding: "14dp 16dp"
  thumbnail:
    backgroundColor: "{colors.surface-container-highest}"
    textColor: "{colors.on-surface-variant}"
    rounded: "{rounded.thumb}"
    width: "104dp"
  progress-bar:
    backgroundColor: "{colors.surface-container-highest}"
    textColor: "{colors.primary}"
    rounded: "{rounded.full}"
    height: "4dp"
  progress-bar-paused:
    backgroundColor: "{colors.surface-container-highest}"
    textColor: "{colors.outline}"
  bottom-sheet:
    backgroundColor: "{colors.surface-container-low}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.sheet}"
  list-item:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-large}"
    height: "56dp"
  snackbar:
    backgroundColor: "{colors.inverse-surface}"
    textColor: "{colors.inverse-on-surface}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.badge}"
---

# Design System: Subaru DL

## Overview

**Creative North Star: "O Padrão Bem Feito"**

O Subaru DL no Android é o padrão da categoria jogado reto: um baixador em Material 3 que qualquer pessoa com Android lê de relance, com o campo do link no alto, os cartões dos downloads embaixo e o botão Colar link debaixo do polegar. A régua de capricho são o Seal e os apps do Google (Arquivos, Fotos). Não há metáfora, painel nem fantasia numa tela de trabalho: a marca entra só pelas três cores Subaru, pela escala de tipo e pelas formas do Material.

O chão é o azul-marinho Subaru, e todas as camadas sobem no mesmo matiz azul: nada de cinza neutro. O amarelo é a cor que age (FAB, progresso, botões principais, títulos de seção) e o dourado quer dizer só uma coisa: terminou. As falhas usam os papéis de erro do Material, sem inventar vermelho próprio. A densidade é a de uma lista do Google: cartões compactos de uma linha de capa, título em duas linhas e uma linha de dados.

Tudo é escuro e tudo é em português do Brasil; não existe tema claro para manter.

**Key Characteristics:**
- Esquema escuro do Material 3 inteiro derivado das três cores Subaru.
- Superfícies em degraus tonais do mesmo azul-marinho; profundidade por tom, não por sombra.
- Roboto do sistema na escala padrão do Material 3; algarismos tabulares em tudo que muda (tamanho, velocidade, tempo).
- Formas do Material sem desvio: cartão 12dp, campo pílula e folha 28dp, FAB 16dp, botões redondos, chips 8dp.
- Ícones Material Symbols contornados (Icons.Outlined); preenchidos só no check de seleção e no play da grade de fotos.
- Movimento do Material: eixo X compartilhado entre telas, itens da lista deslizando para o lugar, FAB que recolhe ao rolar.

## Colors

Um esquema escuro do Material 3 em que o azul-marinho Subaru é a terra, o amarelo é a ação e o dourado é a conclusão.

### Primary
- **Amarelo Subaru** (primary): o FAB "Colar link", a barra de progresso ativa, os botões cheios (Baixar, Abrir, Retomar), os títulos de seção da lista e dos Ajustes, os botões de texto (Pausar tudo, Limpar, o remédio de uma falha), switches e checkboxes marcados, o círculo de seleção da grade de fotos e a fatia do app na linha de armazenamento. O texto sobre ele é o marrom-oliva escuro (on-primary).
- **Oliva Profundo** (primary-container) e **Creme Amarelo** (on-primary-container): os papéis de contêiner do Material que os componentes padrão pedem; nenhuma tela os usa de propósito.

### Secondary
- **Pervinca Clara** (secondary) sobre **Azul de Seleção** (secondary-container): o estado selecionado dos chips de filtro (Vídeo / Áudio / Imagens, qualidades) e dos botões segmentados, e o fundo dos botões tonais (Pausar, Compartilhar, Tentar de novo). É o azul do próprio marinho subindo de tom, não uma quarta cor de marca.

### Tertiary
- **Dourado Subaru** (tertiary): o ícone de check dos downloads concluídos, a conta conectada nos Ajustes e o aviso pós-conclusão na folha de detalhes. Significa "terminou" e nada mais.

### Error
- **Rosa Erro** (error): a causa de uma falha em uma linha no cartão, o texto do erro na folha, a ação destrutiva "Apagar os arquivos do celular".
- **Vinho Erro** (error-container) com **Rosa Claro** (on-error-container): o cartão de aviso quando o link não pôde ser lido; ícone, texto e botões ficam todos em on-error-container.

### Neutral
- **Marinho Subaru** (surface): fundo de todas as telas e da barra de cima em repouso.
- **Degraus do Marinho** (surface-container-lowest até surface-container-highest): low nos cartões de download e nas folhas, container na barra de cima rolada e na busca, high no campo do link e nos avisos neutros, highest no fundo das miniaturas e no trilho das barras de progresso.
- **Branco Azulado** (on-surface): títulos e texto principal.
- **Cinza Pervinca** (on-surface-variant): linhas de dados, dicas, ícones de apoio, rótulos das folhas.
- **Contorno** (outline): borda dos chips e botões contornados, barra de progresso pausada, parte ocupada do disco. **Contorno Suave** (outline-variant): divisórias.
- **Snackbar invertido** (inverse-surface / inverse-on-surface, ação em inverse-primary).

### Named Rules
**A Regra do Marinho Único.** Toda superfície é um degrau do azul-marinho Subaru. Nenhum cinza neutro, nenhuma cor de fundo fora da família surface-container.

**A Regra do Dourado Terminado.** O dourado aparece só onde algo terminou ou está pronto (download concluído, conta conectada). Nunca como destaque, decoração ou segundo amarelo.

**A Regra do Erro sem Amarelo.** Dentro do cartão de erro, os botões usam a cor do texto do aviso (on-error-container), não o amarelo. O amarelo não convive com o vinho.

## Typography

**Display Font:** Roboto (fonte do sistema Android)
**Body Font:** Roboto (fonte do sistema Android)
**Label/Mono Font:** nenhuma separada; números que mudam usam Roboto com algarismos tabulares (`tnum`)

**Character:** A Roboto do próprio Android na escala do Material 3, sem fonte de marca: o app soa como parte do sistema, e a identidade fica com a cor.

### Hierarchy
- **Title Large** (400, 22sp, 28sp): o título "Subaru DL" e o título de cada página dos Ajustes na barra de cima.
- **Title Medium** (500, 16sp, 24sp): título do vídeo no alto das folhas, título do estado vazio.
- **Title Small** (500, 14sp, 20sp): título do download no cartão (2 linhas), títulos de seção em amarelo, rótulos das folhas em cinza pervinca, título dos avisos.
- **Body Large** (400, 16sp, 24sp): texto do campo do link, linhas de lista, valor único de qualidade.
- **Body Medium** (400, 14sp, 20sp): textos de apoio, canal, mensagens das folhas.
- **Body Small** (400, 12sp, 16sp): linha de dados dos cartões, dicas, detalhe técnico; com `tnum` sempre que traz números.
- **Label Large** (500, 14sp, 20sp): botões, chips, FAB.
- **Label Small** (500, 11sp, 16sp, `tnum`): duração sobre a capa e número da foto na grade.

### Named Rules
**A Regra dos Algarismos Fixos.** Tamanhos, velocidades, porcentagens, tempos e durações usam o estilo `.tabular` (`tnum`), para que os números não tremam enquanto mudam.

**A Regra da Escala Pura.** Só os papéis do `Typography()` do Material 3, sem tamanhos inventados. O caráter vem do papel e da cor, não de uma fonte nova.

## Layout

Coluna única, celular primeiro. A lateral padrão é de 16dp (Gutter) em toda borda de conteúdo; os espaços seguem passos de 4dp (2, 4, 8, 12, 16, 20) e o estado vazio respira com 32dp nas laterais e 48dp em cima e embaixo. Os cartões ficam separados por 8dp (4dp acima e abaixo de cada um); campo, chips e avisos do alto empilham com 12dp. A lista reserva 88dp no pé para o FAB não cobrir o último cartão.

Em tela larga (tablet, retrato ou paisagem) o conteúdo fica numa coluna central de no máximo 720dp, e a barra de cima alinha os ícones com essa coluna; a rolagem continua valendo na tela toda, como nos apps do Google. Não há navegação em abas: é uma tela principal, com os Ajustes como tela própria de seta para voltar e as opções do link em folhas que sobem de baixo.

Os botões de uma folha ficam lado a lado e descem para a linha de baixo quando a letra do sistema cresce (FlowRow com 8dp). As folhas de opções têm a barra de ação presa no pé, separada por uma divisória.

## Elevation & Depth

A profundidade vem dos degraus tonais do marinho: a tela é surface, os cartões e as folhas são surface-container-low, o campo e os avisos sobem para high, as miniaturas e trilhos ficam em highest. A barra de cima não tem sombra; ao rolar a lista, ela passa para surface-container. Nenhuma sombra própria é declarada no código: os únicos sombreados são os padrões do Material que vêm com o FAB, os menus e o snackbar, e o véu (scrim) atrás das folhas e diálogos.

### Named Rules
**A Regra do Degrau Tonal.** Para separar uma camada, suba um degrau de surface-container; não acrescente sombra, borda ou brilho.

## Shapes

As formas são as do Material 3, sem invenção. Cartões e avisos com cantos de 12dp; o campo do link é uma pílula de 28dp, o mesmo raio do topo das folhas; o FAB tem 16dp; todos os botões são totalmente redondos; chips com 8dp; os botões segmentados com as pontas redondas do Material. As miniaturas da lista e das playlists têm 8dp e proporção 16:9; a capa grande no alto da folha tem 12dp. Etiquetas pequenas sobre imagem (duração, número da foto) têm 4dp.

Na grade de fotos a forma diz o estado, como no Google Fotos: a foto marcada encolhe 10dp para dentro e arredonda de 4dp para 12dp, com um círculo amarelo de check no canto.

## Components

### Buttons
Padrão do Material, sem ajuste de cor fora dos papéis.
- **Shape:** totalmente redondo (pílula).
- **Cheio (Button):** fundo amarelo, texto on-primary; a ação principal de cada folha (Baixar, Abrir, Retomar, o remédio da falha). Nas barras fixas das folhas tem 48dp de altura e ícone de 18dp com 8dp até o texto.
- **Tonal (FilledTonalButton):** fundo secondary-container; a segunda ação (Pausar, Compartilhar, Tentar de novo).
- **Contornado (OutlinedButton):** a alternativa que não é a principal (Pôr na fila, Cancelar download).
- **Texto (TextButton):** ações de seção (Pausar tudo, Retomar tudo, Limpar), o remédio dentro do cartão de falha e os botões de diálogo. Quando fica sob um título, é deslocado 12dp para a esquerda para alinhar o texto com o título.
- **Ícone (IconButton):** a única ação à direita do cartão, as ações da barra de cima.

### Floating Action Button
- **Colar link:** ExtendedFloatingActionButton amarelo (primary / on-primary), 16dp, ícone de colar. Mostra o texto com a lista parada ou subindo e recolhe para só o ícone enquanto a lista desce.

### Chips
- **Estilo:** FilterChip do Material, 8dp, contorno outline quando desmarcado.
- **Estado:** marcado em secondary-container com check à esquerda. Uso: Vídeo / Áudio / Imagens no alto, e qualidades com o tamanho estimado junto ("1080p · 284 MB"), em FlowRow com 8dp.

### Cards / Containers
- **Corner Style:** 12dp.
- **Background:** surface-container-low no cartão de download; surface-container-high no aviso neutro; error-container no aviso de erro.
- **Shadow Strategy:** nenhuma; ver Elevation & Depth.
- **Border:** nenhuma.
- **Internal Padding:** cartão de download com 12dp à esquerda, em cima e embaixo, 4dp à direita (o IconButton completa a área de toque); aviso com 16dp nas laterais e 14dp em cima.

### Cartão de download (assinatura)
Capa 16:9 de 104dp à esquerda (8dp, fundo surface-container-highest com o ícone do tipo enquanto a imagem não chega; mosaico 2x2 para downloads de imagens), 12dp de espaço, título em title-small com até 2 linhas e, embaixo, conforme o estado:
- **Baixando:** barra de progresso de 4dp com pontas redondas (amarelo sobre surface-container-highest) e a linha de dados tabular ("44% · faltam 45 s · 357 KB/s"); ação Pausar.
- **Pausado:** a mesma barra em outline; "Pausado em 44%"; ação Retomar.
- **Na fila:** só a linha de dados; ação Cancelar.
- **Falha:** a causa em uma linha na cor error e o botão de texto com o remédio (Tentar de novo, Entrar na conta, Baixar como imagens, Baixar como vídeo); ação Mais opções.
- **Concluído:** check dourado de 16dp e "MP4 · 1080p · 284 MB"; tocar abre o arquivo.
Os cartões ficam sob títulos de seção (Baixando, Na fila, Falhas, Concluídos) e deslizam para o lugar ao mudar de seção.

### Aviso da versão nova
É o mesmo aviso neutro do alto da tela (surface-container-high, ícone de atualização em on-surface-variant), logo abaixo dos chips, nunca um diálogo na abertura. Ele acompanha a versão nova do app: "Saiu a versão 1.4.0" (tamanho e Baixar), "Baixando a versão 1.4.0" (barra de 4dp amarela embaixo do texto e "29% · 47,2 MB de 162 MB"), "Versão 1.4.0 pronta para instalar" (Instalar) e "Instalando…" (barra sem fim). Os botões de texto ficam à direita e descem para a linha de baixo quando não cabem; "Depois" esconde o aviso até o app abrir de novo, e "Novidades" abre uma folha com as notas da Release e o botão do próximo passo. Nos Ajustes, a linha "Subaru DL 1.4.0" da seção Sobre mostra o mesmo andamento com um botão de texto à direita (Verificar, Baixar, Instalar). Depois de atualizar, um snackbar confirma a versão.

### Inputs / Fields
- **Campo do link:** TextField pílula de 28dp e 56dp de altura, fundo surface-container-high, sem linha indicadora em nenhum estado; ícone de link à esquerda; à direita, colar (vazio) ou limpar (com texto). Colar pelo teclado já lê o link.
- **Campos de escolha (DropField):** OutlinedTextField somente leitura com menu que desce do campo (exposed dropdown); a opção atual aparece em amarelo no menu, com linha de apoio em body-small.
- **Campos de texto livres:** OutlinedTextField padrão, com texto de apoio quando precisa de exemplo.
- **Busca:** a barra de cima vira um campo transparente sobre surface-container, com seta para fechar.

### Navigation
- **Barra de cima:** TopAppBar pequena; na tela principal, "Subaru DL" à esquerda e busca e Ajustes à direita; nos Ajustes, seta para voltar e o título da página.
- **Transição:** eixo X compartilhado do Material entre a tela principal, os Ajustes e as subpáginas: a tela nova entra 1/8 da largura vindo do lado do avanço em 350ms (curva enfatizada de desaceleração), com fade de 200ms após 80ms; a que sai anda 1/8 em 200ms (curva enfatizada de aceleração) e some em 100ms.

### Lists (Ajustes e folhas)
ListItem do Material com fundo transparente, ícone contornado à esquerda, valor atual na linha de apoio e chevron quando abre outra página; linhas com switch ligam pela linha inteira. Grupos sob títulos de seção em title-small amarelo, 48dp de altura.

### Sheets e diálogos
- **Folhas de opções:** ModalBottomSheet do Material, aberta inteira, topo de 28dp; conteúdo rolando com rótulos em title-small cinza pervinca, capa de 136dp com 12dp, Vídeo/Áudio em botão segmentado, chips de qualidade, campos; barra de ação fixa no pé (Pôr na fila + Baixar).
- **Grade de imagens:** fotos quadradas no estilo Google Fotos (ver Shapes), círculo de seleção de 24dp no canto com sombra radial para aparecer em foto clara, número da foto em etiqueta escura de 4dp.
- **Diálogos:** AlertDialog padrão com ícone, título e dois botões de texto.
- **Snackbar:** padrão invertido, com ação "Desfazer" quando a remoção pode voltar.

## Do's and Don'ts

### Do:
- **Do** derivar toda cor nova dos papéis do esquema em Theme.kt; o marinho #0c1430, o amarelo #ffd200 e o dourado #d9b44a são obrigatórios.
- **Do** usar surface-container-low (12dp) para cartões e subir um degrau tonal para destacar, como o campo do link em surface-container-high.
- **Do** aplicar `.tabular` em todo texto com números que mudam.
- **Do** manter 16dp de lateral e a coluna central de 720dp em telas largas.
- **Do** dizer a falha em uma linha em português simples e oferecer o remédio como botão logo abaixo.
- **Do** usar os componentes do Material 3 com seus formatos padrão (botões redondos, chips 8dp, folhas 28dp, FAB 16dp).

### Don't:
- **Don't** criar um tema claro ou cinzas neutros; toda superfície é um degrau do marinho.
- **Don't** usar o dourado como destaque ou enfeite; ele significa só "terminou".
- **Don't** pôr amarelo dentro do cartão de erro; ali tudo usa on-error-container.
- **Don't** adicionar sombras próprias, bordas de cartão ou brilhos; a profundidade é tonal.
- **Don't** trocar a Roboto do sistema por uma fonte de marca nem criar tamanhos fora da escala do Material 3.
- **Don't** usar metáforas de painel, telemetria ou rali numa tela de trabalho.
