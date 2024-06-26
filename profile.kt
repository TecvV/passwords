package com.example.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.webkit.MimeTypeMap
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
//import com.example.app.firebase.FirestoreClasstoreClass
import com.example.app.utils.LoadingDialog
import com.example.app.User
import com.example.app.firebase.FirestoreClass
import com.google.android.gms.auth.api.signin.internal.Storage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException


class profile : AppCompatActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private var mProfileImageUrl : String = " "
//    private lateinit var uid : String

    companion object{
        private const val READ_STORAGE_PERMISSION_CODE = 1
        private const val PICK_IMAGE_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        lateinit var mUserDetails: User
        FirestoreClass().loadUserData(this@profile)

        val floatingActionButton : FloatingActionButton = findViewById(R.id.floatingActionButton)
        val log_out_btn: LinearLayout = findViewById(R.id.log_out_btn)
        val home_btn: LinearLayout = findViewById(R.id.home_btn)
        val profile_btn: LinearLayout = findViewById(R.id.profile_btn)
        val iv_profile_user_image: CircleImageView = findViewById(R.id.iv_profile_user_image)
        val et_name3: TextView = findViewById(R.id.et_name3)
        val et_name2: TextView = findViewById(R.id.et_name2)
        val Phone_number: TextView = findViewById(R.id.Phone_number)
        val window: Window = this@profile.window
        window.statusBarColor = ContextCompat.getColor(this@profile, R.color.backgroundMA)
        window.navigationBarColor = ContextCompat.getColor(this@profile, R.color.backgroundMA)

        floatingActionButton.setOnClickListener{
            if(mSelectedImageFileUri != null){
                uploadUserImage()
            }

        }

        log_out_btn.setOnClickListener {
            val loading = LoadingDialog(this)
            loading.startLoading()
            val handler = Handler()
            handler.postDelayed(object : Runnable {
                override fun run() {
                    loading.isDismiss()
                }
            }, 3000)
            FirebaseAuth.getInstance().signOut()
            finishAffinity()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show()
        }

        iv_profile_user_image.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                showImageChooser()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_STORAGE_PERMISSION_CODE
                )
            }
        }

        home_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val profiletoimg: CircleImageView = findViewById(R.id.iv_profile_user_image)
        val et_name3: TextView = findViewById(R.id.et_name3)
        val et_name2: TextView = findViewById(R.id.et_name2)
        val Phone_number: TextView = findViewById(R.id.Phone_number)
        if (resultCode == Activity.RESULT_OK
            && requestCode == PICK_IMAGE_REQUEST_CODE
            && data!!.data != null
        ) {
            // The uri of selection image from phone storage.
            mSelectedImageFileUri = data.data!!
            //We used try catch because we are uploading something so there could be error while uploading
            try {
                Glide
                    .with(this@profile)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.personholder)
                    .into(profiletoimg); // the view in which the image will be loaded.
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showImageChooser()
            }
        } else {
            Toast.makeText(
                this, "Oops you just rejected the permission of external storage",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showImageChooser() {
        var galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)

    }

    fun setUserDataInUI(user: User) {

        val profiletoimg: CircleImageView = findViewById(R.id.iv_profile_user_image)
        val et_name3: TextView = findViewById(R.id.et_name3)
        val et_name2: TextView = findViewById(R.id.et_name2)
        val Phone_number: TextView = findViewById(R.id.Phone_number)

        Glide
            .with(this@profile)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.personholder)
            .into(profiletoimg);

        et_name3.setText(user.name)
        et_name2.setText(user.email)
        if (user.mobile != 0L) {
            Phone_number.setText(user.mobile.toString())
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun uploadUserImage(){
        if(mSelectedImageFileUri != null){
            val sRef : StorageReference =
                FirebaseStorage.getInstance().reference.child(
                    "USER_IMAGE" + System.currentTimeMillis()
                        + "." + getFileExtension(mSelectedImageFileUri!!))

                sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                    taskSnapshot ->
                    Log.i(
                        "Firebase Image URL",
                        taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                    )
                    taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        uri ->
                        Log.i("Downloaded Image URL", uri.toString())
                        mProfileImageUrl = uri.toString()
                    }
                }.addOnFailureListener{
                           exception ->
                            Toast.makeText(this@profile,
                            exception.message,
                            Toast.LENGTH_LONG
                            ).show()
                }

        }
    }

    private fun getFileExtension(uri: Uri): String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }
}


