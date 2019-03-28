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

@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class BroadcastAddWithMask {
    private final static FloatVector.FloatSpecies PFS;
	private final static Vector.Mask<Float> MASK_C_RE;

    static {
        PFS = FloatVector.preferredSpecies();

		boolean[] alter = new boolean[PFS.length() + 1];
		alter[0] = true;
		for (int i = 1; i < alter.length; i++)
			alter[i] = !alter[i-1];
		MASK_C_RE = FloatVector.maskFromArray(PFS, alter, 0);
    }

    private float x[];
    private float y;
    private FloatVector vy;

    @Setup(Level.Trial)
    public void Setup() {
        x = new float[PFS.length() * 2];

        for (int i = 0; i < x.length; i++) {
            x[i] = (float) (Math.random() * 2.0 - 1.0);
        }
        y = (float) (Math.random() * 2.0 - 1.0);
        vy = PFS.zero().blend(y, MASK_C_RE);
    }


    @Benchmark
    public void addWithMask(Blackhole bh) {
        bh.consume(FloatVector.fromArray(PFS, x, 0).add(y, MASK_C_RE));
    }

    @Benchmark
    public void addWithBroadcasted(Blackhole bh) {
        bh.consume(FloatVector.fromArray(PFS, x, 0).add(vy));
    }
}
