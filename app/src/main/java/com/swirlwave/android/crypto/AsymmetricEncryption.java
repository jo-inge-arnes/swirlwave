package com.swirlwave.android.crypto;

import android.util.Base64;
import android.util.Pair;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class AsymmetricEncryption {
    private KeyPair mKeys;
    private Cipher mEncryptCipher, mDecryptCipher;

    public AsymmetricEncryption(String publicKeyString, String privateKeyString) throws Exception {
        mKeys = new KeyPair(
                decodePublicKey(publicKeyString),
                decodePrivateKey(privateKeyString));

        mEncryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        mEncryptCipher.init(Cipher.ENCRYPT_MODE, mKeys.getPublic());

        mDecryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        mDecryptCipher.init(Cipher.DECRYPT_MODE, mKeys.getPrivate());
    }

    private static PublicKey decodePublicKey(String publicKeyString) throws Exception {
        byte[] bytes = stringDecode(publicKeyString);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private static PrivateKey decodePrivateKey(String privateKeyString) throws Exception {
        byte[] bytes = stringDecode(privateKeyString);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public static byte[] decryptBytes(byte[] encryptedBytes, String keyString, boolean isPrivateKey) throws Exception {
        Key key;
        if (isPrivateKey) key = decodePrivateKey(keyString);
        else key = decodePublicKey(keyString);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedBytes);
    }

    public static byte[] encryptBytes(byte[] cleartextBytes, String keyString, boolean isPublicKey) throws Exception {
        Key key;
        if (isPublicKey) key = decodePublicKey(keyString);
        else key = decodePrivateKey(keyString);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(cleartextBytes);
    }

    private static String stringEncode(Key key) {
        return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
    }

    private static byte[] stringDecode(String keyAsString) {
        return Base64.decode(keyAsString, Base64.NO_WRAP);
    }

    public static Pair<String, String> generateKeys() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.genKeyPair();
        return new Pair<>(
                stringEncode(keyPair.getPublic()),
                stringEncode(keyPair.getPrivate())
        );
    }

    public byte[] encrypt(byte[] bytes) throws Exception {
        return mEncryptCipher.doFinal(bytes);
    }

    public byte[] decrypt(byte[] bytes) throws Exception {
        return mDecryptCipher.doFinal(bytes);
    }
}
