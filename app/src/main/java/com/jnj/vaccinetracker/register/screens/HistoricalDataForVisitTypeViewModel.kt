package com.jnj.vaccinetracker.register.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateTime
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Date
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class HistoricalDataForVisitTypeViewModel @Inject constructor(
   override val dispatchers: AppCoroutineDispatchers,
   private val configurationManager: ConfigurationManager,
   private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

   data class Args(val visitTypeName: String?, val visitUuid: String?)

   private val args = MutableStateFlow<Args?>(null)
   val visitTypeName = MutableLiveData<String?>()
   val substancesData = MutableLiveData<List<SubstanceDataModel>>(emptyList())
   val otherSubstancesData = MutableLiveData<List<OtherSubstanceDataModel>?>(emptyList())
   val substancesAndDates = MutableLiveData<MutableMap<String, String>>(mutableMapOf())
   val otherSubstancesAndValues = MutableLiveData<MutableMap<String, String>>(mutableMapOf())
   val loading = MutableLiveData<Boolean>()
   val errorMessage = MutableLiveData<String>()
   val visitDate = MutableLiveData<DateTime>()
   val isLocalEdit = MutableLiveData<Boolean>(false)

   init {
      observeArgs()
   }

   private fun observeArgs() {
      args.filterNotNull()
         .distinctUntilChanged()
         .onEach { loadData(it) }
         .launchIn(scope)
   }

   fun setArguments(args: Args) {
      this.args.value = args
   }

   fun addVaccineDate(conceptName: String, dateValue: String) {
      updateMap(substancesAndDates, conceptName, dateValue)
   }

   fun removeVaccineDate(conceptName: String) {
      val substancesAndDatesValue = substancesAndDates.value?.toMutableMap() ?: mutableMapOf()
      if (substancesAndDatesValue.containsKey(conceptName)) {
         substancesAndDatesValue.remove(conceptName)
         substancesAndDates.postValue(substancesAndDatesValue)
      }
   }

   private suspend fun loadData(args: Args) {
      loading.postValue(true)
      errorMessage.postValue("")

      withContext(dispatchers.io) {
         try {
            visitTypeName.postValue(args.visitTypeName)
            if (isLocalEdit.value != true){
               args.visitTypeName?.let { visitType ->
                  loadSubstancesData(visitType)
                  loadOtherSubstancesData(visitType)
               }
            }
         } catch (ex: Throwable) {
            handleError(ex)
         } finally {
            loading.postValue(false)
         }
      }
   }

   private suspend fun loadSubstancesData(visitTypeName: String) {
      substancesData.postValue(getSubstancesDataForVisitType(visitTypeName)!!)
   }

   private suspend fun loadOtherSubstancesData(visitTypeName: String) {
      otherSubstancesData.postValue(getOtherDataForVisitType(visitTypeName))
   }

   suspend fun getOtherDataForVisitType(visitTypeName: String): List<OtherSubstanceDataModel> {
      return SubstancesDataUtil.getOtherSubstancesDataForVisitType(visitTypeName, configurationManager) ?: emptyList()
   }

   suspend fun getSubstancesDataForVisitType(visitTypeName: String): List<SubstanceDataModel> {
      return SubstancesDataUtil.getSubstancesDataForVisitType(visitTypeName, configurationManager) ?: emptyList()
   }

   private suspend fun handleError(ex: Throwable) {
      yield()
      ex.rethrowIfFatal()
      errorMessage.postValue(resourcesWrapper.getString(R.string.general_label_error))
   }

   fun addObsToOtherSubstancesObsMap(conceptName: String, value: String) {
      updateMap(otherSubstancesAndValues, conceptName, value)
   }

   private fun updateMap(liveDataMap: MutableLiveData<MutableMap<String, String>>, key: String, value: String) {
      val currentMap = liveDataMap.value ?: mutableMapOf()
      currentMap[key] = value
      liveDataMap.postValue(currentMap)
   }
}
