package com.example.maccabidailynews.di

import com.example.maccabidailynews.data.repository.NewsRepositoryImpl
import com.example.maccabidailynews.domain.repository.NewsRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // פונקציה שמספקת את החיבור למסד הנתונים של פיירבייס פעם אחת לכל האפליקציה (Singleton)
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // פונקציה שמחברת בין ה-Interface ל-Implementation
    @Provides
    @Singleton
    fun provideNewsRepository(firestore: FirebaseFirestore): NewsRepository {
        return NewsRepositoryImpl(firestore)
    }
}