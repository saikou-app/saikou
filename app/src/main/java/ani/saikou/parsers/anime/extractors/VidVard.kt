package ani.saikou.parsers.anime.extractors

import ani.saikou.client
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer
import java.math.BigInteger

class VidVard(override val server: VideoServer) : VideoExtractor() {

    private val mainUrl = "https://videovard.sx"

    override suspend fun extract(): VideoContainer {
        val url = server.embed.url
        val id = url.substringAfter("/e/").substringBefore("/")
        val hash = client.get("$mainUrl/api/make/hash/$id").parsed<HashResponse>().hash
            ?: throw NoSuchElementException("Hash not found")
        val res = client.post(
            "$mainUrl/api/player/setup",
            data = mapOf(
                "cmd" to "get_stream",
                "file_code" to id,
                "hash" to hash
            )
        ).parsed<SetupResponse>()
        val m3u8 = decode(res.src, res.seed)
        return VideoContainer(listOf(Video(null, true, m3u8)))
    }

    private fun decode(data_file: String, seed: String): String {
        val data_seed = replace(seed)
        val new_data_seed = bynarydigest(data_seed)
        val new_data_file = bytes2blocks(ascii2bytes(data_file))
        var a71 = listOf(1633837924, 1650680933).map { it.toBigInteger() }
        val a74 = mutableListOf<BigInteger>()
        for (i in new_data_file.indices step 2) {
            val a73 = new_data_file.slice(i..i+1)
            a74 += xor_blocks(a71, tea_decode(a73, new_data_seed))
            a71 = a73
        }

        val result = replace(unpad(blocks2bytes(a74)).map { it.toInt().toChar() }.joinToString(""))
        println(result)
        return replace(unpad(blocks2bytes(a74)).joinToString { it.toString() })
    }

    private fun bynarydigest(a55: String): List<BigInteger> {
        val a63 = arrayOf(1633837924, 1650680933, 1667523942, 1684366951).map { it.toBigInteger() }
        var a62 = a63.slice(0..1)
        var a61 = a62
        val a59 = bytes2blocks(digest_pad(a55))

        for (i in a59.indices step 4) {
            val a66 = a59.slice(i..i + 1)
            val a68 = a59.slice(i + 2..i + 3)

            a62 = tea_code(xor_blocks(a66, a62), a63).toMutableList()
            a61 = tea_code(xor_blocks(a68, a61), a63).toMutableList()

            val a64 = a62[0]
            a62[0] = a62[1]
            a62[1] = a61[0]
            a61[0] = a61[1]
            a61[1] = a64
        }

        return listOf(a62[0], a62[1], a61[0], a61[1])
    }

    private fun tea_decode(a90: List<BigInteger>, a91: List<BigInteger>): MutableList<BigInteger> {
        var (a95, a96) = a90

        var a97 = (-957401312).toBigInteger()
        for (_i in 0 until 32) {
            a96 -= ((((a95 shl 4) xor rshift(a95, 5)) + a95) xor (a97 + a91[rshift(a97, 11).and(3.toBigInteger()).toInt()]))
            a97 += 1640531527.toBigInteger()
            a95 -= ((((a96 shl 4) xor rshift(a96, 5)) + a96) xor (a97 + a91[a97.and(3.toBigInteger()).toInt()]))

        }

        return mutableListOf(a95, a96)
    }

    private fun digest_pad(string: String): List<BigInteger> {
        val emplist = mutableListOf<BigInteger>()
        val length = string.length
        val extra = 15 - (length % 16)
        emplist.add(extra.toBigInteger())
        for (i in 0 until length) {
            emplist.add(string[i].code.toBigInteger())
        }
        for (i in 0 until extra) {
            emplist.add(0.toBigInteger())
        }

        return emplist
    }

    private fun bytes2blocks(a22: List<BigInteger>): List<BigInteger> {
        val empList = mutableListOf<BigInteger>()
        val length = a22.size
        var listIndex = 0

        for (i in 0 until length) {
            val subIndex = i % 4
            val shiftedByte = a22[i] shl (3 - subIndex) * 8

            if (subIndex == 0) {
                empList.add(shiftedByte)
            } else {
                empList[listIndex] = empList[listIndex] or shiftedByte
            }

            if (subIndex == 3) listIndex += 1
        }

        return empList
    }

    private fun blocks2bytes(inp: List<BigInteger>): List<BigInteger> {
        val temp_list = mutableListOf<BigInteger>()
        for (i in inp.indices) {
            temp_list += (255.toBigInteger() and rshift(inp[i], 24))
            temp_list += (255.toBigInteger() and rshift(inp[i], 16))
            temp_list += (255.toBigInteger() and rshift(inp[i], 8))
            temp_list += (255.toBigInteger() and inp[i])
        }

        return temp_list
    }

    private fun unpad(a46: List<BigInteger>): List<BigInteger> {
        val a52 = mutableListOf<BigInteger>()
        val even_odd = a46[0].mod(2.toBigInteger())
        for (i in 1 until (a46.size - even_odd.toInt())) {
            a52 += a46[i]
        }
        return a52
    }

    private fun xor_blocks(a76: List<BigInteger>, a77: List<BigInteger>): List<BigInteger> {
        return mutableListOf(a76[0] xor a77[0], a76[1] xor a77[1])
    }

    private fun rshift(a: BigInteger, b: Int): BigInteger {
        return a.mod(4294967296.toBigInteger()) shr b
    }

    private fun tea_code(a79: List<BigInteger>, a80: List<BigInteger>): List<BigInteger> {
        var (a85, a83) = a79
        var a87 = 0.toBigInteger()
        for (_i in 0 until 32) {
            a85 += (a83 shl 4 xor rshift(a83, 5)) + a83 xor a87 + a80[a87.and(3.toBigInteger()).toInt()]
            a87 -= (1640531527).toBigInteger()
            a83 += (a85 shl 4 xor rshift(a85, 5)) + a85 xor a87 + a80[rshift(a87, 11).and(3.toBigInteger()).toInt()]
        }

        return mutableListOf(a85, a83)
    }

    private fun ascii2bytes(inp: String): List<BigInteger> {
        val a2b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        var ind = -1
        var length = inp.length
        var listIndex = 0
        val emplist = mutableListOf<BigInteger>()

        while (true) {
            for (i in inp) {
                if (a2b.contains(i)) {
                    ind += 1
                    break
                }
            }

            emplist.add((a2b.indexOf(inp[ind]) * 4).toBigInteger())

            while (true) {
                ind += 1
                if (a2b.contains(inp[ind])) {
                    break
                }
            }

            var a3 = a2b.indexOf(inp[ind])

            emplist[listIndex] = emplist[listIndex] or rshift(a3.toBigInteger(), 4)

            listIndex += 1

            a3 = (15.and(a3))

            if ((a3 == 0) && (ind == (length - 1))) {
                return emplist
            }

            emplist.add((a3 * 16).toBigInteger())


            while (true) {
                ind += 1
                if (ind >= length) {
                    return emplist
                }
                if (a2b.contains(inp[ind])) {
                    break
                }
            }

            a3 = a2b.indexOf(inp[ind])
            emplist[listIndex] = emplist[listIndex] or rshift(a3.toBigInteger(), 2)
            listIndex += 1
            a3 = (3 and a3)
            if ((a3 == 0) && (ind == (length - 1))) {
                return emplist
            }
            emplist.add((a3 shl 6).toBigInteger())
            for (i in inp) {
                ind += 1
                if (a2b.contains(inp[ind])) {
                    break
                }
            }
            emplist[listIndex] = emplist[listIndex].or(a2b.indexOf(inp[ind]).toBigInteger())
            listIndex += 1
        }
    }

    private fun replace(a: String): String {
        val valid = "12567"
        var b = ""
        for (i in a) {
            b += if (i.isDigit() && valid.contains(i)) {
                (i.digitToInt() - 5).toString()
            } else {
                i
            }
        }
        return b
    }

    private data class HashResponse(
        val hash: String? = null
    )

    private data class SetupResponse(
        val seed: String,
        val src: String
    )
}