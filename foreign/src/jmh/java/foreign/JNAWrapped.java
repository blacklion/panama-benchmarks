package foreign;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.nio.DoubleBuffer;

/**
 * @author Lev Serebryakov
 */
public class JNAWrapped extends FFTBenchmarkParams {
	@State(org.openjdk.jmh.annotations.Scope.Thread)
	public static class BenchState extends FFTState {
		DoubleBuffer i;
		DoubleBuffer o;
		FFTW3Library.fftw_plan p = null;

		@Setup(Level.Trial)
		public void Setup(BenchmarkParams params) {
			Setup(Integer.parseInt(params.getParam("size")), Boolean.parseBoolean(params.getParam("inPlace")));
		}

		public void Setup(int size, boolean inPlace) {
			super.Setup(size, inPlace);

			i = DoubleBuffer.wrap(ji);
			// inPlace too
			o = DoubleBuffer.wrap(jo);

			p = FFTW3Library.INSTANCE.fftw_plan_dft_1d(size, i, o, -1, 0);
		}

		@TearDown(Level.Trial)
		public void TearDown() {
			if (p != null)
				FFTW3Library.INSTANCE.fftw_destroy_plan(p);
		}
	}

	@Benchmark
	public void Full(BenchState state) {
		FFTW3Library.INSTANCE.fftw_execute_dft(state.p, state.i, state.o);
	}
}
