package com.subarudl.app.ui.home

/** O caminho que resolve uma falha: vira o botão do cartão e do aviso. */
enum class Remedy(val label: String) {
    RETRY("Tentar de novo"),
    LOGIN("Entrar na conta"),
    AS_VIDEO("Baixar como vídeo"),
    AS_IMAGES("Baixar como imagens"),
    TURBO_OFF("Tentar sem o Turbo"),
}

/** Causa curta (cabe numa linha do cartão) e o caminho que resolve. */
data class Failure(val cause: String, val remedy: Remedy)

/** A frase que manda a pessoa para Ajustes → Contas e cookies: some quando há um botão que faz isso. */
private val loginPath = Regex("\\s*(Entre na (sua )?conta|entre em Ajustes)[^.]*\\.?", RegexOption.IGNORE_CASE)

/** A explicação diz que o site pediu login. */
fun needsLogin(text: String?): Boolean = text != null && (text.contains("Contas e cookies") || text.contains("entrou na conta"))

/** Tira a frase com o caminho dos Ajustes, para não repetir o botão "Entrar na conta" que está ao lado. */
fun withoutLoginPath(text: String): String = text.replace(loginPath, "").trim().trimEnd(':', ',', ';')

/** "O YouTube", "O Instagram"… a partir do link ou do nome do site; null quando não se sabe. */
fun siteSubject(urlOrSite: String): String? {
    val names = listOf(
        "youtu" to "O YouTube", "instagram" to "O Instagram", "tiktok" to "O TikTok", "facebook" to "O Facebook",
        "x.com" to "O X", "twitter" to "O X", "pixiv" to "O Pixiv", "reddit" to "O Reddit", "twitch" to "A Twitch",
    )
    return names.firstOrNull { urlOrSite.contains(it.first, ignoreCase = true) }?.second
}

/** Primeira frase de uma explicação, sem o ponto final. */
private fun firstSentence(text: String): String = text.substringBefore(". ").trimEnd('.')

/** Lê a explicação em português que o motor deu e devolve a causa curta e o caminho. */
fun failureOf(hint: String?, error: String?, where: String): Failure {
    val h = hint.orEmpty()
    return when {
        h.startsWith("Esse vídeo não tem som") -> Failure("O vídeo não tem som", Remedy.AS_VIDEO)
        needsLogin(h) -> Failure("${siteSubject(where) ?: "O site"} pediu login", Remedy.LOGIN)
        h.startsWith("Não achei vídeo") -> Failure("Não há vídeo nesse link", Remedy.AS_IMAGES)
        h.contains("Turbo (aria2) falhou") -> Failure("O Turbo (aria2) falhou", Remedy.TURBO_OFF)
        h.contains("limitou") -> Failure("O site limitou os pedidos", Remedy.RETRY)
        h.isNotEmpty() -> Failure(firstSentence(h), Remedy.RETRY)
        else -> Failure(error?.let(::firstSentence) ?: "O download falhou", Remedy.RETRY)
    }
}
