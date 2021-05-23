/*
 * UicCodes.java
 * Created from uic_codes.c, uic_codes.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem.databits;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Formatter;
import java.util.Map;

import static minimodem.databits.BitOps.bitReverse;
import static minimodem.databits.BitOps.bitWindow;

public class UicCodes {
    public final static int TYPE_GROUNDTRAIN = 0;
    public final static int TYPE_TRAINGROUND = TYPE_GROUNDTRAIN + 1;

    private final static Map<Integer, String> GROUND_TO_TRAIN_MESSAGES = Map.of(
             0x00, "Test" ,
             0x02, "Run slower" ,
             0x03, "Extension of telegram" ,
             0x04, "Run faster" ,
             0x06, "Written order" ,
             0x08, "Speech" ,
             0x09, "Emergency stop" ,
             0x0C, "Announcem. by loudspeaker" ,
             0x55, "Idle"
    );

    private final static Map<Integer, String> TRAIN_TO_GROUND_MESSAGES = Map.of(
             0x08, "Communic. desired" ,
             0x0A, "Acknowl. of order" ,
             0x06, "Advice" ,
             0x00, "Test" ,
             0x09, "Train staff wish to comm." ,
             0x0C, "Telephone link desired" ,
             0x03, "Extension of telegram"
        );

    private static final Logger logger = LogManager.getLogger(UicCodes.class);

    private static String messageMeaning(int code, int type) {
        Map<Integer, String> messages = null;
        if(type == TYPE_GROUNDTRAIN) {
            messages = GROUND_TO_TRAIN_MESSAGES;
        } else if(type == TYPE_TRAINGROUND) {
            messages = TRAIN_TO_GROUND_MESSAGES;
        } else {
            logger.error("Invalid UIC message type code=%x, type=%x", code, type);
            return "Unknown";
        }

        String r = messages.get(code);

        if ( r == null)    { return "Unknown"; }
        return r;
    }

    public static String databitsDecodeUic(long input, int type) {
        StringBuilder sb = new StringBuilder();
        Formatter fmt = new Formatter(sb);
        long code = bitReverse(bitWindow(input, 24, 8), 8);
        fmt.format("Train ID: %X%X%X%X%X%X - Message: %02X (%s)\n",
                bitWindow(input, 0, 4),
                bitWindow(input, 4, 4),
                bitWindow(input, 8, 4),
                bitWindow(input, 12, 4),
                bitWindow(input, 16, 4),
                bitWindow(input, 20, 4),
                code,
                messageMeaning((int) code, type));

        return sb.toString();
    }

}
