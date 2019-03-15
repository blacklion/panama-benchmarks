package vector;

/**
 * @author Lev Serebryakov
 */

import org.openjdk.jmh.annotations.*;

/**
 * @author Lev Serebryakov
 */
@Fork(2)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 5)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class SimpleVectorOps {
}
