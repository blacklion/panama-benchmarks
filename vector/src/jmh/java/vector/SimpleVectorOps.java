package vector;

/**
 * @author Lev Serebryakov
 */

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;
import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Lev Serebryakov
 */
@Fork(2)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 10, time = 5)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class SimpleVectorOps {

    public static void main(String[] args) {
        final FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
        float[] a = new float[PFS.length()];
        float[] b = new float[a.length];
        for (int i = 0; i < a.length; i++)
            a[i] = i + 1.0f;
        FloatVector.fromArray(PFS, a, 0).add(100.0f).intoArray(b, 0);
        System.out.println(Arrays.toString(b));

        Vector.Shuffle<Float> sh = FloatVector.shuffleFromValues(PFS, 0, -1, 1, -1, 2, -1, 3, -1);
        Vector.Mask<Float> im = FloatVector.maskFromValues(PFS, false, true, false, true, false, true, false, true);
        Vector.Mask<Float> re = FloatVector.maskFromValues(PFS, true, false, true, false, true, false, true, false);
        FloatVector.fromArray(PFS, a, 0).rearrange(PFS.zero(), sh, im).intoArray(b, 0);
        System.out.println(Arrays.toString(b));

        FloatVector.fromArray(PFS, a, 0, re, new int[] {0, 0, 1, 1, 2, 2, 3, 3}, 0).intoArray(b, 0);
        System.out.println(Arrays.toString(b));

        FloatVector.Mask m = FloatVector.maskFromValues(PFS, true, false, true, false, true, false, true, false);
        FloatVector v = FloatVector.fromArray(PFS, new float[] { 1.0f, 10.0f, 2.0f, 20.0f, 3.0f, 30.0f, 4.0f, 40.0f }, 0);
        float sum = v.mulAll(m);
        System.out.println(sum);
    }
}
