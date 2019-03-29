package net.pinae.caliperapp

import android.os.Bundle

class Measurement {
    var dBelly: Float? = null
    var dHips: Float? = null
    var dTriceps: Float? = null
    var dChest: Float? = null
    var dTigh: Float? = null
    var dArmpit: Float? = null
    var dScapula: Float? = null

    fun setFromBundle(b: Bundle) {
        if (b.containsKey(BELLY)) dBelly = b.getFloat(BELLY)
        if (b.containsKey(HIPS)) dHips = b.getFloat(HIPS)
        if (b.containsKey(TRICEPS)) dTriceps = b.getFloat(TRICEPS)
        if (b.containsKey(CHEST)) dChest = b.getFloat(CHEST)
        if (b.containsKey(TIGH)) dTigh = b.getFloat(TIGH)
        if (b.containsKey(ARMPIT)) dArmpit = b.getFloat(ARMPIT)
        if (b.containsKey(SCAPULA)) dScapula = b.getFloat(SCAPULA)
    }

    fun allValuesMeasured() :Boolean {
        return dBelly != null && dHips != null && dTriceps != null && dChest != null && dTigh != null &&
                dArmpit != null && dScapula != null
    }

    fun missingOnlyTheLastMeasurement() :Boolean {
        return dBelly != null && dHips != null && dTriceps != null && dChest != null && dTigh != null &&
                dArmpit != null && dScapula == null
    }

    fun getSum() :Float {
        if (this.allValuesMeasured()) {
            return dBelly!! + dHips!! + dTriceps!! + dChest!! + dTigh!! + dArmpit!! + dScapula!!
        }
        if (dBelly != null && prefs.sex == FEMALE && dHips != null && dTriceps != null) {
            return dBelly!! + dHips!! + dTriceps!!
        }
        if (dBelly != null && prefs.sex == MALE && dChest != null && dTigh != null) {
            return dBelly!! + dChest!! + dTigh!!
        }
        if (dBelly != null) {
            return dBelly!!
        }
        throw IllegalStateException("Not enough measurements.")
    }

    fun getFormula() :(Float, Float, Int)->Float {
        if (dBelly != null && dHips != null && dTriceps != null && dChest != null && dTigh != null &&
            dArmpit != null && dScapula != null) {
            return { measurement, age, sex -> siriEquationFatFromBodyDensity(
                jacksonPollockBodyDensitySevenMeasurements(measurement, age, sex)) }
        }
        if ((dBelly != null && prefs.sex == FEMALE && dHips != null && dTriceps != null) ||
            (dBelly != null && prefs.sex == MALE && dChest != null && dTigh != null)) {
            return { measurement, age, sex -> siriEquationFatFromBodyDensity(
                jacksonPollockBodyDensityThreeMeasurements(measurement, age, sex)) }
        }
        if (dBelly != null) {
            return { measurement, age, sex -> singeMeasurementFormula(measurement, age, sex) }
        }
        throw IllegalStateException("Not enough measurements.")
    }

    fun getNextMeasurePosition() :String {
        if (this.allValuesMeasured()) {
            return ""
        }
        if (this.missingOnlyTheLastMeasurement()) {
            return SCAPULA
        }
        if (dBelly != null && dHips != null && dTriceps != null && dChest != null && dTigh != null &&
            dArmpit == null) {
            return ARMPIT
        }
        if (dBelly != null && prefs.sex == FEMALE && dHips != null && dTriceps != null && dChest != null &&
            dTigh == null) {
            return TIGH
        }
        if (dBelly != null && prefs.sex == MALE && dChest != null && dTigh != null && dHips != null &&
            dTriceps == null) {
            return TRICEPS
        }
        if (dBelly != null && prefs.sex == FEMALE && dHips != null && dTriceps != null && dChest == null) {
            return CHEST
        }
        if (dBelly != null && prefs.sex == MALE && dChest != null && dTigh != null && dHips == null) {
            return HIPS
        }
        if (dBelly != null && prefs.sex == FEMALE && dHips != null && dTriceps == null) {
            return TRICEPS
        }
        if (dBelly != null && prefs.sex == MALE && dChest != null && dTigh == null) {
            return TIGH
        }
        if (dBelly != null && prefs.sex == FEMALE && dHips == null) {
            return HIPS
        }
        if (dBelly != null && prefs.sex == MALE && dChest == null) {
            return CHEST
        }
        if (dBelly == null) {
            return BELLY
        }
        throw IllegalStateException("Illegal combination of measurements.")
    }

    fun writeToBundle() :Bundle {
        val b = Bundle()
        if (dBelly != null) b.putFloat(BELLY, dBelly!!)
        if (dHips != null) b.putFloat(HIPS, dHips!!)
        if (dTriceps != null) b.putFloat(TRICEPS, dTriceps!!)
        if (dChest != null) b.putFloat(CHEST, dChest!!)
        if (dTigh != null) b.putFloat(TIGH, dTigh!!)
        if (dArmpit != null) b.putFloat(ARMPIT, dArmpit!!)
        if (dScapula != null) b.putFloat(SCAPULA, dScapula!!)
        return b
    }

    override fun toString(): String {
        return "(" + dBelly.toString() + ", " + dHips.toString() + ", " + dTriceps.toString() + ", " +
                dChest.toString() + ", " + dTigh.toString() + ", " + dArmpit.toString() + ", " +
                dScapula.toString() + ")"
    }
}