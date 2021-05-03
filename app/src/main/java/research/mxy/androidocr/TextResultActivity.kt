package research.mxy.androidocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText

class TextResultActivity : AppCompatActivity() {

    private var receivedResult: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_result)

        // Retrieve data from MainActivity
        val extras = intent.extras

        setupUI()

        if (extras != null) {

            try {
                receivedResult = extras.getString("result").toString()

                // TextInputEditText widget only accept Editable type, not String
                var toEditable: Editable = SpannableStringBuilder(receivedResult)
                findViewById<TextInputEditText>(R.id.textResult).text = toEditable



            } catch (e: ApiException) {
                Toast.makeText(this, "Failed to output result :(", Toast.LENGTH_LONG)
                    .show()
                Log.w("outputError", "outputResult:failed code=" + e.statusCode)
            }

        } else {
            Toast.makeText(this, "Failed to recognise any text :(", Toast.LENGTH_LONG)
                .show()
        }


    }

    private fun setupUI() {
        var buttonCopy = findViewById<Button>(R.id.buttonCopy)
        buttonCopy.setOnClickListener() {
            copyToClipboard()
            Toast.makeText(this,"All text copied",Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun copyToClipboard() {
        var clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        var clip: ClipData = ClipData.newPlainText("result", receivedResult)
        clipboard.setPrimaryClip(clip)
    }


}