/*****************************************************************************
 * Copyright (c) 2019, Lev Serebryakov <lev@serebryakov.spb.ru>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ****************************************************************************/

package vector.specific;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;
import org.openjdk.jmh.annotations.*;

/** @noinspection PointlessArithmeticExpression, WeakerAccess, CStyleArrayDeclaration */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class RVExpi {
    private final static int MAX_SIZE = 1031;

    // EPV on my system is 8
    @Param({"3", "4", "7", "8", "16", "20", "23", "1024", "1031"})
    public int size;

    private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
    private final static int EPV = PFS.length();
    private final static FloatVector.FloatSpecies PFS2 = FloatVector.species(Vector.Shape.forBitSize(PFS.bitSize() / 2));
    private final static int EPV2 = PFS2.length();

    private final static Vector.Shuffle<Float> SHUFFLE_RV_TO_CV_BOTH = FloatVector.shuffle(PFS, i -> i / 2);;
    private final static Vector.Shuffle<Float> SHUFFLE_RV_TO_CV_RE_LOW = FloatVector.shuffle(PFS, i -> (i % 2 == 0) ? (i / 2) : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_RV_TO_CV_IM_LOW = FloatVector.shuffle(PFS, i-> (i % 2 == 0) ? 0 : (i / 2));
    private final static Vector.Shuffle<Float> SHUFFLE_RV_TO_CV_RE_HIGH = FloatVector.shuffle(PFS, i -> (i % 2 == 0) ? (i / 2 + EPV / 2) : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_RV_TO_CV_IM_HIGH = FloatVector.shuffle(PFS, i -> (i % 2 == 0) ? 0 : (i / 2 + EPV / 2));

    private final static Vector.Mask<Float> MASK_C_IM;

    static {
        boolean[] alter = new boolean[EPV + 1];
        alter[0] = true;
        for (int i = 1; i < alter.length; i++)
            alter[i] = !alter[i-1];
        MASK_C_IM = FloatVector.maskFromArray(PFS, alter, 1);
    }

    private float x[];
    private float z[];

    @Setup(Level.Trial)
    public void Setup() {
        x = new float[MAX_SIZE];
        z = new float[MAX_SIZE * 2];

        for (int i = 0; i < x.length; i++) {
            x[i] = (float)(Math.random() * 2.0 - 1.0);
        }
    }

    @Benchmark
    public void pfs2_reshape_ops() {
        rv_expi1(z, 0, x, 0, size);
    }

    @Benchmark
    public void pfs2_ops_reshape() {
        rv_expi1a(z, 0, x, 0, size);
    }

    @Benchmark
    public void pfs() {
        rv_expi2(z, 0, x, 0, size);
    }

    @Benchmark
    public void pfs_pfs2_reshape_ops() {
        rv_expi3(z, 0, x, 0, size);
    }

    @Benchmark
    public void pfs_pfs2_ops_reshape() {
        rv_expi3a(z, 0, x, 0, size);
    }


    public static void rv_expi1(float z[], int zOffset, float x[], int xOffset, int count) {
        zOffset <<= 1;
        while (count >= EPV2) {
            final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            final FloatVector vzre = vx.cos();
            final FloatVector vzim = vx.sin();
            vzre.blend(vzim, MASK_C_IM).intoArray(z, zOffset);

            xOffset += EPV2;
            zOffset += EPV;
            count -= EPV2;
        }
        while (count-- > 0) {
            z[zOffset + 0] = (float)Math.cos(x[xOffset]);
            z[zOffset + 1] = (float)Math.sin(x[xOffset]);
            xOffset += 1;
            zOffset += 2;
        }
    }

    public static void rv_expi1a(float z[], int zOffset, float x[], int xOffset, int count) {
        zOffset <<= 1;
        while (count >= EPV2) {
            final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset);
            final FloatVector vzre = vx.cos().reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            final FloatVector vzim = vx.sin().reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            vzre.blend(vzim, MASK_C_IM).intoArray(z, zOffset);

            xOffset += EPV2;
            zOffset += EPV;
            count -= EPV2;
        }
        while (count-- > 0) {
            z[zOffset + 0] = (float)Math.cos(x[xOffset]);
            z[zOffset + 1] = (float)Math.sin(x[xOffset]);
            xOffset += 1;
            zOffset += 2;
        }
    }

    public static void rv_expi2(float z[], int zOffset, float x[], int xOffset, int count) {
        zOffset <<= 1;
        while (count >= EPV) {
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
            final FloatVector vzre = vx.cos();
            final FloatVector vzim = vx.sin();
            // And now we should combine TWO z vectors from re/im, as they are packed without empty slots
            vzre.rearrange(SHUFFLE_RV_TO_CV_RE_LOW).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_LOW), MASK_C_IM).intoArray(z, zOffset);
            vzre.rearrange(SHUFFLE_RV_TO_CV_RE_HIGH).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_HIGH), MASK_C_IM).intoArray(z, zOffset + EPV);

            // We stored twice as much complex numbers
            xOffset += EPV;
            zOffset += EPV * 2;
            count -= EPV;
        }
        while (count-- > 0) {
            z[zOffset + 0] = (float)Math.cos(x[xOffset]);
            z[zOffset + 1] = (float)Math.sin(x[xOffset]);
            xOffset += 1;
            zOffset += 2;
        }
    }

    public static void rv_expi3(float z[], int zOffset, float x[], int xOffset, int count) {
        zOffset <<= 1;
        while (count >= EPV) {
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
            final FloatVector vzre = vx.cos();
            final FloatVector vzim = vx.sin();
            // And now we should combine TWO z vectors from re/im, as they are packed without empty slots
            vzre.rearrange(SHUFFLE_RV_TO_CV_RE_LOW).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_LOW), MASK_C_IM).intoArray(z, zOffset);
            vzre.rearrange(SHUFFLE_RV_TO_CV_RE_HIGH).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_HIGH), MASK_C_IM).intoArray(z, zOffset + EPV);

            // We stored twice as much complex numbers
            xOffset += EPV;
            zOffset += EPV * 2;
            count -= EPV;
        }
        // If we have half-vector
        if (count >= EPV2) {
            final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            final FloatVector vzre = vx.cos();
            final FloatVector vzim = vx.sin();
            vzre.blend(vzim, MASK_C_IM).intoArray(z, zOffset);

            xOffset += EPV2;
            zOffset += EPV;
            count -= EPV2;
        }
        while (count-- > 0) {
            z[zOffset + 0] = (float)Math.cos(x[xOffset]);
            z[zOffset + 1] = (float)Math.sin(x[xOffset]);
            xOffset += 1;
            zOffset += 2;
        }
    }

    public static void rv_expi3a(float z[], int zOffset, float x[], int xOffset, int count) {
        zOffset <<= 1;
        while (count >= EPV) {
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
            final FloatVector vzre = vx.cos();
            final FloatVector vzim = vx.sin();
            // And now we should combine TWO z vectors from re/im, as they are packed without empty slots
            vzre.rearrange(SHUFFLE_RV_TO_CV_RE_LOW).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_LOW), MASK_C_IM).intoArray(z, zOffset);
            vzre.rearrange(SHUFFLE_RV_TO_CV_RE_HIGH).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_HIGH), MASK_C_IM).intoArray(z, zOffset + EPV);

            // We stored twice as much complex numbers
            xOffset += EPV;
            zOffset += EPV * 2;
            count -= EPV;
        }
        // If we have half-vector
        if (count >= EPV2) {
            final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset);
            final FloatVector vzre = vx.cos().reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            final FloatVector vzim = vx.sin().reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            vzre.blend(vzim, MASK_C_IM).intoArray(z, zOffset);

            xOffset += EPV2;
            zOffset += EPV;
            count -= EPV2;
        }
        while (count-- > 0) {
            z[zOffset + 0] = (float)Math.cos(x[xOffset]);
            z[zOffset + 1] = (float)Math.sin(x[xOffset]);
            xOffset += 1;
            zOffset += 2;
        }
    }
}
