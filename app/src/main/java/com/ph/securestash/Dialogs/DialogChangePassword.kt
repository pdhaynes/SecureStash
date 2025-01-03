package com.ph.securestash.Dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.ph.securestash.Helpers.CredentialManager
import com.ph.securestash.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class DialogChangePassword(context: Context) : Dialog(context), View.OnClickListener {
    private lateinit var accept: MaterialButton
    private lateinit var cancel: MaterialButton

    private lateinit var savedUserPin: String
    private var newPin: String = ""

    private var errorMessage: String = ""
    private var errorsExist: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_change_password)

        accept = findViewById(R.id.accept_button)
        cancel = findViewById(R.id.cancel_button)
        accept.setOnClickListener(this)
        cancel.setOnClickListener(this)

        val sharedPreferences = context.getSharedPreferences("secure_stash", MODE_PRIVATE)
        savedUserPin = sharedPreferences.getString("user_pin_enc", null)!!

        val newPinStatus: MaterialTextView = findViewById(R.id.new_pin_status)

        val pinInputEditText: TextInputEditText = findViewById(R.id.new_pin_input)
        pinInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                pinInputEditText.background.setTint(Color.WHITE)
                newPinStatus.visibility = View.GONE

                val inputText = charSequence.toString()
                if (charSequence!!.count() < 6) {
                    errorsExist = true
                    errorMessage = "PIN must be 6 digits."
                    pinInputEditText.background.setTint(context.getColor(R.color.rust))

                    newPinStatus.text = errorMessage
                    newPinStatus.visibility = View.VISIBLE
                } else {
                    errorsExist = false
                    errorMessage = ""
                    newPinStatus.visibility = View.GONE
                }
                newPin = inputText
            }

            override fun afterTextChanged(editable: Editable?) {
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept_button -> {
                if (errorsExist) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    return
                }

                val sharedPreferences = context.getSharedPreferences("secure_stash", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("user_pin_enc", CredentialManager().encryptData(newPin))
                editor.apply()

                Toast.makeText(context, "PIN successfully changed", Toast.LENGTH_LONG).show()
                dismiss()
            }
            R.id.cancel_button -> dismiss()
        }
    }
}