package ani.saikou.android.manga.source.parsers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
import kotlin.math.floor

/**
 * Fixes the MangaReader images by cropping and moving around chunks of the image
 * Made by LagradOst
 * */
@Suppress("SameParameterValue")
class MangaReaderToTransformation : Transformation<File> {
    private val id = this.javaClass.name
    private val idBytes = id.toByteArray(Charset.defaultCharset())
    private val currentList = mutableListOf<Int>()
    private var j = 0
    private var i = 0

    private fun seedRand(start: Int, stop: Int): Int {
        val rand = run_0x416663()
        val returnValue = floor(rand * (stop - start + 1)).toInt() + start
        if (returnValue > stop) return 0
        return returnValue
    }

    private fun initList() {
        currentList.clear()
        i = 0
        j = 0
        val initVars = listOf(115, 116, 97, 121)

        for (i in 0 until 256) {
            currentList.add(i)
        }

        var temp = 0
        for (i in 0 until 256) {
            val oldSpot = currentList[i]
            temp = 255 and (temp + initVars[i % initVars.size] + oldSpot)
            // Swaps
            currentList[i] = currentList[temp]
            currentList[temp] = oldSpot
        }

        run_0x20a9d0(256)
    }

    private fun run_0x416663(): Float {
        var _0x5abc8f = run_0x20a9d0(6)
        // This can get bigger than a long, hence BigInteger
        var _0x248754 = BigInteger("281474976710656")
        var _0x166b5d = 0L
        val _0x4c3ba3 = 4503599627370496
        val _0x502fe4 = 0x100
        val _0x5f099b = 0x2 * _0x4c3ba3

        while (_0x5abc8f < _0x4c3ba3) {
            _0x5abc8f = (_0x5abc8f + _0x166b5d) * _0x502fe4
            // 18446744073709552000
            _0x248754 = _0x248754.multiply(BigInteger("256"))
            _0x166b5d = run_0x20a9d0(1)
        }

        while (_0x5f099b <= _0x5abc8f) {
            _0x5abc8f /= 0x2
            _0x248754 = _0x248754.divide(BigInteger("2"))
            _0x166b5d = _0x166b5d shr 1
        }
        return (_0x5abc8f + _0x166b5d).toFloat() / _0x248754.toLong()
    }

    private fun run_0x20a9d0(runs: Int): Long {
        var _0x33fdb5 = 0L
        var _0x1f6d89 = i
        var _0x36b3d8 = j

        for (i in runs - 1 downTo 0) {
            // 1 -> 255 -> 0
            _0x1f6d89 = 255 and (_0x1f6d89 + 1)

            val _0x285233 = currentList[_0x1f6d89]
            _0x36b3d8 = 255 and (_0x36b3d8 + _0x285233)

            currentList[_0x1f6d89] =
                currentList[_0x36b3d8]
            currentList[_0x36b3d8] = _0x285233

            _0x33fdb5 =
                _0x33fdb5 * 256 + currentList[255 and (currentList[_0x1f6d89] + currentList[_0x36b3d8])]
        }
        i = _0x1f6d89
        j = _0x36b3d8
        return _0x33fdb5
    }

    private fun getShuffle(size: Int): MutableList<Int> {
        initList()
        // Getting changed
        val _0x47c787 = mutableListOf<Int>()
        // Empty to be filled
        val _0x5b93da = mutableListOf<Int>()
        for (i in 0 until size) {
            _0x47c787.add(i)
            _0x5b93da.add(0)
        }

        for (i in 0 until size) {
            val value = seedRand(0, size - i - 1)

            val _0x40e593 = _0x47c787[value]
            _0x47c787.removeAt(value)

            _0x5b93da[_0x40e593] = i
        }
        return _0x5b93da
    }


    override fun transform(
        context: Context,
        resource: Resource<File>,
        outWidth: Int,
        outHeight: Int
    ): Resource<File> {
        // Chunk size, i think there could be different sizes here but i haven't found on site
        val width = 200
        val height = 200

        // Setup file
        val file = resource.get()
        val bitmap = BitmapFactory.decodeFile(file.path)
        val realWidth = bitmap.width
        val realHeight = bitmap.height

        val columns = (realWidth / width)
        val rows = (realHeight / height)
        val size = columns * rows
        val shuffle = getShuffle(size)

        val diffList = shuffle.mapIndexed { index, it ->
            Point(it % columns, it / columns) to Point(index % columns, index / columns)
        }.toMutableList()

        // The sides and bottom are handled independently if the size isn't a factor of 200
        if (realWidth % 200 != 0) {
            val rightSideShuffle = getShuffle(rows)
            rightSideShuffle.mapIndexed { index, it ->
                diffList.add(
                    Point(columns, it) to Point(columns, index)
                )
            }
        }
        if (realHeight % 200 != 0) {
            val bottomSideShuffle = getShuffle(columns)
            bottomSideShuffle.mapIndexed { index, it ->
                diffList.add(
                    Point(it, rows) to Point(index, rows)
                )
            }
            // Corner is always same place
            diffList.add(
                Point(columns, rows) to Point(columns, rows)
            )
        }


        val image = Bitmap.createBitmap(realWidth, realHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        diffList.forEach {
            val diffWidth =
                if (it.first.x >= columns) (realWidth % width) else width
            val diffHeight =
                if (it.first.y >= rows) (realHeight % height) else height

            val part = Bitmap.createBitmap(
                bitmap,
                it.first.x * width,
                it.first.y * height,
                diffWidth,
                diffHeight
            )
            canvas.drawBitmap(
                part,
                (it.second.x * width).toFloat(),
                (it.second.y * height).toFloat(),
                null
            )
        }

        // Write the bitmap to a file and return the file
        val newFile = File(context.cacheDir, file.name)
        newFile.createNewFile()
        val bos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 0, bos)
        val bitmapData = bos.toByteArray()
        newFile.writeBytes(bitmapData)

        return object : Resource<File> {
            override fun getResourceClass(): Class<File> {
                return newFile.javaClass
            }

            override fun get(): File {
                return newFile
            }

            override fun getSize(): Int {
                return newFile.length().toInt()
            }

            override fun recycle() {
                newFile.delete()
            }
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(idBytes)
    }

    override fun equals(other: Any?): Boolean {
        return other is MangaReaderToTransformation
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
