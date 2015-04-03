package example.chatea.servicecamera.messages;

import java.util.ArrayList;
import java.util.List;

public class MessageHUB {

    public static final int MEDIA_BUTTON_CLICKED = 0;

    public static final int MEASUREMENT_STEP = 1;

    public static final int DONE_CALIBRATION = 2;

    public static final int DONE_MEASUREMENT = 3;

    public static final int LOG_ONSERVICE = 4;

    public static final int LOG_PREVIEWFRAME = 5;

    public static final int LOG_CAMERA_FAILED = 6;

    public static final int LOG_PARA = 7;

    public static final int LOG_PREVIEW = 8;
    public static final int setPreviewDisplay_error = 9;



    private static MessageHUB _instance = new MessageHUB();

    public static MessageHUB get() {
        return _instance;
    }

    private final List<MessageListener> _listenerList = new ArrayList<MessageListener>();

    public boolean registerListener(final MessageListener listener) {
        if (!_listenerList.contains(listener)) {
            return _listenerList.add(listener);
        }

        return false;
    }

    public boolean unregisterListener(final MessageListener listener) {
        return _listenerList.remove(listener);
    }

    public void sendMessage(final int messageID, final Object message) {
        for (MessageListener ml : _listenerList) {
            ml.onMessage(messageID, message);
        }
    }

}
