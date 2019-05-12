package foreign;

import org.openjdk.jmh.annotations.*;

/**
 * @author Lev Serebryakov
 */
@Fork(2)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 5)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class FFTBenchmarkParams {
	// @Param({"16", "17", "32", "37", "64", "67", "128", "131", "256", "257", "512", "521", "1024", "1031", "2048", "2053", "4096", "4099", "8192", "8209", "16384", "16411", "32768", "32771", "65536", "65537"})
	@Param({"16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536", "131072"})
	public int size;

	@Param({"false", "true"})
	public boolean inPlace;
}
