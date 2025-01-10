package com.idi.vaccinetracker.participantflow.model

import android.os.Parcelable
import com.idi.vaccinetracker.common.domain.entities.ImageBytes
import kotlinx.parcelize.Parcelize

@Parcelize
class ParticipantImageUiModel(val byteArray: ByteArray) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    companion object {
        fun ImageBytes.toUiModel() = ParticipantImageUiModel(bytes)
        fun ParticipantImageUiModel.toDomain() = ImageBytes(byteArray)
    }

}