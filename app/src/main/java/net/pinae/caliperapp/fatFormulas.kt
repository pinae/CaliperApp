package net.pinae.caliperapp

const val FEMALE = 0
const val MALE = 1

fun singeMeasurementFormula(measurement: Float, age: Float, sex: Int = FEMALE): Float {
    if (sex == FEMALE) {
        return -81.11624781500858f +
                16.24696401279887f * age +
                1.260578767579677f * measurement +
                -1.2122698920932902f * age*age +
                -0.02218128304949981f * measurement*measurement +
                0.047391185293791833f * age*age*age +
                0.0007896756020451277f * measurement*measurement*measurement +
                -0.0010161128685791494f * age*age*age*age +
                -3.1758015883456974e-05f * measurement*measurement*measurement*measurement +
                1.1332827778985168e-05f * age*age*age*age*age +
                5.612344592680556e-07f * measurement*measurement*measurement*measurement*measurement +
                -5.138295199899927e-08f * age*age*age*age*age*age +
                -3.397678636263858e-09f * measurement*measurement*measurement*measurement*measurement*measurement
    } else if (sex == MALE) {
        return -6.38085408325614f +
                0.08725614951926948f * age +
                1.1860707716479835f * measurement +
                0.027892271320127063f * age*age +
                0.005122875031618992f * measurement*measurement +
                -0.0016398143066375533f * age*age*age +
                -0.0010253027092373772f * measurement*measurement*measurement +
                4.397993730799001e-05f * age*age*age*age +
                1.67809443769388e-05f * measurement*measurement*measurement*measurement +
                -5.626665656874099e-07f * age*age*age*age*age +
                -2.0716803545210805e-08f * measurement*measurement*measurement*measurement*measurement +
                2.7858988551576793e-09f * age*age*age*age*age*age +
                -7.923464855087344e-10f * measurement*measurement*measurement*measurement*measurement*measurement
    } else {
        throw Exception("Unsupported sex: $sex")
    }
}

fun jacksonPollockBodyDesityThreeMeasurements(measurementsSum: Float, age: Float, sex: Int = FEMALE): Float {
    if (sex == FEMALE) {
        return 1.0994921f +
                -0.0009929f * measurementsSum +
                0.0000023f * measurementsSum * measurementsSum +
                -0.0001392f * age
    } else if (sex == MALE) {
        return 1.10938f +
                -0.0008267f * measurementsSum +
                0.0000016f * measurementsSum * measurementsSum +
                -0.0002574f * age
    } else {
        throw Exception("Unsupported sex: $sex")
    }
}

fun jacksonPollockBodyDesitySevenMeasurements(measurementsSum: Float, age: Float, sex: Int = FEMALE): Float {
    if (sex == FEMALE) {
        return 1.097f +
                -469.71e-6f * measurementsSum +
                0.56e-6f * measurementsSum * measurementsSum +
                -288.26e-6f * age
    } else if (sex == MALE) {
        return 1.112f +
                -434.99e-6f * measurementsSum +
                0.55e-6f * measurementsSum * measurementsSum +
                -128.28e-6f * age
    } else {
        throw Exception("Unsupported sex: $sex")
    }
}

fun siriEquationFatFromBodyDensity(bodyDensity: Float): Float {
    return 4.95f / bodyDensity - 4.5f
}