package com.jnj.vaccinetracker.common.domain.entities


data class ParticipantIdentificationCriteria(
    val participantId: String?,
    val phone: String?,
    val motherName: String?,
    val biometricsTemplate: BiometricsTemplateBytes?,
) {
    init {
        require(!participantId.isNullOrEmpty() || !phone.isNullOrEmpty() || !motherName.isNullOrEmpty() || biometricsTemplate != null)
    }

    val isTemplateOnly = phone.isNullOrEmpty() && participantId.isNullOrEmpty() && motherName.isNullOrEmpty() && biometricsTemplate != null
}