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

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.plweegie.android.telladog.MainActivity
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.adapters.PhotoGridAdapter
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.databinding.FragmentDogListBinding
import com.plweegie.android.telladog.viewmodels.PredictionListViewModel
import com.plweegie.android.telladog.viewmodels.PredictionListViewModelFactory
import javax.inject.Inject


class DogListFragment : Fragment(), PhotoGridAdapter.PhotoGridListener, FirebaseDialog.FirebaseDialogListener {

    @Inject
    lateinit var mViewModelFactory: PredictionListViewModelFactory

    private val mViewModel: PredictionListViewModel by viewModels { mViewModelFactory }

    private lateinit var mAdapter: PhotoGridAdapter
    private lateinit var mFragmentSwitchListener: FragmentSwitchListener
    private lateinit var binding: FragmentDogListBinding

    private var currentPrediction: DogPrediction? = null
    private var userId: String? = null

    override fun onAttach(context: Context) {
        (activity?.application as MyApp).mAppComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mFragmentSwitchListener = activity as MainActivity

        val orientation = PreferenceManager.getDefaultSharedPreferences(activity)
                .getInt(MainActivity.ORIENTATION_PREFERENCE, 0)

        mAdapter = PhotoGridAdapter(orientation).apply {
            setHasStableIds(true)
            onItemClickListener = this@DogListFragment
        }

        userId = arguments?.getString(USER_ID_ARG)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDogListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = GridLayoutManager(activity, 1)

        binding.predictionsList.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            adapter = mAdapter
        }

        mViewModel.getPredictionList().observe(viewLifecycleOwner, Observer {

            it?.run {
                binding.onboardingTv.visibility = View.GONE
                mAdapter.setContent(this)

                if (this.isEmpty()) {
                    binding.onboardingTv.visibility = View.VISIBLE
                }
            }
        })

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dog_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_to_camera -> {
                mFragmentSwitchListener.onCameraFragmentSelect()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onDeleteClicked(prediction: DogPrediction?) {
        mViewModel.deletePrediction(prediction, userId!!)
    }

    override fun onSyncClicked(prediction: DogPrediction?) {
        PreferenceManager.getDefaultSharedPreferences(activity).run {
            if (contains(FIREBASE_SYNC_PREFERENCE)) {
                mViewModel.syncToFirebase(prediction, userId!!, getBoolean(FIREBASE_SYNC_PREFERENCE, false))
            } else {
                currentPrediction = prediction
                val firebaseDialog = FirebaseDialog().apply {
                    listener = this@DogListFragment
                }

                firebaseDialog.show(parentFragmentManager, "FirebaseDialog")
            }
         }
    }

    override fun onPositiveClick(dialog: DialogFragment, isPermanent: Boolean) {
        if (isPermanent) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                    .putBoolean(FIREBASE_SYNC_PREFERENCE, true)
                    .apply()
        }

        mViewModel.syncToFirebase(currentPrediction, userId!!, true)
    }

    override fun onNegativeClick(dialog: DialogFragment, isPermanent: Boolean) {
        if (isPermanent) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                    .putBoolean(FIREBASE_SYNC_PREFERENCE, false)
                    .apply()
        }
    }

    companion object {
        const val TAG = "DogListFragment"
        const val FIREBASE_SYNC_PREFERENCE = "firebase_sync_preference"

        private const val USER_ID_ARG = "user_id_arg"

        fun newInstance(userId: String?): DogListFragment {
            val bundle = Bundle().apply { putString(USER_ID_ARG, userId) }
            return DogListFragment().apply { arguments = bundle }
        }
    }
}