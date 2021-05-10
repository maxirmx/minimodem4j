package minimodem;

public class Transmitter {
        public static boolean txTransmitting = false;
        public static boolean txPrintEot = false;
        public static int txLeaderBitsLen = 2;
        public static int txTrailerBitsLen = 2;
        public static AbstractData txSaOut;
        public static float txBfskMarkF;
        public static int txBitNsamples_U;
        public static int txFlushNsamples_U;

        public static void txStopTransmitSighandler(int sig) {
            for(int j = 0; j < txTrailerBitsLen; j++) {
                simpleaudioTone(txSaOut, txBfskMarkF, txBitNsamples_U);
            }

            if(txFlushNsamples_U != 0) {
                simpleaudioTone(txSaOut, 0, txFlushNsamples_U);
            }

            txTransmitting = false;
            if(txPrintEot) {
                System.err.println("### EOT");
            }
        }
}
