package foreign;

import org.fftw3.fftw3;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.foreign.NativeTypes;
import java.foreign.memory.Array;
import java.foreign.memory.Pointer;

import static org.fftw3.fftw3_h.*;

/**
 * @author Lev Serebryakov
 */
public class PanamaBulk extends FFTBenchmarkParams {

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
	public void Full(BenchState state) {
		Array.assign(state.ji, state.i);
		fftw_execute(state.p);
		Array.assign(state.o, state.jo);
	}
}
