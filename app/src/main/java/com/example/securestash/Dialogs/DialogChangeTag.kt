package com.example.securestash.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import com.example.securestash.R
import com.google.android.material.button.MaterialButton


class CustomDialogClass(context: Context) : Dialog(context), View.OnClickListener {
    private lateinit var accept: MaterialButton
    private lateinit var cancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_tag_changer)

        accept = findViewById(R.id.accept_button);
        cancel = findViewById(R.id.cancel_button);
        accept.setOnClickListener(this);
        cancel.setOnClickListener(this);
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept_button -> dismiss()
            R.id.cancel_button -> dismiss()
        }
    }
}