package wien.technikum.mio.app.kraft.signapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream


object Utils {

    private val PICK_PDF_FILE = 2

    suspend fun readPDFAsBase64(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        val bufferSize = 1048576 // Adjust the buffer size as needed
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

        inputStream?.use { inputStream ->
            val buffer = ByteArray(bufferSize)
            val outputStream = ByteArrayOutputStream()

            var bytesRead: Int
            while (inputStream.read(buffer, 0, bufferSize).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            // Convert to Base64
            return@withContext Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }
            ?: throw Exception("Failed to open input stream for URI: $uri")
    }

    fun isATrustSignatureResponseURL(url: String?): Boolean {
        // response format:
        // https://www.handy-signatur.at/mobile/https-security-layer-request/response.aspx?sid=A3rDQNTCTKNJCKNGHJDKAICLZJRWNYD

        if (url == null){
            return false;
        }

        return url.contains("response")

    }

    // todo move to asset file, and construct by established means
    fun returnStaticHTML(pdfBase64: String): String{
        return """<?xml version='1.0' encoding='UTF-8'?>
<html>
<head>
    <title>Handy-Signatur Client Demo</title>
</head>
<body>
    <form method='post' id='form1' action="https://www.handy-signatur.at/mobile/https-security-layer-request/" >
        <input type='text' name='XMLRequest' value="<?xml version='1.0' encoding='UTF-8'?>
        <sl:CreateCMSSignatureRequest xmlns:sl='http://www.buergerkarte.at/namespaces/securitylayer/1.2#' Structure='enveloping'>
          <sl:KeyboxIdentifier>SecureSignatureKeypair</sl:KeyboxIdentifier>
          <sl:DataObject>
            <sl:MetaInfo>
              <sl:MimeType>application/pdf</sl:MimeType>
            </sl:MetaInfo>
            <sl:Content>
              <sl:Base64Content>$pdfBase64</sl:Base64Content>
            </sl:Content>
          </sl:DataObject>
        </sl:CreateCMSSignatureRequest>" id='XMLRequest'  />
        <br/>
        <input type='submit' name='submit' id='submit' value='start'  />        
    </form>
</body>
</html>""";
    }

    fun openFileIntent(pickerInitialUri: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        return intent;
    }

    fun uriFromFile(context:Context, file:File):Uri {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
    }

    fun intentFromHTML(responseHTML: String, context: Context): Intent? {
        // extract base64 from response
        val base64payload = responseHTML
            .substringAfter("\\u003Csl:CMSSignature>")
            .substringBefore("\\u003C/sl:CMSSignature>")

        // decode base64 to file
        val decodedBytes = Base64.decode(base64payload, Base64.DEFAULT)

        // todo remove file afer sharing
        val file = File(context.filesDir, "signed.pdf")
        file.writeBytes(decodedBytes)


        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Sharing file from the AppName"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Sharing file from the AppName with some description"
            )
            val fileURI = FileProvider.getUriForFile(
                context!!, context!!.packageName + ".provider",
                file
            )
            putExtra(Intent.EXTRA_STREAM, fileURI)
        }

        return shareIntent
    }


}