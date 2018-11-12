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

package com.plweegie.android.telladog.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.plweegie.android.telladog.MainActivity
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.adapters.PhotoGridAdapter
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.viewmodels.PredictionListViewModel
import com.plweegie.android.telladog.viewmodels.PredictionListViewModelFactory
import kotlinx.android.synthetic.main.fragment_dog_list.*
import javax.inject.Inject


class DogListFragment : Fragment(), PhotoGridAdapter.PhotoGridListener {

    @Inject
    lateinit var mViewModelFactory: PredictionListViewModelFactory

    private lateinit var mViewModel: PredictionListViewModel
    private lateinit var mAdapter: PhotoGridAdapter
    private lateinit var mFragmentSwitchListener: FragmentSwitchListener

    override fun onAttach(context: Context?) {
        (activity?.application as MyApp).mAppComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mViewModel = ViewModelProviders.of(activity as FragmentActivity, mViewModelFactory)
                .get(PredictionListViewModel::class.java)

        mFragmentSwitchListener = activity as MainActivity

        mAdapter = PhotoGridAdapter()
        mAdapter.setHasStableIds(true)
        mAdapter.onItemClickListener = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_dog_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = GridLayoutManager(activity, 1)
        predictions_list.layoutManager = layoutManager
        predictions_list.setHasFixedSize(true)
        predictions_list.adapter = mAdapter

        mViewModel.getPredictionList().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                mAdapter.setContent(it)
            }
        })

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.fragment_dog_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.change_to_camera -> {
                mFragmentSwitchListener.onCameraFragmentSelect()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDeleteClicked(itemId: Long) {
        mViewModel.deletePrediction(itemId)
    }

    override fun onSyncClicked(prediction: DogPrediction?) {
        mViewModel.syncToFirebase(prediction)
    }

    companion object {
        const val TAG = "DogListFragment"

        fun newInstance(): DogListFragment = DogListFragment()
    }
}