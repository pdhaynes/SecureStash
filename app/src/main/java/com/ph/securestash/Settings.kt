package com.ph.securestash

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ph.securestash.DataModels.ItemType
import com.ph.securestash.Dialogs.DialogChangePassword
import com.ph.securestash.Helpers.CryptographyHelper
import com.ph.securestash.Helpers.UtilityHelper
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class Settings : AppCompatActivity() {

    var versionName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "N/A"
        }

        val totalDocumentsStored: TextView = findViewById(R.id.amount_documents_stored)
        val documentLoadingBar: ProgressBar = findViewById(R.id.documents_loading_spinner)

        CoroutineScope(Dispatchers.Main).launch {
            val documents = loadDocumentsInBackground()

            totalDocumentsStored.text = String.format(Locale.getDefault(), documents.toString())
            totalDocumentsStored.visibility = View.VISIBLE
            documentLoadingBar.visibility = View.GONE
        }

        val totalImagesStored: TextView = findViewById(R.id.amount_images_stored)
        val imageLoadingBar: ProgressBar = findViewById(R.id.image_loading_spinner)

        CoroutineScope(Dispatchers.Main).launch {
            val images = loadImagesInBackground()

            totalImagesStored.text = String.format(Locale.getDefault(), images.toString())
            totalImagesStored.visibility = View.VISIBLE
            imageLoadingBar.visibility = View.GONE
        }

        val totalItemsStored: TextView = findViewById(R.id.amount_items_stored)
        val itemLoadingBar: ProgressBar = findViewById(R.id.items_loading_spinner)

        CoroutineScope(Dispatchers.Main).launch {
            val items = loadItemsInBackground()

            totalItemsStored.text = String.format(Locale.getDefault(), items.toString())
            totalItemsStored.visibility = View.VISIBLE
            itemLoadingBar.visibility = View.GONE
        }

        val buildCode: TextView = findViewById(R.id.build_code)
        buildCode.text = "Version: $versionName"

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = this.resources.getColor(R.color.brandeisblue)

        val changePassword: MaterialButton = findViewById(R.id.change_password)
        changePassword.setOnClickListener {
            DialogChangePassword(this)
                .show()
        }

        val deleteAccount: MaterialButton = findViewById(R.id.delete_all_data_and_account)
        deleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Are you certain? This will delete ALL files and directories made in the app as well as removing all login information")
                .setPositiveButton("DELETE ALL DATA AND ACCOUNT") { _, _ ->
                    val sharedPreferences = getSharedPreferences("secure_stash", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.remove("user_pin_enc")
                    editor.apply()

                    showLoadingScreen(dataDir, true)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val deleteData: MaterialButton = findViewById(R.id.delete_all_data)
        deleteData.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Are you certain? This will delete ALL files and directories made in the app.")
                .setPositiveButton("DELETE ALL DATA") { _, _ ->
                    showLoadingScreen(dataDir, false)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val reportBugs: MaterialButton = findViewById(R.id.bug_report_button)
        reportBugs.setOnClickListener {
            sendBugReportEmail(this)
        }

        val showTutorials: MaterialButton = findViewById(R.id.show_tutorials)
        showTutorials.setOnClickListener {
            Toast.makeText(baseContext, "No tutorials available yet, if you are a tester reach out to me directly.", Toast.LENGTH_LONG).show()
//            val intent = Intent(baseContext, Tutorials::class.java)
//            startActivity(intent)
        }

        val backButton: MaterialButton = findViewById(R.id.back_fab)
        backButton.setOnClickListener {
            finish()
        }
    }

    fun showLoadingScreen(targetDirectory: File, accountDeleted: Boolean) {
        val intent = Intent(this, LoadingScreen::class.java)
        intent.putExtra("SPECIFIED_DIR", targetDirectory.toString())
        intent.putExtra("ACCOUNT_DELETED", accountDeleted)
        intent.putExtra("LOAD_TYPE", "DELETE")

        startActivity(intent)
        finish()
    }

    private suspend fun loadDocumentsInBackground(): Int {
        return withContext(Dispatchers.IO) {
            val cryptoHelper = CryptographyHelper()

            val targetType = ItemType.DOCUMENT
            UtilityHelper.recursivelyGrabFileList(File(filesDir, "Files"))
                .filter { it.isFile }
                .mapNotNull { file ->
                    try {
                        val secretKey = cryptoHelper.getSecretKeyFromKeystore(file.name)
                        val decodedData = cryptoHelper.decodeFile(file, secretKey)
                        val metadata = cryptoHelper.readMetadata(decodedData.second)

                        if (metadata.first == targetType) file else null
                    } catch (e: Exception) {
                        Log.e("FileProcessing", "Error decoding file: ${file.name}", e)
                        null
                    }
                }.count()
        }
    }

    private suspend fun loadImagesInBackground(): Int {
        return withContext(Dispatchers.IO) {
            val cryptoHelper = CryptographyHelper()

            val targetType = ItemType.IMAGE
            UtilityHelper.recursivelyGrabFileList(File(filesDir, "Files"))
                .filter { it.isFile }
                .mapNotNull { file ->
                    try {
                        val secretKey = cryptoHelper.getSecretKeyFromKeystore(file.name)
                        val decodedData = cryptoHelper.decodeFile(file, secretKey)
                        val metadata = cryptoHelper.readMetadata(decodedData.second)

                        if (metadata.first == targetType) file else null
                    } catch (e: Exception) {
                        Log.e("FileProcessing", "Error decoding file: ${file.name}", e)
                        null
                    }
                }.count()
        }
    }

    private suspend fun loadItemsInBackground(): Int {
        return withContext(Dispatchers.IO) {
            UtilityHelper.recursivelyGrabFileList(File(filesDir, "Files")).filter {
                it.isFile
            }.count()
        }
    }

    fun sendBugReportEmail(context: Context) {
        val recipient = "support@example.com"
        val subject = "Bug Report"
        val body = """
        Please describe the bug you encountered:
        
        ---
        App Version: ${versionName}
        Android Version: ${android.os.Build.VERSION.RELEASE}
        Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
        """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipient")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        if (emailIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(emailIntent)
        } else {
            Toast.makeText(context, "No email client found. Please install one to report bugs.", Toast.LENGTH_LONG).show()
        }
    }
}