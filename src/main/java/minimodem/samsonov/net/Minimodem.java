package minimodem.samsonov.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/* This is a simple Java program.
FileName : "HelloWorld.java". */
class Minimodem
{
	private static final Logger logger = LogManager.getLogger("Minimodem");

	// Your program begins with a call to main().
	// Prints "Hello, World" to the terminal window.
	public static void main(String[] args)
	{
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

	}
}
