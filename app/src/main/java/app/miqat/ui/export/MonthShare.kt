package app.miqat.ui.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Sharing the month.
 *
 * The image goes through the platform's FileProvider (no new dependency, no
 * permission, nothing left on the device): the user gets the card in whatever
 * app they share it to, and the app keeps nothing.
 */
object MonthShare {

    private const val FILE_NAME = "miqat-month.png"

    /** Write the card and open the share sheet. */
    fun share(context: Context, bitmap: Bitmap, text: String) {
        try {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, FILE_NAME)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", file,
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "مشاركة دفتر الشهر").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // sharing is a convenience — it must never crash the app
        }
    }
}
