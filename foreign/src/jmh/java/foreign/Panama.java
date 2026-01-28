package foreign;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.lang.foreign.*;

/**
 * @author Lev Serebryakov
 */
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class Panama extends FFTBenchmarkParams {
	@Param({"false", "true"})
	public boolean aligned;

	@State(org.openjdk.jmh.annotations.Scope.Thread)
	public static class BenchState extends FFTState {

		FFTW3PanamaLibrary library;
		Arena arena = null;
		MemorySegment i;
		MemorySegment o;
		MemorySegment plan = null;

		@Setup(Level.Trial)
		public void Setup(BenchmarkParams params) {
			Setup(
					Integer.parseInt(params.getParam("size")),
					Boolean.parseBoolean(params.getParam("inPlace")),
					Boolean.parseBoolean(params.getParam("aligned")));
		}

		public void Setup(int size, boolean inPlace, boolean aligned) {
			super.Setup(size, inPlace);
			library = new FFTW3PanamaLibrary();
			arena = Arena.ofShared();
			i = allocateCFVData(size, aligned);
			if (inPlace)
				o = i;
			else
				o = allocateCFVData(size, aligned);

			plan = library.planDFT1D(size, i, o, -1, 0);
		}

		@TearDown(Level.Trial)
		public void TearDown() {
			if (library != null) {
				if (plan != null) {
					library.destroyPlan(plan);
				}
				library.close();
			}
			if (arena != null)
				arena.close();
		}

		private MemorySegment allocateCFVData(int count, boolean aligned) {
			return arena.allocate(
					MemoryLayout
							.sequenceLayout(count, FFTW3PanamaLibrary.DOUBLE_COMPLEX)
							.withByteAlignment(aligned ? 64 : FFTW3PanamaLibrary.DOUBLE_COMPLEX.byteAlignment())
			);
		}
	}

	@Benchmark
	public void FFTOnly(BenchState state) {
		state.library.execute(state.plan);
	}

	@Benchmark
	public void Full(BenchState state) {
		// In place or not we need to copy data in and copy data out
		// maybe, between two arrays and not four, but still
		for (int i = 0; i < state.size * 2; i++)
			state.i.setAtIndex(ValueLayout.JAVA_DOUBLE, i, state.ji[0]);
		state.library.execute(state.plan);
		for (int i = 0; i < state.size * 2; i++)
			state.jo[i] = state.o.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
	}

	@Benchmark
	public void FullBatch(BenchState state) {
		MemorySegment.copy(state.ji, 0, state.i, ValueLayout.JAVA_DOUBLE, 0L, state.size * 2);
		state.library.execute(state.plan);
		MemorySegment.copy(state.o, ValueLayout.JAVA_DOUBLE, 0L, state.jo, 0, state.size * 2);
	}
}
