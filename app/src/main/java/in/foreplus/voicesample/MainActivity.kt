package `in`.foreplus.voicesample

import `in`.foreplus.voicesample.utils.FileUtil
import `in`.foreplus.voicesample.utils.SelectedFilePath
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


const val FILE_CHOOSE_CODE = 1
const val APP_NAME = "VOICE_SAMPLE"
const val READ_PERMISSION_CODE = 10
const val WRITE_PERMISSION_CODE = 20
const val DATA_FILE = "data.txt"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUploadData.setOnClickListener {
            if (!hasReadPermission()) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_PERMISSION_CODE
                )
            }
            if (!hasWritePermission()) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_PERMISSION_CODE
                )
            }
            if (hasReadPermission() && hasWritePermission()) {
                openFileDialog()
            }
        }

        btnRecordScreen.setOnClickListener {
            val recordScreen = Intent(this@MainActivity, RecordActivity::class.java)
            startActivity(recordScreen)
        }

        btnViewData.setOnClickListener {
            val data = FileUtil.decodeData(File(externalMediaDirs[0], DATA_FILE))
            val sampleDataName = data.first
            val dataSet = data.second
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Data File: $sampleDataName")
                .setItems(dataSet.toTypedArray()) { _, _ -> }
                .setNeutralButton("OK") { d, _ -> d.dismiss() }
                .show()
        }

        btnAboutApp.setOnClickListener {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("About Us")
                .setView(R.layout.about_app_dialog)
                .setNeutralButton("OK") { d, _ -> d.dismiss() }
                .show()
        }

        btnOpenSourceLicenses.setOnClickListener {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Open Source Licenses")
                .setMessage(R.string.os_licenses)
                .setNeutralButton("OK"){d,_ -> d.dismiss()}
                .show()
        }

        btnViewSamples.setOnClickListener {
            // TODO: Make View sample screen
        }

    }


    // region { Permission methods }
    private fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasReadPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    // endregion

    // region { Utility Methods }
    private fun openFileDialog() {
        val fileChooser = Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
            }, "Choose a Data file"
        )
        startActivityForResult(fileChooser, FILE_CHOOSE_CODE)
    }

    private fun msgBox(title: String, message: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton("OK") { d, _ -> d.dismiss() }.show()
    }
    // endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == FILE_CHOOSE_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also {
                val source = File(SelectedFilePath.getPath(this@MainActivity, it))
                val destination = File(externalMediaDirs[0], DATA_FILE)
                FileUtil.copy(source, destination)
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileDialog()
                }
            }
        }
    }
}