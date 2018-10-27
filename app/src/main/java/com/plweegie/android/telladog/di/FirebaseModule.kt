package com.plweegie.android.telladog.di

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseStorageReference(): StorageReference = FirebaseStorage.getInstance().reference

    @Provides
    @Singleton
    fun provideFirebaseDatabaseReference(): DatabaseReference = FirebaseDatabase.getInstance().reference
}