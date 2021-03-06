/* Copyright 2018 Jan K Szymanski. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.plweegie.android.telladog.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.plweegie.android.telladog.databinding.InferenceListItemBinding


class InferenceAdapter(private val context: Context) : RecyclerView.Adapter<InferenceAdapter.InferenceHolder>() {

    private var mLabels: MutableList<Pair<String, Float>> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InferenceHolder {
        val inflater = LayoutInflater.from(context)
        val itemBinding = InferenceListItemBinding.inflate(inflater, parent, false)
        return InferenceHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: InferenceHolder, position: Int) {
        holder.bind(mLabels[position])
    }

    override fun getItemCount(): Int = mLabels.size

    fun setPredictions(predictions: List<Pair<String, Float>>) {
        mLabels = predictions.toMutableList()
        notifyDataSetChanged()
    }

    class InferenceHolder(private val itemBinding: InferenceListItemBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(prediction: Pair<String, Float>) {
            itemBinding.breedTv.text = prediction.first
            itemBinding.confidenceTv.text = "%.1f %%".format(100.0 * prediction.second)
        }
    }
}