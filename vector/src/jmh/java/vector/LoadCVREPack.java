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

package vector;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.Vector;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;

@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class LoadCVREPack {
    private final static FloatVector.FloatSpecies PFS;

    private final static int[] LOAD_CV_TO_CV_PACK_RE;
   	private final static int[] LOAD_CV_TO_CV_PACK_IM;

    private final static FloatVector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_FIRST;
    private final static FloatVector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_FIRST;

    private final static FloatVector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_SECOND;
    private final static FloatVector.Shuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_SECOND;

   	private final static FloatVector.Mask<Float> MASK_SECOND_HALF;

    static {
        PFS = FloatVector.preferredSpecies();

        LOAD_CV_TO_CV_PACK_RE = new int[PFS.length()];
        LOAD_CV_TO_CV_PACK_IM = new int[PFS.length()];

        int[] re1 = new int[PFS.length()];
        int[] im1 = new int[PFS.length()];
        int[] re2 = new int[PFS.length()];
        int[] im2 = new int[PFS.length()];

        for (int i = 0; i < PFS.length(); i++) {
            // Vector complex to vector of twice as much real parts
            // [(re1, im1), (re2, im2), ...] -> [re1, re2, ...]
            LOAD_CV_TO_CV_PACK_RE[i] = i * 2;

            // Vector complex to vector of twice as much imaginary parts
            // [(re1, im1), (re2, im2), ...] -> [im1, im2, ...]
            LOAD_CV_TO_CV_PACK_IM[i] = i * 2 + 1;

            // [(re1, im1), (re2, im2), ...] -> [re1, re2, ...]
            if (i < PFS.length() / 2) {
                re1[i] = i * 2;
                im1[i] = i * 2 + 1;
            }
            // Second
            // [(re1, im1), (re2, im2), ...] -> [..., re1, re2, ...]
            if (i >= PFS.length() / 2) {
                re2[i] = i * 2 - PFS.length();
                im2[i] = i * 2 + 1 - PFS.length();
            }
        }

        SHUFFLE_CV_TO_CV_PACK_RE_FIRST = FloatVector.shuffleFromArray(PFS, re1, 0);
        SHUFFLE_CV_TO_CV_PACK_IM_FIRST = FloatVector.shuffleFromArray(PFS, im1, 0);

        SHUFFLE_CV_TO_CV_PACK_RE_SECOND = FloatVector.shuffleFromArray(PFS, re2, 0);
        SHUFFLE_CV_TO_CV_PACK_IM_SECOND = FloatVector.shuffleFromArray(PFS, im2, 0);

        boolean[] mask = new boolean[PFS.length()];
        Arrays.fill(mask, PFS.length() / 2, mask.length, true);
        MASK_SECOND_HALF = FloatVector.maskFromArray(PFS, mask, 0);
    }

    private float x[];

    @Setup(Level.Trial)
    public void Setup() {
        x = new float[PFS.length() * 2];

        for (int i = 0; i < x.length; i++) {
            x[i] = (float) (Math.random() * 2.0 - 1.0);
        }
    }


    @Benchmark
    public void loadTwice(Blackhole bh) {
        final FloatVector vxre = FloatVector.fromArray(PFS, x, 0, LOAD_CV_TO_CV_PACK_RE, 0);
		final FloatVector vxim = FloatVector.fromArray(PFS, x, 0, LOAD_CV_TO_CV_PACK_IM, 0);
        bh.consume(vxre);
        bh.consume(vxim);
    }

    @Benchmark
    public void loadAndReshuffle(Blackhole bh) {
        final FloatVector vx1 = FloatVector.fromArray(PFS, x, 0);
        final FloatVector vx2 = FloatVector.fromArray(PFS, x, PFS.length());

        final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
        final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

        final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
        final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

        final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
        final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);
        bh.consume(vxre);
        bh.consume(vxim);
    }
}
