/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
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

package io.nuls.network.manager;

import io.nuls.common.NulsCoresConfig;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.dto.NetTimeUrl;
import io.nuls.network.model.message.GetTimeMessage;
import io.nuls.network.utils.LoggerUtil;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 时间服务类：用于同步网络标准时间
 * Time service class:Used to synchronize network standard time.
 *
 * @author vivi & lan
 */
public class TimeManager {
    private static TimeManager instance = new TimeManager();
    /**
     * NTP服务器网站url集合，用于同步网络时间
     */
    private List<String> ntpSeverUrlList = new ArrayList<>();

    private List<NetTimeUrl> netTimeSevers = new CopyOnWriteArrayList<>();
    /**
     * 时间偏移差距触发点，超过该值会导致本地时间重设，单位毫秒
     * Time migration gap trigger point, which can cause local time reset, unit milliseconds.
     **/
    public static final long TIME_OFFSET_BOUNDARY = 3000L;
    /**
     * 等待对等节点回复时间
     **/
    public static final long TIME_WAIT_PEER_RESPONSE = 2000L;
    /**
     * 重新同步时间间隔
     * Resynchronize the interval.
     * 2 minutes;
     */
    public static final long NET_REFRESH_TIME = 2 * 60 * 1000L;
    /**
     * 网络时间偏移值
     */
    public static long netTimeOffset;
    /**
     * 上次同步时间点
     * The last synchronization point.
     */
    public static long lastSyncTime;

    //询问对等节点网络时间最大数量
    private static final int MAX_REQ_PEER_NUMBER = 8;
    private static Map<String, Long> peerTimesMap = new ConcurrentHashMap<>();
    private static long currentRequestId;
    private long syncStartTime;
    private long syncEndTime;
    private long netTime;

    public static TimeManager getInstance() {
        return instance;
    }

    private TimeManager() {
        if (0 == ntpSeverUrlList.size()) {
            NulsCoresConfig networkConfig = SpringLiteContext.getBean(NulsCoresConfig.class);
            String timeServers = networkConfig.getTimeServers();
            if (StringUtils.isNotBlank(timeServers)) {
                String[] urlArray = timeServers.split(NetworkConstant.COMMA);
                ntpSeverUrlList.addAll(Arrays.asList(urlArray));
            }
        }
    }

    /**
     * 初始化时，同步网络时间，将同步成功的放入集合备用
     * 并按相应时间排序
     */
    public void initWebTimeServer() {
        CountDownLatch latch = new CountDownLatch(ntpSeverUrlList.size());
        for (String url : ntpSeverUrlList) {
            ThreadUtils.asynExecuteRunnable(()->{
                long syncStartTime = System.currentTimeMillis();
                long netTime = getWebTime(url);
                if (netTime > 0) {
                    NetTimeUrl netTimeUrl = new NetTimeUrl(url, (System.currentTimeMillis() - syncStartTime));
                    netTimeSevers.add(netTimeUrl);
                }
                latch.countDown();
            });
        }
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.error("等待获取网络时间发生异常");
            System.exit(0);
        }
        if(netTimeSevers.size() < 3){
            LoggerUtil.COMMON_LOG.warn("可用服务器小于3个");
        }
        Collections.sort(netTimeSevers);
        LoggerUtil.COMMON_LOG.info("初始化时间服务器完成");
        LoggerUtil.COMMON_LOG.info("=".repeat(100));
        netTimeSevers.forEach(d->{
            LoggerUtil.COMMON_LOG.info("site:{} 耗时:{}",d.getUrl(),d.getTime());
        });
        LoggerUtil.COMMON_LOG.info("=".repeat(100));
    }

    /**
     * 同步网络时间
     * 如果成功同步到三个即可
     */
    public void syncWebTime() {
        int count = 0;
        long[] times = {0, 0, 0};
        for (int i = 0; i < netTimeSevers.size(); i++) {
            syncStartTime = System.currentTimeMillis();
            netTime = getWebTime(netTimeSevers.get(i).getUrl());
            if (netTime == 0) {
                continue;
            }
            syncEndTime = System.currentTimeMillis();
            times[count] = (netTime + (syncEndTime - syncStartTime) / 2) - syncEndTime;
            count++;
            /*
             * 有3个网络时间正常返回就可以
             */
            if (count >= 3) {
                break;
            }
        }
        if (count == 3) {
            calNetTimeOffset(times[0], times[1], times[2]);
        } else {
            //从对等网络去获取时间
            LoggerUtil.COMMON_LOG.debug("count={} syncPeerTime .....", count);
            syncPeerTime();
        }
        lastSyncTime = currentTimeMillis();
    }

    public static void calNetTimeOffset(long time1, long time2, long time3) {
        //3个网络时间里去除 与其他2个偏差大于500ms的时间值。
        long differMs = 500;
        int count = 3;
        if (Math.abs(time1 - time2) > differMs && Math.abs(time1 - time3) > differMs) {
            time1 = 0;
            count--;
        }
        if (Math.abs(time2 - time1) > differMs && Math.abs(time2 - time3) > differMs) {
            time2 = 0;
            count--;
        }
        if (Math.abs(time3 - time1) > differMs && Math.abs(time3 - time2) > differMs) {
            time3 = 0;
            count--;
        }
        if (count > 1) {
            netTimeOffset = (time1 + time2 + time3) / count;
        }
    }

    /**
     * 向对等节点同步时间
     */
    private synchronized void syncPeerTime() {
        //设置请求id
        currentRequestId = System.currentTimeMillis();
        long beginTime = currentRequestId;
        // peerTimesMap 清空
        peerTimesMap.clear();
        //随机发出请求
        List<NodeGroup> list = NodeGroupManager.getInstance().getNodeGroups();
        if (list.size() == 0) {
            return;
        }
        Collections.shuffle(list);
        int count = 0;
        boolean nodesEnough = false;
        for (NodeGroup nodeGroup : list) {
            Collection<Node> nodes = nodeGroup.getLocalNetNodeContainer().getConnectedNodes().values();
            for (Node node : nodes) {
                sendGetTimeMessage(node);
                count++;
                if (count >= MAX_REQ_PEER_NUMBER) {
                    nodesEnough = true;
                    break;
                }
            }
            if (nodesEnough) {
                break;
            }
        }
        if (count == 0) {
            return;
        }

        //数量或时间满足要求
        long intervalTime = 0;
        while (peerTimesMap.size() < MAX_REQ_PEER_NUMBER && intervalTime < TIME_WAIT_PEER_RESPONSE) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                LoggerUtil.COMMON_LOG.error(e);
                Thread.currentThread().interrupt();
            }
            intervalTime = System.currentTimeMillis() - beginTime;
        }

        int size = peerTimesMap.size();
        if (size > 0) {
            long sum = 0L;
            Set set = peerTimesMap.keySet();
            //计算
            for (Object aSet : set) {
                sum += peerTimesMap.get(aSet.toString());
            }
            netTimeOffset = sum / size;
            LoggerUtil.COMMON_LOG.debug("syncPeerTime netTimeOffset={}", netTimeOffset);
        }
    }


    public static void addPeerTime(String nodeId, long requestId, long time) {
        if (currentRequestId == requestId) {
            if (MAX_REQ_PEER_NUMBER > peerTimesMap.size()) {
                long localBeforeTime = currentRequestId;
                long localEndTime = System.currentTimeMillis();
                long value = (time + (localEndTime - localBeforeTime) / 2) - localEndTime;
                peerTimesMap.put(nodeId, value);
            }
        }
    }

    private void sendGetTimeMessage(Node node) {
        GetTimeMessage getTimeMessage = MessageFactory.getInstance().buildTimeRequestMessage(node.getMagicNumber(), currentRequestId);
        MessageManager.getInstance().sendHandlerMsg(getTimeMessage, node, true);
    }

    /**
     * 获取网络时间
     *
     * @return long
     */
    private long getWebTime(String address) {
        try {
            NTPUDPClient client = new NTPUDPClient();
            client.setDefaultTimeout(500);
            client.open();
            client.setSoTimeout(500);
            InetAddress inetAddress = InetAddress.getByName(address);
            //Log.debug("start ask time....");
            TimeInfo timeInfo = client.getTime(inetAddress);
            //Log.debug("done!");
            return timeInfo.getMessage().getTransmitTimeStamp().getTime();
        } catch (Exception e) {
            LoggerUtil.COMMON_LOG.warn("address={} sync time fail", address);
            return 0L;
        }
    }

    /**
     * 获取当前网络时间毫秒数
     * Gets the current network time in milliseconds.
     *
     * @return long
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis() + netTimeOffset;
    }

}
