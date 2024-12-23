package com.example.securestash

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.io.File

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = this.resources.getColor(R.color.brandeisblue)

        val changePassword: MaterialButton = findViewById(R.id.change_password)
        changePassword.setOnClickListener {
            TODO()
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
}