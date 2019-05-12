package foreign;

import org.jtransforms.fft.DoubleFFT_1D;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;

/**
 * @author Lev Serebryakov
 */
public class JTransforms extends FFTBenchmarkParams {
	@State(org.openjdk.jmh.annotations.Scope.Thread)
	public static class BenchState extends FFTState {
		DoubleFFT_1D p;

		@Setup(Level.Trial)
		public void Setup(BenchmarkParams params) {
			Setup(Integer.parseInt(params.getParam("size")), Boolean.parseBoolean(params.getParam("inPlace")));
		}

		public void Setup(int size, boolean inPlace) {
			super.Setup(size, inPlace);
			p = new DoubleFFT_1D(size);
		}
	}

	@Benchmark
	public void FFTOnly(BenchState state) {
		// Simply transform
		state.p.complexForward(state.jo);
	}

	@Benchmark
	public void Full(BenchState state) {
		// Prepare out for in-place transform
		if (!state.inPlace)
			System.arraycopy(state.ji, 0, state.jo, 0, state.ji.length);
		state.p.complexForward(state.jo);
	}
}
