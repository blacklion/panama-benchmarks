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
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

/** @noinspection CStyleArrayDeclaration, SameParameterValue */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class RVsum {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();

	private final static FloatVector ZERO = FloatVector.zero(PFS);

	private float x[];
	/** @noinspection unused */
	@Param({"128"})
	private int count;

	@Setup(Level.Trial)
	public void Setup() {
		Random r = new Random(SEED);

		x = new float[count];

		for (int i = 0; i < x.length; i++) {
			x[i] = r.nextFloat() * 2.0f - 1.0f;
		}
	}

	@Benchmark
	public void nv(Blackhole bh) { bh.consume(rv_sum_0(x, 0, count)); }

	@Benchmark
	public void saccum_add_lanes(Blackhole bh) { bh.consume(rv_sum_1(x, 0, count)); }

	@Benchmark
	public void vaccum_add_lanes(Blackhole bh) { bh.consume(rv_sum_2(x, 0, count)); }

	private static float rv_sum_0(float x[], int xOffset, int count) {
		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++];
		return sum;
	}

	private static float rv_sum_1(float x[], int xOffset, int count) {
		float sum = 0.0f;

		while (count >= EPV) {
			sum += FloatVector.fromArray(PFS, x, xOffset).addLanes();

			xOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++];
		return sum;
	}

	private static float rv_sum_2(float x[], int xOffset, int count) {
		FloatVector vsum = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPV) {
			vsum = vsum.add(FloatVector.fromArray(PFS, x, xOffset));

			xOffset += EPV;
			count -= EPV;
		}

		float sum = needLanes ? vsum.addLanes() : 0.0f;
		while (count-- > 0)
			sum += x[xOffset++];
		return sum;
	}
}

