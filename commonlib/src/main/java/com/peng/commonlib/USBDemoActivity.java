package com.peng.commonlib;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.blankj.utilcode.util.ToastUtils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * USB 连接 Demo
 *
 * @author pq
 *         create at 2019/1/28
 */
public class USBDemoActivity extends AppCompatActivity {

    // 权限 Intent
    private PendingIntent mPermissionIntent;
    // 定义权限字符串
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    // 创建处理权限，标记连接的设备是否获取到权限
    private boolean mPermissionRequestPending;
    // USB 管理类
    private UsbManager mUsbManager;
    // USB 配件管理类
    private UsbAccessory mAccessory;
    // 用于操作原始文件的描述符
    private ParcelFileDescriptor mParcelFileDescriptor;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    // 定义通信格式中的命令类型
    private static final byte COMMAND_LED = 0x2;
    // 定义通信格式中的命令目标
    private static final byte TARGET_PIN_2 = 0x2;
    // 定义通信命令内容
    private static final byte VALUE_ON = 0x1;
    private static final byte VALUE_OFF = 0x0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // 增加USB拔出过滤
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        setContentView(R.layout.activity_usbdemo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果当前存在通信，则返回
        if (mFileInputStream != null && mFileOutputStream != null) {
            return;
        }
        // 获取当前连接的USB列表
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        // 获取当前连接的第一个USB设备
        UsbAccessory accessory = accessories == null ? null : accessories[0];
        if (accessory != null) {
            // 是否具有权限
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbManager) {
                    // 获取USB设备的权限
                    mUsbManager.requestPermission(accessory, mPermissionIntent);
                    mPermissionRequestPending = true;
                }
            }
        } else {
            ToastUtils.showShort("没有连接USB设备，accessory 为 null");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeAccessory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }

    /**
     * 发送命令到USB设备
     *
     * @author pq
     * create at 2019/1/28
     */
    public void sendUSBCommand(byte target, boolean isLedOn) {
        byte[] buffer = new byte[3];
        buffer[0] = COMMAND_LED;
        buffer[1] = target;
        if (isLedOn) {
            buffer[2] = VALUE_ON;
        } else {
            buffer[2] = VALUE_OFF;
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭USB设备
     *
     * @author pq
     * create at 2019/1/28
     */
    private void closeAccessory() {
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mParcelFileDescriptor = null;
                mAccessory = null;
            }
        }
    }

    /**
     * 打开设备
     *
     * @author pq
     * create at 2019/1/28
     */
    private void openAccessory(UsbAccessory accessory) {
        mParcelFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mParcelFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mParcelFileDescriptor.getFileDescriptor();
            mFileInputStream = new FileInputStream(fd);
            mFileOutputStream = new FileOutputStream(fd);
        } else {
            ToastUtils.showShort("设备打开失败");
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 收到请求USB权限的广播
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        ToastUtils.showShort("设备请求权限被拒绝");
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) { // 设备拔出
                UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null) {
                    closeAccessory();
                }
            }
        }
    };
}
