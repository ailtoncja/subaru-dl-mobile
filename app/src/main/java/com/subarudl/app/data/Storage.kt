package com.subarudl.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Onde os arquivos ficam. O yt-dlp baixa numa pasta de trabalho do app; quando termina, cada arquivo é entregue:
 *  - pasta padrão: Filmes/Música/Imagens › Subaru DL (MediaStore, sem pedir permissão);
 *  - pasta escolhida pelo usuário: o seletor de pastas do Android (SAF).
 */
object Storage {
    const val APP_FOLDER = "Subaru DL"

    fun workDir(ctx: Context, id: String): File {
        val base = ctx.getExternalFilesDir("downloads") ?: File(ctx.filesDir, "downloads")
        return File(base, id).also { it.mkdirs() }
    }

    fun cleanWorkDirs(ctx: Context, keep: Set<String>) {
        val base = ctx.getExternalFilesDir("downloads") ?: return
        base.listFiles()?.filter { it.isDirectory && it.name !in keep }?.forEach { it.deleteRecursively() }
    }

    fun mimeOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "srt" -> "application/x-subrip"
            "vtt" -> "text/vtt"
            "ass" -> "text/x-ssa"
            "opus" -> "audio/opus"
            "m4a" -> "audio/mp4"
            "mkv" -> "video/x-matroska"
            "json" -> "application/json"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    /** Arquivos temporários do yt-dlp que nunca devem ser entregues. */
    fun isTemp(name: String) = Regex("\\.(part|ytdl|temp|tmp)$|\\.f\\d+\\.[a-z0-9]+$|\\.part-Frag\\d+$", RegexOption.IGNORE_CASE).containsMatchIn(name)

    /** Move [file] da pasta de trabalho para o destino, mantendo as subpastas ([rel] = caminho relativo). */
    fun deliver(ctx: Context, file: File, rel: String, destination: String?): OutputFile? {
        if (!file.exists() || file.length() == 0L) return null
        val mime = mimeOf(file.name)
        val dir = rel.substringBeforeLast('/', "")
        val out = runCatching {
            if (destination != null) toTree(ctx, file, dir, mime, Uri.parse(destination)) else toMediaStore(ctx, file, dir, mime)
        }.onFailure { AppLog.e("Não consegui salvar ${file.name}: ${it.message}") }.getOrNull() ?: return null
        val size = sizeOf(ctx, out)
        file.delete()
        return OutputFile(name = file.name, uri = out.toString(), mime = mime, size = size, rel = rel)
    }

    private fun toMediaStore(ctx: Context, file: File, dir: String, mime: String): Uri {
        val (collection, base) = when {
            mime.startsWith("video/") -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "Movies"
            mime.startsWith("audio/") -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "Music"
            mime.startsWith("image/") -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "Pictures"
            else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to "Download"
        }
        val relPath = "$base/$APP_FOLDER/" + if (dir.isNotEmpty()) "$dir/" else ""
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(collection, values) ?: error("o Android recusou criar o arquivo")
        try {
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out, 256 * 1024) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        return uri
    }

    private fun toTree(ctx: Context, file: File, dir: String, mime: String, tree: Uri): Uri {
        var parent = DocumentFile.fromTreeUri(ctx, tree) ?: error("pasta inválida")
        for (part in dir.split('/').filter { it.isNotEmpty() }) {
            parent = parent.findFile(part)?.takeIf { it.isDirectory } ?: parent.createDirectory(part) ?: error("não consegui criar a pasta $part")
        }
        // Nome livre: "arquivo.mp4", "arquivo (1).mp4"…
        var name = file.name
        var n = 1
        while (parent.findFile(name) != null) {
            name = file.nameWithoutExtension + " ($n)." + file.extension
            n++
        }
        val created = parent.createFile(mime, name) ?: error("não consegui criar $name")
        ctx.contentResolver.openOutputStream(created.uri)!!.use { out -> file.inputStream().use { it.copyTo(out, 256 * 1024) } }
        return created.uri
    }

    private fun sizeOf(ctx: Context, uri: Uri): Long = runCatching {
        ctx.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    }.getOrDefault(0L)

    fun exists(ctx: Context, uri: String): Boolean = runCatching {
        ctx.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    }.getOrDefault(false)

    fun delete(ctx: Context, uri: String): Boolean = runCatching {
        val u = Uri.parse(uri)
        if (DocumentsContract.isDocumentUri(ctx, u)) DocumentsContract.deleteDocument(ctx.contentResolver, u) else ctx.contentResolver.delete(u, null, null) > 0
    }.getOrDefault(false)

    fun viewIntent(file: OutputFile): Intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.uri), file.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    fun shareIntent(file: OutputFile): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType(file.mime).putExtra(Intent.EXTRA_STREAM, Uri.parse(file.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            "Compartilhar",
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Nome legível da pasta de destino escolhida (ou a pasta padrão). */
    fun destinationLabel(ctx: Context, destination: String?, audio: Boolean): String {
        if (destination == null) return if (audio) "Música/$APP_FOLDER" else "Filmes/$APP_FOLDER"
        return runCatching { DocumentFile.fromTreeUri(ctx, Uri.parse(destination))?.name }.getOrNull() ?: "Pasta escolhida"
    }

    fun galleryLabel(ctx: Context, destination: String?): String =
        if (destination == null) "Imagens/$APP_FOLDER" else destinationLabel(ctx, destination, false)

    /** Espaço do armazenamento interno. */
    data class Space(val free: Long, val total: Long)

    fun space(ctx: Context): Space {
        val stat = StatFs(ctx.filesDir.absolutePath)
        return Space(free = stat.availableBytes, total = stat.totalBytes)
    }

    /** Soma o tamanho dos arquivos baixados pelo app, conforme a lista. */
    fun appUsage(items: List<QueueItem>): Long = items.filter { it.status == Status.DONE }.sumOf { it.sizeBytes }
}
