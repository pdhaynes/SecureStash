package com.example.securestash

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securestash.Helpers.Cache
import com.example.securestash.Helpers.CredentialManager
import com.example.securestash.Helpers.UtilityHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = this.getColor(R.color.isabelline)
        window.navigationBarColor = this.getColor(R.color.paledogwood)

        // region UI Variable Assignments

        // region Signup UI Elements

        val signupPinHeader: MaterialTextView = findViewById(R.id.signup_pin_header)

        val signupPinField: TextInputEditText = findViewById(R.id.signup_pin_field)
        signupPinField.filters = arrayOf(InputFilter.LengthFilter(6))

        val signupConfirmPinHeader: MaterialTextView = findViewById(R.id.signup_pin_confirm_header)
        val signupConfirmPinField: TextInputEditText = findViewById(R.id.signup_pin_confirm_field)
        signupConfirmPinField.filters = arrayOf(InputFilter.LengthFilter(6))

        val signupPinStatus: MaterialTextView = findViewById(R.id.signup_pin_status)
        val signupConfirmPinStatus: MaterialTextView = findViewById(R.id.signup_pin_confirm_status)

        val signupButton: MaterialButton = findViewById(R.id.signup_button)

        // endregion

        // region Login UI Elements

        val loginPinHeader: MaterialTextView = findViewById(R.id.login_pin_header)
        val loginPinField: TextInputEditText = findViewById(R.id.login_pin_field)
        val loginPinStatus: MaterialTextView = findViewById(R.id.login_pin_status)

        val loginButton: MaterialButton= findViewById(R.id.login_button)

        // endregion

        // endregion

        // region Compartment Functions
        fun shouldHideLoginItems(should: Boolean) {
            if (should) {
                loginPinHeader.visibility = View.GONE
                loginPinField.visibility = View.GONE
                loginButton.visibility = View.GONE

                signupPinHeader.visibility = View.VISIBLE
                signupPinField.visibility = View.VISIBLE

                signupConfirmPinHeader.visibility = View.VISIBLE
                signupConfirmPinField.visibility = View.VISIBLE

                signupButton.visibility = View.VISIBLE
            } else {
                loginPinHeader.visibility = View.VISIBLE
                loginPinField.visibility = View.VISIBLE
                loginButton.visibility = View.VISIBLE

                signupPinHeader.visibility = View.GONE
                signupPinField.visibility = View.GONE

                signupConfirmPinHeader.visibility = View.GONE
                signupConfirmPinField.visibility = View.GONE

                signupButton.visibility = View.GONE
            }

        }

        fun userLogin() {
            shouldHideLoginItems(false)

            var inputPin = ""
            val sharedPreferences = getSharedPreferences("secure_stash", MODE_PRIVATE)

            loginPinField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                    loginPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.white))
                    loginPinStatus.visibility = View.GONE

                    val inputText = charSequence.toString()
                    inputPin = inputText
                }

                override fun afterTextChanged(editable: Editable?) {
                }
            })

            loginButton.setOnClickListener() {
                val storedPin = CredentialManager().decryptData(
                    sharedPreferences.getString(
                        "user_pin_enc",
                        null
                    ).toString()
                )

                if (inputPin.length == 0) {
                    val errMsg = "Please enter a pin."
                    loginPinStatus.text = errMsg
                    loginPinStatus.visibility = View.VISIBLE
                    loginPinField.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.scarlet))

                } else if (inputPin.length < 6) {
                    val errMsg = "PIN must be 6 digits."
                    loginPinStatus.text = errMsg
                    loginPinStatus.visibility = View.VISIBLE

                    loginPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.scarlet))
                } else if (storedPin != inputPin){
                    loginPinStatus.text = getString(R.string.incorrect_pin)
                    loginPinStatus.visibility = View.VISIBLE

                    loginPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.scarlet))
                } else{
                    val intent = Intent(this, FileDirectory::class.java)
                    startActivity(intent)

                    finish()
                    }
                }
        }

        fun userSignUp() {
            shouldHideLoginItems(true)

            var initialPass = ""
            var confirmPass = ""

            signupPinField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                    signupPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.white))

                    val inputText = charSequence.toString()
                    if (inputText.length < 6) {
                        signupPinStatus.text = getString(R.string.err_improper_pin_size)
                        signupPinStatus.visibility = View.VISIBLE

                        signupPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.scarlet))
                    } else {
                        signupPinStatus.visibility = View.GONE
                        initialPass = inputText
                    }
                }

                override fun afterTextChanged(editable: Editable?) {
                }
            })

            signupConfirmPinField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                    signupConfirmPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.white))

                    val inputText = charSequence.toString()
                    if (inputText != initialPass) {
                        signupConfirmPinStatus.text = getString(R.string.err_pins_dont_match)
                        signupConfirmPinStatus.visibility = View.VISIBLE

                        signupConfirmPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.scarlet))
                    }
                    else {
                        signupConfirmPinStatus.visibility = View.GONE
                    }
                    confirmPass = inputText
                }

                override fun afterTextChanged(editable: Editable?) {
                }
            })

            signupButton.setOnClickListener {
                if (initialPass.isEmpty()) {
                    val errMsg = "Please enter a pin."
                    signupPinStatus.text = errMsg
                    signupPinStatus.visibility = View.VISIBLE
                    signupPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.scarlet))
                } else if (initialPass != confirmPass) {
                    val errMsg = "PINs do not match."
                    signupConfirmPinStatus.text = errMsg
                    signupConfirmPinStatus.visibility = View.VISIBLE
                    Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show()

                    signupConfirmPinField.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.scarlet))

                } else {
                    val sharedPreferences = getSharedPreferences("secure_stash", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("user_pin_enc", CredentialManager().encryptData(initialPass))
                    editor.apply()

                    val intent = Intent(this, FileDirectory::class.java)
                    startActivity(intent)

                    finish()
                }
            }
        }

        // endregion

        val sharedPreferences = getSharedPreferences("secure_stash", MODE_PRIVATE)
        val storedUserPin = sharedPreferences.getString("user_pin_enc", null)

        val fileDirectory = File(filesDir, "Files")
        if (!fileDirectory.exists()) {
            fileDirectory.mkdir()
        }

        val configFile = File(cacheDir, "config.json")
        if (!configFile.exists()) {
            configFile.createNewFile()
        }

        val tagFile = File(cacheDir, "tags.json")
        if (!tagFile.exists()) {
            tagFile.createNewFile()
        }

        val statsFile = File(cacheDir, "stats.json")
        if (!statsFile.exists()) {
            statsFile.createNewFile()
        }

        val tempDirectory = File(filesDir, "Temp")
        if (tempDirectory.exists()) {
            val fileList = tempDirectory.listFiles()
            if (fileList !=null) {
                for (file in fileList) {
                    file.delete()
                }
            }
        }

        val cacheList = UtilityHelper.recursivelyGrabFileList(File(filesDir, "Files")).filter {
            it.isFile
        }
        Cache.buildCache(cacheList)

        if (storedUserPin.isNullOrEmpty()) {
            userSignUp()
        } else {
            userLogin()
        }

    }

}