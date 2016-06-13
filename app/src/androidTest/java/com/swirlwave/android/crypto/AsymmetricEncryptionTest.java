package com.swirlwave.android.crypto;

import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AsymmetricEncryptionTest {

    @Test
    public void encryptionAndDecryptionWithGeneratedKeys_should_decryptedArrayEqualOriginal() throws Exception {
        Pair<String, String> keyStrings = AsymmetricEncryption.generateKeys();
        AsymmetricEncryption ae = new AsymmetricEncryption(keyStrings.first, keyStrings.second);
        byte[] original = new byte[] { 0x0, 0xF, 0x1, 0x9 };
        byte[] encrypted = ae.encrypt(original);
        byte[] decrypted = ae.decrypt(encrypted);

        assertEquals(original[0], decrypted[0]);
        assertEquals(original[1], decrypted[1]);
        assertEquals(original[2], decrypted[2]);
        assertEquals(original[3], decrypted[3]);
    }
}
