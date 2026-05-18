package org.entur.otp.setup.model

/**
 * NeTEx entity types that OTP does not process. Used to strip dead weight from filtered output.
 *
 * FareFrame is absent here — it is skipped at the SAX level via withSkipElements, so its
 * entities never reach the entity model.
 *
 * Within each frame, only the types listed below are ignored; all others are assumed used.
 * Kept types per frame:
 *   SiteFrame            — StopPlace, Quay
 *   ServiceFrame         — Line, Route, JourneyPattern, StopPointInJourneyPattern,
 *                          DestinationDisplay, PassengerStopAssignment, Notice, NoticeAssignment
 *   TimetableFrame       — ServiceJourney, TimetabledPassingTime, ServiceJourneyInterchange
 *   ResourceFrame        — Operator, Authority
 *   ServiceCalendarFrame — DayType, DayTypeAssignment, OperatingPeriod, AvailabilityCondition
 */
object OtpIgnoredEntities {

    val SITE_FRAME = setOf(
        "Parking", "Access", "NavigationPath", "PathJunction", "PathLink",
        "PointOfInterest", "SiteFacilitySet"
    )

    val SERVICE_FRAME = setOf(
        "RouteLink", "RoutePoint", "ScheduledStopPoint", "ServiceLink",
        "StopArea", "TariffZone", "TimeDemandType", "ServicePattern",
        "Connection", "Direction", "LineNetwork"
    )

    val TIMETABLE_FRAME = setOf(
        "VehicleType", "TrainNumber", "GroupOfServices", "CoupledJourney",
        "JourneyMeeting", "InterchangeRule", "FrequencyGroup"
    )

    val RESOURCE_FRAME = setOf(
        "Vehicle", "VehicleModel", "Equipment", "ControlCentre",
        "DataSource", "ResponsibilitySet"
    )

    val SERVICE_CALENDAR_FRAME = setOf("Timeband", "GroupOfTimebands")

    val ALL = SITE_FRAME + SERVICE_FRAME + TIMETABLE_FRAME + RESOURCE_FRAME + SERVICE_CALENDAR_FRAME
}
