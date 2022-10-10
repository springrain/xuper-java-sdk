package com.baidu.xuper.crypto.xchain;

import com.baidu.xuper.crypto.AES;
import com.baidu.xuper.crypto.Base58;
import com.baidu.xuper.crypto.Common;
import com.baidu.xuper.crypto.Crypto;
import com.baidu.xuper.crypto.account.ECDSAAccount;
import com.baidu.xuper.crypto.account.ECDSAInfo;
import com.baidu.xuper.crypto.wordlists.WordList;
import com.baidu.xuper.crypto.xchain.account.FileKey;
import com.baidu.xuper.crypto.xchain.bip39.MnemonicCode;
import com.baidu.xuper.crypto.xchain.hash.Hash;
import com.baidu.xuper.crypto.xchain.hdWallet.Key;
import com.baidu.xuper.crypto.xchain.hdWallet.Rand;
import com.baidu.xuper.crypto.xchain.sign.ECKeyPair;
import com.baidu.xuper.crypto.xchain.sign.Ecc;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import javax.crypto.Cipher;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;


public class XChainCryptoClient implements Crypto {
    public XChainCryptoClient(){
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    /**
     * 产生随机熵
     *
     * @param bitSize
     * @return byte[]
     */
    @Override
    public byte[] generateEntropy(int bitSize) {
        return Rand.generateEntropy(bitSize);
    }

    /**
     * 将随机熵转为助记词
     *
     * @param entropy
     * @param language
     * @return Strings
     */
    @Override
    public String generateMnemonic(byte[] entropy, Integer language) {
        WordList wordList = Common.getWordList(language);
        MnemonicCode mc = new MnemonicCode(wordList);
        return mc.createMnemonic(entropy);
    }

    /**
     * 创建含有助记词的新的账户，返回的字段：（助记词、私钥的json、公钥的json、钱包地址） as ECDSAAccount，以及可能的错误信息
     *
     * @param language
     * @param strength
     * @return
     */
    @Override
    public ECDSAAccount createNewAccountWithMnemonic(Integer language, Integer strength) {
        return FileKey.createNewAccountWithMnemonic(language, strength, 1);
    }

    /**
     * 从助记词恢复钱包账户
     * TODO: 后续可以从助记词中识别出语言类型
     *
     * @param mnemonic
     * @param language
     * @return
     */
    @Override
    public ECDSAAccount retrieveAccountByMnemonic(String mnemonic, Integer language) {
        return FileKey.generateAccountByMnemonic(mnemonic, language);
    }

    /**
     * 从助记词恢复钱包账户，并用支付密码加密私钥后存在本地，
     * 返回的字段：（随机熵（供其他钱包软件推导出私钥）、助记词、私钥的json、公钥的json、钱包地址） as ECDSAAccount，以及可能的错误信息
     *
     * @param path
     * @param language
     * @param mnemonic
     * @param password
     * @return
     */
    @Override
    public ECDSAInfo retrieveAccountByMnemonicAndSavePrivKey(String path, Integer language, String mnemonic, String password) {
        return Key.createAndSaveSecretKeyWithMnemonic(path, language, mnemonic, password);
    }

    /**
     * 使用支付密码从导出的私钥文件读取私钥
     *
     * @param path
     * @param password
     * @return
     */
    @Override
    public byte[] getEcdsaPrivateKeyFromFileByPassword(String path, String password) {
        String fileName = path + "/private.key";
        byte[] content = Common.readFileWithBASE64Decode(fileName);
        byte[] newPasswd = Hash.doubleSha256(password.getBytes());
        return AES.decrypt(content, newPasswd);
    }

    /**
     * 使用ECC私钥来签名
     *
     * @param msg
     * @param privateKey
     * @return
     */
    @Override
    public byte[] signECDSA(byte[] msg, BigInteger privateKey) throws IOException {
        return Ecc.sign(msg, privateKey);
    }

    /**
     * 使用单个公钥来生成钱包地址
     *
     * @param publicKey 公钥
     * @return address
     */
    @Override
    public String getAddressFromPublicKey(ECPoint publicKey) {
        byte[] hash = Hash.ripeMD160(Hash.hashUsingSha256(publicKey.getEncoded(false)));
        String address = Base58.encodeChecked(Common.nist, hash);
        return address;
    }

    /**
     * using random create ECKeyPair
     *
     * @return
     */
    @Override
    public ECKeyPair createECKeyPair() {
        return ECKeyPair.create();
    }

    /**
     * @param privateKey
     * @return
     */
    @Override
    public ECKeyPair getECKeyPairFromPrivateKey(BigInteger privateKey) {
        return  ECKeyPair.create(privateKey);
    }

    @Override
    public byte[] encryptByEcdsaKey(byte[] msg, ECPoint ecPoint) throws Exception {

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPublicKeySpec cardKeySpec = new ECPublicKeySpec(ecPoint, ecSpec);

        KeyFactory kf=KeyFactory.getInstance("EC");
        PublicKey publicKey = kf.generatePublic(cardKeySpec);
        Cipher cipher = Cipher.getInstance("ECIES","BC");//写不写 BC都可以，都是会选择BC实现来做
        cipher.init(Cipher.ENCRYPT_MODE,publicKey);
        return cipher.doFinal(msg);
    }

    @Override
    public byte[] decryptByEcdsaKey(byte[] cypherText, BigInteger privateKey) throws Exception {
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPrivateKeySpec cardKeySpec=new ECPrivateKeySpec(privateKey,ecSpec);
        KeyFactory kf=KeyFactory.getInstance("EC");
        PrivateKey ecPrivateKey =kf.generatePrivate(cardKeySpec);
        Cipher cipher = Cipher.getInstance("ECIES","BC");
        cipher.init(Cipher.DECRYPT_MODE, ecPrivateKey);
        return cipher.doFinal(cypherText);
    }

    /*
     public static void main(String[] args) throws Exception {
         //Config.setConfigPath("src/main/resources/sdk.yaml");
         // TODO  此处修改路径  即加载不同的公私钥   对应不同银行
         File file = new File("E://keys");
         //获取Account
         Account account = Account.getAccountFromPlainFile(file.getPath());
         System.out.println(account.getKeyPair().getJSONPrivateKey());
         Crypto cryptoClient = new XChainCryptoClient();
         String dajdkaljda = new String("dajdkaljda");
         byte[] bytes = cryptoClient.encryptByEcdsaKey(dajdkaljda.getBytes(StandardCharsets.UTF_8), account.getKeyPair().getPublicKey());
         byte[] bytes1 = cryptoClient.decryptByEcdsaKey(bytes, account.getKeyPair().getPrivateKey());
         System.out.println(new String(bytes1));
    }
     */

}
