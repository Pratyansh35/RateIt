package com.parawale.rateit

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
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

            showPhoneNumberDialog()
        }
    }
    private fun showPhoneNumberDialog() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_PHONE
        editText.hint = "Enter phone number"

        AlertDialog.Builder(this)
            .setTitle("Phone Verification")
            .setMessage("Enter your mobile number to verify before submission")
            .setView(editText)
            .setPositiveButton("Send OTP") { _, _ ->
                val number = editText.text.toString()
                if (number.isNotBlank()) {
                    sendOtp(number)
                } else {
                    Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun sendOtp(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@AddRatingActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@AddRatingActivity.verificationId = verificationId
                    showOtpDialog()
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpDialog() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.hint = "Enter OTP"

        AlertDialog.Builder(this)
            .setTitle("Enter OTP")
            .setView(editText)
            .setPositiveButton("Verify") { _, _ ->
                val otp = editText.text.toString()
                verificationId?.let {
                    val credential = PhoneAuthProvider.getCredential(it, otp)
                    signInWithPhoneAuthCredential(credential)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        loadingDialog.show()
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    submitProduct()
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "OTP verification failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun submitProduct() {
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
                context = this@AddRatingActivity
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


