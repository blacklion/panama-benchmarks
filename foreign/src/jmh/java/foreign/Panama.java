package foreign;

import org.fftw3.fftw3;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.foreign.NativeTypes;
import java.foreign.layout.Padding;
import java.foreign.memory.*;

import static org.fftw3.fftw3_h.*;

/**
 * @author Lev Serebryakov
 */
public class Panama extends FFTBenchmarkParams {

	@State(org.openjdk.jmh.annotations.Scope.Thread)
	public static class BenchState extends FFTState {
		java.foreign.Scope scope = null;
		Array<Double> i;
		Array<Double> o;
		Pointer<fftw3.fftw_plan_s> p = null;

		@Setup(Level.Trial)
		public void Setup(BenchmarkParams params) {
			Setup(Integer.parseInt(params.getParam("size")), Boolean.parseBoolean(params.getParam("inPlace")));
		}

		public void Setup(int size, boolean inPlace) {
			super.Setup(size, inPlace);
			scope = scope().fork();
			i = scope.allocateArray(NativeTypes.DOUBLE, size * 2);
			if (inPlace)
				o = i;
			else
				o = scope.allocateArray(NativeTypes.DOUBLE, size * 2);
			Array<Array<Double>> pi = i.elementPointer().cast(NativeTypes.VOID).cast(NativeTypes.DOUBLE.array(2)).withSize(size);
			Array<Array<Double>> po = o.elementPointer().cast(NativeTypes.VOID).cast(NativeTypes.DOUBLE.array(2)).withSize(size);
			p = fftw_plan_dft_1d(size, pi.elementPointer(), po.elementPointer(), -1, 0);
		}

		@TearDown(Level.Trial)
		public void TearDown() {
			if (p != null)
				fftw_destroy_plan(p);
			if (scope != null)
				scope.close();
		}
	}

	@Benchmark
	public void FFTOnly(BenchState state) {
		fftw_execute(state.p);
	}

	@Benchmark
	public void Full(BenchState state) {
		// In place or not we need to copy data in and copy data out
		// maybe, between two arrays and not four, but still
		for (int i = 0 ; i < state.size * 2; i++)
			state.i.set(i, state.ji[0]);
		fftw_execute(state.p);
		for (int i = 0 ; i < state.size * 2; i++)
			state.jo[i] = state.o.get(i);
	}
}
