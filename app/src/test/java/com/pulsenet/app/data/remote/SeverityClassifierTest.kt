package com.pulsenet.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SeverityClassifierTest {

    @Test
    fun trappedIsCritical() = assertEquals("CRITICAL", SeverityClassifier.classify("I am trapped under debris"))

    @Test
    fun bleedingIsCritical() = assertEquals("CRITICAL", SeverityClassifier.classify("Heavy bleeding, need help"))

    @Test
    fun injuredIsHigh() = assertEquals("HIGH", SeverityClassifier.classify("My leg is injured"))

    @Test
    fun ambulanceIsHigh() = assertEquals("HIGH", SeverityClassifier.classify("Need an ambulance"))

    @Test
    fun foodRequestIsMedium() = assertEquals("MEDIUM", SeverityClassifier.classify("We need food and water"))

    @Test
    fun genericMessageIsLow() = assertEquals("LOW", SeverityClassifier.classify("All good here, just checking in"))

    @Test
    fun classificationIsCaseInsensitive() = assertEquals("CRITICAL", SeverityClassifier.classify("TRAPPED under rubble"))

    @Test
    fun criticalTakesPriorityOverMedium() = assertEquals("CRITICAL", SeverityClassifier.classify("trapped without food"))
}
