package minimodem;

public class BitOps {
    /**
     * Reverses the ordering of the bits on an integer
     */
    private static long reverse_U(long value_U, int bits_U) {
        int out_U = 0;

        while(bits_U-- != 0) {
            out_U = (int)(Integer.toUnsignedLong(out_U << 1) | value_U & 1);
            value_U >>>= 1;
        }

        return Integer.toUnsignedLong(out_U);
    }

    /**
     * Gets "bits" bits from "value" starting "offset" bits from the start
     */
    private static long window_U(long value_U, int offset_U, int bits_U) {
        long mask_U = (1l << bits_U) - 1;
        if(mask_U == 0 /* handle bits==64 */) {
            return value_U >>> offset_U;
        }
        value_U = value_U >>> offset_U & mask_U;
        return value_U;
    }
}

