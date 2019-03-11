# Panama Foreign Benchmarks.

I've performed some comparison between Foreign interface and [JNA](https://github.com/java-native-access/jna) (which is JNI under the bonnet). Also, I've throw pure-Java implementation of same task to the mix, but as algorithms are not exactly the same, it is more for reference.

First, I want to describe what I measure. If you know what FFT is and how FFTW library work, you could skip these parts.
 

## What is FFT?

Fast Fourier Transform, or FFT for short, is family of algorithms to calculate Discrete Fourier Transform, which is used widely in many areas, as digital signal processing, imaging, and others. Main parameter of this algorithm is size of transform. Size defines amount of data needed for one algorithm execution, and frequency resolution you could achieve.

Theoretically, these algorithms have `O(n*log(n))` complexity in size of transform (naive implementation of Discrete Fourier Transform is `O(n^2)`). But real-life implementations have many subtleties, and constant (which is not accounted by O-notation) could wary widely, and depends both on transform size and implementation details. Most straightforward «textbook» implementations works well only with power-of-2 sizes, for example.


## What is FFTW3 which is used in this benchmark?

Best general-purpose implementation of FFT algorithms is [FFTW3](https://www.fftw3.org) library. It is highly-optimized and very sophisticated library, written in pure C. It supports very clever methods to decompose work at hand, and its API works like this:

1. You ask library to create "transform plan", and pass all details about transform you need: its size (main one is parameter, of course), input and output arrays (yes, at planning stage) and some flags, like could library destroy data in input array or is should use temporary array as working memory area. This "planning" stage could take some time, as large transform sizes could be decomposed into sub-transforms by multitude of means, and library do some internal benchmarking to select best way. This operation must be doe only once for given transform size and input-output configuration.
1. You call transform with created plan multiple times on different data. Here are two possibilities: 
  * You use the same input and output arrays, that were used at plan creation time. It is best way, according to FFTW documentation.
  * You could pass new input and output arrays for each execution. These arrays must be aligned as arrays passed to planning stage.


## JTransforms

[JTransforms](https://github.com/wendykierp/JTransforms) is pure-Java FFT library which is well-optimised but is not as «state-of-art» as FFTW. It has much simpler planner, it could not have vectorized algorithms, and it depends on HotSpot heavily. But it works on native Java `double[]` arrays and don't need any data preparation to use.
  
I've added JTransforms to benchmark to have some baseline, as it has no overhead to call, and at small transform sizes call overhead dominates. It is interesting to see, when more effective native code overcomes Java-Native call overhead.


## What do I measure?

I want to measure «real-world» calculation of FFTs of different size in Java. It means, I want to have input data and output data in Java-native `double[]` arrays, as I emulate Java calculations, and I assume, that other data processing is pure-Java, so data need to be in Java-native data structures.

I measure two versions: «in-place» (input and output array are the one array) and «out-of-place» (input and output arrays are different ones) transforms.

I measure only simplest, power-of-2 sizes, from `2^4` (`16`) to `2^18` (`131072`) complex numbers, so, there are twice as much `double` elements in arrays.

Additionally, I measure pure FFT speed, for reference. What does it mean? Pure FFT speed doesn't include transfer of data from binding-dependent structure from native Java array and back. Some bindings could avoid this transfer (which is pure overhead) and for them «Pure FFT» and «Full Calculation» are the same.
 
I do *not* measure plan creation!


## Benchmarks implemented

1. `JNAAllocated` — this is JNA-created bindings, which use off-heap allocated `java.nio` native buffers `DoubleBuffer`. It copy data in and out Java arrays with `DoubleBuffer` API.
1. `JNAWrapperd` — this is JNA-created bindings, which use `DoubleBuffer`-wrapped Java-native arrays and uses «execute with new buffers» FFTW3 API to allow using Java-natibve arrays in native code.
1. `JTransforms` — this is simple pure-Java implementation with JTransforms library. As JTransforms are always in-place transforms, it needs to copy input data to make input array intact.
1.  `Panama` — this is Panama/jextract-created bindings, which use Panama `Array<Double>` with Panama allocator and such. It needs to copy in and out data before and after transform.

You could see all benchmarks and framework here:

[https://github.com/blacklion/panama-benchmarks](https://github.com/blacklion/panama-benchmarks)
 
I plan to add more benchmarks in the future for different Panama sub-projects.


## Results.

All results are collected on [panama-jdk-13-44](https://download.java.net/java/early_access/panama/44/openjdk-13-foreign+44_windows-x64_bin.zip) on Windows/10, on i5-4570, with single thread.

Full results could be seen at this [google sheet](https://docs.google.com/spreadsheets/d/1-7O16o-38yFIVx-LWTIpVxRnXCvdvs4NmNXLThT2KHI/edit?usp=sharing).

Please note, there are 3 Sheets: first one is plain CSV report from JMH, second one is some analysis of out-of-place tasks and third one is same analysis for in-place tasks.

Please, note, that charts are effectively in Log-Log scale.

Some summary of results: 

1. Data copying to and from Panama arrays is enormously expensive and dominate "full" execution time for any size. It makes Panama implementation always slower that any other (including Pure Java).
1. Pure FFT calls cost the same for JNA and Panama.
1. JNA with new (wrapped) arrays is slower than JNA with off-heap buffers for pure FFT. It needs to be investigated further, I think, it is due to array alignment and selection (or absence of) of vectorized code by FFTW. But at sizes larger than ~2048 full speed becomes equal.
1. Pure-Java JTransforms for this simple case (power-of-2 transform) is not much worse for large sizes and much better for small sizes. Data copy in/out and native calls are still very, very expensive.
