package id.semetondevs.momcall

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.annotation.DrawableRes
import android.widget.ImageView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.nio.channels.FileChannel

fun ImageView.loadFromPath(filePath: String?, @DrawableRes errorDrawable: Int, async: Boolean = true) {
    if (filePath != null && !filePath.isEmpty()) {
        val streamGetUri = Single.create<Uri>{
            val photoFile = File(filePath)
            if (photoFile.exists()) {
                it.onSuccess(Uri.fromFile(photoFile))
            } else {
                it.onError(FileNotFoundException("File $filePath not found"))
            }
        }

        val doInThread = if (async) Schedulers.computation() else AndroidSchedulers.mainThread()
        streamGetUri.subscribeOn(doInThread)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    this.scaleType = ImageView.ScaleType.CENTER_CROP
                    this.setImageURI(it)
                },{
                    loadErrorImage(errorDrawable)
                })
    } else {
        loadErrorImage(errorDrawable)
    }
}

fun ImageView.loadErrorImage(@DrawableRes errorDrawable: Int) {
    this.scaleType = ImageView.ScaleType.CENTER
    this.setImageResource(errorDrawable)
}

@Throws(IOException::class)
fun File.moveFile(outputDir: File, fileName: String): File {
    val newFile = File(outputDir, fileName)
    var outputChannel: FileChannel? = null
    var inputChannel: FileChannel? = null
    try {
        outputChannel = FileOutputStream(newFile).channel
        inputChannel = FileInputStream(this).channel
        inputChannel.transferTo(0, inputChannel.size(), outputChannel)
        inputChannel.close()
        this.delete()
    } finally {
        if (inputChannel != null) inputChannel.close()
        if (outputChannel != null) outputChannel.close()
    }

    return newFile
}

fun File.compressImageFile(outputDir: File, fileName: String, compressQuality: Int = 70, scaleQuality: Int = 50): File? {
    // get bitmap from original file
    val fis = FileInputStream(this)
    val originalBitmap = BitmapFactory.decodeStream(fis)
    fis.close()

    val outputFile = File(outputDir, fileName)
    val fos = FileOutputStream(outputFile)
    val scaledWidth = originalBitmap.width * scaleQuality/100
    val scaledHeight = originalBitmap.height * scaleQuality/100
    val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            scaledWidth,
            scaledHeight,
            true)
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, fos)
    fos.close()

    if (!originalBitmap.isRecycled) originalBitmap.recycle()
    if (!scaledBitmap.isRecycled) scaledBitmap.recycle()

    return if (outputFile.exists()) outputFile else null
}
