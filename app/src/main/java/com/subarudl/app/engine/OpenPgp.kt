package com.subarudl.app.engine

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

/**
 * Conferência mínima de assinaturas OpenPGP (RFC 4880), só o que o yt-dlp usa: assinatura v4 separada ("detached"),
 * de um documento binário, com chave RSA e SHA-256/384/512. Qualquer outra coisa é recusada.
 *
 * A chave pública é a oficial do yt-dlp, a mesma de https://github.com/yt-dlp/yt-dlp/blob/master/public.key
 * (impressão digital AC0C BBE6 848D 6A87 3464 AF4E 57CF 6593 3B5A 7581, publicada em 2023 e também em keys.openpgp.org).
 */
object OpenPgp {
    const val YTDLP_FINGERPRINT = "AC0CBBE6848D6A873464AF4E57CF65933B5A7581"

    private const val YTDLP_KEY = """-----BEGIN PGP PUBLIC KEY BLOCK-----

mQINBGP78C4BEAD0rF9zjGPAt0thlt5C1ebzccAVX7Nb1v+eqQjk+WEZdTETVCg3
WAM5ngArlHdm/fZqzUgO+pAYrB60GKeg7ffUDf+S0XFKEZdeRLYeAaqqKhSibVal
DjvOBOztu3W607HLETQAqA7wTPuIt2WqmpL60NIcyr27LxqmgdN3mNvZ2iLO+bP0
nKR/C+PgE9H4ytywDa12zMx6PmZCnVOOOu6XZEFmdUxxdQ9fFDqd9LcBKY2LDOcS
Yo1saY0YWiZWHtzVoZu1kOzjnS5Fjq/yBHJLImDH7pNxHm7s/PnaurpmQFtDFruk
t+2lhDnpKUmGr/I/3IHqH/X+9nPoS4uiqQ5HpblB8BK+4WfpaiEg75LnvuOPfZIP
KYyXa/0A7QojMwgOrD88ozT+VCkKkkJ+ijXZ7gHNjmcBaUdKK7fDIEOYI63Lyc6Q
WkGQTigFffSUXWHDCO9aXNhP3ejqFWgGMtCUsrbkcJkWuWY7q5ARy/05HbSM3K4D
U9eqtnxmiV1WQ8nXuI9JgJQRvh5PTkny5LtxqzcmqvWO9TjHBbrs14BPEO9fcXxK
L/CFBbzXDSvvAgArdqqlMoncQ/yicTlfL6qzJ8EKFiqW14QMTdAn6SuuZTodXCTi
InwoT7WjjuFPKKdvfH1GP4bnqdzTnzLxCSDIEtfyfPsIX+9GI7Jkk/zZjQARAQAB
tDdTaW1vbiBTYXdpY2tpICh5dC1kbHAgc2lnbmluZyBrZXkpIDxjb250YWN0QGdy
dWI0ay54eXo+iQJOBBMBCgA4FiEErAy75oSNaoc0ZK9OV89lkztadYEFAmP78C4C
GwMFCwkIBwIGFQoJCAsCBBYCAwECHgECF4AACgkQV89lkztadYEVqQ//cW7TxhXg
7Xbh2EZQzXml0egn6j8QaV9KzGragMiShrlvTO2zXfLXqyizrFP4AspgjSn/4NrI
8mluom+Yi+qr7DXT4BjQqIM9y3AjwZPdywe912Lxcw52NNoPZCm24I9T7ySc8lmR
FQvZC0w4H/VTNj/2lgJ1dwMflpwvNRiWa5YzcFGlCUeDIPskLx9++AJE+xwU3LYm
jQQsPBqpHHiTBEJzMLl+rfd9Fg4N+QNzpFkTDW3EPerLuvJniSBBwZthqxeAtw4M
UiAXh6JvCc2hJkKCoygRfM281MeolvmsGNyQm+axlB0vyldiPP6BnaRgZlx+l6MU
cPqgHblb7RW5j9lfr6OYL7SceBIHNv0CFrt1OnkGo/tVMwcs8LH3Ae4a7UJlIceL
V54aRxSsZU7w4iX+PB79BWkEsQzwKrUuJVOeL4UDwWajp75OFaUqbS/slDDVXvK5
OIeuth3mA/adjdvgjPxhRQjA3l69rRWIJDrqBSHldmRsnX6cvXTDy8wSXZgy51lP
m4IVLHnCy9m4SaGGoAsfTZS0cC9FgjUIyTyrq9M67wOMpUxnuB0aRZgJE1DsI23E
qdvcSNVlO+39xM/KPWUEh6b83wMn88QeW+DCVGWACQq5N3YdPnAJa50617fGbY6I
gXIoRHXkDqe23PZ/jURYCv0sjVtjPoVC+bg=
=bJkn
-----END PGP PUBLIC KEY BLOCK-----"""

    class RsaKey(val n: BigInteger, val e: BigInteger, val fingerprint: ByteArray) {
        val keyId: ByteArray get() = fingerprint.copyOfRange(fingerprint.size - 8, fingerprint.size)
    }

    /** A chave do yt-dlp, conferida pela impressão digital (se o texto acima for mexido, nada passa). */
    val ytdlpKey: RsaKey by lazy {
        val key = publicKeys(dearmor(YTDLP_KEY)).firstOrNull() ?: error("chave do yt-dlp ilegível")
        check(hex(key.fingerprint) == YTDLP_FINGERPRINT) { "chave do yt-dlp diferente da esperada" }
        key
    }

    private class Packet(val tag: Int, val body: ByteArray)

    /** Tira o "ASCII armor": cabeçalhos, a soma de conferência (=xxxx) e as linhas BEGIN/END. */
    fun dearmor(text: String): ByteArray {
        val lines = text.replace("\r", "").lines()
        val begin = lines.indexOfFirst { it.startsWith("-----BEGIN PGP") }
        val end = lines.indexOfFirst { it.startsWith("-----END PGP") }
        require(begin >= 0 && end > begin) { "armor" }
        val inner = lines.subList(begin + 1, end)
        val blank = inner.indexOfFirst { it.isBlank() }
        val body = (if (blank >= 0) inner.drop(blank + 1) else inner).filter { it.isNotBlank() && !it.startsWith("=") }
        return Base64.getDecoder().decode(body.joinToString(""))
    }

    private fun u16(b: ByteArray, i: Int) = ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    private fun u32(b: ByteArray, i: Int): Long =
        ((b[i].toLong() and 0xFF) shl 24) or ((b[i + 1].toLong() and 0xFF) shl 16) or ((b[i + 2].toLong() and 0xFF) shl 8) or (b[i + 3].toLong() and 0xFF)

    /** Separa os pacotes (formato antigo e novo; tamanhos parciais não são aceitos). */
    private fun packets(data: ByteArray): List<Packet> {
        val out = mutableListOf<Packet>()
        var i = 0
        while (i < data.size) {
            val h = data[i++].toInt() and 0xFF
            require(h and 0x80 != 0) { "pacote" }
            val tag: Int
            val len: Long
            if (h and 0x40 != 0) {
                tag = h and 0x3F
                val first = data[i++].toInt() and 0xFF
                len = when {
                    first < 192 -> first.toLong()
                    first < 224 -> (((first - 192) shl 8) + (data[i++].toInt() and 0xFF) + 192).toLong()
                    first == 255 -> u32(data, i).also { i += 4 }
                    else -> throw IllegalArgumentException("tamanho parcial")
                }
            } else {
                tag = (h shr 2) and 0x0F
                len = when (h and 3) {
                    0 -> (data[i++].toLong() and 0xFF)
                    1 -> u16(data, i).toLong().also { i += 2 }
                    2 -> u32(data, i).also { i += 4 }
                    else -> throw IllegalArgumentException("tamanho indefinido")
                }
            }
            require(len >= 0 && i + len <= data.size) { "pacote cortado" }
            out += Packet(tag, data.copyOfRange(i, i + len.toInt()))
            i += len.toInt()
        }
        return out
    }

    /** Lê o número (MPI) que começa em [at]; devolve o valor e onde o próximo começa. */
    private fun mpi(b: ByteArray, at: Int): Pair<ByteArray, Int> {
        val bits = u16(b, at)
        val n = (bits + 7) / 8
        require(at + 2 + n <= b.size) { "mpi" }
        return b.copyOfRange(at + 2, at + 2 + n) to at + 2 + n
    }

    /** Chaves RSA v4 (principal e subchaves) de um bloco de chave pública. */
    fun publicKeys(data: ByteArray): List<RsaKey> = packets(data).filter { it.tag == 6 || it.tag == 14 }.mapNotNull { p ->
        val b = p.body
        if (b.size < 6 || b[0].toInt() != 4 || (b[5].toInt() and 0xFF) !in setOf(1, 3)) return@mapNotNull null
        val (n, next) = mpi(b, 6)
        val (e, _) = mpi(b, next)
        val fp = MessageDigest.getInstance("SHA-1").run {
            update(0x99.toByte())
            update(byteArrayOf((b.size shr 8).toByte(), b.size.toByte()))
            update(b)
            digest()
        }
        RsaKey(BigInteger(1, n), BigInteger(1, e), fp)
    }

    /** true só se [signature] (o arquivo .sig, binário ou em armor) for uma assinatura válida de [data] feita por [key]. */
    fun verify(data: ByteArray, signature: ByteArray, key: RsaKey = ytdlpKey): Boolean = runCatching {
        val raw = if (signature.size > 5 && String(signature, 0, 5, Charsets.US_ASCII) == "-----") dearmor(String(signature, Charsets.US_ASCII)) else signature
        val sigs = packets(raw)
        if (sigs.size != 1 || sigs[0].tag != 2) return false
        val b = sigs[0].body
        // v4, documento binário (0x00), RSA (1 ou 3)
        if (b[0].toInt() != 4 || b[1].toInt() != 0x00 || (b[2].toInt() and 0xFF) !in setOf(1, 3)) return false
        val algorithm = when (b[3].toInt() and 0xFF) {
            8 -> "SHA256withRSA"
            9 -> "SHA384withRSA"
            10 -> "SHA512withRSA"
            else -> return false
        }
        val hashedLen = u16(b, 4)
        val hashedEnd = 6 + hashedLen
        if (!subpacketsOk(b, 6, hashedEnd, key, hashed = true)) return false
        val unhashedLen = u16(b, hashedEnd)
        val unhashedEnd = hashedEnd + 2 + unhashedLen
        if (!subpacketsOk(b, hashedEnd + 2, unhashedEnd, key, hashed = false)) return false
        val (sig, end) = mpi(b, unhashedEnd + 2)
        if (end != b.size) return false
        // O "trailer" v4: a parte com hash da assinatura + 0x04 0xFF + o tamanho dela em 4 bytes
        val trailer = b.copyOfRange(0, hashedEnd) + byteArrayOf(0x04, 0xFF.toByte()) +
            byteArrayOf((hashedEnd ushr 24).toByte(), (hashedEnd ushr 16).toByte(), (hashedEnd ushr 8).toByte(), hashedEnd.toByte())
        // A assinatura RSA precisa ter o tamanho do módulo (o MPI corta zeros à esquerda)
        val size = (key.n.bitLength() + 7) / 8
        if (sig.size > size) return false
        val padded = ByteArray(size - sig.size) + sig
        val pub = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(key.n, key.e))
        Signature.getInstance(algorithm).run {
            initVerify(pub)
            update(data)
            update(trailer)
            verify(padded)
        }
    }.getOrDefault(false)

    /**
     * Confere os subpacotes: o emissor (se informado) tem de ser a chave esperada, a assinatura não pode estar vencida
     * e nenhum subpacote marcado como "crítico" pode ser desconhecido (a regra do RFC 4880).
     */
    private fun subpacketsOk(b: ByteArray, from: Int, to: Int, key: RsaKey, hashed: Boolean): Boolean {
        if (to > b.size) return false
        var i = from
        var created = -1L
        var expires = -1L
        while (i < to) {
            val first = b[i++].toInt() and 0xFF
            val len = when {
                first < 192 -> first
                first < 255 -> (((first - 192) shl 8) + (b[i++].toInt() and 0xFF) + 192)
                else -> u32(b, i).toInt().also { i += 4 }
            }
            if (len < 1 || i + len > to) return false
            val type = b[i].toInt() and 0x7F
            val critical = b[i].toInt() and 0x80 != 0
            val value = b.copyOfRange(i + 1, i + len)
            when (type) {
                2 -> if (hashed) created = u32(value, 0)
                3 -> if (hashed) expires = u32(value, 0)
                16 -> if (!value.contentEquals(key.keyId)) return false
                33 -> if (value.size < 2 || !value.copyOfRange(1, value.size).contentEquals(key.fingerprint)) return false
                else -> if (critical) return false
            }
            i += len
        }
        if (expires > 0 && created >= 0 && System.currentTimeMillis() / 1000 > created + expires) return false
        return true
    }

    private fun hex(b: ByteArray) = b.joinToString("") { "%02X".format(it) }
}
