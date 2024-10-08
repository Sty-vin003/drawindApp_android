package com.example.drawing_app

import android.app.Dialog
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var mImageCurrentPaint : ImageButton? = null

    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
                permissions.entries.forEach{
                    val permissionsName = it.key
                    val isGranted = it.value

                    if(isGranted){
                        Toast.makeText(this,
                            "Permission granted you can read the storage files",
                            Toast.LENGTH_LONG).show()
                    }else{
                        if(permissionsName == Manifest.permission.READ_EXTERNAL_STORAGE){
                            Toast.makeText(this,
                            "Ooops you just denied the permission",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                }

        }




    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_preesd)
        )

//        val ibredo: ImageButton = findViewById(R.id.ib_redo)
//        ibredo.setOnClickListener{
//            drawingView?.onClickRedo()
//        }


        val ibundo: ImageButton = findViewById(R.id.ib_undo)
        ibundo.setOnClickListener{
            drawingView?.onClickUndo()
        }
        val ibsave: ImageButton = findViewById(R.id.ib_save)
        ibsave.setOnClickListener{
           if(isReadstorageallowed()){
               val fl_drawing_view_container = findViewById<FrameLayout>(R.id.fl_drawing_view_container)
               BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
           }else{
               requestStoragePermission()
           }
        }


        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{

            if(isReadstorageallowed()){

                val pickPhotoIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent, GALLERY)



            }else{
                requestStoragePermission()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                try{
                if(data!!.data != null){
                    var iv_background = findViewById<ImageView>(R.id.iv_background)
                    iv_background.visibility == View.VISIBLE
                    iv_background.setImageURI(data.data)
                }else{
                    Toast.makeText(this@MainActivity,
                        "Error in passing the image or its corrupted.",
                        Toast.LENGTH_SHORT).show()
                }
                }catch(e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }
    fun paintClicked(view: View){
        if(view != mImageCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setcolor(colorTag)

                imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_preesd)
            )

            mImageCurrentPaint?.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_preesd)
            )
            mImageCurrentPaint = view
        }

    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this, "need permission to add a background",
                Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isEmpty() && grantResults[0]
                == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(
                    this,
                    "permission granted now you can read storage file",
                    Toast.LENGTH_LONG
                ).show()
            }else{
                Toast.makeText(
                    this,
                    "Oops you just denied the permisson", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadstorageallowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap : Bitmap) :
        AsyncTask<Any, Void, String>(){

            private lateinit var mProgressDialog : Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }



        override fun doInBackground(vararg params: Any?): String {

            var result = ""

            if(mBitmap != null){

            }

//            STRUCTURE WHEN YOU WANT TO INSTALL SOMETHING IN YOUR DEVICE

            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "DrawingApp_"
                            + System.currentTimeMillis() / 1000 + ".png" )

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath

                }catch(e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        return result
        }

//       ^ ^ ^ ^ ^ ^

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (!result!!.isEmpty()){
                Toast.makeText(this@MainActivity,
                    "File saved successfully : $result",
                    Toast.LENGTH_SHORT
                    ).show()
            }
            else{
                Toast.makeText(this@MainActivity,
                    "something went wrong when saving the file",
                    Toast.LENGTH_SHORT)
                    .show()
            }

//            FOR SHARING
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type =  "image/png"

                startActivity(
                    Intent.createChooser(
                        shareIntent, "share"
                    )

                )

            }
        }



        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity,)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }
    }



    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

//    private fun requestStoragePermission(){
//        if(ActivityCompat.shouldShowRequestPermissionRationale(
//            this,
//            Manifest.permission.READ_EXTERNAL_STORAGE)
//            ){
//            showRationaleDialog("drawing app", "drawing app" +
//                        "needs to access your external storage")
//        }else{
//            requestPermission.launch(arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE
//                // TODO - ADD writing eternal storage permission
//            ))
//        }
//    }
//
//    private fun showRationaleDialog(
//        title : String,
//        message: String,
//    ){
//        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
//        builder.setTitle(title)
//            .setMessage(message)
//            .setPositiveButton("Cancel"){dialog, _->
//                dialog.dismiss()
//            }
//        builder.create().show()
//    }



}
