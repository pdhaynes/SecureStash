package com.ph.securestash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.ph.securestash.Adapters.TutorialsAdapter
import com.ph.securestash.DataModels.Tutorial

class Tutorials : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutorials)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tutorials_page)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = this.getColor(R.color.brandeisblue)
        window.navigationBarColor = this.getColor(R.color.paledogwood)

        val tutorialsRecyclerView: RecyclerView = findViewById(R.id.tutorials_recycler_view)

        val tutorialsList = listOf(
            Tutorial("Uploading Items", R.drawable.ic_question_mark_24, Introduction()),
            Tutorial("Tags", R.drawable.ic_question_mark_24, Introduction()),
        )

        val backButton: MaterialButton = findViewById(R.id.back_fab)
        backButton.setOnClickListener {
            finish()
        }

        tutorialsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        val adapter = TutorialsAdapter(tutorialsList)
        tutorialsRecyclerView.adapter = adapter
    }
}