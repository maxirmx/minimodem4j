package minimodem;

import java.io.File;
import java.util.concurrent.Callable;

import minimodem.arghelpers.*;
import minimodem.databits.*;
import minimodem.simpleaudio.SaAudioFile;
import minimodem.simpleaudio.SaDirection;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static minimodem.simpleaudio.SaDirection.*;

import minimodem.simpleaudio.SaToneGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(name = "minimodem4j", mixinStandardHelpOptions = true, version = "Version: 0.0.1",
		description = "Minimodem Java port", usageHelpWidth = 120)
class Minimodem implements Callable<Integer> {
	private static final Logger fLogger = LogManager.getFormatterLogger("Minimodem");

	static class OpMode {
		@Option(names = {"-t", "--tx", "--transmit", "--write"}, required = true) 		protected boolean oTx = false;
		@Option(names = {"-r", "--rx", "--receive", "--read"}, required = true) 		protected boolean oRx = false;
	}
	@CommandLine.ArgGroup()
	protected OpMode opMode;
	@Option(names = {"-c", "--confidence"}, paramLabel = "{min-confidence-threshold}", defaultValue = "1.5f",
			description = "Signal-to-noise squelch control, The minimum SNR-ish confidence level seen as 'a signal'")
			protected float fskConfidenceThreshold;
	@Option(names = {"-l", "--limit"}, paramLabel = "{max-confidence-search-limit}", defaultValue = "2.3f",
			description = "Performance vs. quality control. If we find a frame with " +
					"confidence > confidence_search_limit, quit searching for a better frame. " +
					"confidence_search_limit has dramatic effect on performance (high value yields " +
					"low performance, but higher decode quality, for noisy or hard-to-discern signals " +
					"(Bell 103, or skewed rates).")
			protected float fskConfidenceSearchLimit;
	@Option(names = {"-a", "--auto-carrier"},
			parameterConsumer = AutoDetectCarrierParameterConsumer.class,
			description = "Automatically detect mark and space frequencies from carrier.")
			protected float carrierAutodetectThreshold = 0.0f;
	@Option(names = {"-i", "--inverted"},
			description = "Invert the mark and space frequencies (applies whether the " +
					"frequencies are defaults, discovered by --auto-carrier, or specified manually).")
			protected boolean bfskInvertedFreqs = false;
	static class DataBits {
		@Option(names = {"-8", "--ascii"}, description = "ASCII 8-N-1") 	protected boolean ascii8N1 = false;
		@Option(names = {"-7"}, description = "ASCII 7-N-1") 				protected boolean ascii7N1 = false;
		@Option(names = {"-5", "--baudot"}, description = "Baudot 5-N-1") 	protected boolean baudot5N1 = false;
	}
	@CommandLine.ArgGroup()
	protected DataBits dataBits;
	@Option(names = {"-u", "--usos"},  paramLabel = "{0|1}",
			description="Enable or disable USOS (UnShift on Space) for baudot (defaule: 1, i.e.: enable). +" +
					" USOS is a convention whereby a SPACE also implies a switch to LETTERS character " +
					"set.  This switch can be used to disable USOS so that streams not " +
					"adhering to this convention can be decoded correctly (e.g. German " +
					"DWD's RTTY maritime weather report and forecast).",
			parameterConsumer = USoSParameterConsumer.class)
			protected boolean baudotUSOS = true;
	@Option(names = {"--msb-first"}) 										protected boolean bfskMsbFirst=false;
	@Option(names = {"-f", "--file"}, paramLabel = "{filename.flac}", arity = "1",
			description="Encode or decode an audio file (extension sets audio format)")
			protected File file=null;
	@Option(names = {"-b", "--bandwidth"}, paramLabel = "{rx_bandwidth}",
			parameterConsumer = BandwidthParameterConsumer.class)
			protected float bandWidth = 0.0f;
	@Option(names = {"-v", "--volume"}, paramLabel = "{amplitude or 'E'}",
			parameterConsumer = VolumeParameterConsumer.class,
			description = "Sets the generated signal amplitude (default is 1.0).  As a special case " +
				"useful for testing, the value 'E' sets the amplitude to the very small value" +
				"FLT_EPSILON.  (This option applies to --tx mode only).")
			protected float txAmplitude = 1.0f;
	@Option(names = {"-M", "--mark"}, paramLabel = "{mark_freq}",
			description = "Sets the mark frequency. Shall be >0).",
			parameterConsumer = MarkFreqParameterConsumer.class)
		 	protected float bfskMarkF = 0.0f;
	@Option(names = {"-S", "--space"}, paramLabel = "{space_freq}",
			description = "Sets the space frequency. Shall be >0).",
			parameterConsumer = SpaceFreqParameterConsumer.class)
			protected float bfskSpaceF = 0.0f;
	@Option(names = {"--startbits"}, paramLabel = "{n}",
			description = "Sets the number of start bits (default is 1 for most baudmodes. Shall be <=20).",
			parameterConsumer = NStartBitsParameterConsumer.class)
			protected int bfskNStartBits = -1;
	@Option(names = {"--stopbits"},	paramLabel = "{n.n}",
			description = "Sets the number of stop bits (default is 1.0 for most baudmodes. Shall be >=0).",
			parameterConsumer = NStopBitsParameterConsumer.class)
			protected float bfskNStopBits = -1.0f;
	@Option(names = {"--invert-start-stop"})											protected boolean invertStartStop;
	@Option(names = {"--sync-byte"}, paramLabel = "{0xXX}")								protected int bfskSyncByte = -1;
	@Option(names = {"-q", "--quiet"},
			description="Do not report CARRIER / NOCARRIER or signal analysis metrics.")
			protected boolean quiteMode;
	@Option(names = {"-R", "--samplerate"}, paramLabel = "{rate}",
			description = "Set the audio sample rate (default rate is 48000 Hz).",
			parameterConsumer = SampleRateParameterConsumer.class)
			protected int sampleRate = 48000;
	@Option(names = {"--lut"}, paramLabel = "{tx_sin_table_len}",
			description="Minimodem uses a precomputed sine wave lookup table of 1024 elements," +
					"or the size specified here.  Use --lut=0 to disable the use of " +
					"the sine wave lookup table.  (This option applies to --tx mode only).")
			protected int txSinTableLen = 4096;
	@Option(names = {"--float-samples"},
			description = "Generate 32-bit floating-point format audio samples, instead of the " +
						"default 16-bit signed integer format (applies to --tx mode only; " +
						"--rx mode always uses 32-bit floating-point).")
			protected boolean floatSamples;
	@Option(names = {"--rx-one"},
			description = "Quit after the first carrier/no-carrier event (applies to --rx mode only).")
			protected boolean rxOne;
	@Option(names = {"--benchmarks"})													protected boolean oBenchmarks;
	@Option(names = {"--binary-output"},
			description="Print received data bits as raw binary output using characters '0' and '1'. " +
					"The bits are printed in the order they are received.  Framing bits (start " +
					"and stop bits) are omitted from the output. (This option applies to --rx mode only).")
			protected boolean outputModeBinary;
	@Option(names = {"--binary-raw"}, paramLabel = "{nbits}",
			description="Print all received bits (data bits and any framing bits) as raw binary output " +
					"using characters '0' and '1'.  Framing bits are not interpreted, but simply " +
					"passed through to the output.  The bits are printed in the order they are " +
					"received, in lines {nbits} wide.  So in order to display a standard 8-N-1 " +
					"bitstream (8 databits + 1 start bit + 1 stop bit), use '--binary-raw 10' " +
					" or a multiple of 10. (This option applies to --rx mode only).")
			protected int outputModeRawNBits=0;
	@Option(names = {"--print-filter"})													protected boolean oPrintFilter;
	@Option(names = {"--print-eot"},
			description="Print '### EOT' to log after each transmit completes." )
			protected boolean txPrintEot = false;
	@Option(names = {"--Xrxnoise"}, paramLabel = "{rx-noise-factor}")			protected float rxNoiseFactor = 0.0f;

	@Parameters(index = "0", paramLabel = "{baudmod}",
			description =
		"   any_number_N       Bell-like      N bps --ascii%n" +
		"		    1200       Bell202     1200 bps --ascii%n" +
		"		     300       Bell103      300 bps --ascii%n" +
		"		    rtty       RTTY       45.45 bps --baudot --stopbits=1.5%n" +
		"		     tdd       TTY/TDD    45.45 bps --baudot --stopbits=2.0%n" +
		"		    same       NOAA SAME 520.83 bps --sync-byte=0xAB ...%n" +
		"		callerid       Bell202 CID 1200 bps%n" +
		"uic{-train,-ground}   UIC-751-3 Train/Ground 600 bps%n%n")	protected String modemMode;

	protected SaDirection txMode;
	protected float bfskDataRate = 0.0f;           //  Data rate (baud rate) is deduced from the parameter above

	protected int bfskDoTxSyncBytes = 0;
	protected int bfskNDataBits = 0;
	protected int bfskFrameNBits = 0;

	protected static  int nChannels = 1; // FIXME: only works with one channel

	private boolean	bfskDoRxSync = false;
	private int txLeaderBitsLen = 2;
	private byte[] expectDataString = null;
	private int expectNBits;
	private int autodetectShift;


	protected IEncodeDecode bfskDatabitsEncodeDecode = new DataBitsAscii8();    // Character encoder/decoder

	/**
	 * main
	 * Implements Callable, so parsing, error handling and handling user
	 * requests for usage help or version are done by picocli with one line of code.
	 * @param args as usual :)
	 */
	public static void main(String... args) {
		fLogger.info("Running minimodem4j ...");
		int exitCode = new CommandLine(new Minimodem()).execute(args);
		System.exit(exitCode);
	}

	/**
	 * call - the shadow entry point for picocli
	 * @return application exit code
	 */
	@Override
	public Integer call() {
		fLogger.info("Configuring ...");
		int cfgRc = configure();
		if (cfgRc != 0) {
			return cfgRc;
		}
		return (txMode.equals(SA_RECEIVE))?transmit():receive();
	}

	protected int transmit() {
		SaToneGenerator toneGenerator = new SaToneGenerator();
		toneGenerator.toneInit(txSinTableLen, txAmplitude);
		fLogger.info("Tone Generator is ready");

		SaAudioFile saOut = new SaAudioFile();
		if (!saOut.open(file,
				floatSamples?PCM_FLOAT:PCM_SIGNED,
				SA_TRANSMIT,
				sampleRate,
				nChannels,
				bfskMsbFirst)) {
			return -1;
		}

		fLogger.info("Audio file is ready");

		fLogger.info("Transmitting ...");
		Transmitter tx = new Transmitter(saOut,
				toneGenerator,
				bfskDataRate,
				bfskMarkF,
				bfskSpaceF,
				bfskNDataBits,
				bfskNStartBits,
				bfskNStopBits,
				invertStartStop,
				bfskMsbFirst,
				bfskDoTxSyncBytes,
				bfskSyncByte,
				txPrintEot);

		tx.fskTransmitStdin(bfskDatabitsEncodeDecode);
		saOut.close();
		return 0;
	}

	protected int receive() {
		fLogger.info("Receiving ...");
		SaAudioFile saIn = new SaAudioFile();
		if (!saIn.open(file,
				floatSamples?PCM_FLOAT:PCM_SIGNED,
				SA_RECEIVE,
				sampleRate,
				nChannels,
				bfskMsbFirst)) {
			return -1;
		}
		saIn.setRxNoise(rxNoiseFactor);
		fLogger.info("Audio file is ready");

		Receiver rx = new Receiver(saIn,
				bfskDataRate,
				bfskNStartBits,
				bfskNStopBits,
				bfskNDataBits,
				bfskFrameNBits,
				invertStartStop,
				bfskDoRxSync,
				bfskSyncByte,
				bfskMarkF,
				bfskSpaceF,
				bandWidth,
				carrierAutodetectThreshold,
				autodetectShift,
				bfskInvertedFreqs,
				fskConfidenceSearchLimit,
				fskConfidenceThreshold);

		rx.configure(expectDataString);
		rx.receive(quiteMode, rxOne);

		return 0;
	}

	/**
	 * configure
	 * Builds modem configuration from the command line parameters
	 * @return 0 if successful, application return code otherwise
	 */
	protected int configure() {
		if (opMode != null && opMode.oTx) {
			txMode = SA_RECEIVE;
		} else {
			txMode = SA_TRANSMIT;
		}
		if (dataBits != null) {
			if (dataBits.ascii8N1) {                    // ASCII 8-N-1
				bfskNDataBits = 8;
			} else if (dataBits.ascii7N1) {                // ASCII 7-N-1
				bfskNDataBits = 7;
			} else if (dataBits.baudot5N1) {            // Baudot 5-N-1
				bfskNDataBits = 5;
				bfskDatabitsEncodeDecode = new DataBitsBaudot(baudotUSOS);
			}
		}
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
			bfskDoTxSyncBytes = 16;
			bfskSyncByte = 0xAB;
			bfskMarkF = (float)(2083.0 + 1 / 3.0);
			bfskSpaceF = 1562.5f;
			bandWidth = bfskDataRate;
		} else if(modemMode.equalsIgnoreCase("caller")) {
			if(txMode.equals(SA_RECEIVE)){
				fLogger.fatal ("callerid --tx mode is not supported.");
				return 1;
			}
			if(carrierAutodetectThreshold > 0.0f) {
				fLogger.warn ("callerid with --auto-carrier is not recommended.");
			}
			bfskDataRate = 1200f;
			bfskNDataBits = 8;
			bfskDatabitsEncodeDecode = new DataBitsCallerId();
		} else if(modemMode.toLowerCase().startsWith("uic")) {
			if(txMode.equals(SA_RECEIVE)) {
				fLogger.fatal ("uic-751-3 --tx mode is not supported.");
				return 1;
			}
			bfskDataRate = 600;
			bfskNDataBits = 39;
			bfskMarkF = 1300f;
			bfskSpaceF = 1700f;
			bfskNStartBits = 8;
			bfskNStopBits = 0;

			expectDataString = "11110010ddddddddddddddddddddddddddddddddddddddd\0".getBytes();
			expectNBits = 47;

			if(modemMode.equalsIgnoreCase("uic-train")) {
				bfskDatabitsEncodeDecode = new DataBitsUicGround2Train();
			} else if (modemMode.equalsIgnoreCase("uic-ground")) {
				bfskDatabitsEncodeDecode = new DataBitsUicTrain2Ground();
			} else{
				bfskDataRate = 0.0f;
			}
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

		if (outputModeBinary || outputModeRawNBits!=0) {
			bfskDatabitsEncodeDecode = new DataBitsBinary();
		}
		if (outputModeRawNBits!=0) {
			bfskNStartBits = 0;
			bfskNStopBits = 0;
			bfskNDataBits = outputModeRawNBits;
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
		if(bfskNStartBits < 0) {
			bfskNStartBits = 1;
		}
		if(bfskNStopBits < 0) {
			bfskNStopBits = 1.0f;
		}

		// n databits plus bfsk_startbit start bits plus bfsk_nstopbit stop bits:
		bfskFrameNBits = (int)(bfskNDataBits + bfskNStartBits + bfskNStopBits);
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
