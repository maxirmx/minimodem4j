package minimodem.samsonov.net;

import java.io.File;
import java.util.Stack;
import java.util.concurrent.Callable;

import minimodem.samsonov.net.helpers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "minimodem", mixinStandardHelpOptions = true, version = "Version: j.0.0.1",
		description = "Minimodem Java port", usageHelpWidth = 100)
class Minimodem implements Callable<Integer> {
	private static final Logger fLogger = LogManager.getFormatterLogger("Minimodem");

	static class OpMode {
		@Option(names = {"-t", "--tx", "--transmit", "--write"}, required = true) 		private boolean oTransmit;
		@Option(names = {"-r", "--rx", "--receive", "--read"}, required = true) 		private boolean oReceive;
	}
	@CommandLine.ArgGroup(exclusive = true, multiplicity = "1") 						private OpMode opMode;

	private enum TXMODE { UNKNOWN, RECEIVE, TRANSMIT };
	protected TXMODE txMode;

	@Option(names = {"-c", "--confidence"}, paramLabel = "{min-confidence-threshold}", defaultValue = "1.5f",
			description = "Signal-to-noise squelch control, The minimum SNR-ish confidence level seen as \"a signal\"")
			private float fskConfidenceThreshold;
	@Option(names = {"-l", "--limit"}, paramLabel = "{max-confidence-search-limit}", defaultValue = "2.3f",
			description = "Performance vs. quality control. If we find a frame with " +
					"confidence > confidence_search_limit, quit searching for a better frame. " +
					"confidence_search_limit has dramatic effect on performance (high value yields " +
					"low performance, but higher decode quality, for noisy or hard-to-discern signals " +
					"(Bell 103, or skewed rates).")
			private float fskConfidenceSearchLimit;
	@Option(names = {"-a", "--auto-carrier"}, parameterConsumer = AutoDetectCarrierParameterConsumer.class)
	protected float carrierAutodetectThreshold = 0.0f;

	@Option(names = {"-i", "--inverted"}) 									private boolean bfskInvertedFreqs;
	static class DataBits {
		@Option(names = {"-8", "--ascii"}, description = "ASCII  8-N-1") 	private boolean o8;
		@Option(names = {"-7"}, description = "ASCII  7-N-1") 				private boolean o7;
		@Option(names = {"-5", "--baudot"}, description = "Baudot  5-N-1") 	private boolean o5;
	}
	@CommandLine.ArgGroup(exclusive = true) 								DataBits oDatabits;
	@Option(names = {"-u", "--usos"},  paramLabel = "{0|1}") 				private int oUsos;
	@Option(names = {"--msb-first"}) 										private boolean oMsbFirst;
	@Option(names = {"-f", "--file"}, paramLabel = "{filename.flac}")       private File oFile;
	@Option(names = {"-b", "--bandwidth"}, paramLabel = "{rx_bandwidth}") 	private float oBandwidth;
	@Option(names = {"-v", "--volume"}, paramLabel = "{amplitude or 'E'}") 	private String oVolume;
	@Option(names = {"-M", "--mark"}, paramLabel = "{mark_freq}", defaultValue = "0.0f") 	private float bfskMarkF;
	@Option(names = {"-S", "--space"}, paramLabel = "{space_freq}", defaultValue = "0.0f")	private float bfskSpaceF;
	@Option(names = {"--startbits"}, paramLabel = "{n}", defaultValue = "-1")				private int bfskNStartBits;
	@Option(names = {"--stopbits"},	paramLabel = "{n.n}", defaultValue = "-1.0f") 			private float bfskNStopBits;
	@Option(names = {"--invert-start-stop"})								private boolean oInvertStartStop;
	@Option(names = {"--sync-byte"}, paramLabel = "{0xXX}", defaultValue = "-1")			private Byte bfskSyncByte;
	@Option(names = {"-q", "--quiet"})										private boolean oQuite;
	@Option(names = {"-A", "--alsa"}, arity = "0..1",
			paramLabel = "{plughw:X,Y}", fallbackValue = "")				private String oALSA;
	@Option(names = {"-s", "sndio"}, arity = "0..1",
			paramLabel = "{device}", fallbackValue = "")					private String oDevice;
	@Option(names = {"-R", "--samplerate"}, paramLabel = "{rate}")			private int oSampleRate;
	@Option(names = {"--lut"}, paramLabel = "{tx_sin_table_len}")			private int oLut;
	@Option(names = {"--float-samples"})									private boolean oFloatSamples;
	@Option(names = {"--rx-one"})											private boolean oRxOne;
	@Option(names = {"--benchmarks"})										private boolean oBenchmarks;
	@Option(names = {"--binary-output"})									private boolean oBinaryOutput;
	@Option(names = {"--binary-raw"}, paramLabel = "{nbits}", defaultValue = "0")			private int rawNBits;
	@Option(names = {"--print-filter"})										private boolean oPrintFilter;
	@Option(names = {"--print-eot"})										private boolean oPrintEot;
	@Option(names = {"--Xrxnoise"}, paramLabel = "{rx-noise-factor}")		private boolean oXrxNoise;
	@Option(names = {"--tx-carrier"})										private boolean oTxCarrier;

	@Parameters(index = "0", paramLabel = "{baudmod}", description =
		"   any_number_N       Bell-like      N bps --ascii%n" +
		"		    1200       Bell202     1200 bps --ascii%n" +
		"		     300       Bell103      300 bps --ascii%n" +
		"		    rtty       RTTY       45.45 bps --baudot --stopbits=1.5%n" +
		"		     tdd       TTY/TDD    45.45 bps --baudot --stopbits=2.0%n" +
		"		    same       NOAA SAME 520.83 bps --sync-byte=0xAB ...%n" +
		"		callerid       Bell202 CID 1200 bps%n" +
		"uic{-train,-ground}   UIC-751-3 Train/Ground 600 bps%n%n")	private String modemMode;

	protected float bfskDataRate = 0.0f;           //  Data rate (baud rate) is deduced from the parameter above

	private boolean	bfskDoRxSync = false;
	private int bfskDoTxSyncBytes = 0;
	private int bfskNDataBits = 0;
	private float bandWidth = 0.0f;
	private int txLeaderBitsLen = 2;
	private byte[] expectDataString = null;
	private byte[] expectSyncString = null;
	private int expectNBits;
	private int autodetectShift;

	@Override
	public Integer call() {
		int quietMode = 0;
		int outputPrintFilter = 0;
		int invertStartStop = 0;

		int cfgRc = configure();
		if (cfgRc != 0) {
			return cfgRc;
		}

		fLogger.info("Running minimodem4j ...");

		fLogger.trace("We've just greeted the user!");
		fLogger.debug("We've just greeted the user!");
		fLogger.info("We've just greeted the user!");
		fLogger.warn("We've just greeted the user!");
		fLogger.error("We've just greeted the user!");
		fLogger.fatal("We've just greeted the user!");

		EncoderDecoder e = new DatabitsBaudot();
		int[] db = new int[2];
		int rb = e.encode(db, (byte)'A');
		rb = e.encode(db, (byte)0x12);

		e = new DatabitsAscii8();
		int rd = e.encode(db, (byte)'A');

		return 0;
	}

	// Implements Callable, so parsing, error handling and handling user
	// requests for usage help or version are done by picocli with one line of code.
	public static void main(String... args) {
		int exitCode = new CommandLine(new Minimodem()).execute(args);
		System.exit(exitCode);
	}

	/**
	 * configure
	 * Builds modem configureation from the command line parameters
	 * @return 0 if successful, application return code otherwise
	 */
	protected int configure() {
		if(modemMode.equalsIgnoreCase("rtty")) {
			bfskDataRate = 45.45f;
			if (bfskNDataBits == 0) { bfskNDataBits = 5; }
			if (bfskNStopBits < 0)  { bfskNStopBits = 1.5f; }
		} else if(modemMode.equalsIgnoreCase("tdd")) {
			bfskDataRate = 45.45f;
			if (bfskNDataBits == 0) { bfskNDataBits = 5; }
			if (bfskNStopBits < 0)  { bfskNStopBits = 2.0f; }
			bfskMarkF = 1400f;
			bfskSpaceF = 1800f;
		} else if(modemMode.equalsIgnoreCase("same")) {
			// http://www.nws.noaa.gov/nwr/nwrsame.htm
			bfskDataRate = (float)(520.0 + 5 / 6.0);
			bfskNDataBits = 8;
			bfskNStartBits = 0;
			bfskNStopBits = 0;
			bfskDoRxSync = true;
			oUsos = 16;
			bfskSyncByte = (byte)0xAB;
			bfskMarkF = (float)(2083.0 + 1 / 3.0);
			bfskSpaceF = 1562.5f;
			bandWidth = bfskDataRate;
		} else if(modemMode.equalsIgnoreCase("caller")) {
			if(txMode != TXMODE.RECEIVE) {
				fLogger.fatal ("callerid --tx mode is not supported.");
				return 1;
			}
			if(carrierAutodetectThreshold > 0.0f) {
				fLogger.warn ("callerid with --auto-carrier is not recommended.");
			}
			bfskDataRate = 1200f;
			bfskNDataBits = 8;
		} else if(modemMode.equalsIgnoreCase("uic")) {
			if(txMode != TXMODE.RECEIVE) {
				fLogger.fatal ("uic-751-3 --tx mode is not supported.");
				return 1;
			}
			// http://ec.europa.eu/transport/rail/interoperability/doc/ccs-tsi-en-annex.pdf

			bfskDataRate = 600;
			bfskNDataBits = 39;
			bfskMarkF = 1300f;
			bfskSpaceF = 1700f;
			bfskNStartBits = 8;
			bfskNStopBits = 0;

			expectDataString = "11110010ddddddddddddddddddddddddddddddddddddddd\0".getBytes();
			expectNBits = 47;
		} else if(modemMode.equalsIgnoreCase("V.21")) {
			bfskDataRate = 300;
			bfskMarkF = 980f;
			bfskSpaceF = 1180f;
			bfskNDataBits = 8;
		} else {
			try {
				bfskDataRate = Float.parseFloat(modemMode);
			} catch (Exception e) {
				bfskDataRate = 0.0f;
			}
			if (bfskNDataBits == 0) {  bfskNDataBits = 8; }
		}
		if(bfskDataRate == 0.0f) {
			fLogger.fatal ("Invalid modem mode and/or data rate [%s].", modemMode);
			CommandLine.usage(this, System.out);
		}

		if (rawNBits!=0) {                   // Outpu mode: binary raw
			bfskNStartBits = 0;
			bfskNStopBits = 0;
			bfskNDataBits = rawNBits;
		}

		if(bfskDataRate >= 400) {
			/*
			 * Bell 202:     baud=1200 mark=1200 space=2200
			 */
			autodetectShift = (int)-(bfskDataRate * 5 / 6);
			if(bfskMarkF == 0) {
				bfskMarkF = bfskDataRate / 2 + 600;
			}
			if(bfskSpaceF == 0) {
				bfskSpaceF = bfskMarkF - autodetectShift;
			}
			if(bandWidth == 0) {
				bandWidth = 200;
			}
		} else if(bfskDataRate >= 100) {
			/*
			 * Bell 103:     baud=300 mark=1270 space=1070
			 */
			autodetectShift = 200;
			if(bfskMarkF == 0) {
				bfskMarkF = 1270f;
			}
			if(bfskSpaceF == 0) {
				bfskSpaceF = bfskMarkF - autodetectShift;
			}
			if(bandWidth == 0) {
				bandWidth = 50; // close enough
			}
		}  else {
			/*
			 * RTTY:     baud=45.45 mark/space=variable shift=-170
			 */
			autodetectShift = 170;
			if(bfskMarkF == 0) {
				bfskMarkF = 1585f;
			}
			if(bfskSpaceF == 0) {
				bfskSpaceF = bfskMarkF - autodetectShift;
			}
			if(bandWidth == 0) {
				bandWidth = 10; // FIXME chosen arbitrarily
			}
		}

		// defaults: 1 start bit, 1 stop bit
		if(bfskNStartBits < 0) { bfskNStartBits = 1; }
		if(bfskNStopBits < 0) { bfskNStopBits = 1.0f; }

		// n databits plus bfsk_startbit start bits plus bfsk_nstopbit stop bits:
		int bfskFrameNBits = (int)(bfskNDataBits + bfskNStartBits + bfskNStopBits);
		if(bfskFrameNBits > 64) {
			fLogger.fatal("Total number of bits per frame must be <= 64, got [%d]", bfskNDataBits);
			return 1;
		}

		// do not transmit any leader tone if no start bits
		if(bfskNStartBits == 0) {
			txLeaderBitsLen = 0;
		}

		if(bfskInvertedFreqs) {
			float t = bfskMarkF;
			bfskMarkF = bfskSpaceF;
			bfskSpaceF = t;
		}

		/* restrict band_width to <= data rate (FIXME?) */
		if(bandWidth > bfskDataRate) {
			bandWidth = bfskDataRate;
		}

		// sanitize confidence search limit
		if(fskConfidenceSearchLimit < fskConfidenceThreshold) {
			fskConfidenceSearchLimit = fskConfidenceThreshold;
		}

		return 0;
	}
}
