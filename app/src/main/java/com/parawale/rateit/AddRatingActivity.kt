package com.parawale.rateit

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.parawale.rateit.Models.Product
import com.parawale.rateit.Models.Rating
import com.parawale.rateit.network.FirebaseStoreApi
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class AddRatingActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPrice: EditText
    private lateinit var etCategory: EditText
    private lateinit var etProductLink: EditText
    private lateinit var ratingBar: RatingBar
    private lateinit var btnSubmit: Button
    private lateinit var invoiceContainer: LinearLayout
    private lateinit var productImageContainer: LinearLayout
    private lateinit var loadingDialog: ProgressDialog
    private var verificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private var invoiceUri: Uri? = null
    private val productImageUris = mutableListOf<Uri>()

    companion object {
        private const val PICK_INVOICE_REQUEST = 1001
        private const val PICK_PRODUCT_IMAGES_REQUEST = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_rating)

        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etCategory = findViewById(R.id.etCategory)
        etProductLink = findViewById(R.id.etProductLink)
        ratingBar = findViewById(R.id.ratingBar)
        btnSubmit = findViewById(R.id.btnSubmit)
        invoiceContainer = findViewById(R.id.invoiceContainer)
        productImageContainer = findViewById(R.id.productImagesContainer)
        firebaseAuth = FirebaseAuth.getInstance()
        loadingDialog = ProgressDialog(this).apply {
            setMessage("Uploading...")
            setCancelable(false)
        }

        findViewById<MaterialCardView>(R.id.cardAddInvoice).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_INVOICE_REQUEST)
        }

        findViewById<MaterialCardView>(R.id.cardAddProductImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Product Images"), PICK_PRODUCT_IMAGES_REQUEST)
        }
        val currentUser = firebaseAuth.currentUser?.email ?: "Unknown"
        Toast.makeText(this, "Current User: $currentUser", Toast.LENGTH_SHORT).show()
        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString()
            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val description = etDescription.text.toString()
            val category = etCategory.text.toString()
            val link = etProductLink.text.toString()
            val rating = ratingBar.rating.toDouble()

            if (title.isBlank() || category.isBlank() || description.isBlank()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showPhoneNumberDialog(currentUser)
        }
    }
    private fun showPhoneNumberDialog(currentUser: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_phone_number, null)
        val etPhoneInput = view.findViewById<EditText>(R.id.etPhoneInput)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Send OTP", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val number = etPhoneInput.text.toString().trim()
                if (number.length == 10) {
                    dialog.dismiss()
                    showSendingOtpDialog()
                    sendOtp(number, currentUser)
                } else {
                    etPhoneInput.error = "Enter a valid 10-digit number"
                }
            }
        }

        dialog.show()
    }

    private var sendingOtpDialog: ProgressDialog? = null

    private fun showSendingOtpDialog() {
        sendingOtpDialog = ProgressDialog(this).apply {
            setMessage("Sending OTP, please wait...")
            setCancelable(false)
            show()
        }
    }
    private fun sendOtp(phoneNumber: String,currentUser: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    sendingOtpDialog?.dismiss()
                    signInWithPhoneAuthCredential(credential, currentUser)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    sendingOtpDialog?.dismiss()
                    Toast.makeText(this@AddRatingActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    sendingOtpDialog?.dismiss()
                    this@AddRatingActivity.verificationId = verificationId
                    showOtpDialog(currentUser)
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun showOtpDialog(currentUser: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_otp_verification, null)
        val etOtpInput = view.findViewById<EditText>(R.id.etOtpInput)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Verify", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val otp = etOtpInput.text.toString().trim()
                if (otp.length == 6 && verificationId != null) {
                    val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                    dialog.dismiss()
                    signInWithPhoneAuthCredential(credential, currentUser)
                } else {
                    etOtpInput.error = "Enter a valid 6-digit OTP"
                }
            }
        }

        dialog.show()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, currentUser: String) {
        loadingDialog.show()
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    submitProduct(currentUser)
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "OTP verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun submitProduct(currentUser: String) {
        val title = etTitle.text.toString()
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val description = etDescription.text.toString()
        val category = etCategory.text.toString()
        val link = etProductLink.text.toString()
        val rating = ratingBar.rating.toDouble()

        lifecycleScope.launch {
            val product = FirebaseStoreApi.uploadProductData(
                title,
                price,
                description,
                category,
                link,
                rating,
                productImageUris,
                invoiceUri,
                context = this@AddRatingActivity,
                createdBy = currentUser
            )

            loadingDialog.dismiss()

            if (product != null) {
                val resultIntent = Intent().apply {
                    putExtra("product", product)
                }
                setResult(RESULT_OK, resultIntent)
                Toast.makeText(this@AddRatingActivity, "Product submitted successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AddRatingActivity, "Failed to submit product", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PICK_INVOICE_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    invoiceUri = data?.data ?: return
                    invoiceContainer.removeViews(1, invoiceContainer.childCount - 1)

                    val imageView = ImageView(this).apply {
                        layoutParams = ViewGroup.LayoutParams(120.dp, 160.dp)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageURI(invoiceUri)
                        setPadding(8, 8, 8, 8)
                    }

                    val card = MaterialCardView(this).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(120.dp, 160.dp).apply {
                            setMargins(0, 0, 12, 0)
                        }
                        radius = 12f
                        elevation = 4f
                        addView(imageView)
                    }

                    invoiceContainer.addView(card)
                }
            }

            PICK_PRODUCT_IMAGES_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    productImageUris.clear()
                    productImageContainer.removeViews(1, productImageContainer.childCount - 1)

                    if (data?.clipData != null) {
                        val count = data.clipData!!.itemCount
                        for (i in 0 until count) {
                            val uri = data.clipData!!.getItemAt(i).uri
                            productImageUris.add(uri)
                            addImagePreviewToContainer(uri, productImageContainer)
                        }
                    } else {
                        data?.data?.let { uri ->
                            productImageUris.add(uri)
                            addImagePreviewToContainer(uri, productImageContainer)
                        }
                    }
                }
            }
        }
    }

    private fun addImagePreviewToContainer(uri: Uri, container: LinearLayout) {
        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(120.dp, 160.dp)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageURI(uri)
            setPadding(8, 8, 8, 8)
        }

        val card = MaterialCardView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(120.dp, 160.dp).apply {
                setMargins(0, 0, 12, 0)
            }
            radius = 12f
            elevation = 4f
            addView(imageView)
        }

        container.addView(card)
    }

    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}


