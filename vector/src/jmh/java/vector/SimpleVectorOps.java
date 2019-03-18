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

        Random rg = new Random();
        float re0 = rg.nextFloat();
        float im0 = rg.nextFloat();
        float re1 = rg.nextFloat();
        float im1 = rg.nextFloat();
        while (true) {
            float abs0abs = Math.abs(re0) + Math.abs(im0);
            float abs0sq = (float)Math.sqrt(re0 * re0 + im0 * im0);
            float abs1abs = Math.abs(re1) + Math.abs(im1);
            float abs1sq = (float)Math.sqrt(re1 * re1 + im1 * im1);
            if (Float.compare(abs0abs, abs1abs) != Float.compare(abs0sq, abs1sq)) {
                System.out.println("Woooooow! " + re0 + "+ i" + im0 + " <-> " + re1 + "+ i" + im0);
                System.out.println("0: " + re0 + "+ i" + im0 + " -> " + abs0abs + ", " + abs0sq);
                System.out.println("1: " + re1 + "+ i" + im1 + " -> " + abs1abs + ", " + abs1sq);
            }
            re1 = re0; im1 = im0;
            re0 = rg.nextFloat();
            im0 = rg.nextFloat();
        }
    }
}
