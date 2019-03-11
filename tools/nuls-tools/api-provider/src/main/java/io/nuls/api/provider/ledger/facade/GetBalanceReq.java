package io.nuls.api.provider.ledger.facade;

import io.nuls.api.provider.BaseReq;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-08 15:36
 * @Description:
 * 获取指定账户余额
 * get account balance
 */
@Data
@AllArgsConstructor
public class GetBalanceReq extends BaseReq {

    Integer assetId;

    Integer assetChainId;

    String address;

}