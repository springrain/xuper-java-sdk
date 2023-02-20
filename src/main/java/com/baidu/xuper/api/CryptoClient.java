package com.baidu.xuper.api;

import com.baidu.xuper.config.Config;
import com.baidu.xuper.crypto.Crypto;
import com.baidu.xuper.crypto.gm.GmCryptoClient;
import com.baidu.xuper.crypto.xchain.XChainCryptoClient;

public class CryptoClient {
    /**
     * 读取配置文件，获取加密方式
     *
     * @return CryptoClient
     */
    public static Crypto getCryptoClient(Config config) {
        if (Config.CRYPTO_GM.equals(config.getCrypto())) {
            return new GmCryptoClient();
        }
        return new XChainCryptoClient();
    }

}
