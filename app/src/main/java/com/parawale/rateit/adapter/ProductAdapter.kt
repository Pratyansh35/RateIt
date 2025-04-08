package com.parawale.rateit.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.parawale.rateit.Models.Product
import com.parawale.rateit.R
import com.bumptech.glide.Glide
import com.parawale.rateit.RatingActivity

class ProductAdapter(private var productList: MutableList<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    fun addProduct(product: Product) {
        productList.add(0, product)
        notifyItemInserted(0)
    }

    fun updateProducts(newProducts: List<Product>) {
        productList.clear()
        productList.addAll(newProducts)
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductTitle: TextView = itemView.findViewById(R.id.tvProductTitle)
        private val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvProductRating: TextView = itemView.findViewById(R.id.tvProductRating)
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)

        fun bind(product: Product) {
            // Safely get product image (productImages > invoiceImage > fallback)
            val imageUrl = product.productImages.firstOrNull() ?: product.invoiceImage

            // Set values
            tvProductTitle.text = product.title
            tvProductPrice.text = "₹%.2f".format(product.price)
            tvProductRating.text = "⭐ %.1f (%d)".format(product.rating.rate, product.rating.count)

            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(ivProductImage)

            // Handle click
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, RatingActivity::class.java).apply {
                    putExtra("productName", product.title)
                    putExtra("productPrice", product.price)
                    putExtra("productDescription", product.description)
                    putExtra("userPhone", product.createdBy ?: "Unknown")
                    putExtra("userEmail", product.createdBy ?: "Unknown")

                    // Pass product images as ArrayList<String>
                    putStringArrayListExtra("imageUrls", ArrayList(product.productImages))
                }
                context.startActivity(intent)
            }
        }
    }

}



