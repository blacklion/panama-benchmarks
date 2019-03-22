# Panama Vector API Test and Benchmarks.
## Goals
This project has two goals:

 1. To create set of vector (see below) operations using Vector API from [Project Panama](http://openjdk.java.net/projects/panama/).
 2. To benchmark Vector API against non-vectorized Java implementations of same operations.

## Introduction.
This project contains two implementations of `Math`-like API for math operations over vectors of real (`float`) and complex (pairs of `float`) numbers. Operations are all arithmetic operations plus some specific ones, as exponentiation and complex exponentiation, logarithms, and such.

In context of this API «vector» means long (theoretically infinite) sequence of numbers, and not 3D or 4D vectors from linear algebra, as this library is written for digital signal processing, not for 3D graphics.

One implementation is named `VO` (for «Vector Operations», obviously) and it is imported from real-life (but unreleased) project. This implementation is pure-java, without using of any special or unreleased APIs.

Second implementation is named `VOVec` (guess what does it mean) is rather naive implementation of subset of same API with new Vector API from Panama project.


## API data model.
API supports 4 types of data and uses them in method names consistently. These types are:

 - **Real Scalar** — it is simple single real value. Real scalar is represented as single `float` primitive. It is named `rs` in API. When it is argument for operation, it is represented as simple `float` parameter and when it is result of operation, it is return type of method implementing this operation.
 - **Real Vector** — it is (potentially unlimited) sequence of real values. In practice it is represented as array of `float`, `float[]`. It is named `rv` in API. When it is argument or result of operation, it is represented in the same way: as two parameters for a method, first one is `float[]` and second one `int` offset to this array to be able to load arguments and store results anywhere in array, not only in beginning.
 - **Complex Scalar** — it is single complex value, represented normally in Cartesian form. In API it is represented as array of two floats, where first element contains real part and second element contains imaginary part. It is named `cs` in API. When it is argument or result of operation, it is represented in the same way: as one or two parameters for a method. Some methods take only `float[]` array as parameter for input or output complex scalar, and some methods have variant which takes `float[]` and `int`, as for vectors, but uses only two elements of this array at given complex offset (about meaning of offsets for complex data see later).
 - **Complex Vector** — it is (potentially unlimited) sequence of complex values, represented normally in Cartesian form. It is represented as array of `float`, `float[]` with even length, where even elements (starting from `0`) contain real parts and odd elements (starting from `1`) contain imaginary parts. It is named `cv` in API. When it is argument or result of operation, it is represented in the same way: as two parameters for a method, one `float[]` for numbers and second `int` for starting offset, as for real vectors. As in case of complex scalar, this offset is complex one, see below.

All offsets into vectors, used in this API, is calculated in terms of logical elements, not in terms of native Java `float[]` elements. It means, that for complex vectors offsets are in complex numbers (pairs) and offset `1` means complex number which is physically stored in elements `[2]` (real part) and `[3]` (imaginary part) of corresponding `float[]` array.

## API operational model.
All methods of this API are designed according to several base guidelines. These guidelines are:

 - Each method implements one operation.
 - All implemented operations (with one special exception, which will be described later) are either *binary* or *unary*.
 - At least one argument to method must be *vector*, either real or complex. There is not scalar-scalar operations.
 - Binary operations with two vector arguments consume same number of from both arguments.
 - Binary operations with vector result produce same number of result elements as it consume from arguments.
 - If type of result of operation match type of first argument of operation, operation could be performed *in-place*, when result override first argument.
 - If operation is commutative, only one combination of arguments are implemented.

This model is analogous to implementation of arithmetical operations in C, C++ and Java. Here are some illustrations to terms used in guidelines above, which uses Java operations for simple variables.

 - Unary operation «logical not» used out-of-place:
```java
boolean x = true;
boolean z;
z = !x;
```
 - Unary operation «negation» used in-place:
```java
float z = 1.3f;
z = -z;
```
 - Binary operation «plus» used out-of-place:
```java
int x = 1;
int y = 2;
int z;
z = x + y;
```
 - Binary operation «multiplication» used in-place:
```java
long z = 64;
long x = 5;
z *= x;
```

Operations between vector and scalar could be seen of as operation between provided vector and vector which is filled with provided scalar. So, addition of vector of real numbers and single real number is performed by adding this single real number to each element of real vector.

### Linear combination — only one non-unary and non-binary operation.
One exclusion to rules give above is operation which calculate linear combination of two vector arguments. This operation require four parameters: two vector arguments and two scalar coefficients, one per vector argument. This operation calculates linear combination of its vector arguments weighted by scalar constants, like this:
```java
final float a1 = 0.1f;
final float a2 = 0.9f;
float x = 10.0f;
float y = 20.0f;
float z;
z = a1 * x + a2 * y;
```

## API naming and arguments convention.
All methods of this API are named systematically. Name consist of types of arguments, operation name and optional suffix which describes implementation details. Type of result is not described, but could be deduced from type of arguments and operation semantic. 

Several operations are implemented with overloaded methods, to implement special often used cases. For example, all operations which takes or return complex scalar are implemented with and without additional offset argument for scalar parameter or return value. Some other operations with vector arguments are additionally implemented without offset for one of vector arguments, as they are often used with zero offset. Such additional implementations don't have special names or suffixes and differs from regular implementations by Java method signature.

### Suffixes.
Suffixes are common for all types of operation. Suffix consist of one or more letters from set `w`, `i` and `f`. Each letter have its own meaning:

 - **i** — it is **i**n-place implementation. Default implementation is out-of-place.
 - **w** — it is «**w**rapping» implementation (see below). Default implementation doesn't perform any offset wrapping or boundary checks.
 - **f** — this implementation uses custom «**f**ast» implementation of trigonometry functions. Default implementation uses standard Java functions from `Math` class.

By default method without suffix implements out-of-place operation which doesn't perform offset wrapping (see below) and uses standard Java trigonometric functions if needed.

### Common argument convention.
All operation's vector arguments or result vector are passed to implementing methods as two Java arguments: array of floats and integer offset. Offset's name is always `<array-name>Offset`. There are several exceptions to this rule, but all these exceptions are additional to methods which follow this rule and provided only for convenience.

All operation's complex scalar arguments or results are passed to implementing methods in two different ways: one array of floats or array of floats and integer offsets. If offset is passed, it is named as `<array-name>Offset`. Some operations with complex scalars are implemented by several methods which are distinguished only by signature: with and without offsets for complex scalar arguments and result values.

All opertion's real scalar arguments are passed as simple float values.

All methods for operations, which result is anything but real scalars or integers, have `void` as return type. Real scalars are returned as `float` return type and integers as `int` return type.

If result of operation is not integer or real scalar, place for result is passed as first method's argument which is named `z`.

First argument is called `x` and second one is named `y` for out-of-place implementations of operation..

First argument is called `z` (and coincides with result) and second one is named `x` for in-place implementations of operation..

All methods takes `int count` as last argument, which determine number of vector elements to process. Please note, that as with offsets for complex vectors, `count` means number of complex numbers in case of complex vectors, not number of `float` elements of underlying arrays.

Please note, that vector arguments and results of operations are represented as two Java method's arguments, one array and one offset.

### Offsets wrapping.
All operations are implemented in two versions: default one and offset-wrapping. Wrapping implementations have `w` suffix in their names.

By default, implementation doesn't check passed offsets and number of elements (`count`). In case of negative offset or offset which becomes larger than passed array, `IndexOutOfBoundsException` runtime exception will be thrown by JVM. It allows to have fastest possible implementation with no branching in tight loop.

Wrapping implementations uses all offsets modulo corresponding array length, which allows to perform operations which crosses array boundaries. In case when offset becomes larger than array size it is wrapped to zero. Implementations with wrapping is much slower, as they contains branching in tight loop, but they are exception-safe.

Wrapping implementations have one additional caveat: they hide programmer's error. Wrapping implementations will work without exceptions even if any array is smaller than number of elements to process or produce, as they will be go through such small array again and again.

### Unary operations.
All methods which implements unary operations are named as `<argument-type>_<operation-name>[_<suffix>]`. `<argument-type>` is one of `rs`, `rv`, `cs` or `cv`, which are described in [API data model](#api-data-model). `<operation-name>` is short mnemonic operation name, as `neg` or `log`.

Implementations of unary operations use `z` and (for out-of-place implementations) `x` as argument names.

So, generic in-place operations should be seen as `z = op(z)`, and out-of-place operations are `z = op(x)`.

Here are some examples with explanation:

`float rv_max(float x[], int xOffset, int count)` — this is implementation of searching of maximum (`<operation-name>` is `max`) of vector of reals (`<argument-type>` is `rv`). It takes vector of reals `x` as argument and return real scalar as its return value.

`void rv_abs_i(float z[], int zOffset, int count)` — this is implementation of taking absolute values (`<operation-name>` is `abs`) of vector of reals (`<argument-type>` is `rv`), which returns result in same vector (`<suffix>` is `i`). It takes vector of real numbers as argument `z` and places results in same vector.

`void cv_im_w(float z[], int zOffset, float x[], int xOffset, int count)` — this is implementation of extraction imaginary parts (`<operation-name>` is `im`) of vector of complex numbers (`<argument-type>` is `cv`), which is implemented with warping of offsets (`<suffix>` is `w`). It takes vector of complex numbers `x` and returns vector of real numbers `z`.

`void cv_arg_f(float z[], int zOffset, float x[], int xOffset, int count)` — this is implementation of taking argument (`<operation-name>` is `arg`) of vector of complex numbers (`argument-type` is `cv`), which returns vector of real numbers and uses fast implementation (`<suffix>` is `f`) of `atan2` function. It takes vector of complex numbers `x` and returns vector of real numbers `z`.

Please note, that last two operations could not be implemented in-place, as types of argument and result are different.

### Binary operations.
All methods which implements binary operations are named as `<left-argument-type>_<operation-name>_<right-argument-type>[_<suffix>]`. `<left-argument-type>` and `<right-argument-type>` are one of `rs`, `rv`, `cs` or `cv`, which are described in [API data model](#api-data-model). `<operation-name>` is short mnemonic operation name, as `add` or `div`.

Implementations of binary operations use `z`, `x` and `y` as argument names for out-of-place implementations and `z` and `x` for in-place implementations.

So, generic in-place operations should be seen as as `z = op(z, x)`, and out-of-place operations are `z = op(x, y)`.

Here are some examples with explanation:

`void rv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count)` — this is implementation of addition (`<operation-name>` is `add`) of real vector (`<left-argument-type>` is `rv`) and real scalar (`<right-argument-type>` is `rs`), it adds second argument `y` to each element of vector `x` and return result as vector into `z`. Please note, that there is no `rs_add_rv()` implementation, because addition is commutative.

`void rv_sub_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count)` — this is implementation of substraction (`<operation-name>` is `sub`) of complex vector (`<right-argument-type>` is `cv`) from vector of real numbers (`<left-argument-type>` is `rv`). It takes minuend as argument `x`, subtrahend as argument `y` and return difference into `z`. This method checks and wraps offsets. As substraction is not commutative, here are `cv_sub_rv_w()` method too.

`float rv_dot_rv(float x[], int xOffset, float y[], int yOffset, int count)` — this is implementation if dot product (`<operation-name>` is `dot`) of two real vectors (both `<left-argument-type>` and `<right-argument-type>` are `rv`). It takes two vectors as `x` and `y` and returns result, which is real scalar, as its return value.

`void rv_dot_cv(float z[], float x[], int xOffset, float y[], int yOffset, int count)` — this is implementation if dot product (`<operation-name>` is `dot`) of real vector (`<left-argument-type>` is `rv`) and complex vector (`<right-argument-type>` is `cv`). It takes two vectors, `x` (real one) and `y` (complex one) and return complex scalar as `z`. There is second version of this method, with same name but additional `int zOffset` parameter after `z`.

`void rv_min_rv_i(float z[], int zOffset, float x[], int xOffset, int count)` — this is implementation of selection of maximum (`<operation-name>` is `max`) between elements of two real vectors (both `<left-argument-type>` and `<right-argument-type>` are `rv`), which is implemented in-place. It takes two vectors `z` and `x` as arguments and returns result into vector `z`.

### Linear combination.
Linear combination takes four arguments: two pairs of vector and scalar. Name for linear combination implementation looks like `<left-vector-type>_<left-scalar-type>_lin_<right-vector-type>_<right-scalar-type>[_<suffix>]`.

All generic argument naming rules apply to linear combination implementations, and additionally left scalar is named `a1` and right scalar is named `a2` in all implementations. Scalars are passed right after corresponding vector arguments.

### Additional considerations.
#### Dual operations.
Some operations exist both as unary and binary. Examples are `min` and `max`. Unary version of such operations folds passed vector and binary apply given operation to pairs of elements from left and right arguments. So, unary `min` will find minimal element in given vector and binary `min` will produce vector which contains smallest elements in each pair of elements from input vectors.

#### Non-standard operations.
There are some unusual operations.

 - `10log10` and `20log10` calculates power quantities, `10 × log₁₀(abs(x))` and `20 × log₁₀(abs(x))` respectively, for reals and complex numbers. It has binary form which additionally divide `x` by real scalar `base`.
 - `r2p` converts binary numbers from cartesina (rectangular) form to polar one. Result is stored in single array, with modulus in even elements and argument in odd elements of array. There is in-place implementation of this operation too.
 - `p2r` is inverse of `r2p` and converts complex numbers from polar form to standard Cartesian one.
 - `cpy` is simple copy operation.
 - `rev` is vector reversal.
 - `cvt` converts vector of real numbers to vector of complex numbers, it sets real parts of output to input values and imaginary parts to zero.

## Overview of two implementations.
### Implementation `VO`.
[`VO`](src/main/java/VO.java) is first and «reference» implementation of described API. It is pure Java 8 implementation, which implements all possible variants of all operations. Each operation which could be implemented in-place is implemented both in-place and out-of-place, and each implementation has both default and wrapping varaint. All trigonometry-based operations have standard and fast versions.

This implementation is imported from real (unpublished yet) project. It is thoroughly tested in its mother project, and considered fully correct. There are additional class `FastTrig` which implements very fast (but not-to-last-ulp-exact) trigonometric functions to support `_f` implementations of trigonometric operations in `VO`.

This implementation is used as both test reference and benchmark baseline for `VOVec`.

### Implementation `VOVec`.
[`VOVec`](src/main/java/VOVec.java) is work-in-progress rather naive implementation of subset of API implemented by `VO` with using Vector API from [Project Panama](http://openjdk.java.net/projects/panama/).

`VOVec` is written for simplicity of code now. It has following limitations in comparison to baselane `VO`:

 - It doesn't implement wrapping versions of API, as it is very hard to properly wrap any offsets and process vectors in batches simultaneously.
 - It doesn't implements all possible variant of `lin` operation.
 - It doesn't implement trivial operations `rev` and `cpy`.
 - It doesn't implement «fast» versions of trigonometric operations.
 - It doesn't implement additional varaints of some operations, which differs in Java signatures but not semantics from implemented ones. I.e methods with complex scalar and offset for it are not implemented.

As far I can see, only `lin` implementations and additional signatures are worth fixing. Wrapping operations will make code very complex and could wipe out all performance boost from SIMD operations, and adding fast SIMD trigonometry looks completely impossible.

There are several limitations which are `VOVec`-specific. These are:

 - Some basic operations could be expressed in several ways in Vector API. Now choice of implementation is arbitrary.
 - Implementation doesn't try to check alignment of input and output data and always process vectors in SIMD groups as `FloatVector.preferredSpecies()` starting from passed offsets and finish processing with code copied from `VO` implementation, if needed.

First item needs further investigation and benchmarking. All places where code could be written in several ways need separate benchmarks to take proper decision. I hope to do it in the future.

Second item looks inherent with current Vector API, as here is no way to check and/or enforce alignment of Java arrays.

All places in current implementation which needs separate benchmarking are marked with `//@TODO:` comment.

## Tests of `VOVec`
All `VOVec` methods are tested by comparison with results of `VO`. Tests are generated by perl script [genTest.pl](src/test/perl/genTests.pl), which process both `VO.java` and `VOVec.java` and generates [JUnit 5](https://junit.org/junit5/) [test suite](test/java/VectorTests.java). This test suite uses random data as input for each method.

Now `VOVec` pass all tests with full code coverage, and goal is to commit only changes which are 100% covered by tests.

## Benchmarks of `VOVec`
It is most interesting part.

All methods of `VOVec` are benchmarked against corresponding methods of `VO`. Benchmarks are generated by [genBenchmarks.pl](src/jmh/perl/genBenchmarks.pl) script, which is similar to [getTests.pl](#tests-of-vovec). Benchmarks are based on [JMH](https://openjdk.java.net/projects/code-tools/jmh/) framework.

Each benchmark is parametrized by batch size and start offset and contains loop to process vector 65536 elements in several calls to API, when each call process batch of given size. Default batch sizes are `16`, `1024` and `65536`. Offsets `0` and `1` is used for input vectors.

No results are provided now, as [needed branch](https://hg.openjdk.java.net/panama/dev/shortlog/01bb6f53b843) of [Panama repository](https://hg.openjdk.java.net/panama/dev/) is saw to be unstable right now.

## Future work
 1. Run benchmarks! Preliminary results are promising, but not conclusive and have strange anomalies.
 1. Run benchmarks on different hardware! I have access to desktop-class i7-6700K (AVX2-enabled) CPU only. It is interesting to look at AVX-512 enabled Intel's CPU and AArch64-based systems, too.
 1. Write benchmarks for different ways to implement same things with Vector API. Look for `//@TODO` comments in [VOVec.java](src/main/java/VOVec.java) for ideas.
 1. Investigate better algorithms for complex vector multiplication and division, if there are any. Division is extremely slow now.
