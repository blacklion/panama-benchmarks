# Panama Vector API Test and Benchmarks.
This project contains two implementations of `Math`-like API for math operations over vectors of real (`float`) and complex (pairs of `float`) numbers. Operations are all arithmetic operations plus some specific ones, as exponentiation and complex exponentiation, logarithms, and such.

In context of this API «vector» means long (theoretically infinite) sequence of numbers, and not 3D or 4D vectors from linear algebra, as this library is written for digital signal processing, not for 3D graphics.

One implementation is named `VO` (for «Vector Operations», obviously) and it is imported from real-liefe (but unreleased) project. This implementation is pure-java, without using of any special or unreleased APIs.

Second implementation is named `VOVec` (guess what does it mean) is rather naive implementation of subset of same API with new Vector API from Panama project.

## Data representation.
API supports 4 types of data and uses them in method names consistently. These types are:

 - **Real Scalar** — it is simple single real value. Real scalar is represented as single `float` primitive. It is named `rs` in API. When it is argument for operation, it is represented as simple `float` parameter and when it is result of operation, it is return type of method implementing this operation.
 - **Real Vector** — it is (potentially unlimited) sequence of real values. In practice it is represented as array of `float`, `float[]`. It is named `rv` in API. When it is argument or result of operation, it is represented in the same way: as two parameters for a method, first one is `float[]` and second one `int` offset to this array to be able to load arguments and store results anywhere in array, not only in beginning.
 - **Complex Scalar** — it is single complex value, represented normally in cartesian form. In API it is represented as array of two floats, where first element contains real part and second element contains imaginary part. It is named `cs` in API. When it is argument or result of operation, it is represented in the same way: as one or two parameters for a method. Some methods take only `float[]` array as parameter for input or output complex scalar, and some methods have variant which takes `float[]` and `int`, as for vectors, but uses only two elements of this array at given complex offset (about meaning of offsets for complex data see later).
 - **Complex Vector** — it is (potentially unlimited) sequence of complex values, represented normally in cartesian form. It is represented as array of `float`, `float[]` with even length, where even elements (starting from `0`) contain real parts and odd elements (startiing from `1`) contain imaginary parts. It is named `cv` in API. When it is argument or result of operation, it is represented in the same way: as two parameters for a method, one `float[]` for numbers and second `int` for staarting offset, as for real vectors. As in case of complex scalar, this offset is complex one, see below.

All offsets into vectors, used in this API, is calculated in terms of logical elements, not in terms of native Java `float[]` elements. It means, that for complex vectors offsets are in complex numbers (pairs) and offset `1` means complex number which is physically stored in elements `[2]` (real part) and `[3]` (imaginary part) of corresponding `float[]` array.

## API naming convention.
All methods of this API is named according to strict rules.

 - Each method implements one operation.
 - All operations (with one special exception, which will be described later) are either *binary* or *unary*.
 - At least one argument to method must be *vector*, either real or complex.
 - If type of result of operation match type of first argument of operation, operation could be performed in place, when result override first argument.
 - All binary operations are named as...