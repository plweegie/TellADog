package com.plweegie.android.telladog.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.databinding.GridItemBinding
import com.plweegie.android.telladog.utils.ThumbnailLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PhotoGridAdapter(private val orientation: Int) : RecyclerView.Adapter<PhotoGridAdapter.PhotoGridHolder>() {

    interface PhotoGridListener {
        fun onDeleteClicked(prediction: DogPrediction?)
        fun onSyncClicked(prediction: DogPrediction?)
    }

    private var mPredictions: MutableList<DogPrediction> = mutableListOf()
    lateinit var onItemClickListener: PhotoGridListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoGridHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = GridItemBinding.inflate(inflater, parent, false)
        return PhotoGridHolder(itemBinding)
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

    inner class PhotoGridHolder(private val itemBinding: GridItemBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(prediction: DogPrediction?) {
            itemBinding.apply {
                breedGridTv.text = prediction?.prediction
                confidenceGridTv.text =
                        "%.1f %%".format(100.0 * (prediction?.accuracy as Float))

                deleteIv.setOnClickListener {
                    onItemClickListener.onDeleteClicked(prediction)
                }
                syncIv.setOnClickListener {
                    if (prediction.syncState == DogPrediction.SyncState.NOT_SYNCED.value) {
                        onItemClickListener.onSyncClicked(prediction)
                    }
                }
                syncIv.setImageResource(
                        if (prediction.syncState == DogPrediction.SyncState.SYNCED.value) R.drawable.ic_cloud_done_24dp
                        else R.drawable.ic_cloud_upload_blue_24dp
                )

                syncingProgress.visibility =
                        if (prediction.syncState == DogPrediction.SyncState.SYNCING.value) View.VISIBLE else View.GONE
            }

            GlobalScope.launch(Dispatchers.Main) {
                val bitmap = withContext(Dispatchers.Default) {
                    ThumbnailLoader.decodeBitmapFromFile(prediction?.imageUri,
                            100, 100, orientation)
                }

                itemBinding.thumbnailImageview.setImageBitmap(bitmap)
            }
        }
    }
}