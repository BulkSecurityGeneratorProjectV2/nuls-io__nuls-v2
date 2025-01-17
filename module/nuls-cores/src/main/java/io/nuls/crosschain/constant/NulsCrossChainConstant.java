package io.nuls.crosschain.constant;

/**
 * 跨链模块常量管理类
 * @author tag
 * 2019/04/08
 */
public interface NulsCrossChainConstant {
    /**
     * boot path
     */
    String BOOT_PATH = "io.nuls";

    /**
     * context path
     */
    String CONTEXT_PATH = "io.nuls.crosschain";

    /**
     * rpc file path
     */
    String RPC_PATH = "io.nuls.crosschain.nuls.rpc.cmd";

    /**
     * 数据库表明
     * */
    String DB_NAME_CONSUME_LANGUAGE = "language";
    String DB_NAME_CONSUME_CONGIF = "config";
    /**跨链交易处理状态*/
    String DB_NAME_CTX_STATE ="ctx_state";
    /**新创建和验证通过的交易*/
    String DB_NAME_CTX_STATUS = "new_ctx_status";
    /**协议转换HASH对应表*/
    String DB_NAME_CONVERT_CTX = "convert_ctx";
    /**协议转换HASH对应表*/
    String DB_NAME_CONVERT_HASH_CTX = "convert_hash_ctx";
    /**已提交且链内拜占庭已通过但是还未广播给其他链的跨链交易*/
    String DB_NAME_OTHER_COMMITED_CTX = "commit_other_ctx";
    /**指定高度需发送的跨链交易列表*/
    String DB_NAME_SEND_HEIGHT = "send_height";
    /**已广播的交易高度*/
    String DB_NAME_SENDED_HEIGHT = "sended_height";
    /**已注册跨链的链列表*/
    String DB_NAME_REGISTERED_CHAIN ="registered_chain";
    /**验证人变更交易广播失败的链信息*/
    String DB_NAME_BROAD_FAILED ="verifier_broad_fail";
    /**验证人变更交易广播失败的链信息*/
    String DB_NAME_CROSS_CHANGE_FAILED ="cross_change_broad_fail";
    /**本地验证人信息表*/
    String DB_NAME_LOCAL_VERIFIER ="local_verifier";

    /**使用重置本链验证人列表交易重置本链验证人后，将改变前的验证人列表存在此表中*/
    String DB_NAME_OLD_LOCAL_VERIFIER ="old_local_verifier";



    /**
     * config
     * */
    String SYS_FILE_ENCODING = "file.encoding";

    /**
     * Map初始值
     * */
    int  INIT_CAPACITY =16;

    /**
     * log name
     * */
    String COMMON_LOG_NAME = "common";

    int CHAIN_ID_MIN = 0;

    /**
     * 非锁定交易
     */
    byte UNLOCKED_TX = (byte) 0;

    /**
     * 跨链交易固定为非解锁交易
     */
    byte CORSS_TX_LOCKED = 0;

    int INIT_CAPACITY_8 = 8;

    int INIT_CAPACITY_16 = 16;

    String RPC_VERSION = "1.0";

    String VERSION = "1.0";

    byte[] CROSS_TOKEN_NONCE = new byte[]{0,0,0,0,0,0,0,0};

    /**
     * 拜占庭超时时间
     */
    long BYZANTINE_TIMEOUT = 10 * 1000L;

    int BYZANTINE_TRY_COUNT = 5;

    int FAULT_TOLERANT_RATIO = 10;

    int VERIFIER_CANCEL_MAX_RATE =30;

    int MAGIC_NUM_100 =100;

    long RPC_TIME_OUT = 5 * 1000L;

    int CTX_STAGE_WAIT_RECEIVE = 1;
    Integer CTX_STATE_PROCESSING = 2;


    int NODE_TYPE_CURRENT_CHAIN = 1;
    int NODE_TYPE_OTHER_CHAIN = 2;


    /**
     * cmd
     * 查询已注册跨链交易此案次
     * */
    String VERIFIER_SPLIT = ",";

    String STRING_SPLIT = "_";
}
