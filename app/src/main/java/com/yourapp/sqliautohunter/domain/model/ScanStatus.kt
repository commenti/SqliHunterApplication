package com.yourapp.sqliautohunter.domain.model

enum class ScanStatus {
    PENDING,
    TESTING,
    VULNERABLE,
    NOT_VULNERABLE,
    ERROR
}
