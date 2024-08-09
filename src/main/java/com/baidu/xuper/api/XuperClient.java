package com.baidu.xuper.api;

import com.baidu.xuper.config.Config;
import com.baidu.xuper.crypto.Crypto;
import com.baidu.xuper.crypto.xchain.sign.Ecc;
import com.baidu.xuper.pb.XchainGrpc;
import com.baidu.xuper.pb.XchainOuterClass;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.RandomDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class XuperClient {
    private final ManagedChannel channel;
    private final XchainGrpc.XchainBlockingStub blockingClient;
    private final XendorserClient xendorserClient;
    private XEventServiceListener xeventServiceListener;

    private Crypto cryptoClient ;
    private String chainName = "xuper";
    private final String evmContract = "evm";
    private final String xkernelModule = "xkernel";
    private final String evmJSONEncoded = "jsonEncoded";
    private final String evmJSONEncodedTrue = "true";
    private final String argsInput = "input";
    private final String xkernelDeployMethod = "Deploy";
    private final String xkernelUpgradeMethod = "Upgrade";
    private final String xkernelNewAccountMethod = "NewAccount";
    private final String argAccountName = "account_name";
    private final String argContractName = "contract_name";
    private final String argContractCode = "contract_code";
    private final String argContractDesc = "contract_desc";
    private final String argInitArgs = "init_args";
    private final String argContractAbi = "contract_abi";

    /**
     * @param target the address of xchain node, like 127.0.0.1:37101
     */
    public XuperClient(String target) {
        this(target,true);
    }

    /**
     * @param target the address of xchain node, like 127.0.0.1:37101
     */
    public XuperClient(String target,boolean xendorser) {
        this(target, Integer.MAX_VALUE, xendorser);
    }

    /**
     * @param target                the address of xchain node, like 127.0.0.1:37101
     * @param maxInboundMessageSize Sets the maximum message size allowed to be received on the channel, like 52428800 (50M)
     */

    public XuperClient(String target,Integer maxInboundMessageSize) {
        this(target,maxInboundMessageSize,true);
    }

    /**
     * @param target the address of xchain node, like 127.0.0.1:37101
     * @param maxInboundMessageSize Sets the maximum message size allowed to be received on the channel, like 52428800 (50M)
     */
    public XuperClient(String target,Integer maxInboundMessageSize,boolean xendorser) {
        this.channel = ManagedChannelBuilder.forTarget(target)
                .maxInboundMessageSize(maxInboundMessageSize)
                .maxInboundMessageSize(maxInboundMessageSize)
                .directExecutor()
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .enableRetry()
                .defaultLoadBalancingPolicy("round_robin")
                .keepAliveTime(10, TimeUnit.SECONDS)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .build();
        blockingClient = XchainGrpc.newBlockingStub(channel);
        if (xendorser&&Config.hasConfigFile()) {
            xendorserClient = new XendorserClient(Config.getInstance().getEndorseServiceHost());
        } else {
            xendorserClient = null;
        }
        xeventServiceListener =new XEventServiceListener(target,maxInboundMessageSize);

        cryptoClient=CryptoClient.getCryptoClient();
    }




    public void close() {
        channel.shutdownNow();
        if (xendorserClient != null) {
            xendorserClient.close();
        }
        if(xeventServiceListener != null){
            xeventServiceListener.close();
        }
    }

    /**
     * @param name name of chain
     * @return
     */
    public XuperClient setChainName(String name) {
        this.chainName = name;
        return this;
    }

    XchainGrpc.XchainBlockingStub getBlockingClient() {
        return blockingClient;
    }

    public XendorserClient getXendorserClient() {
        return xendorserClient;
    }

    public XEventServiceListener getXEventServiceListener() {
        return xeventServiceListener;
    }

    /**
     * @param from   from address
     * @param to     to address
     * @param amount transfer amount
     * @return
     */
    public Transaction transfer(Account from, String to, BigInteger amount, String fee) {
        return transfer(from, to, amount, fee, null);
    }

    /**
     * @param from   from address
     * @param to     to address
     * @param amount transfer amount
     * @param desc   transfer desc
     * @return
     */
    public Transaction transfer(Account from, String to, BigInteger amount, String fee, String desc) {
        Proposal p = new Proposal()
                .setChainName(chainName)
                .setFee(fee);
        if ((desc != null) && (!desc.equals(""))) {
            p.setDesc(desc);
        }

        if (Config.getInstance().getComplianceCheck().isNeedComplianceCheck()) {
            p.addAuthRequire(Config.getInstance().getComplianceCheck().getComplianceCheckEndorseServiceAddr());
        }
        p.setInitiator(from);
        return p.transfer(to, amount).build(this).sign().send(this);
    }


    /**
     * @param from     the initiator of calling method
     * @param module   module of contract, usually wasm
     * @param contract contract name
     * @param method   contract method
     * @param args     contract method arguments
     * @return
     */
    public Transaction invokeContract(Account from, String module, String contract, String method, Map<String, byte[]> args) {
        return invokeContract(from,module,contract,method,args,null);
    }

    public Transaction invokeContract(Account from, String module, String contract, String method, Map<String, byte[]> args, String desc) {
        Proposal p = new Proposal().setChainName(chainName);
        if (Config.getInstance().getComplianceCheck().isNeedComplianceCheck()) {
            p.addAuthRequire(Config.getInstance().getComplianceCheck().getComplianceCheckEndorseServiceAddr());
        }
        p.setInitiator(from);
        if (desc!=null){
            p.setDesc(desc);
        }
        return p.invokeContract(module, contract, method, args).build(this).sign().send(this);
    }



    /**
     * @param from     the initiator of calling method
     * @param module   module of contract, usually wasm
     * @param contract contract name
     * @param method   contract method
     * @param args     contract method arguments
     * @return
     */
    public Transaction queryContract(Account from, String module, String contract, String method, Map<String, byte[]> args) {
        return new Proposal()
                .setChainName(chainName)
                .setInitiator(from)
                .addAuthRequire(Config.getInstance().getComplianceCheck().getComplianceCheckEndorseServiceAddr())
                .invokeContract(module, contract, method, args)
                .preExec(this);
    }

    /**
     * deploy wasm contract
     *
     * @param from     the contract account to deploy contract
     * @param code     the binary of contract code
     * @param contract the name of contract
     * @param runtime  contract runtime c or go
     * @param initArgs initial argument of initialize method
     * @return
     */
    public Transaction deployWasmContract(Account from, byte[] code, String contract, String runtime, Map<String, byte[]> initArgs) {
        return deployContract(from, code, contract, runtime, initArgs, "wasm");
    }

    /**
     * deploy native contract
     *
     * @param from     the contract account to deploy contract
     * @param code     the binary of contract code
     * @param contract the name of contract
     * @param runtime  contract runtime c or go
     * @param initArgs initial argument of initialize method
     * @return
     */
    public Transaction deployNativeContract(Account from, byte[] code, String contract, String runtime, Map<String, byte[]> initArgs) {
        return deployContract(from, code, contract, runtime, initArgs, "native");
    }

    /**
     * @param from     the account of create contract.
     * @param bin      evm contract bin byte array.
     * @param abi      evm contract abi byte array.
     * @param contract contract name.
     * @param initArgs constructor args.
     * @return transaction.
     */
    public Transaction deployEVMContract(Account from, byte[] bin, byte[] abi, String contract, Map<String, String> initArgs) {
        if (from.getContractAccount().isEmpty()) {
            throw new RuntimeException("deploy contract must use contract account");
        }
        XchainOuterClass.WasmCodeDesc desc = XchainOuterClass.WasmCodeDesc.newBuilder()
                .setContractType(evmContract)
                .build();

        Map<String, byte[]> evmArgs = this.convertToXuper3EVMArgs(initArgs);

        Gson gson = new Gson();
        byte[] initArgsJson = gson.toJson(evmArgs).getBytes();

        Map<String, byte[]> args = new HashMap<>();
        args.put(argAccountName, from.getContractAccount().getBytes());
        args.put(argContractName, contract.getBytes());
        args.put(argContractCode, bin);
        args.put(argContractDesc, desc.toByteArray());
        args.put(argInitArgs, initArgsJson);
        args.put(argContractAbi, abi);
        return invokeContract(from, xkernelModule, "", xkernelDeployMethod, args);
    }

    /**
     * @param from         the contract account to deploy contract
     * @param code         the binary of contract code
     * @param contract     the name of contract
     * @param runtime      contract runtime c or go
     * @param initArgs     initial argument of initialize method
     * @param contractType contract type  wasm or native
     * @return
     */
    private Transaction deployContract(Account from, byte[] code, String contract, String runtime, Map<String, byte[]> initArgs, String contractType) {
        if (from.getContractAccount().isEmpty()) {
            throw new RuntimeException("deploy contract must use contract account");
        }

        if (contractType.isEmpty()) {
            contractType = "wasm";
        }

        // XchainOuterClass.NativeCodeDesc desc = XchainOuterClass.NativeCodeDesc.newBuilder().build();
        XchainOuterClass.WasmCodeDesc desc = XchainOuterClass.WasmCodeDesc.newBuilder()
                .setRuntime(runtime)
                .setContractType(contractType)
                .build();
        Gson gson = new Gson();
        byte[] initArgsJson = gson.toJson(initArgs).getBytes();

        Map<String, byte[]> args = new HashMap<>();
        args.put(argAccountName, from.getContractAccount().getBytes());
        args.put(argContractName, contract.getBytes());
        args.put(argContractCode, code);
        args.put(argContractDesc, desc.toByteArray());
        args.put(argInitArgs, initArgsJson);
        return invokeContract(from, xkernelModule, "", xkernelDeployMethod, args);
    }

    /**
     * @param from        the use account to create contract account
     * @param accountName the name of contract account
     * @return
     */
    public Transaction createContractAccount(Account from, String accountName) {
        String desc = "{\"aksWeight\": {\"" + from.getAddress() + "\": 1.0}, \"pm\": {\"acceptValue\": 1.0, \"rule\": 1}}";
        Map<String, byte[]> args = new HashMap<>();
        args.put("account_name", accountName.getBytes());
        args.put("acl", desc.getBytes());
        return invokeContract(from, xkernelModule, "", xkernelNewAccountMethod, args);
    }

    /**
     * Get balance of account
     *
     * @param account account name, can be contract account
     * @return
     */
    public BigInteger getBalance(String account) {
        XchainOuterClass.AddressStatus request = XchainOuterClass.AddressStatus.newBuilder()
                .setHeader(Common.newHeader())
                .setAddress(account)
                .addBcs(XchainOuterClass.TokenDetail.newBuilder().setBcname(chainName).build())
                .build();
        XchainOuterClass.AddressStatus response = blockingClient.getBalance(request);

        for (int i = 0; i < response.getBcsCount(); i++) {
            if (response.getBcs(i).getBcname().equals(chainName)) {
                return new BigInteger(response.getBcs(i).getBalance());
            }
        }
        Common.checkResponseHeader(response.getHeader(), "query balance");
        return BigInteger.valueOf(0);
    }

    /**
     * Get balance unfrozen balance and frozen balance of account
     *
     * @param account account name, can be contract account
     * @return balance
     */
    public BalDetails[] getBalanceDetails(String account) {
        XchainOuterClass.AddressBalanceStatus request = XchainOuterClass.AddressBalanceStatus.newBuilder()
                .setHeader(Common.newHeader())
                .setAddress(account)
                .addTfds(XchainOuterClass.TokenFrozenDetails.newBuilder().setBcname(chainName).build())
                .build();
        XchainOuterClass.AddressBalanceStatus response = blockingClient.getBalanceDetail(request);

        XchainOuterClass.TokenFrozenDetails tfds = response.getTfds(0);

        BalDetails[] balDetails = new BalDetails[tfds.getTfdCount()];
        for (int i = 0; i < tfds.getTfdCount(); i++) {
            XchainOuterClass.TokenFrozenDetail tfd = tfds.getTfd(i);
            balDetails[i] = new BalDetails(tfd.getBalance(), tfd.getIsFrozen());
        }

        return balDetails;
    }

    public static class BalDetails {
        private String balance;
        private Boolean isFrozen;

        BalDetails(String bal, Boolean f) {
            balance = bal;
            isFrozen = f;
        }

        public String getBalance() {
            return balance;
        }

        public Boolean getFrozen() {
            return isFrozen;
        }

        public void setBalance(String balance) {
            this.balance = balance;
        }

        public void setFrozen(Boolean frozen) {
            isFrozen = frozen;
        }
    }

    /**
     * queryTx query transaction
     *
     * @param txid the id of transaction
     * @return
     */
    public XchainOuterClass.Transaction queryTx(String txid) {
        XchainOuterClass.TxStatus request = XchainOuterClass.TxStatus.newBuilder()
                .setHeader(Common.newHeader())
                .setBcname(chainName)
                .setTxid(ByteString.copyFrom(Hex.decode(txid)))
                .build();
        XchainOuterClass.TxStatus response = blockingClient.queryTx(request);
        Common.checkResponseHeader(response.getHeader(), "query transaction");
        return response.getTx();
    }

    /**
     * queryBlock get Block
     *
     * @param blockid the id of block
     * @return
     */
    public XchainOuterClass.InternalBlock queryBlock(String blockid) {
        XchainOuterClass.BlockID request = XchainOuterClass.BlockID.newBuilder()
                .setHeader(Common.newHeader())
                .setBcname(chainName)
                .setBlockid(ByteString.copyFrom(Hex.decode(blockid)))
                .setNeedContent(true)
                .build();
        XchainOuterClass.Block response = blockingClient.getBlock(request);
        Common.checkResponseHeader(response.getHeader(), "query transaction");
        return response.getBlock();
    }

    /**
     * queryBlockByHeight  get Block
     * @param blockHeight the height of block
     * @return
     */
    public XchainOuterClass.InternalBlock queryBlockByHeight(long blockHeight) {
        XchainOuterClass.BlockHeight request =  XchainOuterClass.BlockHeight.newBuilder()
                .setHeader(Common.newHeader())
                .setBcname(chainName)
                .setHeight(blockHeight)
                //.setBlockid(ByteString.copyFrom(Hex.decode(blockid)))
                //.setNeedContent(true)
                .build();
        XchainOuterClass.Block response = blockingClient.getBlockByHeight(request);
        Common.checkResponseHeader(response.getHeader(), "query transaction");
        return response.getBlock();
    }





    /**
     * getSystemStatus get system status contains all blockchains
     *
     * @return instance of SystemsStatus
     */
    public XchainOuterClass.SystemsStatus getSystemStatus() {
        XchainOuterClass.CommonIn request = XchainOuterClass.CommonIn.newBuilder()
                .setHeader(Common.newHeader())
                .build();
        XchainOuterClass.SystemsStatusReply response = blockingClient.getSystemStatus(request);
        Common.checkResponseHeader(response.getHeader(), "query system status");
        return response.getSystemsStatus();
    }

    /**
     * getBlockchainStatus get the status of given blockchain
     *
     * @param chainName the name of blockchain
     * @return instance of BCStatus
     */
    public XchainOuterClass.BCStatus getBlockchainStatus(String chainName) {
        XchainOuterClass.BCStatus request = XchainOuterClass.BCStatus.newBuilder()
                .setHeader(Common.newHeader())
                .setBcname(chainName)
                .build();
        XchainOuterClass.BCStatus bcs = blockingClient.getBlockChainStatus(request);
        Common.checkResponseHeader(bcs.getHeader(), "query blockchain status");
        return bcs;
    }

    /**
     * @param from     the initiator of calling method.
     * @param contract contract name.
     * @param method   contract method.
     * @param args     method args.
     * @return transaction.
     */
    public Transaction queryEVMContract(Account from, String contract, String method, Map<String, String> args) {
        Map<String, byte[]> evmArgs = this.convertToXuper3EVMArgs(args);

        return new Proposal()
                .setChainName(chainName)
                .setInitiator(from)
                .addAuthRequire(Config.getInstance().getComplianceCheck().getComplianceCheckEndorseServiceAddr())
                .invokeContract(evmContract, contract, method, evmArgs)
                .preExec(this);
    }

    /**
     * @param from     the initiator of calling method.
     * @param contract contract name.
     * @param method   contract method.
     * @param args     contract method args.
     * @param amount   amount of transfer to contract when call payable method.
     * @return transaction.
     */
    public Transaction invokeEVMContract(Account from, String contract, String method, Map<String, String> args, BigInteger amount) {
        Proposal p = new Proposal().setChainName(chainName);
        if (Config.getInstance().getComplianceCheck().isNeedComplianceCheck()) {
            p.addAuthRequire(Config.getInstance().getComplianceCheck().getComplianceCheckEndorseServiceAddr());
        }
        p.setInitiator(from);

        Map<String, byte[]> evmArgs = this.convertToXuper3EVMArgs(args);

        if (amount == null || amount.compareTo(BigInteger.ZERO) == 0) {
            return p.invokeContract(evmContract, contract, method, evmArgs).build(this).sign().send(this);
        }
        return p.transfer(contract, amount).invokeContract(evmContract, contract, method, evmArgs).build(this).sign().send(this);
    }

    private Map<String, byte[]> convertToXuper3EVMArgs(Map<String, String> initArgs) {
        Map<String, Object> args = new HashMap<>();
        if (initArgs != null) {
            for (Map.Entry<String, String> entry : initArgs.entrySet()) {
                args.put(entry.getKey(), entry.getValue());
            }
        }

        Gson gson = new Gson();
        byte[] initArgsJson = gson.toJson(args).getBytes();

        Map<String, byte[]> evmArgs = new HashMap<>();
        evmArgs.put(argsInput, initArgsJson);
        evmArgs.put(evmJSONEncoded, evmJSONEncodedTrue.getBytes());

        return evmArgs;
    }

    public Transaction upgradeWasmContract(Account from, byte[] code, String name) {
        return upgradeContract(from, code, name, "wasm");
    }

    public Transaction upgradeNativeContract(Account from, byte[] code, String name) {
        return upgradeContract(from, code, name, "native");
    }

    private Transaction upgradeContract(Account from, byte[] code, String contract, String contractType) {
        if (from.getContractAccount().isEmpty()) {
            throw new RuntimeException("deploy contract must use contract account");
        }

        if (contractType.isEmpty()) {
            contractType = "wasm";
        }

        XchainOuterClass.WasmCodeDesc desc = XchainOuterClass.WasmCodeDesc.newBuilder()
                .setContractType(contractType)
                .build();

        Map<String, byte[]> args = new HashMap<>();
        args.put(argAccountName, from.getContractAccount().getBytes());
        args.put(argContractName, contract.getBytes());
        args.put(argContractCode, code);
        args.put(argContractDesc, desc.toByteArray());

        return invokeContract(from, xkernelModule, "", xkernelUpgradeMethod, args);
    }
    public  boolean verifyXuperSignature(String chainAddress, String sig,String msg) throws Exception {
        byte[] signature = Hex.decode(sig);
        byte[] rBytes =Arrays.copyOfRange(signature, 0, 32);
        byte[] sBytes= Arrays.copyOfRange(signature, 32, 64);
        BigInteger r = new BigInteger(1, rBytes);
        BigInteger s = new BigInteger(1, sBytes);

        // 根据您的需求，可以提取公钥的 X 和 Y 值
        BigInteger publicKeyX = new BigInteger(1, Arrays.copyOfRange(signature, 64, 96));
        BigInteger publicKeyY = new BigInteger(1, Arrays.copyOfRange(signature, 96, 128));
        byte[] data = Arrays.copyOfRange(signature, 128, signature.length);

        // 使用 ECNamedCurveTable 获取 secp256r1 曲线参数
        //ECNamedCurveParameterSpec curveParams = ECNamedCurveTable.getParameterSpec("secp256r1");

        // 构造 ECPoint 对象
        //ECPoint ecPoint = curveParams.getCurve().createPoint(publicKeyX, publicKeyY);

        ECPoint ecPoint =Ecc.curve.getCurve().createPoint(publicKeyX, publicKeyY);



        // 构造 ECPoint 对象
        // ECPoint ecPoint = new ECPoint(publicKeyX, publicKeyY);

        // 构造 ECDomainParameters 对象
        //ECDomainParameters domainParameters = new ECDomainParameters(curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH());

        // 构造 ECPublicKeyParameters 对象
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(ecPoint, Ecc.domain);

        // 验证 address是否符合
        String address=cryptoClient.getAddressFromPublicKey(publicKeyParameters.getQ());
        if (!address.equals(chainAddress)){
            return false;
        }
        if (msg!=null&&!msg.equals(new String(data))){
            return false;
        }



        // 使用 Bouncy Castle 的 ECDSASigner 进行签名验证
        ECDSASigner ecdsaSigner = new ECDSASigner(new RandomDSAKCalculator());
        ecdsaSigner.init(false, publicKeyParameters);

        // 传递 R 和 S 值
        return ecdsaSigner.verifySignature(data,r, s);
    }

}
