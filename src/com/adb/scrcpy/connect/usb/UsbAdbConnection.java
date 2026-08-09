package com.adb.scrcpy.connect.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

public class UsbAdbConnection {
    private final UsbManager usbManager;
    private final UsbDevice usbDevice;
    private UsbDeviceConnection connection;
    private UsbEndpoint epIn;
    private UsbEndpoint epOut;

    public UsbAdbConnection(UsbManager usbManager, UsbDevice usbDevice) {
        this.usbManager = usbManager;
        this.usbDevice = usbDevice;
    }

    public boolean open() {
        if (!usbManager.hasPermission(usbDevice)) {
            return false;
        }

        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            UsbInterface iface = usbDevice.getInterface(i);
            // ADB class = 255, subclass = 66, protocol = 1
            if (iface.getInterfaceClass() == 255 && iface.getInterfaceSubclass() == 66 && iface.getInterfaceProtocol() == 1) {
                connection = usbManager.openDevice(usbDevice);
                if (connection != null && connection.claimInterface(iface, true)) {
                    for (int j = 0; j < iface.getEndpointCount(); j++) {
                        UsbEndpoint ep = iface.getEndpoint(j);
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                epIn = ep;
                            } else {
                                epOut = ep;
                            }
                        }
                    }
                    return epIn != null && epOut != null;
                }
            }
        }
        return false;
    }

    public int write(byte[] buffer, int timeout) {
        if (connection == null || epOut == null) return -1;
        return connection.bulkTransfer(epOut, buffer, buffer.length, timeout);
    }

    public int read(byte[] buffer, int timeout) {
        if (connection == null || epIn == null) return -1;
        return connection.bulkTransfer(epIn, buffer, buffer.length, timeout);
    }

    public void close() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
