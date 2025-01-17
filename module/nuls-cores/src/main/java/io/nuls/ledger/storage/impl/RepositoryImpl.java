/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.common.NulsCoresConfig;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.manager.RocksDBManager;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.ledger.model.ChainHeight;
import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.BlockSnapshotAccounts;
import io.nuls.ledger.storage.DataBaseArea;
import io.nuls.ledger.storage.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.nuls.ledger.utils.LoggerUtil.logger;

/**
 * Created by wangkun23 on 2018/11/19.
 *
 * @author lanjinsheng
 */
@Component
public class RepositoryImpl implements Repository, InitializingBean {

    @Autowired
    private NulsCoresConfig config;
    /**
     * key1=chainId,  Map1=确认账户状态， key2= addr+assetkey  value=AccountState
     */
    Map<String, Map<String, AccountState>> memChainsAccounts = new ConcurrentHashMap<>(16);

    public RepositoryImpl() {

    }

    @Override
    public void batchUpdateAccountState(int addressChainId, Map<byte[], byte[]> accountStateMap, Map<String, AccountState> accountStateMemMap) throws Exception {
        if (null == memChainsAccounts.get(String.valueOf(addressChainId))) {
            memChainsAccounts.put(String.valueOf(addressChainId), new ConcurrentHashMap<>(1024));
        }
        if (null != accountStateMemMap) {
            memChainsAccounts.get(String.valueOf(addressChainId)).putAll(accountStateMemMap);
        }
        //update account
        RocksDBService.batchPut(getLedgerAccountTableName(addressChainId), accountStateMap);

    }

    @Override
    public void clearAccountStateMem(int addressChainId, Map<String, AccountState> accountStateMemMap) throws Exception {
        if (null == memChainsAccounts.get(String.valueOf(addressChainId))) {
            memChainsAccounts.put(String.valueOf(addressChainId), new ConcurrentHashMap<>(1024));
        }
        if (null != accountStateMemMap) {
            memChainsAccounts.get(String.valueOf(addressChainId)).putAll(accountStateMemMap);
        }
    }


    @Override
    public void delBlockSnapshot(int chainId, long height) throws Exception {
        RocksDBService.delete(getBlockSnapshotTableName(chainId), ByteUtils.longToBytes(height));
    }

    @Override
    public void saveBlockSnapshot(int chainId, long height, BlockSnapshotAccounts blockSnapshotAccounts) throws Exception {
        RocksDBService.put(getBlockSnapshotTableName(chainId), ByteUtils.longToBytes(height), blockSnapshotAccounts.serialize());

    }

    @Override
    public BlockSnapshotAccounts getBlockSnapshot(int chainId, long height) {
        byte[] stream = RocksDBService.get(getBlockSnapshotTableName(chainId), ByteUtils.longToBytes(height));
        if (stream == null) {
            return null;
        }
        BlockSnapshotAccounts blockSnapshotAccounts = new BlockSnapshotAccounts();
        try {
            blockSnapshotAccounts.parse(new NulsByteBuffer(stream));
        } catch (NulsException e) {
            logger(chainId).error("getAccountState serialize error.", e);
        }
        return blockSnapshotAccounts;
    }


    /**
     * get accountState from rocksdb
     *
     * @param key
     * @return
     */
    @Override
    public AccountState getAccountState(int chainId, byte[] key) {
        byte[] stream = RocksDBService.get(getLedgerAccountTableName(chainId), key);
        if (stream == null) {
            return null;
        }
        AccountState accountState = new AccountState();
        try {
            accountState.parse(new NulsByteBuffer(stream));
        } catch (NulsException e) {
            logger(chainId).error("getAccountState serialize error.", e);
        }
        return accountState;
    }

    @Override
    public AccountState getAccountStateByMemory(int chainId, String key) {
        //缓存有值,则直接获取
        if (null != memChainsAccounts.get(String.valueOf(chainId))) {
            AccountState accountStateMem = memChainsAccounts.get(String.valueOf(chainId)).get(key);
            if (null != accountStateMem) {
                AccountState accountState = new AccountState();
                System.arraycopy(accountStateMem.getNonce(), 0, accountState.getNonce(), 0, accountStateMem.getNonce().length);
                accountState.setTotalFromAmount(accountStateMem.getTotalFromAmount());
                accountState.setTotalToAmount(accountStateMem.getTotalToAmount());
                accountState.setLatestUnFreezeTime(accountStateMem.getLatestUnFreezeTime());
                accountState.getFreezeHeightStates().addAll(accountStateMem.getFreezeHeightStates());
                accountState.getFreezeLockTimeStates().addAll(accountStateMem.getFreezeLockTimeStates());
                return accountState;
            }
        }
        return null;
    }

    @Override
    public long getBlockHeight(int chainId) {
        byte[] stream = RocksDBService.get(getChainsHeightTableName(), ByteUtils.intToBytes(chainId));
        if (stream == null) {
            return -1;
        }
        try {
            long height = ByteUtils.byteToLong(stream);
            return height;
        } catch (Exception e) {
            logger(chainId).error("getBlockHeight serialize error.", e);
        }
        return -1;
    }

    @Override
    public void saveOrUpdateBlockHeight(int chainId, long height) {
        try {
            RocksDBService.put(getChainsHeightTableName(), ByteUtils.intToBytes(chainId), ByteUtils.longToBytes(height));
        } catch (Exception e) {
            logger(chainId).error("saveBlockHeight serialize error.", e);
        }

    }

    @Override
    public List<ChainHeight> getChainsBlockHeight() {
        List<Entry<byte[], byte[]>> list = RocksDBService.entryList(getChainsHeightTableName());
        List<ChainHeight> rtList = new ArrayList<>();
        if (null == list || 0 == list.size()) {
            return null;
        }
        for (Entry<byte[], byte[]> entry : list) {
            ChainHeight chainHeight = new ChainHeight();
            chainHeight.setChainId(ByteUtils.bytesToInt(entry.getKey()));
            chainHeight.setBlockHeight(ByteUtils.byteToLong(entry.getValue()));
            rtList.add(chainHeight);
        }
        return rtList;
    }

    String getChainTableName(String tableName, int chainId) {
        return tableName + "_" + chainId;
    }

    String getLedgerAccountTableName(int chainId) {
        return getChainTableName(DataBaseArea.TB_LEDGER_ACCOUNT, chainId);
    }

    String getBlockSnapshotTableName(int chainId) {
        return getChainTableName(DataBaseArea.TB_LEDGER_ACCOUNT_BLOCK_SNAPSHOT, chainId);
    }

    public String getChainsHeightTableName() {
        return DataBaseArea.TB_LEDGER_BLOCK_HEIGHT;
    }

    /**
     * 初始化数据库
     */
    public void initChainDb(int addressChainId) {
        try {
            RocksDBManager.setDataPath(config.getDataPath() + File.separator + ModuleE.LG.name);
            if (!RocksDBService.existTable(getLedgerAccountTableName(addressChainId))) {
                RocksDBService.createTable(getLedgerAccountTableName(addressChainId));
            }
            if (!RocksDBService.existTable(getBlockSnapshotTableName(addressChainId))) {
                RocksDBService.createTable(getBlockSnapshotTableName(addressChainId));
            }
        } catch (Exception e) {
            logger(addressChainId).error(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws NulsException {

    }

    @Override
    public void initTableName() throws NulsException {
        try {
            RocksDBManager.setDataPath(config.getDataPath() + File.separator + ModuleE.LG.name);
            if (!RocksDBService.existTable(getChainsHeightTableName())) {
                RocksDBService.createTable(getChainsHeightTableName());
            } else {
                Log.info("table {} exist.", getChainsHeightTableName());
            }
        } catch (Exception e) {
            Log.error(e);
            throw new NulsException(e);
        }
    }
}
