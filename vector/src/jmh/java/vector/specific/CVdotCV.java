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

import java.util.Random;

/** @noinspection PointlessArithmeticExpression, CStyleArrayDeclaration */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class CVdotCV {
    private final static int SEED = 42; // Carefully selected, plucked by hands random number

    private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
    private final static int EPV = PFS.length();
    private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));
    private final static int EPV2 = PFS2.length();

    private final static VectorMask<Float> MASK_C_RE;
    private final static VectorMask<Float> MASK_C_IM;

    private final static VectorShuffle<Float> SHUFFLE_CV_SWAP_RE_IM = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? i + 1 : i - 1);
    private final static VectorShuffle<Float> SHUFFLE_CV_SPREAD_RE = VectorShuffle.shuffle(PFS, i -> i - i % 2);
    private final static VectorShuffle<Float> SHUFFLE_CV_SPREAD_IM = VectorShuffle.shuffle(PFS, i -> i - i % 2 + 1);
    private final static VectorShuffle<Float> SHUFFLE_CV_FRONT_RE = VectorShuffle.shuffle(PFS, i -> i * 2 < EPV ? i * 2 : i);
    private final static VectorShuffle<Float> SHUFFLE_CV_FRONT_IM = VectorShuffle.shuffle(PFS, i -> i * 2 + 1 < EPV ? i * 2 + 1 : i);

    private final static FloatVector ZERO = FloatVector.zero(PFS);

    static {
        boolean[] alter = new boolean[EPV + 1];
        alter[0] = true;
        for (int i = 1; i < alter.length; i++)
            alter[i] = !alter[i-1];
        MASK_C_RE = VectorMask.fromArray(PFS, alter, 0);
        MASK_C_IM = VectorMask.fromArray(PFS, alter, 1);
    }

    private float z[];
    private float x[];
    private float y[];
    private int count = 128;

    @Setup
    public void Setup() {
        Random r = new Random(SEED);

        z = new float[2];
        x = new float[65536 * 2];
        y = new float[65536 * 2];

        for (int i = 0; i < y.length; i++)
            y[i] = r.nextFloat() * 2.0f - 1.0f;
    }

    @Benchmark
    public void addLanes_mask() { cv_dot_cv0(z, x, 0, y, 0, count); }

    @Benchmark
    public void addLanes_reshaped() { cv_dot_cv1(z, x, 0, y, 0, count); }

    @Benchmark
    public void addLanes_zeroed() { cv_dot_cv1(z, x, 0, y, 0, count); }

    private static void cv_dot_cv0(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        float re = 0.0f;
        float im = 0.0f;
        xOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            //@DONE: one load & two reshuffles are faster
            // vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

            // vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

            // vmulyre is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
            final FloatVector vmulyre = vx.mul(vyre);
            // vmulyim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
            final FloatVector vmulyim = vx.mul(vyim);
            // vmulximswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
            final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            //@DONE: vmulyre.sub(vmulximswap, MASK_C_RE) / vmulyre.add(vmulximswap, MASK_C_IM) are slower
            // vrre is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
            final FloatVector vrre = vmulyre.sub(vmulximswap);
            // vrim is ([?, x[0].im * y.re + x[0].re * y.im], ...)
            final FloatVector vrim = vmulyre.add(vmulximswap);

            re += vrre.addLanes(MASK_C_RE);
            im += vrim.addLanes(MASK_C_IM);

            xOffset += EPV;
            yOffset += EPV;
            count -= EPV2;
        }

        float k0, k1;
        while (count-- > 0) {
            k0 = x[xOffset + 0] * y[yOffset + 0];
            k1 = x[xOffset + 1] * y[yOffset + 1];
            re += k0 - k1;
            im += (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
            xOffset += 2;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }

    private static void cv_dot_cv1(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        float re = 0.0f;
        float im = 0.0f;
        xOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            //@DONE: one load & two reshuffles are faster
            // vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

            // vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

            // vmulyre is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
            final FloatVector vmulyre = vx.mul(vyre);
            // vmulyim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
            final FloatVector vmulyim = vx.mul(vyim);
            // vmulximswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
            final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            //@DONE: vmulyre.sub(vmulximswap, MASK_C_RE) / vmulyre.add(vmulximswap, MASK_C_IM) are slower
            // vrre is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
            final FloatVector vrre = vmulyre.sub(vmulximswap);
            // vrim is ([?, x[0].im * y.re + x[0].re * y.im], ...)
            final FloatVector vrim = vmulyre.add(vmulximswap);

            // Reshape to addLines
            re += vrre.rearrange(SHUFFLE_CV_FRONT_RE).reshape(PFS2).addLanes();
            im += vrre.rearrange(SHUFFLE_CV_FRONT_IM).reshape(PFS2).addLanes();

            xOffset += EPV;
            yOffset += EPV;
            count -= EPV2;
        }

        float k0, k1;
        while (count-- > 0) {
            k0 = x[xOffset + 0] * y[yOffset + 0];
            k1 = x[xOffset + 1] * y[yOffset + 1];
            re += k0 - k1;
            im += (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
            xOffset += 2;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }

    private static void cv_dot_cv2(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
        float re = 0.0f;
        float im = 0.0f;
        xOffset <<= 1;
        yOffset <<= 1;

        while (count >= EPV2) {
            //@DONE: one load & two reshuffles are faster
            // vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
            final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
            // vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
            final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
            // vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
            final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

            // vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
            final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

            // vmulyre is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
            final FloatVector vmulyre = vx.mul(vyre);
            // vmulyim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
            final FloatVector vmulyim = vx.mul(vyim);
            // vmulximswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
            final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

            //@DONE: vmulyre.sub(vmulximswap, MASK_C_RE) / vmulyre.add(vmulximswap, MASK_C_IM) are slower
            // vrre is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
            final FloatVector vrre = vmulyre.sub(vmulximswap);
            // vrim is ([?, x[0].im * y.re + x[0].re * y.im], ...)
            final FloatVector vrim = vmulyre.add(vmulximswap);

            re += vrre.blend(ZERO, MASK_C_IM).addLanes();
            im += vrim.blend(ZERO, MASK_C_RE).addLanes();

            xOffset += EPV;
            yOffset += EPV;
            count -= EPV2;
        }

        float k0, k1;
        while (count-- > 0) {
            k0 = x[xOffset + 0] * y[yOffset + 0];
            k1 = x[xOffset + 1] * y[yOffset + 1];
            re += k0 - k1;
            im += (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
            xOffset += 2;
            yOffset += 2;
        }
        z[0] = re;
        z[1] = im;
    }
}
