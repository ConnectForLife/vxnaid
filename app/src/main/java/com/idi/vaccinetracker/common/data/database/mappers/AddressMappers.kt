package com.idi.vaccinetracker.common.data.database.mappers

import com.idi.vaccinetracker.common.data.database.entities.ParticipantAddressEntity
import com.idi.vaccinetracker.common.data.database.entities.draft.DraftParticipantAddressEntity
import com.idi.vaccinetracker.common.data.database.models.common.RoomAddressModel
import com.idi.vaccinetracker.common.data.models.api.response.AddressDto
import com.idi.vaccinetracker.common.domain.entities.Address

fun DraftParticipantAddressEntity.toDomain() = Address(address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)
fun ParticipantAddressEntity.toDomain() = Address(address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)
fun RoomAddressModel.toDomain() = Address(address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)
fun Address.toPersistence(participantUuid: String) = ParticipantAddressEntity(participantUuid, address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)
fun Address.toDraftPersistence(participantUuid: String) =
    DraftParticipantAddressEntity(participantUuid, address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)

fun Address.toDto() = AddressDto(address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)
fun AddressDto.toDomain() = Address(address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode)