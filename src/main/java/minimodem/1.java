 https://www.nayuki.io/res/free-small-fft-in-multiple-languages/Fft.java

public class Translation {
	public static float fskFindFrame(FskPlan[] fskp, float[] samples, int frameNsamples_U, int tryFirstSample_U, int tryMaxNsamples_U, int tryStepNsamples_U, float tryConfidenceSearchLimit, String8 expectBitsString, long[] bitsOutp_U, float[] amplOutp, int[] frameStartOutp_U) {
		int expectNBits = expectBitsString.length();
		assert expectNBits <= 64; // protect fsk_frame_analyze()
		float samplesPerBit = Integer.toUnsignedLong(frameNsamples_U) / expectNBits;
		// try_step_nsamples = 1;	// pedantic TEST
		int bestT_U = 0;
		float bestC = 0.0f, bestA = 0.0f;
		long bestBits_U = 0;
// Scan the frame positions starting with the one try_first_sample,
    // alternating between a step above that, a step below that, above, below,
    // and so on, until we've scanned the whole try_max_nsamples range.
		for(int j = 0;; j++) {
			int up = j % 2 != 0 ? 1 : -1;
			int t = tryFirstSample_U + up * ((j + 1) / 2) * tryStepNsamples_U;
			if(t >= tryMaxNsamples_U) {
				break;
			}
			if(t < 0) {
				continue;
			}
			float c;
			FloatContainer amplOut = FloatContainer.fromData(0.0f);
			LongContainer bitsOut_U = LongContainer.fromData(0);
			debugLog(cs8("try fsk_frame_analyze at t=%d\n"), t);
			c = fskFrameAnalyze(fskp, nnc(samples).shift(t), samplesPerBit, expectNBits, expectBitsString, bitsOut_U, amplOut);
			if(bestC < c) {
				bestT_U = t;
				bestC = c;
				bestA = amplOut;
				bestBits_U = bitsOut_U;
				// If we find a frame with confidence > try_confidence_search_limit
				// quit searching.
				if(bestC >= tryConfidenceSearchLimit) {
					break;
				}
		}
		bitsOutp_U[0] = bestBits_U;
		amplOutp[0] = bestA;
		frameStartOutp_U[0] = bestT_U;

		float confidence = bestC;

		if(confidence == 0) {
			return 0;
		}
#ifdef
	byte bitchar_U;
    // FIXME? hardcoded chop off framing bits for debug
    // Hmmm... we have now way to  distinguish between:
    // 		8-bit data with no start/stopbits == 8 bits
    // 		5-bit with prevstop+start+stop == 8 bits
		switch(expectNBits) {
		case 11:
			bitchar_U = (byte)(bitsOutp_U[0] >>> 2 & 0xFF);
			break;
		case 8:
		default:
			bitchar_U = (byte)(bitsOutp_U[0] & 0xFF);
			break;
		}
		debugLog(cs8("FSK_FRAME bits='"));
		for(int j = 0; j < expectNBits; j++) {
			debugLog(cs8("%c"), (bitsOutp_U[0] >>> j & 1) != 0 ? '1' : '0');
		}
		debugLog(cs8("' datum='%c' (0x%02x)   c=%f  a=%f  t=%u\n"), isprint(bitchar_U) != 0 || isspace(bitchar_U) != 0 ? Byte.toUnsignedInt(bitchar_U) : '.', bitchar_U, (MethodRef0<Integer>)ImplicitDeclarations::confidence, (MethodRef0<Integer>)ImplicitDeclarations::bestA, bestT_U);
#endif		
		return confidence;
	}



	public static int fskDetectCarrier(FskPlan[] fskp, float[] samples, int nsamples_U, float minMagThreshold) {
		assert Integer.compareUnsigned(nsamples_U, fskp[0].getFftsize()) <= 0;
		int paNchannels_U = 1; // FIXME
		fskp[0].getFftin().fill(0, fskp[0].getFftsize() * paNchannels_U, 0);
		nnc(fskp[0].getFftin()).copyFrom(samples, nsamples_U);
		fftwfExecute(fskp[0].getFftplan());
		float magscalar = 1.0f / (Integer.toUnsignedLong(nsamples_U) / 2.0f);
		float maxMag = 0.0f;
		int maxMagBand = -1;
		int i = 1; /* start detection at the first non-DC band */
		int nbands = fskp[0].getNbands_U();
		for(; i < nbands; i++) {
			float mag = bandMag(fskp[0].getFftout(), i, magscalar);
			if(mag < minMagThreshold) {
				continue;
			}
			if(maxMag < mag) {
				maxMag = mag;
				maxMagBand = i;
			}
		}
		if(maxMagBand < 0) {
			return -1;
		}

		return maxMagBand;
	}
	public static int fskDetectCarrier(FskPlan[] fskp, float[] samples, int nsamples_U, float minMagThreshold) {
		assert Integer.compareUnsigned(nsamples_U, fskp[0].getFftsize()) <= 0;

		int paNchannels_U = 1; // FIXME
		fskp[0].getFftin().fill(0, fskp[0].getFftsize() * paNchannels_U, 0);
		nnc(fskp[0].getFftin()).copyFrom(samples, nsamples_U);
		fftwfExecute(fskp[0].getFftplan());
		float magscalar = 1.0f / (Integer.toUnsignedLong(nsamples_U) / 2.0f);
		float maxMag = 0.0f;
		int maxMagBand = -1;
		int i = 1; /* start detection at the first non-DC band */
		int nbands = fskp[0].getNbands_U();
		return 0;
	}
}

	

}
