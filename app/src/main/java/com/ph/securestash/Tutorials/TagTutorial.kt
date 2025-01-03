package com.ph.securestash

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroCustomLayoutFragment
import com.github.appintro.AppIntroPageTransformerType

class TagTutorial : AppIntro2() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("secure_stash", MODE_PRIVATE)

        isIndicatorEnabled = true

        setIndicatorColor(
            selectedIndicatorColor = getColor(R.color.scarlet),
            unselectedIndicatorColor = getColor(R.color.azure)
        )
        setProgressIndicator()
        setTransformer(AppIntroPageTransformerType.Fade)

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.intro_secure_stash)
        )

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.intro_secure_encryption)
        )

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.intro_complete_control)
        )

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.intro_effortless_use)
        )

        addSlide(
            AppIntroCustomLayoutFragment.newInstance(R.layout.intro_tutorial_showcase)
        )

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)

        notifyTutorialOver()

//        val intent = Intent(this, FileDirectory::class.java)
//        startActivity(intent)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        notifyTutorialOver()

//        val intent = Intent(this, FileDirectory::class.java)
//        startActivity(intent)
        finish()
    }

    fun notifyTutorialOver() {
        val editor = sharedPreferences.edit()
        editor.putString("intro", "true")
        editor.apply()
    }
}