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
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class Offsets {
    private final static int MAX_OFFSET = 3;
    @Param({"0", "1", "2", "3"})
    public int offset;

    private FloatVector.FloatSpecies PFS;
    private FloatVector zero;
    private int EPV;
    private float x[];
    private float y[];
    
    
    @Setup(Level.Trial)
    public void Setup() {
        PFS = FloatVector.preferredSpecies();
        zero = PFS.zero();
        EPV = PFS.length();
        x = new float[EPV + MAX_OFFSET];
        y = new float[EPV + MAX_OFFSET];
        
        for (int i = 0; i < x.length; i++) {
            x[i] = (float)(Math.random() * 2.0 - 1.0);
            y[i] = (float)(Math.random() * 2.0 - 1.0);
        }
    }


    @Benchmark
    public void load(Blackhole bh) {
        bh.consume(FloatVector.fromArray(PFS, x, offset));
    }

    @Benchmark
    public void store() {
        zero.intoArray(y, offset);
    }

    @Benchmark
    public void loadstore() {
         FloatVector.fromArray(PFS, x, offset).intoArray(y, offset);
    }
}
