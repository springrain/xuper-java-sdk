package com.baidu.xuper.api;

import com.baidu.xuper.crypto.Base58;
import com.baidu.xuper.crypto.Crypto;
import com.baidu.xuper.crypto.account.ECDSAAccount;
import com.baidu.xuper.crypto.account.PrivatePubKey;
import com.baidu.xuper.crypto.xchain.hash.Hash;
import com.baidu.xuper.crypto.xchain.sign.ECKeyPair;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

public class Account {
    private final ECKeyPair ecKeyPair;
    private final String address;
    private String contractAccount;
    private String mnemonic;

    private Account(ECKeyPair ecKeyPair, String address) {
        this.ecKeyPair = ecKeyPair;
        this.address = address;
    }

    private Account(ECKeyPair ecKeyPair, String address, String mnemonic) {
        this.ecKeyPair = ecKeyPair;
        this.address = address;
        this.mnemonic = mnemonic;
    }

    /**
     * create an account.
     *
     * @param strength 助记词强度, 1弱（12个助记词），2中（18个助记词），3强（24个助记词）。
     * @param language 助记词语言，1中文，2英文。
     * @return Account 账户信息。
     */
    public static Account create(int strength, int language) {
        Crypto cli = CryptoClient.getCryptoClient();
        ECDSAAccount ecdsaAccount = cli.createNewAccountWithMnemonic(language, strength);
        return new Account(ecdsaAccount.ecKeyPair, ecdsaAccount.address, ecdsaAccount.mnemonic);
    }

    /**
     * retrieve account from mnemonic.
     *
     * @param mnemonic 助记词，例如："玉 脸 驱 协 介 跨 尔 籍 杆 伏 愈 即"。
     * @param language 助记词语言，1中文，2英文。
     * @return Account 账户信息。
     */
    public static Account retrieve(String mnemonic, int language) {
        Crypto cli = CryptoClient.getCryptoClient();
        ECDSAAccount ecdsaAccount = cli.retrieveAccountByMnemonic(mnemonic, language);
        return new Account(ecdsaAccount.ecKeyPair, ecdsaAccount.address, ecdsaAccount.mnemonic);
    }

    /**
     * @param path     保存的路径。
     * @param passwd   密码。
     * @param strength 助记词强度, 1弱（12个助记词），2中（18个助记词），3强（24个助记词）。
     * @param language 助记词语言，1中文，2英文。
     * @return Account 账户信息。
     */
    public static Account createAndSaveToFile(String path, String passwd, Integer strength, Integer language) {
        Crypto cli = CryptoClient.getCryptoClient();
        ECDSAAccount ecdsaAccount = cli.createNewAccountWithMnemonic(language, strength);

        Common.mkdir(path);
        cli.retrieveAccountByMnemonicAndSavePrivKey(path, language, ecdsaAccount.mnemonic, passwd);

        return new Account(ecdsaAccount.ecKeyPair, ecdsaAccount.address, ecdsaAccount.mnemonic);
    }

    /**
     * import account from plain files which are JSON encoded
     *
     * @param path 文件路径。
     *             The structure of path is like below:
     *             - keys
     *             |-- address
     *             |-- private.key
     *             |-- public.key
     *             |-- mnemonic
     * @return Account 账户信息。
     */
    public static Account getAccountFromPlainFile(String path) {
        try {
            byte[] address = Files.readAllBytes(Paths.get(path + "/address"));
            byte[] pubKey = Files.readAllBytes(Paths.get(path + "/public.key"));
            byte[] priKey = Files.readAllBytes(Paths.get(path + "/private.key"));

            Gson gson = new Gson();
            PrivatePubKey privatePubKey = gson.fromJson(new String(priKey), PrivatePubKey.class);
            if (privatePubKey.D == null) {
                throw new RuntimeException("invalid private.key file");
            }

            Crypto cli = CryptoClient.getCryptoClient();
            Account account = create(cli.getECKeyPairFromPrivateKey(privatePubKey.D));
            if (!account.getAKAddress().equals(new String(address))) {
                throw new RuntimeException("address and private key not match.");
            }

            if (!account.getKeyPair().getJSONPublicKey().equals(new String(pubKey))) {
                throw new RuntimeException("public key and private key not match.");
            }
            return account;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字节数组转16进制大写字符串
     *
     * @param bytes
     * @return
     */
    public static String hexEncodeUpperToString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (int index = 0, len = bytes.length; index <= len - 1; index += 1) {
            String invalue1 = Integer.toHexString((bytes[index] >> 4) & 0xF);
            String intValue2 = Integer.toHexString(bytes[index] & 0xF);
            result.append(invalue1);
            result.append(intValue2);
        }
        return result.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * AK 转 EVM Address
     * @param akAddress
     * @return
     * @throws Exception
     */

    public static String xchainAKToEVMAddress(String akAddress) throws Exception {
        if (akAddress == null) {
            throw new RuntimeException("akAddress is null");
        }
        byte[] rawAddr = Base58.decode(akAddress);

        if (rawAddr.length < 21) {
            throw new RuntimeException("bad address");
        }

        byte[] ripemd160Hash = Arrays.copyOfRange(rawAddr, 1, 21);
        return hexEncodeUpperToString(ripemd160Hash);
    }


    /**
     * EVM Address 转 AK
     * @param evmAddress
     * @return
     * @throws Exception
     */
    public static String evmAddressToXchainAK(String evmAddress) throws Exception {
        if (evmAddress == null) {
            throw new RuntimeException("evmAddress is null");
        }
        byte[] outputRipemd160 = hexStringToBytes(evmAddress);
        if (outputRipemd160.length != 20) {
            throw new RuntimeException("bad address");
        }

        byte[] bufVersion = new byte[]{(byte) 1};
        byte[] strSlice = new byte[outputRipemd160.length + bufVersion.length];
        System.arraycopy(bufVersion, 0, strSlice, 0, bufVersion.length);
        System.arraycopy(outputRipemd160, 0, strSlice, 1, outputRipemd160.length);
        byte[] checkCode = Hash.doubleSha256(strSlice);

        byte[] slice = new byte[strSlice.length + 4];
        System.arraycopy(strSlice, 0, slice, 0, strSlice.length);
        System.arraycopy(checkCode, 0, slice, strSlice.length, 4);
        String encode = Base58.encode(slice);

        return encode;
    }


    private static byte[] hexStringToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }


    /**
     * get an account from file and password.
     *
     * @param path     文件路径。
     * @param password 密码。
     * @return Account 账户信息。
     */
    public static Account getAccountFromFile(String path, String password) {
        Crypto cli = CryptoClient.getCryptoClient();
        byte[] p = cli.getEcdsaPrivateKeyFromFileByPassword(path, password);

        Gson gson = new Gson();
        PrivatePubKey privatePubKeyJson = gson.fromJson(new String(p), PrivatePubKey.class);
        if (privatePubKeyJson.D == null) {
            throw new RuntimeException("invalid private.key file");
        }
        return create(cli.getECKeyPairFromPrivateKey(privatePubKeyJson.D));
    }

    /**
     * @param keyPath the path to ./data/keys which contains private.key file
     * @return
     * @throws Exception
     */
    public static Account create(String keyPath) {
        try {
            String privateKeyPath = Paths.get(keyPath, "private.key").toString();
            File privateKeyFile = new File(privateKeyPath);
            return create(new FileInputStream(privateKeyFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 账户地址，如果存在合约账户地址，返回合约账户地址。
     */
    public String getAddress() {
        if (this.contractAccount != null) {
            return this.contractAccount;
        }
        return this.address;
    }

    /**
     * @return 账户地址，不会返回合约账户地址。
     */
    public String getAKAddress() {
        return this.address;
    }

    /**
     * @return 助记词。
     */
    public String getMnemonic() {
        return this.mnemonic;
    }

    /**
     * @return 合约账户信息。
     */
    public String getContractAccount() {
        return this.contractAccount;
    }

    /**
     * @param inputStream ./data/keys/private.key fileInputStream
     * @return
     * @throws Exception
     */
    public static Account create(InputStream inputStream) {
        Gson gson = new Gson();
        PrivatePubKey json;
        Crypto cli = CryptoClient.getCryptoClient();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            json = gson.fromJson(reader, PrivatePubKey.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (json.D == null) {
            throw new RuntimeException("invalid private.key file");
        }
        return create(cli.getECKeyPairFromPrivateKey(json.D));
    }

    /**
     * @param keyPair the private and public key of account
     * @return
     */
    public static Account create(ECKeyPair keyPair) {
        Crypto cli = CryptoClient.getCryptoClient();
        String address = cli.getAddressFromPublicKey(keyPair.publicKey);
        return new Account(keyPair, address);
    }

    /**
     * Create a account using random private key
     *
     * @return
     */
    public static Account create() {
        Crypto cli = CryptoClient.getCryptoClient();
        return create(cli.createECKeyPair());
    }

    /**
     * @return 公私钥相关。
     */
    public ECKeyPair getKeyPair() {
        return ecKeyPair;
    }

    /**
     * @param name 合约账户。
     */
    public void setContractAccount(String name) {
        // todo name正则表达验证
        this.contractAccount = name;
    }

    /**
     * remove contract account from this account.
     */
    public void RemoveContractAccount() {
        this.contractAccount = "";
    }

    /**
     * @return String
     */
    public String getAuthRequireId() {
        if (this.contractAccount != null) {
            return this.contractAccount + "/" + this.address;
        }
        return this.address;
    }

    /**
     * @return boolean
     */
    public boolean HasContractAccount() {
        return this.contractAccount != "";
    }
}
