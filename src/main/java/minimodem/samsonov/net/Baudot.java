/**
 * Baudot.java
 * Created from baudot.c, baudot.h @ https://github.com/kamalmostafa/minimodem
 */
package minimodem.samsonov.net;

import java.lang.Character;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Baudot {
    private static final Logger fLogger = LogManager.getFormatterLogger("Baudot");

    private final static byte[/* 32 */][/* 3 */] _decodeTable =
            {       /* letter, U.S. figs, CCITT No.2 figs (Europe) */
                    {'_', '^', '^'} /* NUL (underscore and caret marks for debugging) */,
                    {'E', '3', '3'},
                    {0xA, 0xA, 0xA}, /* LF */
                    {'A', '-', '-'},
                    {' ', ' ', ' '}, /* SPACE */
                    {'S', 0x7, '\''},/* BELL or apostrophe */
                    {'I', '8', '8'},
                    {'U', '7', '7'},
                    {0xD, 0xD, 0xD}, /* CR */
                    {'D', '$', '^'}, /* '$' or ENQ */
                    {'R', '4', '4'},
                    {'J', '\'', 0x7},/* apostrophe or BELL */
                    {'N', ',', ','},
                    {'F', '!', '!'},
                    {'C', ':', ':'},
                    {'K', '(', '('},
                    {'T', '5', '5'},
                    {'Z', '"', '+'},
                    {'L', ')', ')'},
                    {'W', '2', '2'},
                    {'H', '#', '%'}, /* '#' or British pounds symbol	// FIXME */
                    {'Y', '6', '6'},
                    {'P', '0', '0'},
                    {'Q', '1', '1'},
                    {'O', '9', '9'},
                    {'B', '?', '?'},
                    {'G', '&', '&'},
                    {'%', '%', '%'}, /* FIGS (symbol % for debug; won't be printed) */
                    {'M', '.', '.'},
                    {'X', '/', '/'},
                    {'V', ';', '='},
                    {'%', '%', '%'} /* LTRS (symbol % for debug; won't be printed) */
            };

    private static final byte[/* 0x60 */][ /* 3 */] _encodeTable =
            {
                  /* index: ascii char; values: bits, ltrs_or_figs_or_neither_or_both */
                  /* 0x00 */
                    /* NUL */ {0x00, 3},   /* NUL */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* BEL */ {0x05, 2},   /* BELL (or CCITT2 apostrophe) */
                    /* BS */  {0, 0},      /* non-encodable (FIXME???) */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* LF */ {0x02, 3} ,   /* LF */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* 0xD */ {0x08, 3},   /* CR */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                  /* 0x10 */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                    /* xxx */ {0, 0},      /* non-encodable */
                  /* 0x20 */
                    /*   */   {0x04, 3},   /* SPACE */
                    /* ! */   {0x0d, 2},   /*  */
                    /* " */   {0x11, 2},   /*  */
                    /* # */   {0x14, 2},   /* '#' (or CCITT2 British pounds symbol) */
                    /* $ */   {0x09, 2},   /* '$' (or CCITT2 ENQ) */
                    /* % */   {0, 0},      /* non-encodable */
                    /* & */   {0x1a, 2},   /*  */
                    /* ' */   {0x0b, 2},   /* apostrophe (or CCITT2 BELL) */
                    /* ( */   {0x0f, 2},   /*  */
                    /* ) */   {0x12, 2},   /*  */
                    /* * */   {0, 0},      /* non-encodable */
                    /* + */   {0x12, 2},   /*  */
                    /* , */   {0x0c, 2},   /*  */
                    /* - */   {0x03, 2},   /*  */
                    /* . */   {0x1c, 2},   /*  */
                    /* / */   {0x1d, 2},   /*  */
                  /* 0x30 */
                    /* 0 */   {0x16, 2},   /*  */
                    /* 1 */   {0x17, 2},   /*  */
                    /* 2 */   {0x13, 2},   /*  */
                    /* 3 */   {0x01, 2},   /*  */
                    /* 4 */   {0x0a, 2},   /*  */
                    /* 5 */   {0x10, 2},   /*  */
                    /* 6 */   {0x15, 2},   /*  */
                    /* 7 */   {0x07, 2},   /*  */
                    /* 8 */   {0x06, 2},   /*  */
                    /* 9 */   {0x18, 2},   /*  */
                    /* : */   {0x0e, 2},   /*  */
                    /* ; */   {0x1e, 2},   /*  */
                    /* < */   {0, 0},      /* non-encodable */
                    /* = */   {0, 0},      /* non-encodable */
                    /* > */   {0, 0},      /* non-encodable */
                    /* ? */   {0x19, 2},   /*  */
                  /* 0x40 */
                    /* @ */   {0, 0},      /* non-encodable */
                    /* A */   {0x03, 1},   /*  */
                    /* B */   {0x19, 1},   /*  */
                    /* C */   {0x0e, 1},   /*  */
                    /* D */   {0x09, 1},   /*  */
                    /* E */   {0x01, 1},   /*  */
                    /* F */   {0x0d, 1},   /*  */
                    /* G */   {0x1a, 1},   /*  */
                    /* H */   {0x14, 1},   /*  */
                    /* I */   {0x06, 1},   /*  */
                    /* J */   {0x0b, 1},   /*  */
                    /* K */   {0x0f, 1},   /*  */
                    /* L */   {0x12, 1},   /*  */
                    /* M */   {0x1c, 1},   /*  */
                    /* N */   {0x0c, 1},   /*  */
                    /* O */   {0x18, 1},   /*  */
                  /* 0x50 */
                    /* P */   {0x16, 1},   /*  */
                    /* Q */   {0x17, 1},   /*  */
                    /* R */   {0x0a, 1},   /*  */
                    /* S */   {0x05, 1},   /*  */
                    /* T */   {0x10, 1},   /*  */
                    /* U */   {0x07, 1},   /*  */
                    /* V */   {0x1e, 1},   /*  */
                    /* W */   {0x13, 1},   /*  */
                    /* X */   {0x1d, 1},   /*  */
                    /* Y */   {0x15, 1},   /*  */
                    /* Z */   {0x11, 1},   /*  */
                    /* [ */   {0, 0},      /* non-encodable */
                    /* \\ */  {0, 0},      /* non-encodable */
                    /* ] */   {0, 0},      /* non-encodable */
                    /* ^ */   {0, 0},      /* non-encodable */
                    /* _ */   {0, 0},      /* non-encodable */
            };


    /**
     * 0 unknown state
     * 1 LTRS state
     * 2 FIGS state
     */
    private final static int _LTRS  = 0x1F;
    private final static int _FIGS  = 0x1B;
    private final static int _SPACE = 0x04;

    private int _charset = 0;       /* FIXME */

    /**
     * UnShift on space
     */
    public int unshiftOnSpace = 1;

    public void reset() {
        _charset = 1;
    }

    /**
     * Returns 1 if *char_outp was stuffed with an output character
     * or 0 if no output character was stuffed (in other words, returns
     * the count of characters decoded and stuffed).
     */
    public int decode(byte[] charOutp, byte databits) {
        /* Baudot (RTTY) */
        assert (Byte.toUnsignedInt(databits) & ~0x1F) == 0;

        int stuffChar = 1;
        if (Byte.toUnsignedInt(databits) == _FIGS) {
            _charset = 2;
            stuffChar = 0;
        } else if (Byte.toUnsignedInt(databits) == _LTRS) {
            _charset = 1;
            stuffChar = 0;
        } else if (Byte.toUnsignedInt(databits) == _SPACE && (unshiftOnSpace != 0)) {
            /* RX un-shift on space */
            _charset = 1;
        }
        if (stuffChar != 0) {
            int t;
            if (_charset == 1) { t = 0; }
            else                     { t = 1; }  // U.S. figs
                                    // t = 2;	// CCITT figs
            charOutp[0] = _decodeTable[Byte.toUnsignedInt(databits)][t];
        }
        return stuffChar;
    }

    /**
     * baudotSkipWarning
     * Emits warning message on non-encodable character
     */

    private void _skipWarning(byte charOut) {
        fLogger.warn("Skipping non-encodable character '%c' 0x%02x", (char)Byte.toUnsignedInt(charOut), Byte.toUnsignedInt(charOut));
    }

    private void _fatalError(byte charOut) {
        fLogger.fatal("Input character failed '%c' 0x%02x", (char)Byte.toUnsignedInt(charOut), charOut);
        fLogger.fatal("_charset==" + Integer.toUnsignedString(_charset));
        System.exit(-1);
    }

    /**
     * Returns the number of 5-bit data words stuffed into *databits_outp (1 or 2)
     */
    public int encode(int[] databitsOutp, byte charOut) {
        charOut = (byte)Character.toUpperCase(charOut);
        if(charOut >= 0x60 || charOut < 0) {
            _skipWarning(charOut);
            return 0;
        }

        byte ind = (byte) Byte.toUnsignedInt (charOut);

        int n = 0;

        byte charsetMask = _encodeTable[Byte.toUnsignedInt(ind)][1];

        fLogger.trace("(_charset==%d)   input character '%c' 0x%02x charsetMask=%d", _charset, charOut, charOut, charsetMask);

        if ((_charset & Byte.toUnsignedInt(charsetMask)) == 0) {
            if (Byte.toUnsignedInt(charsetMask) == 0) {
                _skipWarning(charOut);
                return 0;
            }

            if (_charset == 0) { _charset = 1; }

            if (Byte.toUnsignedInt(charsetMask) != 3) { _charset = Byte.toUnsignedInt(charsetMask); }

            if      (_charset == 1)      {  databitsOutp[n++] = _LTRS; }
            else if (_charset == 2)      {  databitsOutp[n++] = _FIGS; }
            else    { _fatalError(charOut); }
            fLogger.trace("emit charset select 0x%02x", databitsOutp[n - 1]);
        }

        if(!(_charset == 1 || _charset == 2)) { _fatalError(charOut); }


        databitsOutp[n++] = _encodeTable[Byte.toUnsignedInt(ind)][0];

        /* TX un-shift on space */
        if(charOut == ' ' && (unshiftOnSpace !=0)) {
            _charset = 1;
        }

        return n;
    }

}



