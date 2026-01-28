package foreign;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

class FFTW3PanamaLibrary implements AutoCloseable {
    public static final String PANAMA_LIBRARY_NAME = "libfftw3-3";

    static MemoryLayout DOUBLE_COMPLEX = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("re"),
            ValueLayout.JAVA_DOUBLE.withName("im")
    );

    private final Arena arena;
    private final MethodHandle fftw_plan_dft_1d;
    private final MethodHandle fftw_execute;
    private final MethodHandle fftw_destroy_plan;

    // Resolve all functions and such
    FFTW3PanamaLibrary() {
        arena = Arena.ofShared();

        Linker linker = Linker.nativeLinker();
        SymbolLookup fftw3lib = SymbolLookup.libraryLookup(PANAMA_LIBRARY_NAME, arena);
        fftw_plan_dft_1d = linker.downcallHandle(
                fftw3lib.find("fftw_plan_dft_1d").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,  ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
        );

        fftw_execute = linker.downcallHandle(
                fftw3lib.find("fftw_execute").orElseThrow(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );

        fftw_destroy_plan = linker.downcallHandle(
                fftw3lib.find("fftw_destroy_plan").orElseThrow(),
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );
    }

    public MemorySegment planDFT1D(int size, MemorySegment in, MemorySegment out, int sign, int flags) {
        try {
            return (MemorySegment)fftw_plan_dft_1d.invokeExact(size, in, out, sign, flags);
        } catch (Throwable e) {
            return null;
        }
    }

    public void execute(MemorySegment plan) {
        try {
            fftw_execute.invokeExact(plan);
        } catch (Throwable _) {}
    }

    public void destroyPlan(MemorySegment plan) {
        try {
            fftw_destroy_plan.invokeExact(plan);
        } catch (Throwable _) {}
    }

    @Override
    public void close() {
        arena.close();
    }
}
