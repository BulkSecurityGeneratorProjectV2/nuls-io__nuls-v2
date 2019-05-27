/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.chain.rpc.cmd;

import io.nuls.chain.rpc.call.RpcService;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.CacheDataService;
import io.nuls.chain.service.ChainService;
import io.nuls.chain.service.ValidateService;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

/**
 * @author lan
 * @date 2018/11/22
 */
@Component
public class TxModuleCmd extends BaseChainCmd {
    @Autowired
    private ChainService chainService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private CacheDataService cacheDataService;
    @Autowired
    private ValidateService validateService;
    @Autowired
    private RpcService rpcService;


//    /**
//     * chainModuleTxValidate
//     * 批量校验
//     */
//    @CmdAnnotation(cmd = BaseConstant.TX_VALIDATOR, version = 1.0,
//            description = "chainModuleTxValidate")
//    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
//    @Parameter(parameterName = "txList", parameterType = "String")
//    public Response chainModuleTxValidate(Map params) {
//        try {
//            ObjectUtils.canNotEmpty(params.get("chainId"));
//            ObjectUtils.canNotEmpty(params.get("txList"));
//            List<String> txHexList = (List) params.get("txList");
//            List<Transaction> txList = new ArrayList<>();
//            List<String> errorList = new ArrayList<>();
//            Response parseResponse = parseTxs(txHexList, txList);
//            if (!parseResponse.isSuccess()) {
//                return parseResponse;
//            }
//            //1获取交易类型
//            //2进入不同验证器里处理
//            //3封装失败交易返回
//            Map<String, Integer> chainMap = new HashMap<>();
//            Map<String, Integer> assetMap = new HashMap<>();
//            BlockChain blockChain = null;
//            Asset asset = null;
//            ChainEventResult chainEventResult = ChainEventResult.getResultSuccess();
//            for (Transaction tx : txList) {
//                String txHash = tx.getHash().toHex();
//                switch (tx.getType()) {
//                    case TxType.REGISTER_CHAIN_AND_ASSET:
//                        blockChain = TxUtil.buildChainWithTxData(tx, false);
//                        asset = TxUtil.buildAssetWithTxChain(tx);
//                        String assetKey = CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId());
//                        chainEventResult = validateService.batchChainRegValidator(blockChain, asset, chainMap, assetMap);
//                        if (chainEventResult.isSuccess()) {
//                            ChainManagerUtil.putChainMap(blockChain, chainMap);
//                            assetMap.put(assetKey, 1);
//                            LoggerUtil.logger().debug("txHash = {},chainId={} reg batchValidate success!", txHash, blockChain.getChainId());
//                        } else {
//                            LoggerUtil.logger().error("txHash = {},chainId={},magicNumber={} reg batchValidate fail!", txHash, blockChain.getChainId(), blockChain.getMagicNumber());
//                            errorList.add(txHash);
//                        }
//                        break;
//                    case TxType.DESTROY_CHAIN_AND_ASSET:
//                        blockChain = TxUtil.buildChainWithTxData(tx, true);
//                        chainEventResult = validateService.chainDisableValidator(blockChain);
//                        if (chainEventResult.isSuccess()) {
//                            LoggerUtil.logger().debug("txHash = {},chainId={} destroy batchValidate success!", txHash, blockChain.getChainId());
//                        } else {
//                            LoggerUtil.logger().error("txHash = {},chainId={} destroy batchValidate fail!", txHash, blockChain.getChainId());
//                            errorList.add(txHash);
//                        }
//                        break;
//
//                    case TxType.ADD_ASSET_TO_CHAIN:
//                        asset = TxUtil.buildAssetWithTxChain(tx);
//                        String assetKey2 = CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId());
//                        chainEventResult = validateService.batchAssetRegValidator(asset, assetMap);
//                        if (chainEventResult.isSuccess()) {
//                            assetMap.put(assetKey2, 1);
//                            LoggerUtil.logger().debug("txHash = {},assetKey={} reg batchValidate success!", txHash, assetKey2);
//                        } else {
//                            LoggerUtil.logger().error("txHash = {},assetKey={} reg batchValidate fail!", txHash, assetKey2);
//                            errorList.add(txHash);
//                        }
//                        break;
//                    case TxType.REMOVE_ASSET_FROM_CHAIN:
//                        asset = TxUtil.buildAssetWithTxChain(tx);
//                        chainEventResult = validateService.assetDisableValidator(asset);
//                        if (chainEventResult.isSuccess()) {
//                            LoggerUtil.logger().debug("txHash = {},assetKey={} disable batchValidate success!", txHash, CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId()));
//                        } else {
//                            LoggerUtil.logger().error("txHash = {},assetKey={} disable batchValidate fail!", txHash, CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId()));
//                            errorList.add(txHash);
//                        }
//                        break;
//                    default:
//                        break;
//                }
//            }
//            Map<String, Object> rtMap = new HashMap<>(1);
//            rtMap.put("list", errorList);
//            return success(rtMap);
//        } catch (Exception e) {
//            LoggerUtil.logger().error(e);
//            return failed(e.getMessage());
//        }
//    }
//
//    /**
//     * moduleTxsRollBack
//     * 回滚
//     */
//    @CmdAnnotation(cmd = BaseConstant.TX_ROLLBACK, version = 1.0,
//            description = "moduleTxsRollBack")
//    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
//    @Parameter(parameterName = "txList", parameterType = "array")
//    @Parameter(parameterName = "blockHeader", parameterType = "array")
//    public Response moduleTxsRollBack(Map params) {
//        Map<String, Boolean> resultMap = new HashMap<>();
//        resultMap.put("value", true);
//        try {
//            ObjectUtils.canNotEmpty(params.get("chainId"));
//            ObjectUtils.canNotEmpty(params.get("blockHeader"));
//            Integer chainId = (Integer) params.get("chainId");
//            List<String> txHexList = (List) params.get("txList");
//            String blockHeaderStr = (String) params.get("blockHeader");
//            BlockHeader blockHeader = new BlockHeader();
//            blockHeader.parse(RPCUtil.decode(blockHeaderStr), 0);
//            long commitHeight = blockHeader.getHeight();
//            List<Transaction> txList = new ArrayList<>();
//            Response parseResponse = parseTxs(txHexList, txList);
//            if (!parseResponse.isSuccess()) {
//                return parseResponse;
//            }
//            //获取回滚信息
//            CacheDatas moduleTxDatas = cacheDataService.getCacheDatas(commitHeight - 1);
//            //通知远程调用回滚
//            chainService.rpcBlockChainRollback(txList);
//            if (null == moduleTxDatas) {
//                LoggerUtil.logger().info("chain module height ={} bak datas is null,maybe had rolled", commitHeight);
//                return success(resultMap);
//            }
//            //进行数据回滚
//            cacheDataService.rollBlockTxs(chainId, commitHeight);
//        } catch (Exception e) {
//            LoggerUtil.logger().error(e);
//            return failed(e.getMessage());
//        }
//        rpcService.crossChainRegisterChange(CmRuntimeInfo.getMainIntChainId());
//        return success(resultMap);
//    }
//
//    /**
//     * moduleTxsCommit
//     * 批量提交
//     */
//    @CmdAnnotation(cmd = BaseConstant.TX_COMMIT, version = 1.0,
//            description = "moduleTxsCommit")
//    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
//    @Parameter(parameterName = "txList", parameterType = "array")
//    @Parameter(parameterName = "blockHeader", parameterType = "array")
//    public Response moduleTxsCommit(Map params) {
//        try {
//            ObjectUtils.canNotEmpty(params.get("chainId"));
//            ObjectUtils.canNotEmpty(params.get("txList"));
//            ObjectUtils.canNotEmpty(params.get("blockHeader"));
//            Integer chainId = (Integer) params.get("chainId");
//            List<String> txHexList = (List) params.get("txList");
//            String blockHeaderStr = (String) params.get("blockHeader");
//            BlockHeader blockHeader = new BlockHeader();
//            blockHeader.parse(RPCUtil.decode(blockHeaderStr), 0);
//            long commitHeight = blockHeader.getHeight();
//            List<Transaction> txList = new ArrayList<>();
//            Response parseResponse = parseTxs(txHexList, txList);
//            if (!parseResponse.isSuccess()) {
//                return parseResponse;
//            }
//            /*begin bak datas*/
//            BlockHeight dbHeight = cacheDataService.getBlockHeight(chainId);
//            cacheDataService.bakBlockTxs(chainId, commitHeight, txList, false);
//            /*end bak datas*/
//            /*begin bak height*/
//            cacheDataService.beginBakBlockHeight(chainId, commitHeight);
//            /*end bak height*/
//            BlockChain blockChain = null;
//            Asset asset = null;
//            try {
//                for (Transaction tx : txList) {
//                    switch (tx.getType()) {
//                        case TxType.REGISTER_CHAIN_AND_ASSET:
//                            blockChain = TxUtil.buildChainWithTxData(tx, false);
//                            asset = TxUtil.buildAssetWithTxChain(tx);
//                            chainService.registerBlockChain(blockChain, asset);
//                            break;
//                        case TxType.DESTROY_CHAIN_AND_ASSET:
//                            blockChain = TxUtil.buildChainWithTxData(tx, true);
//                            chainService.destroyBlockChain(blockChain);
//                            break;
//                        case TxType.ADD_ASSET_TO_CHAIN:
//                            asset = TxUtil.buildAssetWithTxChain(tx);
//                            assetService.registerAsset(asset);
//                            break;
//                        case TxType.REMOVE_ASSET_FROM_CHAIN:
//                            asset = TxUtil.buildAssetWithTxChain(tx);
//                            assetService.deleteAsset(asset);
//                            break;
//                        default:
//                            break;
//                    }
//                }
//                /*begin bak height*/
//                cacheDataService.endBakBlockHeight(chainId, commitHeight);
//                /*end bak height*/
//                rpcService.crossChainRegisterChange(CmRuntimeInfo.getMainIntChainId());
//            } catch (Exception e) {
//                LoggerUtil.logger().error(e);
//                //通知远程调用回滚
//                chainService.rpcBlockChainRollback(txList);
//                //进行回滚
//                cacheDataService.rollBlockTxs(chainId, commitHeight);
//                return failed(e.getMessage());
//            }
//        } catch (Exception e) {
//            LoggerUtil.logger().error(e);
//            return failed(e.getMessage());
//        }
//        Map<String, Boolean> resultMap = new HashMap<>();
//        resultMap.put("value", true);
//        return success(resultMap);
//    }
}