package com.adb.scrcpy.connect.scrcpy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScrcpyControlMessage {
    public static final byte TYPE_INJECT_KEYCODE = 0;
    public static final byte TYPE_INJECT_TOUCH_EVENT = 2;
    public static final byte TYPE_BACK_OR_SCREEN_ON = 4;

    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;

    public static byte[] createTouchEvent(int action, long pointerId, int x, int y, int width, int height, float pressure) {
        ByteBuffer buf = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
        buf.put(TYPE_INJECT_TOUCH_EVENT);
        buf.put((byte) action);
        buf.putLong(pointerId);
        buf.putInt(x);
        buf.putInt(y);
        buf.putShort((short) width);
        buf.putShort((short) height);
        buf.putShort((short) (pressure * 65535));
        buf.putInt(1); // action button (primary)
        buf.putInt(1); // buttons state
        return buf.array();
    }

    public static byte[] createKeycodeEvent(int action, int keycode, int metastate) {
        ByteBuffer buf = ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN);
        buf.put(TYPE_INJECT_KEYCODE);
        buf.put((byte) action);
        buf.putInt(keycode);
        buf.putInt(0); // repeat
        buf.putInt(metastate);
        return buf.array();
    }
}
