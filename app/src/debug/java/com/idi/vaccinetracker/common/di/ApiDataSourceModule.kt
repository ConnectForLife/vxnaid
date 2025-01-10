package com.idi.vaccinetracker.common.di

import com.idi.vaccinetracker.common.data.network.VaccineTrackerApiDataSource
import com.idi.vaccinetracker.common.data.network.VaccineTrackerApiDataSourceDefault
import com.idi.vaccinetracker.common.helpers.NetworkConnectivity
import com.idi.vaccinetracker.common.helpers.NetworkConnectivityDefault
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSourceDefault
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApiDataSourceModule {

    @Provides
    @Singleton
    fun provideVaccineTrackerSyncApiDataSource(
        impl: VaccineTrackerSyncApiDataSourceDefault,
    ): VaccineTrackerSyncApiDataSource = impl

    @Provides
    @Singleton
    fun provideVaccineTrackerApiDataSource(
        impl: VaccineTrackerApiDataSourceDefault,
    ): VaccineTrackerApiDataSource = impl

    @Provides
    @Singleton
    fun provideNetworkConnectivity(impl: NetworkConnectivityDefault): NetworkConnectivity = impl
}