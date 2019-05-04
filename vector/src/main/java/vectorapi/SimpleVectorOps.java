package vectorapi;

/**
 * @author Lev Serebryakov
 */

import jdk.incubator.vector.*;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Lev Serebryakov
 */
public class SimpleVectorOps {

    public static void main(String[] args) {
        final VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
        final VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));
        float[] a = new float[PFS.length()];
        float[] b = new float[a.length];
        for (int i = 0; i < a.length; i++)
            a[i] = i + 1.0f;
        FloatVector.fromArray(PFS, a, 0).add(100.0f).intoArray(b, 0);
        System.out.println(Arrays.toString(b));

        VectorShuffle<Float> sh = VectorShuffle.fromValues(PFS, 0, -1, 1, -1, 2, -1, 3, -1);
        VectorMask<Float> im = VectorMask.fromValues(PFS, false, true, false, true, false, true, false, true);
        VectorMask<Float> re = VectorMask.fromValues(PFS, true, false, true, false, true, false, true, false);
        FloatVector.fromArray(PFS, a, 0).rearrange(FloatVector.zero(PFS), sh, im).intoArray(b, 0);
        System.out.println(Arrays.toString(b));

        FloatVector.fromArray(PFS, a, 0, re, new int[] {0, 0, 1, 1, 2, 2, 3, 3}, 0).intoArray(b, 0);
        System.out.println(Arrays.toString(b));

        VectorMask<Float> m = VectorMask.fromValues(PFS, true, false, true, false, true, false, true, false);
        FloatVector v = FloatVector.fromArray(PFS, new float[] { 1.0f, 10.0f, 2.0f, 20.0f, 3.0f, 30.0f, 4.0f, 40.0f }, 0);
        float sum = v.mulLanes(m);
        System.out.println(sum);

        // One more test

        int l = PFS.length() - 1;
        VectorShuffle<Float> SHUFFLE_RV_TO_CV_RE_ZERO = VectorShuffle.fromValues(PFS, 0, l, 1, l, 2, l, 3, l);
        FloatVector xx = FloatVector.fromArray(PFS2, a, 0).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE_ZERO);
        System.out.println(Arrays.toString(xx.toArray()));

        // And more
        FloatVector _10log10 = xx.abs().log10().mul(10.0f);
        System.out.println(Arrays.toString(_10log10.toArray()));

    }
}
