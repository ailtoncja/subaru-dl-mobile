package com.subarudl.app.engine

import android.content.Context
import com.subarudl.app.data.AppLog
import com.subarudl.app.data.AppSettings
import com.subarudl.app.data.GalleryFile
import com.subarudl.app.data.GalleryOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * gallery-dl no Android: o código (Python puro + requests) vem num zip dentro do app e roda no Python
 * que a biblioteca do yt-dlp já traz. Montamos aqui o mesmo ambiente que ela usa para o yt-dlp.
 */
object GalleryDl {
    private const val BUNDLE_VERSION = 1
    private val processes = java.util.concurrent.ConcurrentHashMap<String, Process>()

    private fun pythonRoot(ctx: Context) = File(ctx.noBackupFilesDir, "youtubedl-android/packages/python/usr")
    private fun libDir(ctx: Context) = File(ctx.noBackupFilesDir, "gallerydl/lib")
    private fun stamp(ctx: Context) = File(ctx.noBackupFilesDir, "gallerydl/bundle.version")

    @Synchronized
    fun ensureInstalled(ctx: Context) {
        val stampFile = stamp(ctx)
        val ok = libDir(ctx).resolve("gallery_dl/__init__.py").exists() && stampFile.exists() && stampFile.readText().trim() == BUNDLE_VERSION.toString()
        if (ok) return
        AppLog.i("Instalando o gallery-dl…")
        libDir(ctx).deleteRecursively()
        libDir(ctx).mkdirs()
        ctx.assets.open("gallerydl.zip").use { unzip(it, libDir(ctx)) }
        stampFile.writeText(BUNDLE_VERSION.toString())
    }

    private fun unzip(input: InputStream, target: File) {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val out = File(target, entry.name)
                // evita sair da pasta de destino
                if (!out.canonicalPath.startsWith(target.canonicalPath)) throw SecurityException("zip inválido")
                if (entry.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
                entry = zip.nextEntry
            }
        }
    }

    fun version(ctx: Context): String? = runCatching {
        Regex("__version__\\s*=\\s*\"([^\"]+)\"").find(libDir(ctx).resolve("gallery_dl/version.py").readText())?.groupValues?.get(1)
    }.getOrNull()

    private fun builder(ctx: Context, args: List<String>): ProcessBuilder {
        val bin = File(ctx.applicationInfo.nativeLibraryDir)
        val py = pythonRoot(ctx)
        val pkgs = File(ctx.noBackupFilesDir, "youtubedl-android/packages")
        val pb = ProcessBuilder(listOf(File(bin, "libpython.so").absolutePath, "-m", "gallery_dl") + args)
        pb.environment().apply {
            put("LD_LIBRARY_PATH", "${py.absolutePath}/lib:${pkgs.absolutePath}/ffmpeg/usr/lib:${pkgs.absolutePath}/aria2c/usr/lib")
            put("SSL_CERT_FILE", "${py.absolutePath}/etc/tls/cert.pem")
            put("PATH", (System.getenv("PATH") ?: "") + ":" + bin.absolutePath)
            put("PYTHONHOME", py.absolutePath)
            put("PYTHONPATH", libDir(ctx).absolutePath)
            put("PYTHONIOENCODING", "utf-8")
            put("PYTHONUTF8", "1")
            put("HOME", py.absolutePath)
            put("TMPDIR", ctx.cacheDir.absolutePath)
            put("NO_COLOR", "1")
        }
        return pb
    }

    // ---------- Acesso (cookies, proxy) ----------

    private fun access(ctx: Context, s: AppSettings): List<String> {
        val a = mutableListOf<String>()
        val cookies = Args.cookiesFile(ctx)
        if (s.cookiesEnabled && cookies.exists() && cookies.length() > 0) a += listOf("--cookies", cookies.absolutePath)
        if (s.proxy.isNotBlank()) a += listOf("--proxy", s.proxy.trim())
        return a
    }

    fun organizeDirs(o: GalleryOptions): List<String>? = when (o.organize) {
        "flat" -> emptyList()
        "site" -> listOf("{category}")
        "profile" -> listOf(PROFILE_FOLDER)
        "custom" -> o.customFormat.split(Regex("[\\\\/]+")).map { it.trim() }.filter { it.isNotEmpty() }
        else -> null
    }

    /**
     * Uma pasta com o nome do perfil. Cada site chama o perfil por um campo diferente; o gallery-dl usa o
     * primeiro que existir e, se nenhum existir, o nome do site.
     */
    const val PROFILE_FOLDER =
        "{username|user[account]|user[name]|author[handle]|author[name]|owner[username]|blog_name|blog[name]|subreddit|uploader|search_tags|category}"

    fun downloadArgs(ctx: Context, url: String, o: GalleryOptions, s: AppSettings, workDir: File): List<String> {
        val a = mutableListOf("--no-colors", "-d", workDir.absolutePath) + access(ctx, s)
        organizeDirs(o)?.let { a += listOf("-o", "directory=" + JSONArray(it).toString()) }
        if (s.gallerySkipDone) a += listOf("--download-archive", File(ctx.noBackupFilesDir, "gallerydl/baixados.sqlite3").absolutePath)
        if (o.range.isNotBlank()) a += listOf("--range", o.range)
        if (s.gallerySleep > 0) a += listOf("--sleep", s.gallerySleep.toString())
        a += listOf("--", url)
        return a
    }

    /** Começa o download; cada linha de saída (caminho do arquivo ou "# " se pulado) vai para [onLine]. Devolve o código de saída. */
    fun download(ctx: Context, id: String, args: List<String>, onLine: (String) -> Unit, onErr: (String) -> Unit): Int {
        ensureInstalled(ctx)
        AppLog.cmd("gallery-dl " + args.joinToString(" ") { if (Regex("\\s").containsMatchIn(it)) "\"$it\"" else it }, id)
        val p = builder(ctx, args).start()
        processes[id] = p
        val errThread = Thread { p.errorStream.bufferedReader().forEachLine(onErr) }.also { it.start() }
        p.inputStream.bufferedReader().forEachLine(onLine)
        val code = p.waitFor()
        errThread.join(2000)
        processes.remove(id)
        return code
    }

    fun cancel(id: String) {
        processes.remove(id)?.let { p ->
            p.destroy()
            Thread { runCatching { Thread.sleep(1500); if (p.isAlive) p.destroyForcibly() } }.start()
        }
    }

    // ---------- Prévia: listar os arquivos sem baixar ----------

    class Listing(val files: List<GalleryFile>, val end: Boolean)

    /** Lista os arquivos de [start] em diante (posições do --range). */
    fun list(ctx: Context, url: String, start: Int, count: Int, s: AppSettings): Listing {
        ensureInstalled(ctx)
        val args = listOf("--no-colors", "-J", "--range", "$start-${start + count - 1}") + access(ctx, s) + listOf("--", url)
        AppLog.cmd("gallery-dl " + args.joinToString(" "))
        val p = builder(ctx, args).start()
        val err = StringBuilder()
        val errThread = Thread { p.errorStream.bufferedReader().forEachLine { err.appendLine(it) } }.also { it.start() }
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        errThread.join(2000)
        val files = mutableListOf<GalleryFile>()
        runCatching {
            val arr = JSONArray(out.ifBlank { "[]" })
            for (i in 0 until arr.length()) {
                val m = arr.optJSONArray(i) ?: continue
                if (m.optInt(0) != 3) continue // mensagem 3 = arquivo
                val link = m.optString(1)
                val d = m.optJSONObject(2) ?: JSONObject()
                val ext = d.optString("extension").lowercase()
                files += GalleryFile(
                    index = start + files.size,
                    url = link.removePrefix("ytdl:"),
                    name = d.optString("filename") + if (ext.isNotEmpty()) ".$ext" else "",
                    ext = ext,
                    width = d.optInt("width").takeIf { it > 0 },
                    height = d.optInt("height").takeIf { it > 0 },
                    video = link.startsWith("ytdl:") || ext in setOf("mp4", "webm", "mov", "mkv", "m4v"),
                )
            }
        }
        if (files.isEmpty() && (code != 0 || err.contains("[error]", true))) {
            val msg = err.lines().lastOrNull { it.contains("[error]", true) }?.replace(Regex("^\\[[^]]+]\\[error]\\s*"), "")
                ?: if (code == 64) "Site não suportado pelo gallery-dl" else "O gallery-dl terminou com o código $code"
            AppLog.e(msg)
            throw EngineException(msg, galleryHint(msg))
        }
        return Listing(files, files.size < count)
    }

    fun galleryHint(raw: String): String? = when {
        Regex("429|Too Many Requests|rate.?limit", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O site limitou as requisições. Espere alguns minutos ou ative a pausa entre arquivos nos Ajustes."
        Regex("403|Forbidden", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "O site recusou o acesso. Entre na sua conta em Ajustes → Contas e cookies."
        Regex("login|authentication|AuthRequired|NotFoundError|private", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Esse conteúdo precisa de login. Entre na sua conta em Ajustes → Contas e cookies."
        Regex("Unsupported URL|NoExtractorError", RegexOption.IGNORE_CASE).containsMatchIn(raw) -> "Esse link não é suportado pelo gallery-dl."
        else -> null
    }

    // ---------- Atualização (pelo PyPI) ----------

    /** Baixa a versão mais nova do gallery-dl e troca o código no app. Devolve a versão instalada. */
    fun update(ctx: Context): String {
        val json = JSONObject(httpGet("https://pypi.org/pypi/gallery-dl/json"))
        val latest = json.getJSONObject("info").getString("version")
        if (latest == version(ctx)) return latest
        val files = json.getJSONArray("urls")
        var wheel: String? = null
        for (i in 0 until files.length()) {
            val f = files.getJSONObject(i)
            if (f.optString("packagetype") == "bdist_wheel") wheel = f.getString("url")
        }
        val url = wheel ?: throw EngineException("Não achei o pacote do gallery-dl no PyPI")
        val tmp = File(ctx.cacheDir, "gallery_dl.whl")
        URL(url).openStream().use { i -> tmp.outputStream().use { i.copyTo(it) } }
        ensureInstalled(ctx)
        val target = libDir(ctx)
        File(target, "gallery_dl").deleteRecursively()
        // O wheel é um zip com a pasta gallery_dl/ dentro
        tmp.inputStream().use { input ->
            ZipInputStream(input).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    if (e.name.startsWith("gallery_dl/") && !e.isDirectory) {
                        val out = File(target, e.name)
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zip.copyTo(it) }
                    }
                    e = zip.nextEntry
                }
            }
        }
        tmp.delete()
        return version(ctx) ?: latest
    }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "SubaruDL")
        return c.inputStream.bufferedReader().readText()
    }
}
