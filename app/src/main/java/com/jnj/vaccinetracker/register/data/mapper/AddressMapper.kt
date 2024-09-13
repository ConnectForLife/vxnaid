package com.jnj.vaccinetracker.register.data.mapper

import com.jnj.vaccinetracker.common.data.database.mappers.toDomain
import com.jnj.vaccinetracker.common.data.models.api.response.AddressDto
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.AddressValueType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressMapper @Inject constructor(moshi: Moshi) {

    private val addressMapAdapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
    private val addressAdapter = moshi.adapter(AddressDto::class.java)

    private fun emptyAddress() = Address(
        address1 = null,
        address2 = null,
        cityVillage = null,
        stateProvince = null,
        country = null,
        countyDistrict = null,
        postalCode = null
    )

    /**
     * [addressMap] is address field to value
     */
    fun toDomain(addressMap: Map<AddressValueType, String>): Address {
        val json = addressMapAdapter.toJson(addressMap.map { (addressValueType, value) -> addressValueType.fieldName to value }.toMap())
        val dto = addressAdapter.fromJson(json)
        return dto?.toDomain() ?: emptyAddress()
    }

    fun toAddressValueType(address: Address): Map<AddressValueType, String> {
        val addressMap = mutableMapOf<AddressValueType, String>()

        address.address1?.let { addressMap[AddressValueType.ADDRESS_1] = it }
        address.address2?.let { addressMap[AddressValueType.ADDRESS_2] = it }
        address.cityVillage?.let { addressMap[AddressValueType.CITY_VILLAGE] = it }
        address.stateProvince?.let { addressMap[AddressValueType.STATE_PROVINCE] = it }
        address.country?.let { addressMap[AddressValueType.COUNTRY] = it }
        address.countyDistrict?.let { addressMap[AddressValueType.COUNTY_DISTRICT] = it }
        address.postalCode?.let { addressMap[AddressValueType.POSTAL_CODE] = it }

        return addressMap
    }
}