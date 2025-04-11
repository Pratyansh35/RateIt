package com.parawale.rateit.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        private val tvGenuineBadge: TextView = itemView.findViewById(R.id.tvGenuineBadge)

        fun bind(product: Product) {
            val imageUrl = product.productImages.firstOrNull() ?: product.invoiceImage

            tvProductTitle.text = product.title
            tvProductPrice.text = "₹%.2f".format(product.price)

            // ✅ Correct dynamic rating calculation
            val reviews = product.reviews.values.toList() ?: emptyList()
            val validReviews = reviews.filter { it.rating > 0 }
            val avgRating = if (validReviews.isNotEmpty()) validReviews.sumOf { it.rating.toDouble() } / validReviews.size else 0.0
            val reviewCount = validReviews.size
            tvProductRating.text = "⭐ %.1f (%d)".format(avgRating, reviewCount)

            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(ivProductImage)

            tvGenuineBadge.apply {
                text = if (product.genuine) "GENUINE" else "Not Verified"
                setBackgroundResource(R.drawable.ribbon_background)
                setBackgroundTintList(
                    ContextCompat.getColorStateList(
                        context,
                        if (product.genuine) R.color.green_700 else R.color.error
                    )
                )

                visibility = View.VISIBLE
                alpha = 0f
                scaleX = 0.8f
                scaleY = 0.8f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, RatingActivity::class.java).apply {
                    putExtra("productName", product.title)
                    putExtra("productPrice", product.price)
                    putExtra("productDescription", product.description)
                    putExtra("userPhone", product.createdBy ?: "Unknown")
                    putExtra("userEmail", product.createdBy ?: "Unknown")
                    putExtra("productId", product.id)
                    putStringArrayListExtra("imageUrls", ArrayList(product.productImages))
                }
                context.startActivity(intent)
            }
        }


    }

}



