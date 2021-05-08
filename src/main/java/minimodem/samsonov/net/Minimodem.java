package minimodem.samsonov.net;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "minimodem", mixinStandardHelpOptions = true, version = "j 0.0.1",
		description = "Minimodem Java port")
class Minimodem implements Callable<Integer> {
	private static final Logger logger = LogManager.getLogger("Minimodem");

	@Option(names = {"-V", "--version"}) 									private boolean oVersion = false;
	static class OpMode {
		@Option(names = {"-t", "--tx", "--transmit", "--write"}, required = true) 		private boolean oTransmit;
		@Option(names = {"-r", "--rx", "--receive", "--read"}, required = true) 		private boolean oReceive;
	}
	@CommandLine.ArgGroup(exclusive = true, multiplicity = "1") 						OpMode oOpmode;
	@Option(names = {"-c", "--confidence"}, paramLabel = "min-confidence-threshold") 	private int oConfidence;
	@Option(names = {"-l", "--limit"}, paramLabel = "{max-confidence-search-limit}")  	private int oLimit;
	@Option(names = {"-a", "--auto-carrier"}) 								private boolean oAutoCarrier;
	@Option(names = {"-i", "--inverted"}) 									private boolean oInverted;
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
	@Option(names = {"-M", "--mark"}, paramLabel = "{mark_freq}")			private float oMark;
	@Option(names = {"-S", "--space"}, paramLabel = "{space_freq}")			private float oSpace;
	@Option(names = {"--startbits"}, paramLabel = "{n}")					private int oStartBits;
	@Option(names = {"--stopbits"},	paramLabel = "{n.n}")					private String oStopBits;
	@Option(names = {"--invert-start-stop"})								private boolean oInvertStartStop;
	@Option(names = {"--sync-byte"}, paramLabel = "{0xXX}")					private String oSyncByte;
	@Option(names = {"-q", "--quiet"})										private boolean oQuite;
	@Option(names = {"-A", "--alsa"}, arity = "0..1",
			paramLabel = "[plughw:X,Y]", fallbackValue = "")				private String oALSA;
	@Option(names = {"-s", "sndio"}, arity = "0..1",
			paramLabel = "[device]", fallbackValue = "")					private String oDevice;
	@Option(names = {"-R", "--samplerate"}, paramLabel = "{rate}")			private int oSampleRate;
	@Option(names = {"--lut"}, paramLabel = "{tx_sin_table_len}")			private int oLut;
	@Option(names = {"--float-samples"})									private boolean oFloatSamples;
	@Option(names = {"--rx-one"})											private boolean oRxOne;
	@Option(names = {"--benchmarks"})										private boolean oBenchmarks;
	@Option(names = {"--binary-output"})									private boolean oBinaryOutput;
	@Option(names = {"--binary-raw"}, paramLabel = "{nbits}")				private int oBinaryRaw;
	@Option(names = {"--print-filter"})										private boolean oPrintFilter;
	@Option(names = {"--print-eot"})										private boolean oPrintEot;
	@Option(names = {"--Xrxnoise"}, paramLabel = "{rx-noise-factor}")		private boolean oXrxNoise;
	@Option(names = {"--tx-carrier"})										private boolean oTxCarrier;

	@Parameters(index = "0", paramLabel = "{baudmod}", description =
		"		{baudmode}%n" +
		"	    any_number_N       Bell-like      N bps --ascii%n" +
		"		    1200       Bell202     1200 bps --ascii%n" +
		"		     300       Bell103      300 bps --ascii%n" +
		"		    rtty       RTTY       45.45 bps --baudot --stopbits=1.5%n" +
		"		     tdd       TTY/TDD    45.45 bps --baudot --stopbits=2.0%n" +
		"		    same       NOAA SAME 520.83 bps --sync-byte=0xAB ...%n" +
		"		callerid       Bell202 CID 1200 bps%n" +
		"     uic{-train,-ground}       UIC-751-3 Train/Ground 600 bps%n")	private String oBaudmode;

	@Override
	public Integer call() {
		System.out.println("Hello, World");
		logger.trace("We've just greeted the user!");
		logger.debug("We've just greeted the user!");
		logger.info("We've just greeted the user!");
		logger.warn("We've just greeted the user!");
		logger.error("We've just greeted the user!");
		logger.fatal("We've just greeted the user!");

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
	}}
