package com.idi.vaccinetracker.e2etest.helper

import com.idi.vaccinetracker.common.domain.entities.Gender
import com.idi.vaccinetracker.robots.login
import com.idi.vaccinetracker.robots.participantflow.irisscan.participantFlowIrisScanLeft
import com.idi.vaccinetracker.robots.participantflow.irisscan.participantFlowIrisScanRight
import com.idi.vaccinetracker.robots.participantflow.participantFlowMatching
import com.idi.vaccinetracker.robots.participantflow.participantFlowParticipantId
import com.idi.vaccinetracker.robots.participantflow.participantFlowPhone
import com.idi.vaccinetracker.robots.participantflow.participationFlowIntro
import com.idi.vaccinetracker.robots.register.registerParticipantDetailsRobot
import com.idi.vaccinetracker.robots.register.registerParticipantPictureRobot
import com.idi.vaccinetracker.robots.register.registerSuccess
import com.idi.vaccinetracker.robots.siteSelection

fun goThroughRegistrationFlow(skipLogin: Boolean) {
    val participantId = "123123"
    val phone = "033534566"
    val phoneAreaCode = "+32"
    val gender = Gender.MALE
    val birthYear = 1994
    val street = "Koekoekstraa<t"
    val houseNumber = "41"
    if (!skipLogin) {
        login {
            username("admin")
            password("Admin123")
            submit()
        }
    }
    siteSelection {
        selectSite("Bangalore clinic")
        submit()
    }
    participationFlowIntro {
        startWorkFlow()
    }
    participantFlowParticipantId {
        participantId(participantId)
        submit()
    }
    participantFlowPhone {
        areaCode(phoneAreaCode)
        phone(phone)
        submit()
    }
    participantFlowIrisScanRight {
        if (loadImage())
            submit()
        else
            skip()
    }
    participantFlowIrisScanLeft {
        if (loadImage())
            submit()
        else
            skip()
    }
    participantFlowMatching {
        newParticipant()
    }
    registerParticipantPictureRobot {
        takePicture()
        submit()
    }
    registerParticipantDetailsRobot {
        gender(gender)
        birthYear(birthYear)
        homeLocation(street = street, houseNumber = houseNumber)
        regimen(0)
        language(0)
        submit()
    }

    registerSuccess {
        finishWorkflow()
        verifyIsHome()
    }
}