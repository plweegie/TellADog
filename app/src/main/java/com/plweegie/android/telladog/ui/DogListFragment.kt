package com.plweegie.android.telladog.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository
import com.plweegie.android.telladog.viewmodels.PredictionListViewModel
import com.plweegie.android.telladog.viewmodels.PredictionListViewModelFactory
import kotlinx.android.synthetic.main.fragment_dog_list.*
import javax.inject.Inject


class DogListFragment : Fragment() {

    @Inject
    lateinit var mRepository: PredictionRepository

    @Inject
    lateinit var mViewModelFactory: PredictionListViewModelFactory

    private lateinit var mViewModel: PredictionListViewModel

    override fun onAttach(context: Context?) {
        (activity?.application as MyApp).mAppComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(activity as FragmentActivity, mViewModelFactory)
                .get(PredictionListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dog_list, container, false)

        mViewModel.getPredictionList().observe(this, Observer {
            Log.d("dogs", it?.size.toString())
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        take_photo_fab.setOnClickListener { mRepository.add(DogPrediction()) }
    }

    companion object {
        fun newInstance(): DogListFragment = DogListFragment()
    }
}