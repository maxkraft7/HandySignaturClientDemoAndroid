package wien.technikum.mio.app.kraft.signapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import wien.technikum.mio.app.kraft.signapp.ui.theme.PDFSignAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            PDFSignAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigatorComposeable(this.applicationContext)
                }
            }
        }


    }
}

@Composable
fun NavigatorComposeable(context: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "filechoose") {
        composable("filechoose") { FileChooseSection(context, navController) }
        composable("pdfsignwebview/{fileURI}") { backStackEntry ->
            SignatureWebviewSection(
                context,
                backStackEntry.arguments?.getString("fileURI")?.let { Uri.parse(it) }!!
            )
        }
    }

}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SignatureWebviewSection(context: Context, filepath: Uri, modifier: Modifier = Modifier) {

    val coroutineScope = rememberCoroutineScope();

    // store html that gets shown as state
    val (generatedBase64PDF, setGeneratedBase64PDF) = remember { mutableStateOf<String?>(null) }
    val (responseHTML, setResponseHTML) = remember { mutableStateOf<String?>(null) }


    val webView = remember { WebView(context) }

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "This is my text to send.")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val base64pdf = Utils.readPDFAsBase64(filepath, context)

            setGeneratedBase64PDF(base64pdf)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (responseHTML !== null) {
            Button(onClick = {
                context.startActivity(Utils.intentFromHTML(responseHTML,context))
            }) {
                Text(text = "Share signed PDF")
            }

        } else if (generatedBase64PDF !== null) {


            // Text(generatedBase64PDF)
            AndroidView(
                { webView }, modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) { webView ->
                webView.settings.javaScriptEnabled = true;
                webView.settings.allowFileAccess = true;
                webView.settings.allowContentAccess = true;
                webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE;

                webView.webViewClient = object : WebViewClient() {

                    // todo change signature
                    // https://developer.android.com/reference/android/webkit/WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView,%20android.webkit.WebResourceRequest)
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        Log.i(TAG, "Processing webview url click...")
                        view.loadUrl(url)
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Get the current URL of the WebView.
                        val currentUrl = view!!.url

                        if (Utils.isATrustSignatureResponseURL(currentUrl)) {
                            view.evaluateJavascript("document.body.innerHTML") { value ->
                                run {
                                    // pass value to results composable
                                    setResponseHTML(value);
                                }
                            }
                        }
                    }
                }

                webView.loadDataWithBaseURL(
                    null,
                    Utils.returnStaticHTML(generatedBase64PDF),
                    "text/html",
                    "UTF-8",
                    null
                );
            }


        } else {
            Text("Loading...")
        }

    }
}

@Composable
fun FileChooseSection(
    context: Context,
    navigator: NavHostController,
    modifier: Modifier = Modifier
) {

    // store file path as state
    val (filePathURI, setFilePathURI) = remember { mutableStateOf<Uri?>(null) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { pdfUri ->
        setFilePathURI(pdfUri)

        if (pdfUri != null) {
            // navigate to signactivity
            // navigator.navigate("pdfsignwebview${pdfUri.path}")
        }

    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (filePathURI == null) {

            Button(onClick = { pickPdfLauncher.launch("application/pdf") })
            { Text("Choose file") }

            Text("File path: $filePathURI")
        } else {
            SignatureWebviewSection(context, filePathURI)
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PDFSignAppTheme {

    }
}