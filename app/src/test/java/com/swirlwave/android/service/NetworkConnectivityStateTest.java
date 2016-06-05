package com.swirlwave.android.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkConnectivityStateTest {
    @Test
    public void generateFileFriendlyName_whitespaces_are_removed() throws Exception {
        String name = NetworkConnectivityState.generateFileFriendlyLocationName(" a b c ", " x  y z ");
        assertEquals("xyzabc", name);
    }

    @Test
    public void generateFileFriendlyName_long_name_is_truncated() throws Exception {
        String name = NetworkConnectivityState.generateFileFriendlyLocationName(
                "012345678901234567890123456789",
                "012345678901234567890123456789");
        assertEquals("01234567890123456789012345678901234567890123456789", name);
    }

    @Test
    public void generateFileFriendlyName_special_chars_are_removed() throws Exception {
        String name = NetworkConnectivityState.generateFileFriendlyLocationName(
                "a+-_%bc",
                "x#<>.yz");
        assertEquals("xyzabc", name);
    }

    @Test
    public void generateFileFriendlyName_uppercase_chars_become_lowercase() throws Exception {
        String name = NetworkConnectivityState.generateFileFriendlyLocationName(
                "ABC",
                "DEF");
        assertEquals("abcdef", name);
    }

    @Test
    public void generateFileFriendlyName_nulls_are_accepted() throws Exception {
        String name = NetworkConnectivityState.generateFileFriendlyLocationName(
                null,
                null);
        assertEquals("nullnull", name);
    }
}
