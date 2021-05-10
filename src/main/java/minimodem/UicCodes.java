/**
 * UicCodes.java
 * Created from uic_codes.c, uic_codes.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

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

    public static String messageMeaning(int code, int type) {
        Map<Integer, String> messages = null;
        if(type == TYPE_GROUNDTRAIN) {
            messages = GROUND_TO_TRAIN_MESSAGES;
        } else if(type == TYPE_TRAINGROUND) {
            messages = TRAIN_TO_GROUND_MESSAGES;
        } else {
            logger.fatal("Invalid UIC message type code=%x, type=%x", code, type);
            System.exit(-1);
        }

        String r = messages.get(code);

        if ( r == null)    { return "Unknown"; }
        return r;
    }

}
