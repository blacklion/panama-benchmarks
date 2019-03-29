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

import java.util.Arrays;

/** @noinspection CStyleArrayDeclaration, PointlessArithmeticExpression, WeakerAccess */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class CVP2R {
    private final static int MAX_SIZE = 1031;

    // EPV on my system is 8
    @Param({"3", "4", "7", "8", "16", "20", "23", "1024", "1031"})
    public int size;

    private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
    private final static int EPV = PFS.length();
    private final static FloatVector.FloatSpecies PFS2 = FloatVector.species(Vector.Shape.forBitSize(PFS.bitSize() / 2));
    private final static int EPV2 = PFS2.length();

    private final static Vector.Shuffle<Float> SHUFFLE_CV_SPREAD_IM = FloatVector.shuffle(PFS, i -> i - i % 2 + 1);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_SPREAD_RE = FloatVector.shuffle(PFS, i -> i - i % 2);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_FIRST = FloatVector.shuffle(PFS, i -> (i < EPV2) ? i * 2 : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_FIRST = FloatVector.shuffle(PFS, i -> (i < EPV2) ? i * 2 + 1 : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_SECOND = FloatVector.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_SECOND = FloatVector.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV + 1: 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST = FloatVector.shuffle(PFS, i -> (i % 2 == 0) ? i / 2 : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST = FloatVector.shuffle(PFS, i -> (i % 2 == 1) ? i / 2 : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND = FloatVector.shuffle(PFS, i -> (i % 2 == 0) ? i / 2 + EPV2 : 0);
    private final static Vector.Shuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND = FloatVector.shuffle(PFS, i -> (i % 2 == 1) ? i / 2 + EPV2 : 0);

    private final static Vector.Mask<Float> MASK_SECOND_HALF;
    private final static Vector.Mask<Float> MASK_C_IM;

    static {
        boolean[] alter = new boolean[EPV + 1];
        alter[0] = true;
        for (int i = 1; i < alter.length; i++)
            alter[i] = !alter[i-1];
        MASK_C_IM = FloatVector.maskFromArray(PFS, alter, 1);

        boolean[] sh = new boolean[EPV];
        Arrays.fill(sh, EPV / 2, sh.length, true);
        MASK_SECOND_HALF = FloatVector.maskFromArray(PFS, sh, 0);
    }

    private float z[];

    @Setup(Level.Trial)
    public void Setup() {
        z = new float[MAX_SIZE * 2];

        for (int i = 0; i < z.length; i++) {
            z[i] = (float)(Math.random() * 2.0 - 1.0);
        }
    }

    @Benchmark
    public void nv() {
        cv_p2r_i0(z, 0, size);
    }

    @Benchmark
    public void epv2() {
        cv_p2r_i1(z, 0, size);
    }

    @Benchmark
    public void epv() {
        cv_p2r_i2(z, 0, size);
    }

    @Benchmark
    public void epv_epv2() {
        cv_p2r_i3(z, 0, size);
    }


    public static void cv_p2r_i0(float z[], int zOffset, int count) {
   		float re, im;
   		zOffset <<= 1;
   		while (count-- > 0) {
   			re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
   			im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
   			z[zOffset + 0] = re;
   			z[zOffset + 1] = im;
   			zOffset += 2;
   		}
   	}

    public static void cv_p2r_i1(float z[], int zOffset, int count) {
   		zOffset <<= 1;

   		while (count >= EPV2) {
   			//@DONE: one load & two reshuffles are faster
   			//@TODO: check, do we need pack and process twice elements, and save result twice
   			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
   			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
   			// vzreezp is [(z[0].re, z[0].re), (z[1].re, z[1].re), ...]
   			final FloatVector vzre = vz.rearrange(SHUFFLE_CV_SPREAD_RE);
   			// vzim is [(z[0].im, z[0].im), (z[1].im, z[1].im), ...]
   			final FloatVector vzim = vz.rearrange(SHUFFLE_CV_SPREAD_IM);

   			//@DONE: .cos(MASK_C_IM)/.sin(MASK_C_RE) is much slower
   			final FloatVector vrre = vzre.mul(vzim.cos());
   			final FloatVector vrim = vzre.mul(vzim.sin());

   			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

   			zOffset += EPV;
   			count -= EPV2;
   		}

   		float re, im;
   		while (count-- > 0) {
   			re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
   			im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
   			z[zOffset + 0] = re;
   			z[zOffset + 1] = im;
   			zOffset += 2;
   		}
   	}

    public static void cv_p2r_i2(float z[], int zOffset, int count) {
        zOffset <<= 1;

        while (count >= EPV) {
            final FloatVector vz1 = FloatVector.fromArray(PFS, z, zOffset);
            final FloatVector vz2 = FloatVector.fromArray(PFS, z, zOffset + EPV);

            // Ger vz1re/vz1im till vz2 is loading
            final FloatVector vz1re = vz1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
            final FloatVector vz1im = vz1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

            // Ger vz2re/vz2im
            final FloatVector vz2re = vz2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
            final FloatVector vz2im = vz2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

            // Combine them
            final FloatVector vzre = vz1re.blend(vz2re, MASK_SECOND_HALF);
            final FloatVector vzim = vz1im.blend(vz2im, MASK_SECOND_HALF);

            final FloatVector vrre = vzre.mul(vzim.cos());
   			final FloatVector vrim = vzre.mul(vzim.sin());

            // And combine & store twice
            vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
            vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

            zOffset += EPV * 2;
            count -= EPV;
        }

        float abs, arg;
        while (count-- > 0) {
            abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
            arg = (float)Math.atan2(z[zOffset + 1], z[zOffset + 0]);
            z[zOffset + 0] = abs;
            z[zOffset + 1] = arg;
            zOffset += 2;
        }
    }

    public static void cv_p2r_i3(float z[], int zOffset, int count) {
        zOffset <<= 1;

        while (count >= EPV) {
            final FloatVector vz1 = FloatVector.fromArray(PFS, z, zOffset);
            final FloatVector vz2 = FloatVector.fromArray(PFS, z, zOffset + EPV);

            // Ger vz1re/vz1im till vz2 is loading
            final FloatVector vz1re = vz1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
            final FloatVector vz1im = vz1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

            // Ger vz2re/vz2im
            final FloatVector vz2re = vz2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
            final FloatVector vz2im = vz2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

            // Combine them
            final FloatVector vzre = vz1re.blend(vz2re, MASK_SECOND_HALF);
            final FloatVector vzim = vz1im.blend(vz2im, MASK_SECOND_HALF);

            final FloatVector vrre = vzre.mul(vzim.cos());
            final FloatVector vrim = vzre.mul(vzim.sin());

            // And combine & store twice
            vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
            vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

            zOffset += EPV * 2;
            count -= EPV;
        }

        if (count >= EPV2) {
            //@DONE: one load & two reshuffles are faster
            //@TODO: check, do we need pack and process twice elements, and save result twice
            // vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
            final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
            // vzreezp is [(z[0].re, z[0].re), (z[1].re, z[1].re), ...]
            final FloatVector vzre = vz.rearrange(SHUFFLE_CV_SPREAD_RE);
            // vzim is [(z[0].im, z[0].im), (z[1].im, z[1].im), ...]
            final FloatVector vzim = vz.rearrange(SHUFFLE_CV_SPREAD_IM);

            //@DONE: .cos(MASK_C_IM)/.sin(MASK_C_RE) is much slower
            final FloatVector vrre = vzre.mul(vzim.cos());
            final FloatVector vrim = vzre.mul(vzim.sin());

            vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

            zOffset += EPV;
            count -= EPV2;
        }

        float re, im;
        while (count-- > 0) {
            re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
            im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
            z[zOffset + 0] = re;
            z[zOffset + 1] = im;
            zOffset += 2;
        }
    }
}
