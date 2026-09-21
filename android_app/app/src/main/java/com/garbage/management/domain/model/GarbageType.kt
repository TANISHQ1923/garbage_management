package com.garbage.management.domain.model

enum class GarbageType(val displayName: String, val description: String) {
    OVERFLOWING_BIN("Overflowing Bin", "Public dustbin spilling onto street or sidewalk"),
    FESTIVAL_WASTE("Festival Waste", "Waste accumulated during festivals or celebrations"),
    EVENT_WASTE("Event Waste", "Debris from community events, gatherings, or markets"),
    ROADSIDE_GARBAGE("Roadside Garbage", "Dumping on roadside corners or vacant plots"),
    FLOOD_WASTE("Flood Waste", "Waterlogged debris or clogged municipal drains"),
    OTHER_WASTE("Other Waste", "Unclassified, hazardous, or bulk municipal waste")
}
