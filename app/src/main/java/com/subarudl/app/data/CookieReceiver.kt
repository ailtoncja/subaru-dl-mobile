package com.subarudl.app.data

import android.content.Context
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Recebe o cookies.txt do Subaru DL do computador, pelo Wi-Fi.
 *
 * Enquanto a tela "Receber do PC" está aberta, o celular escuta numa porta e mostra um código de 8 letras.
 * O PC acha o celular na rede, pede o "sal" (GET /subaru), cifra os cookies com AES-256-GCM usando uma chave
 * derivada do código (PBKDF2) e manda (POST /cookies). Sem o código certo nada é aceito, e depois de
 * [MAX_ATTEMPTS] tentativas erradas o receptor fecha. Os cookies nunca trafegam em texto puro.
 */
object CookieReceiver {
    const val PORT = 38475
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val ITERATIONS = 120_000
    private const val MAX_BODY = 2 * 1024 * 1024
    private const val MAX_ATTEMPTS = 5

    sealed interface State {
        data object Off : State
        data class Waiting(val code: String, val addresses: List<String>, val port: Int) : State
        data class Done(val imported: Cookies.Imported) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state

    private var server: ServerSocket? = null
    private var attempts = 0

    /** Código na tela, em dois blocos de quatro para ficar fácil de digitar. */
    fun pretty(code: String) = code.chunked(4).joinToString("  ")

    @Synchronized
    fun start(ctx: Context) {
        stop()
        val app = ctx.applicationContext
        val random = SecureRandom()
        val code = buildString { repeat(8) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val socket = bind() ?: run {
            _state.value = State.Failed("Não consegui abrir a porta para receber. Feche e abra esta tela de novo.")
            return
        }
        server = socket
        attempts = 0
        _state.value = State.Waiting(code, localAddresses(), socket.localPort)
        Thread({ serve(app, socket, code, salt) }, "cookie-receiver").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        shutdown()
        _state.value = State.Off
    }

    private fun shutdown() {
        runCatching { server?.close() }
        server = null
    }

    /** Encerra e define o estado final, mas só se esta ainda é a escuta atual (uma tela reaberta cria outra). */
    @Synchronized
    private fun finish(owner: ServerSocket, final: State) {
        if (server !== owner) return
        _state.value = final
        shutdown()
    }

    private fun bind(): ServerSocket? {
        for (port in listOf(PORT, 0)) {
            try {
                return ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(port)) }
            } catch (_: Exception) {
            }
        }
        return null
    }

    /** Endereços do celular nas redes locais (Wi-Fi, ponto de acesso, cabo), sem os da internet móvel e de VPN. */
    fun localAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !Regex("rmnet|ccmni|pdp|dummy|tun|ppp|clat|v4-", RegexOption.IGNORE_CASE).containsMatchIn(it.name) }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filter { it.isSiteLocalAddress }
            .mapNotNull { it.hostAddress }
            .distinct()
    }.getOrDefault(emptyList())

    // ---------- Servidor ----------

    private fun serve(app: Context, socket: ServerSocket, code: String, salt: ByteArray) {
        val key = runCatching { deriveKey(code, salt) }.getOrElse {
            finish(socket, State.Failed("Este aparelho não conseguiu preparar a criptografia."))
            return
        }
        while (!socket.isClosed) {
            val client = try { socket.accept() } catch (_: Exception) { break }
            try {
                client.soTimeout = 5000
                if (handle(app, socket, client, key, salt)) break
            } catch (_: Exception) {
            } finally {
                runCatching { client.close() }
            }
        }
        // Parou sem sucesso, sem excesso de tentativas e sem a tela fechar: a escuta caiu
        finish(socket, State.Failed("O recebimento foi interrompido. Abra esta tela de novo."))
    }

    private class Request(val method: String, val path: String, val body: ByteArray)

    private class TooBig : Exception()

    /** Atende uma conexão. Devolve true quando o receptor deve fechar (recebeu, ou tentativas demais). */
    private fun handle(app: Context, owner: ServerSocket, client: Socket, key: SecretKeySpec, salt: ByteArray): Boolean {
        val out = client.getOutputStream()
        val req = try { readRequest(client.getInputStream()) } catch (_: TooBig) {
            respond(out, 413, JSONObject().put("ok", false).put("erro", "grande"))
            return false
        } ?: run {
            respond(out, 400, JSONObject().put("ok", false).put("erro", "invalido"))
            return false
        }
        when {
            req.method == "GET" && req.path == "/subaru" -> {
                respond(out, 200, JSONObject().put("app", "subaru-dl").put("v", 1).put("nome", Build.MODEL ?: "Android").put("sal", Base64.encodeToString(salt, Base64.NO_WRAP)).put("iteracoes", ITERATIONS))
            }
            req.method == "POST" && req.path == "/cookies" -> {
                val text = runCatching {
                    val json = JSONObject(String(req.body, Charsets.UTF_8))
                    decrypt(key, Base64.decode(json.getString("iv"), Base64.DEFAULT), Base64.decode(json.getString("dados"), Base64.DEFAULT))
                }.getOrElse { e ->
                    if (e is AEADBadTagException || e is javax.crypto.BadPaddingException) {
                        attempts++
                        if (attempts >= MAX_ATTEMPTS) {
                            respond(out, 429, JSONObject().put("ok", false).put("erro", "tentativas"))
                            finish(owner, State.Failed("O PC errou o código várias vezes e o recebimento foi fechado. Abra esta tela de novo para gerar outro código."))
                            return true
                        }
                        respond(out, 403, JSONObject().put("ok", false).put("erro", "codigo"))
                    } else respond(out, 400, JSONObject().put("ok", false).put("erro", "invalido"))
                    return false
                }
                val imported = Cookies.importText(app, text)
                if (imported.cookies == 0) {
                    respond(out, 400, JSONObject().put("ok", false).put("erro", "vazio"))
                    return false
                }
                respond(out, 200, JSONObject().put("ok", true).put("cookies", imported.cookies).put("sites", imported.sites))
                finish(owner, State.Done(imported))
                return true
            }
            else -> respond(out, 404, JSONObject().put("ok", false).put("erro", "nao-existe"))
        }
        return false
    }

    private fun readRequest(input: InputStream): Request? {
        val head = ByteArrayOutputStream()
        var last4 = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            head.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) break
            if (head.size() > 16 * 1024) return null
        }
        val lines = head.toString(Charsets.ISO_8859_1.name()).split("\r\n")
        val first = lines.firstOrNull()?.split(" ") ?: return null
        if (first.size < 2) return null
        val length = lines.drop(1).firstNotNullOfOrNull { l ->
            if (l.substringBefore(":", "").trim().equals("content-length", ignoreCase = true)) l.substringAfter(":").trim().toIntOrNull() else null
        } ?: 0
        if (length < 0) return null
        if (length > MAX_BODY) throw TooBig()
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) return null
            read += n
        }
        return Request(first[0], first[1].substringBefore('?'), body)
    }

    private fun respond(out: OutputStream, status: Int, json: JSONObject) {
        val body = json.toString().toByteArray(Charsets.UTF_8)
        val reason = when (status) { 200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; 413 -> "Payload Too Large"; 429 -> "Too Many Requests"; else -> "Error" }
        out.write("HTTP/1.1 $status $reason\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
    }

    // ---------- Criptografia (o PC faz o mesmo em src/main/celular.ts) ----------

    private fun deriveKey(code: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATIONS, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    private fun decrypt(key: SecretKeySpec, iv: ByteArray, data: ByteArray): String {
        require(iv.size == 12) { "iv" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }
}
