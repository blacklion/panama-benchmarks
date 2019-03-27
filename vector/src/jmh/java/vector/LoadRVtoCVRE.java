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

@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class LoadRVtoCVRE {
    private final static FloatVector.FloatSpecies PFS;
    private final static FloatVector.FloatSpecies PFS2;
    private final static int[] LOAD_RV_TO_CV_RE;
    private final static Vector.Mask<Float> MASK_C_RE;
    private final static Vector.Mask<Float> MASK_C_IM;
    private final static FloatVector.Shuffle<Float> SHUFFLE_RV_TO_CV_RE;
    private final static FloatVector.Shuffle<Float> SHUFFLE_RV_TO_CV_RE_ZERO;

    static {
        PFS = FloatVector.preferredSpecies();
        PFS2 = FloatVector.species(Vector.Shape.forBitSize(PFS.bitSize() / 2));
        LOAD_RV_TO_CV_RE = new int[PFS.length()];
        int[] load_rv2cv_re_zero = new int[PFS.length()];
        for (int i = 0; i < PFS.length(); i++) {
			LOAD_RV_TO_CV_RE[i] = i / 2;
            load_rv2cv_re_zero[i] = (i % 2 == 0) ? (i / 2) : (PFS.length() - 1);
        }
        SHUFFLE_RV_TO_CV_RE = FloatVector.shuffleFromArray(PFS, LOAD_RV_TO_CV_RE, 0);
        SHUFFLE_RV_TO_CV_RE_ZERO = FloatVector.shuffleFromArray(PFS, load_rv2cv_re_zero, 0);

        boolean[] alter = new boolean[PFS.length() + 1];
   		alter[0] = true;
   		for (int i = 1; i < alter.length; i++)
   			alter[i] = !alter[i-1];
   		MASK_C_RE = FloatVector.maskFromArray(PFS, alter, 0);
        MASK_C_IM = FloatVector.maskFromArray(PFS, alter, 1);
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
    public void complexLoad(Blackhole bh) {
        bh.consume(FloatVector.fromArray(PFS, x, 0, MASK_C_RE, LOAD_RV_TO_CV_RE, 0));
    }

    @Benchmark
    public void loadAndReshuffleAndBlend(Blackhole bh) {
        final FloatVector vr = FloatVector.fromArray(PFS2, x, 0);
        bh.consume(vr.reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE).blend(PFS.zero(), MASK_C_IM));
    }

    @Benchmark
    public void loadAndReshuffle(Blackhole bh) {
        final FloatVector vr = FloatVector.fromArray(PFS2, x, 0);
        bh.consume(vr.reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE_ZERO));
    }
}
