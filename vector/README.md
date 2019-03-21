# Panama Vector API Test and Benchmarks.

This project contains two implementations of `Math`-like library for math operations over vectors of real (`float`) and complex (pairs of `float`) numbers. Operations are all arithmetic operations plus some specific ones, as exponentiation and complex exponentiation, logarithms, and such. «Vector» means long (theoretically infinite) sequence of numbers, and not 3D or 4D vectors from linear algebra.

One implementation is named `VO` (for «Vector Operations», obviously) and it is imported from real-liefe (but unreleased) project. This implementation is pure-java, without any special or unreleased APIs.

Second implementation is named `VOVec` (guess what does it mean) is rather naive implementation of subset of same API with new Vector API from Panama project.

## Data representation

Library support 4 types of data and uses it in method names consistently. It is:

Real Scalars
:          Real scalar is represented as single `float` value. If it is input