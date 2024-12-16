package com.example.securestash

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewAnimator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.securestash.ExternalPackages.PDFPagerAdapter
import com.example.securestash.ExternalPackages.VerticalViewPager
import com.example.securestash.Helpers.CryptographyHelper
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayInputStream
import java.io.File

class ContentDisplay : AppCompatActivity() {
    private lateinit var pager: VerticalViewPager
    private lateinit var pages: TextView
    private lateinit var animator: ViewAnimator
    private var pagerAdapter: PDFPagerAdapter? = null
    private var currentPosition: Int = 0
    private lateinit var showImage: SubsamplingScaleImageView
    private var currentRotation: Int = 0

    private val cryptoHelper: CryptographyHelper = CryptographyHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_content_display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_display)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val itemPath: String
        val itemType: String

        val extras = intent.extras
        if (extras != null) {
            itemPath = extras.getString("ITEM_PATH", "null")
            itemType = extras.getString("ITEM_TYPE", "null")

        } else {
            throw Exception("Intent extras not provided.")
        }

        val file: File = File(itemPath)
        val secretKey = cryptoHelper.getSecretKeyFromKeystore(file.name)
        val decryptedFileBytes = cryptoHelper.decodeFile(file, secretKey).first

        val optionsButton: MaterialButton = findViewById(R.id.buttonOptions)
        optionsButton.setOnClickListener {
            Toast.makeText(this, "Clicked file options", Toast.LENGTH_SHORT).show()
        }

        val closeButton: MaterialButton = findViewById(R.id.buttonClose)
        closeButton.setOnClickListener {
            finish()
        }

        when (itemType) {
            "DOCUMENT" -> {

                pager = findViewById(R.id.pager)
                pages = findViewById<TextView>(R.id.pages)
                animator = findViewById<ViewAnimator>(R.id.animator)
                (animator as? ViewAnimator)?.visibility = View.VISIBLE

                pager.visibility = View.VISIBLE

                findViewById<View>(R.id.btnLoadExtern).setOnClickListener { _: View? ->
                    val toast = Toast.makeText(
                        this,
                        "Implement an intent to show the pdf externally",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                }


                // For now it looks like I will just have to write this file to cache
                // TODO: Find out how to create a ParcelFileDescriptor from a MemoryFile
//                    val memoryFile = MemoryFile("decrypted_pdf", decryptedFileBytes.size)
//                    memoryFile.writeBytes(decryptedFileBytes, 0, 0, decryptedFileBytes.size)

//                val memPDF = MemoryFile("temp_pdf", decryptedFileBytes.size)
//                memPDF.writeBytes(decryptedFileBytes, 0, 0, decryptedFileBytes.size)


                val pdfFile = File(baseContext.cacheDir, "pdf_temp.pdf")
                pdfFile.writeBytes(decryptedFileBytes)

                pagerAdapter = PDFPagerAdapter(this, pdfFile)
                pager.adapter = pagerAdapter

                updatePageCounter()

                (pager as? VerticalViewPager)?.setOnPageChangeListener(object : SimpleOnPageChangeListener() {
                    override fun onPageSelected(index: Int) {
                        this@ContentDisplay.onPageSelected(index)
                    }
                })

            }
            "IMAGE" -> {
                showImage = findViewById(R.id.shownDocument)
                showImage.visibility = View.VISIBLE

                val rotateLeftButton: MaterialButton = findViewById(R.id.buttonRotateImageLeft)
                rotateLeftButton.visibility = View.VISIBLE
                rotateLeftButton.setOnClickListener {
                    rotateImage(-90)
                }

                val rotateRightButton: MaterialButton = findViewById(R.id.buttonRotateImageRight)
                rotateRightButton.visibility = View.VISIBLE
                rotateRightButton.setOnClickListener {
                    rotateImage(90)
                }

                val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(decryptedFileBytes))
                showImage.setImage(ImageSource.bitmap(bitmap))
//                val source = ImageSource.uri(itemPath)
//                showImage.setImage(source)
            }
        }

        Log.d("EXTRAS", "Type: ${itemType}, Path: ${itemPath}")
    }

    private fun updatePageCounter() {
        pages.text = "${currentPosition + 1}/${pagerAdapter?.count}"
    }

    private fun onPageSelected(position: Int) {
        currentPosition = position
        updatePageCounter()
    }

    private fun rotateImage(degrees: Int) {
        currentRotation = (currentRotation + degrees) % 360
        if (currentRotation < 0) {
            currentRotation += 360
        }
        showImage.setOrientation(currentRotation)
    }
}