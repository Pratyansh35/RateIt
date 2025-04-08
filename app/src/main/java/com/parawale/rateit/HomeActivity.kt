package com.parawale.rateit

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.parawale.rateit.Models.Product
import com.parawale.rateit.adapter.ProductAdapter
import com.parawale.rateit.network.FakeStoreApi
import com.parawale.rateit.network.FirebaseStoreApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_ADD_PRODUCT = 101
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var addReviewButton: FloatingActionButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var searchEditText: EditText

    private var allProducts: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        recyclerView = findViewById(R.id.rvProducts)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(mutableListOf())
        recyclerView.adapter = productAdapter

        addReviewButton = findViewById(R.id.fabAddReview)


        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val headerView = navigationView.getHeaderView(0)


        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)

        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let {
            tvUserName.text = "Welcome, ${it.displayName ?: "User"}"
            tvUserEmail.text = it.email ?: ""
        }

        addReviewButton.setOnClickListener {
            val intent = Intent(this, AddRatingActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_PRODUCT)
        }
        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        searchEditText = findViewById(R.id.etSearch)
        searchEditText.addTextChangedListener {
            val query = it.toString().trim()
            filterProducts(query)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        fetchProducts()
    }

    private fun filterProducts(query: String) {
        val filtered = if (query.isEmpty()) allProducts
        else allProducts.filter { it.title.contains(query, ignoreCase = true) }
        productAdapter.updateProducts(filtered)
    }

    private fun fetchProducts() {
        CoroutineScope(Dispatchers.IO).launch {
            val products = FirebaseStoreApi.fetchProducts()
            withContext(Dispatchers.Main) {
                if (products.isNotEmpty()) {
                    allProducts = products
                    productAdapter.updateProducts(products)
                } else {
                    Toast.makeText(this@HomeActivity, "No products found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            val product = data?.getParcelableExtra<Product>("product")
            val invoiceUri = data?.getStringExtra("invoiceUri")

            product?.let {
                if (!invoiceUri.isNullOrEmpty()) {
                    it.invoiceImage = invoiceUri
                }
                allProducts = listOf(it) + allProducts
                productAdapter.addProduct(it)
                recyclerView.scrollToPosition(0)
            }
        }
    }

}


