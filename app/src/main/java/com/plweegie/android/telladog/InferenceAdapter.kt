package com.plweegie.android.telladog

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.inference_list_item.view.*


class InferenceAdapter(val context: Context) : RecyclerView.Adapter<InferenceAdapter.InferenceHolder>() {

    private var mLabels: MutableList<Pair<String, Float>>  = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): InferenceHolder {
        val inflater = LayoutInflater.from(context)
        return InferenceHolder(inflater, parent, R.layout.inference_list_item)
    }

    override fun onBindViewHolder(holder: InferenceHolder?, position: Int) {
        holder?.bind(mLabels[position])
    }

    override fun getItemCount(): Int = mLabels.size

    fun setPredictions(predictions: List<Pair<String, Float>>) {
        mLabels = predictions.toMutableList()
        notifyDataSetChanged()
    }

    class InferenceHolder(val inflater: LayoutInflater, val parent: ViewGroup?, val layoutResId: Int)
        : RecyclerView.ViewHolder(inflater.inflate(layoutResId, parent, false)) {

        fun bind(prediction: Pair<String, Float>) {
            itemView.breed_tv.text = prediction.first
            itemView.confidence_tv.text = "%.1f %%".format(100.0 * prediction.second)
        }

    }
}