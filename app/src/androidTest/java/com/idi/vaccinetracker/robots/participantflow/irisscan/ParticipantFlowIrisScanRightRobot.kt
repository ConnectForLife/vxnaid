package com.idi.vaccinetracker.robots.participantflow.irisscan

import com.idi.vaccinetracker.robots.participantflow.irisscan.base.ParticipantFlowIrisScanRobotBase

fun participantFlowIrisScanRight(func: ParticipantFlowIrisScanRightRobot.() -> Unit) = ParticipantFlowIrisScanRightRobot().apply(func)

class ParticipantFlowIrisScanRightRobot : ParticipantFlowIrisScanRobotBase()