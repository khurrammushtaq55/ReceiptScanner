package com.mmushtaq.smartreceiptscanner.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateSerializationTest {

    @Test
    fun round_trips_a_single_rate() {
        val rates = mapOf("USD" to 278.5)
        val serialized = RateSerialization.serialize(rates)
        assertEquals(rates, RateSerialization.deserialize(serialized))
    }

    @Test
    fun round_trips_multiple_rates() {
        val rates = mapOf("USD" to 278.5, "EUR" to 302.75, "GBP" to 355.0)
        val serialized = RateSerialization.serialize(rates)
        assertEquals(rates, RateSerialization.deserialize(serialized))
    }

    @Test
    fun deserialize_returns_empty_map_for_null_or_blank() {
        assertTrue(RateSerialization.deserialize(null).isEmpty())
        assertTrue(RateSerialization.deserialize("").isEmpty())
        assertTrue(RateSerialization.deserialize("   ").isEmpty())
    }

    @Test
    fun deserialize_skips_malformed_entries() {
        val result = RateSerialization.deserialize("USD=278.5;GARBAGE;EUR=notanumber;GBP=355.0")
        assertEquals(mapOf("USD" to 278.5, "GBP" to 355.0), result)
    }

    @Test
    fun serialize_empty_map_is_empty_string() {
        assertEquals("", RateSerialization.serialize(emptyMap()))
    }
}
