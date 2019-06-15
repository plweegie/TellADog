package com.plweegie.android.telladog.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.utils.ThumbnailLoader
import kotlinx.android.synthetic.main.grid_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PhotoGridAdapter : RecyclerView.Adapter<PhotoGridAdapter.PhotoGridHolder>() {

    interface PhotoGridListener {
        fun onDeleteClicked(prediction: DogPrediction?)
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

    inner class PhotoGridHolder(inflater: LayoutInflater, parent: ViewGroup, layoutResId: Int) :
            RecyclerView.ViewHolder(inflater.inflate(layoutResId, parent, false)) {

        fun bind(prediction: DogPrediction?) {
            itemView.apply {
                breed_grid_tv.text = prediction?.prediction
                confidence_grid_tv.text =
                        "%.1f %%".format(100.0 * (prediction?.accuracy as Float))

                delete_iv.setOnClickListener {
                    onItemClickListener.onDeleteClicked(prediction)
                }
                sync_iv.setOnClickListener {
                    if (prediction.syncState == DogPrediction.SyncState.NOT_SYNCED.value) {
                        onItemClickListener.onSyncClicked(prediction)
                    }
                }
                sync_iv.setImageResource(
                        if (prediction.syncState == DogPrediction.SyncState.SYNCED.value) R.drawable.ic_cloud_done_24dp
                        else R.drawable.ic_cloud_upload_blue_24dp
                )

                syncing_progress.visibility =
                        if (prediction.syncState == DogPrediction.SyncState.SYNCING.value) View.VISIBLE else View.GONE
            }

            GlobalScope.launch(Dispatchers.Main) {
                val bitmap = withContext(Dispatchers.Default) {
                    ThumbnailLoader.decodeBitmapFromFile(prediction?.imageUri, 100, 100)
                }

                itemView.thumbnail_imageview.setImageBitmap(bitmap)
            }
        }
    }
}