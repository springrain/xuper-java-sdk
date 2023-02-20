package com.baidu.xuper.crypto;

import com.baidu.xuper.api.CryptoClient;
import com.baidu.xuper.config.Config;
import com.baidu.xuper.crypto.xchain.sign.ECKeyPair;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ECCTest {
    String p = getClass().getResource("./conf/sdk.yaml").getPath();
    Config config= Config.getInstance(p);
    @Test
    public void ECKeyPairTest() throws Exception {
        Crypto cli = CryptoClient.getCryptoClient(config);

        ECKeyPair ec = ECKeyPair.create("a".getBytes());
        assertNotNull(ec.getJSONPrivateKey());

        ECKeyPair ecKeyPair = ECKeyPair.create();
        assertNotNull(ecKeyPair.getJSONPrivateKey());
        assertNotNull(ecKeyPair.getJSONPublicKey());
        assertNotNull(ecKeyPair.getPrivateKey());
        assertNotNull(ecKeyPair.getPublicKey());

        try {
            byte[] sign = cli.signECDSA("a".getBytes(), ecKeyPair.getPrivateKey());
            assertNotNull(sign);
        } catch (Exception e) {
            throw e;
        }
    }
}
