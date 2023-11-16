package com.hopecoding.myartbookkotlin

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.hopecoding.myartbookkotlin.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream


class ArtActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArtBinding

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    var selectedBitmap: Bitmap? = null

    private lateinit var database: SQLiteDatabase

    val tableName: String = "Arts"

    var artName: String = ""

    var artistName: String = ""

    var year: String = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase(tableName, MODE_PRIVATE, null)

        registerLaunch()

        val intent = intent

        val info = intent.getStringExtra("info")

        if (info.equals("old")) {
            binding.saveBtn.visibility = View.INVISIBLE
            binding.deleteBtn.visibility = View.VISIBLE
            val selectedId = intent.getIntExtra("id", -1)

            val cursor =
                database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artNameTxt.setText(cursor.getString(artNameIx))
                binding.artistNameTxt.setText(cursor.getString(artistNameIx))
                binding.artYearTxt.setText(cursor.getString(yearIx))

                closeEditText()

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)

            }
            cursor.close()
            binding.deleteBtn.setOnClickListener {
                deleteButtonClicked(binding.artNameTxt.text.toString())
            }
        } else {
            binding.deleteBtn.visibility = View.INVISIBLE

        }

    }


    fun saveButtonClicked(view: View) {

        artName = binding.artNameTxt.text.toString()
        artistName = binding.artistNameTxt.text.toString()
        year = binding.artYearTxt.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 900)

            val outputStream = ByteArrayOutputStream()

            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)

            val byteArray = outputStream.toByteArray()

            try {
                //val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")

                val sqlString =
                    "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)
                statement.execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val intent = Intent(this@ArtActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    fun selectImage(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_DENIED
            ) {
                //rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    )
                ) {
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission", View.OnClickListener {
                            //request permission
                            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                        }).show()

                } else {
                    //request Permission
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)

                }


            } else {
                val intentGallery =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentGallery)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                //rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Give Permission", View.OnClickListener {
                            //request permission
                            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }).show()

                } else {
                    //request Permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)

                }


            } else {
                val intentGallery =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentGallery)
            }
        }

    }

    private fun registerLaunch() {

        // Galeriye gidip görseli seçmekle alakalı.
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == RESULT_OK) {

                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        val imageData = intentFromResult.data
                        //binding.imageView.setImageURI(imageData) kolay şekilde yapabiliriz
                        if (imageData != null) {
                            try {
                                if (Build.VERSION.SDK_INT >= 28) {
                                    val source = ImageDecoder.createSource(
                                        this@ArtActivity.contentResolver,
                                        imageData
                                    )
                                    selectedBitmap = ImageDecoder.decodeBitmap(source)
                                    binding.imageView.setImageBitmap(selectedBitmap)
                                } else {
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                                        contentResolver,
                                        imageData
                                    )
                                    binding.imageView.setImageBitmap(selectedBitmap)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                    }
                }

            }


        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //permission granted
                    val intentGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentGallery)
                } else {
                    Toast.makeText(this, "Permission needed!", Toast.LENGTH_LONG).show()
                }

            }
    }

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio: Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1) {
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        } else {
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }


        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun closeEditText() {
        binding.imageView.isClickable = false
        binding.imageView.isFocusable = false
        binding.imageView.isFocusableInTouchMode = false

        binding.artNameTxt.isFocusable = false
        binding.artNameTxt.isClickable = false
        binding.artNameTxt.isFocusableInTouchMode = false

        binding.artistNameTxt.isFocusable = false
        binding.artistNameTxt.isClickable = false
        binding.artistNameTxt.isFocusableInTouchMode = false

        binding.artYearTxt.isFocusable = false
        binding.artYearTxt.isClickable = false
        binding.artYearTxt.isFocusableInTouchMode = false
    }

    fun deleteButtonClicked(artName:String) {
        try {
            val sqlString = "DELETE FROM Arts WHERE artname = ?"
            val statement: SQLiteStatement = database.compileStatement(sqlString)
            statement.bindString(1, artName)

            statement.execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val intent = Intent(this@ArtActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}