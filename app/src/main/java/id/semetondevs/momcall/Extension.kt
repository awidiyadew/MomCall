package id.semetondevs.momcall

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


