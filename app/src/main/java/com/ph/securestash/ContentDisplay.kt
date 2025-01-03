package com.ph.securestash

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
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
import com.ph.securestash.ExternalPackages.PDFDecoder.PDFPagerAdapter
import com.ph.securestash.Helpers.CryptographyHelper
import com.google.android.material.button.MaterialButton
import com.ph.securestash.ExternalPackages.VerticalViewPager
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

        val file = File(itemPath)
        val secretKey = cryptoHelper.getSecretKeyFromKeystore(file.name)
        val decryptedFileBytes = cryptoHelper.decodeFile(file, secretKey).first

        val optionsButton: MaterialButton = findViewById(R.id.buttonOptions)
        optionsButton.setOnClickListener {
            Toast.makeText(this, "Clicked file options", Toast.LENGTH_SHORT).show()
        }

        val closeButton: MaterialButton = findViewById(R.id.buttonClose)
        closeButton.setOnClickListener {
            val pdfFile = File(baseContext.cacheDir, "pdf_temp.pdf")
            if (pdfFile.exists()) {
                pdfFile.delete()
            }
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

                val inputStream = ByteArrayInputStream(decryptedFileBytes)
                val exif = ExifInterface(inputStream)

                val rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }

                val originalBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(decryptedFileBytes))
                val bitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDegrees.toFloat())
                    Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                } else {
                    originalBitmap
                }
                showImage.setImage(ImageSource.bitmap(bitmap))
            }
        }
    }

    private fun updatePageCounter() {
        pages.text = getString(R.string.pdf_page_counter, currentPosition + 1, pagerAdapter?.count)
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