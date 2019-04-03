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
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

/** @noinspection CStyleArrayDeclaration*/
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class LoadRVtoCVBoth {
    private final static int SEED = 42; // Carefully selected, pucked by hands random number

    private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
    private final static int EPV = PFS.length();
    private final static FloatVector.FloatSpecies PFS2 = FloatVector.species(Vector.Shape.forBitSize(PFS.bitSize() / 2));

    private final static Vector.Shuffle<Float> SHUFFLE_RV_TO_CV_BOTH = FloatVector.shuffle(PFS, i -> i / 2);
    private final static int[] LOAD_RV_TO_CV_BOTH = SHUFFLE_RV_TO_CV_BOTH.toArray();

    private float x[];

    @Setup(Level.Trial)
    public void Setup() {
        Random r = new Random(SEED);

        x = new float[EPV * 2];
        for (int i = 0; i < x.length; i++) {
            x[i] = r.nextFloat() * 2.0f - 1.0f;
        }
    }


    @Benchmark
    public void complexLoad(Blackhole bh) {
        bh.consume(FloatVector.fromArray(PFS, x, 0, LOAD_RV_TO_CV_BOTH, 0));
    }

    @Benchmark
    public void loadAndReshuffle(Blackhole bh) {
        final FloatVector vr = FloatVector.fromArray(PFS2, x, 0);
        bh.consume(vr.reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH));
    }
}
