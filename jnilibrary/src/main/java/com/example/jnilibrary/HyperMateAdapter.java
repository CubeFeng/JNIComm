package com.example.jnilibrary;

import android.content.Context;
import android.util.Log;

import com.polidea.multiplatformbleadapter.BleAdapter;
import com.polidea.multiplatformbleadapter.BleAdapterFactory;
import com.polidea.multiplatformbleadapter.ConnectionOptions;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ：FS
 * @date ：Created in 2025/5/15
 * @description ：
 */
public class HyperMateAdapter {
    private static final String TAG = "HyperMateAdapter";

    private static final String SERVICE_UUID = "00000001-0000-1000-8000-00805f9b34fb";
    private static final String WRITE_UUID = "00000002-0000-1000-8000-00805f9b34fb";
    private static final String NOTIFY_UUID = "00000003-0000-1000-8000-00805f9b34fb";

    private static final int BLE_MTU = 256;

    static final String SCAN_FILTER_MAX = "Digitshield";
    static final String SCAN_FILTER_G2 = "HPYG2";

    // 为了统一流程的假数据
    private static final String CONNECT_DEV_NAME = "HyperPayMax";
    private static final String CONNECT_DEV_UUID = "00000000-0000-0000-0000-000000000000";
    private static final int CONNECT_DEV_TYPE = 0;

    // open session 指令
    private static final String OPEN_SESSION_CMD_HEX = "3F232300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private static final int RESPONSE_TIMEOUT_MS = 10_000;

    private long mDeviceHandle = 0;
    private String mDeviceAddress;

    private final Object mLock = new Object();
    private final Object mWriteLock = new Object();
    private final Object mNotifyLock = new Object();

    private volatile boolean mSessionOpened = false;

    private HyperMateAdapter() {
    }

    public static HyperMateAdapter getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final HyperMateAdapter INSTANCE = new HyperMateAdapter();
    }

    BleAdapter bleAdapter;

    /**
     * 初始化蓝牙适配器。
     * 通过反射获取应用上下文，若上下文获取成功，则创建新的蓝牙适配器客户端。
     */
    public void init() {
        Context context = GetContextByReflection.getApplicationContext();
        if (null == context) {
            return;
        }

        bleAdapter = BleAdapterFactory.getNewAdapter(context);
        bleAdapter.createClient("HyperMateAdapter",
                data -> {
                    Log.d(TAG, "createClient callback1: " + data);
                },
                data -> {
                    Log.d(TAG, "createClient callback2: " + data);
                });
    }

    /**
     * 开始扫描蓝牙设备。
     */
    public void startScan() {
        bleAdapter.startDeviceScan(
                null,
                0,
                1,
                data -> {
                    Log.d(TAG, "scan callback: " + data);
                    if (null == data || null == data.getDeviceName()) {
                        return;
                    }
                    if (data.getDeviceName().startsWith(SCAN_FILTER_MAX)) {
                        // MAX 这条产品代码中 deviceType 没有用到
                        connectDevice(data.getDeviceId(), new long[]{0});
                    }
                }, error -> {
                    Log.e(TAG, "scan onError: " + error.getMessage());
                });
    }

    /**
     * 停止扫描蓝牙设备。
     */
    public void stopScan() {
        bleAdapter.stopDeviceScan();
    }

    /**
     * 连接指定地址的蓝牙设备。
     *
     * @param deviceAddress 要连接的设备地址
     * @param handle        用于存储设备句柄的数组
     */
    public void connectDevice(String deviceAddress, final long[] handle) {
        mDeviceAddress = deviceAddress;

        bleAdapter.stopDeviceScan();

        ConnectionOptions options = new ConnectionOptions(false, BLE_MTU, null, null, 0);
        Log.d(TAG, "connectDevice - thread: " + Thread.currentThread().getName());
        bleAdapter.connectToDevice(
                deviceAddress,
                options,
                data -> {
                    Log.d(TAG, "connectDevice - onSuccessCallback - thread: " + Thread.currentThread().getName());
                    synchronized (mLock) {
                        mLock.notifyAll();
                    }

                }, data -> {
                    Log.d(TAG, "connectDevice - onEventCallback: ");
                }, error -> {
                    Log.e(TAG, "connectDevice - onErrorCallback: ");
                });
        synchronized (mLock) {
            try {
                mLock.wait(5_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Log.d(TAG, "connectDevice - handle 1: " + handle[0]);
        discoverServices(deviceAddress);
    }

    public void disconnect(long deviceHandle) {
        bleAdapter.cancelDeviceConnection(
                mDeviceAddress,
                device -> {
                    Log.d(TAG, "disconnect success");
                },
                bleError -> {
                    Log.d(TAG, "disconnect error");
                });
    }

    /**
     * 发现指定设备的所有服务和特征。
     * 若发现成功，则开启通知以接收设备响应数据。
     *
     * @param deviceAddress 要发现服务和特征的设备地址
     */
    private void discoverServices(String deviceAddress) {
        bleAdapter.discoverAllServicesAndCharacteristicsForDevice(
                deviceAddress,
                "discoverService",
                data -> {
                    Log.d(TAG, "discoverServices onSuccessCallback: " + data);
                    notifyCharacteristic(deviceAddress, SERVICE_UUID, NOTIFY_UUID);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    // 延迟调用，确保 notify 已经打开
                    // 临时措施，并不保险
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.d(TAG, "call getFeatures()");
                            byte[] result = NativeApi.getFeatures();
                            Log.d(TAG, "getFeatures() : " + HexString.byteArrayToHex(result));
                        }
                    }, 1000);
                },
                error -> {
                    Log.d(TAG, "discoverServices error: " + error);
                });
    }

    /**
     * 开启 Notify，用于接收响应数据
     *
     * @param deviceAddress
     * @param serviceUuid
     * @param characteristicUuid
     */
    private void notifyCharacteristic(String deviceAddress, String serviceUuid, String characteristicUuid) {
        bleAdapter.monitorCharacteristicForDevice(
                deviceAddress,
                serviceUuid,
                characteristicUuid,
                "notifyCharacteristic",
                data -> {
                    Log.d(TAG, "notify onSuccessCallback: " + HexString.byteArrayToHex(data.getValue()));
                    NativeApi.sendDataToNative(data.getValue());

//                    boolean finish = ProtocolDecoder.packetCompletionCheck(data.getValue());
//                    if (!finish) {
//                        return;
//                    }
//                    Log.d(TAG, ">>> notify 接收完成：" + ProtocolDecoder.decode());
//                    synchronized (mNotifyLock) {
//                        mNotifyLock.notifyAll();
//                    }
                },
                error -> {
                    Log.d(TAG, "notifyCharacteristic error: " + error);
                });
    }

    /**
     * 打开会话 (GetFeatures 指令)
     */
    private void openSession() {
        Log.d(TAG, ">>> openSession");
        try {
            byte[] openSessionCmd = HexString.hexStr2ByteArr(OPEN_SESSION_CMD_HEX);
            // 直接调用核心写入逻辑
            boolean writeSuccess = writeCommands(openSessionCmd);
            if (!writeSuccess) {
                Log.e(TAG, "openSession: 命令写入失败");
                mSessionOpened = false;
                return;
            }

            MessageResponse result = waitForResponse();
            mSessionOpened = result != null && result.getData().length > 0;
            Log.d(TAG, "Session opened status: " + mSessionOpened);
        } catch (Exception e) {
            Log.e(TAG, "openSession 过程中出现异常", e);
            mSessionOpened = false;
        }
    }

    /**
     * 写入命令并等待响应，确保会话已开启
     *
     * @param cmd 要写入的命令字节数组
     * @return 设备响应的字节数组，若出错或超时则返回 null
     */
    public MessageResponse writeAndWaitForResponse(byte[] cmd) {
//        if (!mSessionOpened) {
//            openSession();
//            if (!mSessionOpened) {
//                Log.e(TAG, "writeAndWaitForResponse: 会话开启失败");
//                return null;
//            }
//        }

        try {
            boolean writeSuccess = writeCommands(cmd);
            if (!writeSuccess) {
                Log.e(TAG, "writeAndWaitForResponse: 命令写入失败");
                return null;
            } else {
                Log.d(TAG, ">>> 命令写入成功");
            }
            return null;
//            return waitForResponse();
        } catch (Exception e) {
            Log.e(TAG, "writeAndWaitForResponse 过程中出现异常", e);
            return null;
        }
    }


    /**
     * 核心写入逻辑，将命令分片并写入设备
     *
     * @param cmd 要发送的命令字节数组
     * @return 若所有命令部分都成功发送则返回 true，否则返回 false
     */
    private boolean writeCommands(byte[] cmd) {
        List<byte[]> cmdList = ProtocolEncoder.slice(cmd);
        AtomicBoolean allCommandsSent = new AtomicBoolean(true);

        for (byte[] cmdPart : cmdList) {
            Log.d(TAG, "write: " + HexString.byteArrayToHex(cmdPart));
            String cmdBase64 = Base64Converter.encode(cmdPart);
            bleAdapter.writeCharacteristicForDevice(
                    mDeviceAddress,
                    SERVICE_UUID,
                    WRITE_UUID,
                    cmdBase64,
                    false,
                    "writeCharacteristicForDevice",
                    data -> {
                        Log.d(TAG, "write onSuccessCallback: " + HexString.byteArrayToHex(data.getValue()));
                        synchronized (mWriteLock) {
                            mWriteLock.notifyAll();
                        }
                    },
                    error -> {
                        Log.e(TAG, "write error: " + error);
                        allCommandsSent.set(false);
                    }
            );
            synchronized (mWriteLock) {
                try {
                    mWriteLock.wait(3_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "writeCommands: 等待写入被中断", e);
                    allCommandsSent.set(false);
                }
            }
        }

        return allCommandsSent.get();
    }

    /**
     * 等待设备响应，设置超时时间
     *
     * @return 设备响应的字节数组，若超时则返回 null
     */
    private MessageResponse waitForResponse() {
        synchronized (mNotifyLock) {
            try {
                mNotifyLock.wait(RESPONSE_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "waitForResponse: 等待响应被中断", e);
                return null;
            }
        }
        return ProtocolDecoder.getInstance().decode();
    }

}
