package com.plweegie.android.telladog.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.utils.ThumbnailLoader
import kotlinx.android.synthetic.main.grid_item.view.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext


class PhotoGridAdapter : RecyclerView.Adapter<PhotoGridAdapter.PhotoGridHolder>() {

    private var mPredictions: MutableList<DogPrediction> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoGridHolder {
        val inflater = LayoutInflater.from(parent.context)
        return PhotoGridHolder(inflater, parent, R.layout.grid_item)
    }

    override fun onBindViewHolder(holder: PhotoGridHolder, position: Int) {
        holder.bind(mPredictions[position])
    }

    override fun getItemCount(): Int  = mPredictions.size

    override fun getItemId(position: Int): Long = mPredictions[position].timestamp

    fun setContent(predictions: List<DogPrediction>) {
        mPredictions = predictions.toMutableList()
        notifyDataSetChanged()
    }

    inner class PhotoGridHolder(private val inflater: LayoutInflater, private val parent: ViewGroup, private val layoutResId: Int) :
            RecyclerView.ViewHolder(inflater.inflate(layoutResId, parent, false)) {

        fun bind(prediction: DogPrediction?) {
            itemView.breed_grid_tv.text = prediction?.prediction
            itemView.confidence_grid_tv.text =
                    "%.1f %%".format(100.0 * (prediction?.accuracy as Float))

            launch(UI) {
                val bitmap = withContext(DefaultDispatcher) {
                    ThumbnailLoader.decodeBitmapFromFile(prediction.imageUri, 50, 50)
                }

                itemView.thumbnail_imageview.setImageBitmap(bitmap)
            }
        }
    }
}