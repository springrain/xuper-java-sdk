package com.baidu.xuper.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Config {
    public static final String CRYPTO_CHAIN = "xchain";
    public static final String CRYPTO_GM = "gm";

    //private   Config singletonConfig;
    private  String confFilePath;
    private  InputStream confFileInputStream;

    private String endorseServiceHost;
    private ComplianceCheck complianceCheck;
    private String minNewChainAmount;
    private String crypto;
    private Integer txVersion=1;

    private Config() {
    }



    public  void setConfigPath(String path) throws FileNotFoundException {
        confFilePath = path;
        setConfigInputStream(new FileInputStream(path));
        //getInstance();
    }
    public  void setConfigInputStream(InputStream inputStream) {
        confFileInputStream = inputStream;
    }
    public  boolean hasConfigFile() {
        return confFilePath != null || confFileInputStream != null;
    }

    /*
    public  Config getInstance() {

        if (singletonConfig!=null){
            return singletonConfig;
        }

        Config config=new Config();
        if (config.hasConfigFile()) {
            try {
                singletonConfig = config.getConfigFromYaml();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            singletonConfig = config.getDefaultConfig();
        }

        return singletonConfig;
    }
*/
    public static Config getInstance(String configPath) {
        try {
            Yaml yaml = new Yaml(new Constructor(Config.class));
            Config config= yaml.load(new FileInputStream(new File(configPath)));
            if (config.getTxVersion()==null){
                config.setTxVersion(1);
            }
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private  Config getConfigFromYaml() throws Exception {
        Yaml yaml = new Yaml(new Constructor(Config.class));
        Config config= yaml.load(confFileInputStream!=null?confFileInputStream:new FileInputStream(new File(confFilePath)));
        if (config.getTxVersion()==null){
            config.setTxVersion(1);
        }
        return config;
    }

    /*
    private  Config getDefaultConfig() {
        singletonConfig = new Config();
        singletonConfig.minNewChainAmount = "100";
        singletonConfig.crypto = CRYPTO_CHAIN;
//        singletonConfig.crypto = CRYPTO_GM;
        singletonConfig.endorseServiceHost = "";

        ComplianceCheck c = new ComplianceCheck();
        c.isNeedComplianceCheck = false;
        c.isNeedComplianceCheckFee = false;
        c.complianceCheckEndorseServiceFee = 10;
        c.complianceCheckEndorseServiceFeeAddr = "XBbhR82cB6PvaLJs3D4uB9f12bhmKkHeX";
        c.complianceCheckEndorseServiceAddr = "TYyA3y8wdFZyzExtcbRNVd7ZZ2XXcfjdw";

        singletonConfig.complianceCheck = c;
        singletonConfig.txVersion = 1;
        return singletonConfig;
    }
     */

    public String getEndorseServiceHost() {
        return endorseServiceHost;
    }

    public ComplianceCheck getComplianceCheck() {
        return complianceCheck;
    }

    public String getMinNewChainAmount() {
        return minNewChainAmount;
    }

    public String getCrypto() {
        return crypto;
    }

    public void setEndorseServiceHost(String endorseServiceHost) {
        this.endorseServiceHost = endorseServiceHost;
    }

    public void setComplianceCheck(ComplianceCheck complianceCheck) {
        this.complianceCheck = complianceCheck;
    }

    public void setMinNewChainAmount(String minNewChainAmount) {
        this.minNewChainAmount = minNewChainAmount;
    }

    public void setCrypto(String crypto) {
        this.crypto = crypto;
    }

    public Integer getTxVersion() {
        return txVersion;
    }

    public void setTxVersion(Integer txVersion) {
        this.txVersion = txVersion;
    }

    public  class ComplianceCheck {
        private boolean isNeedComplianceCheck;
        private boolean isNeedComplianceCheckFee;
        private int complianceCheckEndorseServiceFee;
        private String complianceCheckEndorseServiceFeeAddr;
        private String complianceCheckEndorseServiceAddr;

        public boolean isNeedComplianceCheck() {
            return isNeedComplianceCheck;
        }

        public boolean isNeedComplianceCheckFee() {
            return isNeedComplianceCheckFee;
        }

        public int getComplianceCheckEndorseServiceFee() {
            return complianceCheckEndorseServiceFee;
        }

        public String getComplianceCheckEndorseServiceFeeAddr() {
            return complianceCheckEndorseServiceFeeAddr;
        }

        public String getComplianceCheckEndorseServiceAddr() {
            return complianceCheckEndorseServiceAddr;
        }

        public void setIsNeedComplianceCheck(boolean needComplianceCheck) {
            isNeedComplianceCheck = needComplianceCheck;
        }

        public void setIsNeedComplianceCheckFee(boolean needComplianceCheckFee) {
            isNeedComplianceCheckFee = needComplianceCheckFee;
        }

        public void setComplianceCheckEndorseServiceFee(int complianceCheckEndorseServiceFee) {
            this.complianceCheckEndorseServiceFee = complianceCheckEndorseServiceFee;
        }

        public void setComplianceCheckEndorseServiceFeeAddr(String complianceCheckEndorseServiceFeeAddr) {
            this.complianceCheckEndorseServiceFeeAddr = complianceCheckEndorseServiceFeeAddr;
        }

        public void setComplianceCheckEndorseServiceAddr(String complianceCheckEndorseServiceAddr) {
            this.complianceCheckEndorseServiceAddr = complianceCheckEndorseServiceAddr;
        }
    }
}
