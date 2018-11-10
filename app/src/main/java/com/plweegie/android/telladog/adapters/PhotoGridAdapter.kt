package com.plweegie.android.telladog.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.utils.ThumbnailLoader
import kotlinx.android.synthetic.main.grid_item.view.*
import kotlinx.coroutines.*


class PhotoGridAdapter : RecyclerView.Adapter<PhotoGridAdapter.PhotoGridHolder>() {

    interface PhotoGridListener {
        fun onDeleteClicked(itemId: Long)
        fun onSyncClicked(prediction: DogPrediction?)
    }

    private var mPredictions: MutableList<DogPrediction> = mutableListOf()
    lateinit var onItemClickListener: PhotoGridListener

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
            itemView.apply {
                breed_grid_tv.text = prediction?.prediction
                confidence_grid_tv.text =
                        "%.1f %%".format(100.0 * (prediction?.accuracy as Float))

                delete_iv.setOnClickListener {
                    onItemClickListener.onDeleteClicked(prediction.timestamp)
                }
                sync_iv.setOnClickListener {
                    if (!prediction.isSynced) {
                        onItemClickListener.onSyncClicked(prediction)
                    }
                }
                sync_iv.setBackgroundResource(
                        if (prediction.isSynced) R.drawable.ic_cloud_done_24dp
                        else R.drawable.ic_cloud_upload_blue_24dp
                )
            }

            GlobalScope.launch(Dispatchers.Main) {
                val bitmap = withContext(Dispatchers.Default) {
                    ThumbnailLoader.decodeBitmapFromFile(prediction?.imageUri, 50, 50)
                }

                itemView.thumbnail_imageview.setImageBitmap(bitmap)
            }
        }
    }
}