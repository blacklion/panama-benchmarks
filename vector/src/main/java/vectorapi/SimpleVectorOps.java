package vectorapi;

/**
 * @author Lev Serebryakov
 */

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Lev Serebryakov
 */
public class SimpleVectorOps {

    public static void main(String[] args) {
        final FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
        final FloatVector.FloatSpecies PFS2 = FloatVector.species(Vector.Shape.forBitSize(PFS.bitSize() / 2));
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

        // One more test

        int l = PFS.length() - 1;
        FloatVector.Shuffle<Float> SHUFFLE_RV_TO_CV_RE_ZERO = FloatVector.shuffleFromValues(PFS, 0, l, 1, l, 2, l, 3, l);
        FloatVector xx = FloatVector.fromArray(PFS2, a, 0).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE_ZERO);
        System.out.println(Arrays.toString(xx.toArray()));

        // And more
        FloatVector _10log10 = xx.abs().log10().mul(10.0f);
        System.out.println(Arrays.toString(_10log10.toArray()));

    }
}
