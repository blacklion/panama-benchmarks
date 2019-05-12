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

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Random;

/** @noinspection PointlessArithmeticExpression, CStyleArrayDeclaration, SameParameterValue */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class RVdotCV {
    private final static int SEED = 42; // Carefully selected, plucked by hands random number

    private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
    private final static int EPV = PFS.length();
    private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));
    private final static int EPV2 = PFS2.length();

    private final static VectorMask<Float> MASK_SECOND_HALF;

    private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_BOTH;
    private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_FIRST;
    private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_FIRST;
    private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_SECOND;
    private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_SECOND;
    private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_FRONT_RE;
    private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_FRONT_IM;

    private final static FloatVector ZERO = FloatVector.zero(PFS);
    private final static FloatVector ZERO2 = FloatVector.zero(PFS2);

    static {
        boolean[] secondhalf = new boolean[EPV];
        Arrays.fill(secondhalf, PFS.length() / 2, secondhalf.length, true);
        MASK_SECOND_HALF = VectorMask.fromArray(PFS, secondhalf, 0);

        // [r0, r1, ...] -> [(r0, r0), (r1, r1), ...]
        SHUFFLE_RV_TO_CV_BOTH = VectorShuffle.shuffle(PFS, i -> i / 2);
        // [(re0, im0), (re1, im1), ...] -> [re0, re1, ..., re_len, ?, ...]
        SHUFFLE_CV_TO_CV_PACK_RE_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 : 0);
        // [(re0, im0), (re1, im1), ...] -> [im0, im1, ..., im_len, ?, ...]
        SHUFFLE_CV_TO_CV_PACK_IM_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 + 1 : 0);
        // [(re0, im0), (re1, im1), ...] -> [?, ..., re0, re1, ..., re_len]
        SHUFFLE_CV_TO_CV_PACK_RE_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV : 0);
        // [(re0, im0), (re1, im1), ...] -> [?, ..., im0, im1, ..., im_len]
        SHUFFLE_CV_TO_CV_PACK_IM_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV + 1 : 0);
        // [(re0, im0), (re1, im1), ...] -> [re0, re1, ...]
        SHUFFLE_CV_TO_CV_FRONT_RE = VectorShuffle.shuffle(PFS, i -> i * 2 < EPV ? i * 2 : i);
        // [(re0, im0), (re1, im1), ...] -> [im0, im1, ...]
        SHUFFLE_CV_TO_CV_FRONT_IM = VectorShuffle.shuffle(PFS, i -> i * 2 + 1 < EPV ? i * 2 + 1 : i);
    }

    private float x[];
    private float y[];
    private float z[];
    /** @noinspection unused*/
    @Param({"128"})
    private int count;

    @Setup
    public void Setup() {
        Random r = new Random(SEED);

        x = new float[count];
        y = new float[count * 2];
        z = new float[2];

        for (int i = 0; i < x.length; i++)
            x[i] = r.nextFloat() * 2.0f - 1.0f;
        for (int i = 0; i < y.length; i++)
            y[i] = r.nextFloat() * 2.0f - 1.0f;
    }

    @Benchmark
    public void nv(Blackhole bh) { rv_dot_cv_0(z, x, 0, y, 0, count); }

    @Benchmark
    public void blend_cv_add_lanes(Blackhole bh) { rv_dot_cv_1(z, x, 0, y, 0, count); }

    @Benchmark
    public void blend_cv_add_lanes_at_end(Blackhole bh) { rv_dot_cv_2(z, x, 0, y, 0, count); }

    @Benchmark
    public void reshape_cv_add_lanes(Blackhole bh) { rv_dot_cv_3(z, x, 0, y, 0, count); }

    @Benchmark
    public void reshape_cv_add_lanes_at_end(Blackhole bh) { rv_dot_cv_4(z, x, 0, y, 0, count); }

    private static void rv_dot_cv_0(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        float re = 0.0f;
        float im = 0.0f;
        yOffset <<= 1;
        while (count-- > 0) {
            re += x[xOffset] * y[yOffset + 0];
            im += x[xOffset] * y[yOffset + 1];
            xOffset += 1;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }

    private static void rv_dot_cv_1(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        float re = 0.0f;
        float im = 0.0f;
        yOffset <<= 1;

        while (count >= EPV) {
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
            //@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
            final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
            final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + PFS.length());

            final FloatVector vy1re = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
            final FloatVector vy1im = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

            final FloatVector vy2re = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
            final FloatVector vy2im = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

            final FloatVector vyre = vy1re.blend(vy2re, MASK_SECOND_HALF);
            final FloatVector vyim = vy1im.blend(vy2im, MASK_SECOND_HALF);

            re += vx.mul(vyre).addLanes();
            im += vx.mul(vyim).addLanes();

            xOffset += EPV;
            yOffset += EPV * 2; // We load twice as much complex numbers
            count -= EPV;
        }

        while (count-- > 0) {
            re += x[xOffset] * y[yOffset + 0];
            im += x[xOffset] * y[yOffset + 1];
            xOffset += 1;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }

    private static void rv_dot_cv_2(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        final boolean needLanes = count >= EPV;
        FloatVector vre = ZERO;
        FloatVector vim = ZERO;
        yOffset <<= 1;

        while (count >= EPV) {
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
            //@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
            final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
            final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + PFS.length());

            final FloatVector vy1re = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
            final FloatVector vy1im = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

            final FloatVector vy2re = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
            final FloatVector vy2im = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

            final FloatVector vyre = vy1re.blend(vy2re, MASK_SECOND_HALF);
            final FloatVector vyim = vy1im.blend(vy2im, MASK_SECOND_HALF);

            vre = vre.add(vx.mul(vyre));
            vim = vim.add(vx.mul(vyim));

            xOffset += EPV;
            yOffset += EPV * 2; // We load twice as much complex numbers
            count -= EPV;
        }

        float re = needLanes ? vre.addLanes() : 0.0f;
        float im = needLanes ? vim.addLanes() : 0.0f;
        while (count-- > 0) {
            re += x[xOffset] * y[yOffset + 0];
            im += x[xOffset] * y[yOffset + 1];
            xOffset += 1;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }

    private static void rv_dot_cv_3(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        float re = 0.0f;
        float im = 0.0f;
        yOffset <<= 1;

        while (count >= EPV2) {
            final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            //@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            final FloatVector vymul = vx.mul(vy);

            re += vymul.rearrange(SHUFFLE_CV_TO_CV_FRONT_RE).reshape(PFS2).addLanes();
            im += vymul.rearrange(SHUFFLE_CV_TO_CV_FRONT_IM).reshape(PFS2).addLanes();

            xOffset += EPV2;
            yOffset += EPV; // We load twice as much complex numbers
            count -= EPV2;
        }

        while (count-- > 0) {
            re += x[xOffset] * y[yOffset + 0];
            im += x[xOffset] * y[yOffset + 1];
            xOffset += 1;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }

    private static void rv_dot_cv_4(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        final boolean needLanes = count >= EPV2;
        FloatVector vre = ZERO2;
        FloatVector vim = ZERO2;
        yOffset <<= 1;

        while (count >= EPV2) {
            final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
            //@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            final FloatVector vymul = vx.mul(vy);

            vre = vre.add(vymul.rearrange(SHUFFLE_CV_TO_CV_FRONT_RE).reshape(PFS2));
            vim = vim.add(vymul.rearrange(SHUFFLE_CV_TO_CV_FRONT_IM).reshape(PFS2));

            xOffset += EPV2;
            yOffset += EPV; // We load twice as much complex numbers
            count -= EPV2;
        }

        float re = needLanes ? vre.addLanes() : 0.0f;
        float im = needLanes ? vim.addLanes() : 0.0f;
        while (count-- > 0) {
            re += x[xOffset] * y[yOffset + 0];
            im += x[xOffset] * y[yOffset + 1];
            xOffset += 1;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }
}
