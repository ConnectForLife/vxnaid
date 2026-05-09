package com.jnj.vaccinetracker.common.data.managers

import com.jnj.vaccinetracker.common.data.helpers.SystemLanguageProvider
import com.jnj.vaccinetracker.common.domain.entities.AddressHierarchy
import com.jnj.vaccinetracker.common.domain.entities.AddressValue
import com.jnj.vaccinetracker.common.domain.entities.IdentificationStep
import com.jnj.vaccinetracker.common.domain.entities.TranslationMap
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetAddressHierarchyUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetLocalizationMapUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetNinIdentifiersListUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetOtherSubstancesConfigUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSitesUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSubstancesConfigUseCase
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetSubstancesGroupConfigUseCase
import com.jnj.vaccinetracker.common.exceptions.SiteNotFoundException
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.model.SiteUiModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author maartenvangiel
 * @version 1
 */
@Singleton
class ConfigurationManager @Inject constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val getSitesUseCase: GetSitesUseCase,
    private val getLocalizationMapUseCase: GetLocalizationMapUseCase,
    private val getAddressHierarchyUseCase: GetAddressHierarchyUseCase,
    private val systemLanguageProvider: SystemLanguageProvider,
    private val getSubstancesConfigUseCase: GetSubstancesConfigUseCase,
    private val getSubstancesGroupConfigUseCase: GetSubstancesGroupConfigUseCase,
    private val getNinIdentifiersListUseCase: GetNinIdentifiersListUseCase,
    private val getOtherSubstancesConfigUseCase: GetOtherSubstancesConfigUseCase
) {

    suspend fun getConfiguration() = getConfigurationUseCase.getMasterData()

    suspend fun getSites(): List<com.jnj.vaccinetracker.common.domain.entities.Site> {
        logInfo("📍 ConfigurationManager.getSites() - Fetching sites")
        val sites = getSitesUseCase.getMasterData().results
        logInfo("📍 ConfigurationManager.getSites() - Retrieved ${sites.size} sites")
        sites.forEach { site ->
            logInfo("📍 ConfigurationManager Site: name=${site.name}, uuid=${site.uuid}, locationId=${site.locationId}, parentLocationId=${site.parentLocationId}")
        }
        return sites
    }

    suspend fun getSiteUiModelByUuid(uui: String): SiteUiModel {
        return getSiteByUuid(uui).let { site ->
            val loc = getLocalization()
            SiteUiModel.create(site, loc)
        }
    }

    suspend fun getLocalization(): TranslationMap {
        val loc = getLocalizationMapUseCase.getMasterData()
        val lang = systemLanguageProvider.getSystemLanguage()
        return loc.languages.getTranslationsByLanguage(lang, "en") ?: TranslationMap(emptyMap())
    }

    suspend fun refreshSites(): List<com.jnj.vaccinetracker.common.domain.entities.Site> {
        logInfo("📍 ConfigurationManager.refreshSites() - Force refreshing sites from API")
        return getSitesUseCase.refreshFromRemote().results
    }

    suspend fun getSiteByUuid(uuid: String) = getSites()
        .let { sites ->
            sites.find { it.uuid == uuid } ?: throw SiteNotFoundException(uuid)
        }

    suspend fun getIdentificationSteps(): List<IdentificationStep> = getConfiguration().authSteps

    suspend fun getVaccineManufacturers(vaccineType: String): List<String> = getConfiguration().vaccines.first { it.name == vaccineType }.manufacturers

    suspend fun getAddressHierarchy(): AddressHierarchy = getAddressHierarchyUseCase.getMasterData()

    suspend fun getCountryAddressHierarchy(country: String): List<List<AddressValue>> = getAddressHierarchy().countryAddressMap[country] ?: emptyList()

    suspend fun getSubstancesConfig() = getSubstancesConfigUseCase.getMasterData()

    suspend fun getSubstancesGroupConfig() = getSubstancesGroupConfigUseCase.getMasterData()

    suspend fun getNinIdentifiers() = getNinIdentifiersListUseCase.getMasterData()

    suspend fun getOtherSubstancesConfig() = getOtherSubstancesConfigUseCase.getMasterData()
}