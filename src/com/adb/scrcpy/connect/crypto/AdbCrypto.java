package com.adb.scrcpy.connect.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

public class AdbCrypto {
    private KeyPair keyPair;

    public AdbCrypto() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            this.keyPair = kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AdbCrypto generateOrLoad(File keyFile) {
        AdbCrypto crypto = new AdbCrypto();
        try {
            if (keyFile != null && keyFile.exists()) {
                FileInputStream fis = new FileInputStream(keyFile);
                byte[] data = new byte[(int) keyFile.length()];
                fis.read(data);
                fis.close();
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
                PrivateKey priv = kf.generatePrivate(spec);
                crypto.keyPair = new KeyPair(null, priv);
            } else if (keyFile != null) {
                if (keyFile.getParentFile() != null) keyFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(keyFile);
                fos.write(crypto.keyPair.getPrivate().getEncoded());
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return crypto;
    }

    public byte[] signAdbToken(byte[] token) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
        return cipher.doFinal(token);
    }

    public byte[] getAdbPublicKeyPayload() {
        try {
            byte[] pubBytes = keyPair.getPublic().getEncoded();
            String b64 = Base64.getEncoder().encodeToString(pubBytes);
            String fullKey = b64 + " android@scrcpy-connect\0";
            return fullKey.getBytes("UTF-8");
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
