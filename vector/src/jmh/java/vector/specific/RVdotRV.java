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
import jdk.incubator.vector.VectorOperators;
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
public class RVdotRV {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();
	private final static int EPVx2 = EPV * 2;
	private final static int EPVx3 = EPV * 3;
	private final static int EPVx4 = EPV * 4;

	private final static FloatVector ZERO = FloatVector.zero(PFS);

	private float x[];
	private float y[];
	@Param({"128"})
	private int count = 128;

	@Setup
	public void Setup() {
		Random r = new Random(SEED);

		x = new float[count];
		y = new float[count];

		for (int i = 0; i < y.length; i++)
			y[i] = r.nextFloat() * 2.0f - 1.0f;
	}

	@Benchmark
	public void nv(Blackhole bh) { bh.consume(rv_dot_rv_0(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_1(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_unroll_2_1_fma_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_2(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_unroll_4_2_fma_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_3(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_unroll_4_2_1_fma_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_4(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_unroll_2_1_chain_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_5(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_unroll_4_2_chain_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_6(x, 0, y, 0, count)); }

	@Benchmark
	public void saccum_unroll_4_2_1_chain_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_7(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_8(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_unroll_2_1_personal_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_9(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_unroll_4_2_personal_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_a(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_unroll_4_2_1_personal_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_b(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_unroll_2_1_combine_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_c(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_unroll_4_2_combine_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_d(x, 0, y, 0, count)); }

	@Benchmark
	public void vaccum_unroll_4_2_1_combine_add_lanes(Blackhole bh) { bh.consume(rv_dot_rv_e(x, 0, y, 0, count)); }


	private static float rv_dot_rv_0(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];
		return sum;
	}

	private static float rv_dot_rv_1(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_2(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			// sum += vx1.mul(vy1).add(vx2.mul(vy2)).reduceLanes(VectorOperators.ADD);
			sum += vx1.fma(vy1, vx2.mul(vy2)).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_3(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			// sum += vx1.mul(vy1).add(vx2.mul(vy2)).add(vx3.mul(vy3)).add(vx4.mul(vy4)).reduceLanes(VectorOperators.ADD);
			// Strange, but it is better than 3 fma and one mul
			sum += vx1.fma(vy1, vx2.mul(vy2)).add(vx3.fma(vy3, vx4.mul(vy4))).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_4(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			// sum += vx1.mul(vy1).add(vx2.mul(vy2)).add(vx3.mul(vy3)).add(vx4.mul(vy4)).reduceLanes(VectorOperators.ADD);
			// Strange, but it is better than 3 fma and one mul
			sum += vx1.fma(vy1, vx2.mul(vy2)).add(vx3.fma(vy3, vx4.mul(vy4))).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		if (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			// sum += vx1.mul(vy1).add(vx2.mul(vy2)).reduceLanes(VectorOperators.ADD);
			sum += vx1.fma(vy1, vx2.mul(vy2)).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_5(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			sum += vx1.mul(vy1).add(vx2.mul(vy2)).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_6(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			sum += vx1.mul(vy1).add(vx2.mul(vy2)).add(vx3.mul(vy3)).add(vx4.mul(vy4)).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_7(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			sum += vx1.mul(vy1).add(vx2.mul(vy2)).add(vx3.mul(vy3)).add(vx4.mul(vy4)).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		if (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			sum += vx1.mul(vy1).add(vx2.mul(vy2)).reduceLanes(VectorOperators.ADD);

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).reduceLanes(VectorOperators.ADD);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	private static float rv_dot_rv_8(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum = vsum.add(vx.mul(vy));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum.reduceLanes(VectorOperators.ADD);

		return sum;
	}

	private static float rv_dot_rv_9(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum1 = ZERO;
		FloatVector vsum2 = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum2 = vsum2.add(vx2.mul(vy2));

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum1 = vsum1.add(vx.mul(vy).reduceLanes(VectorOperators.ADD));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum1.reduceLanes(VectorOperators.ADD) + vsum2.reduceLanes(VectorOperators.ADD);

		return sum;
	}

	private static float rv_dot_rv_a(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum1 = ZERO;
		FloatVector vsum2 = ZERO;
		FloatVector vsum3 = ZERO;
		FloatVector vsum4 = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum2 = vsum2.add(vx2.mul(vy2));
			vsum3 = vsum3.add(vx3.mul(vy3));
			vsum4 = vsum4.add(vx4.mul(vy4));

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum1 = vsum1.add(vx.mul(vy));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum1.reduceLanes(VectorOperators.ADD) + vsum2.reduceLanes(VectorOperators.ADD) + vsum3.reduceLanes(VectorOperators.ADD) + vsum4.reduceLanes(VectorOperators.ADD);

		return sum;
	}

	private static float rv_dot_rv_b(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum1 = ZERO;
		FloatVector vsum2 = ZERO;
		FloatVector vsum3 = ZERO;
		FloatVector vsum4 = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum2 = vsum2.add(vx2.mul(vy2));
			vsum3 = vsum3.add(vx3.mul(vy3));
			vsum4 = vsum4.add(vx4.mul(vy4));

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		if (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum3 = vsum3.add(vx2.mul(vy2));

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum1 = vsum1.add(vx.mul(vy));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum1.reduceLanes(VectorOperators.ADD) + vsum2.reduceLanes(VectorOperators.ADD) + vsum3.reduceLanes(VectorOperators.ADD) + vsum4.reduceLanes(VectorOperators.ADD);

		return sum;
	}

	private static float rv_dot_rv_c(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum1 = ZERO;
		FloatVector vsum2 = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum2 = vsum2.add(vx2.mul(vy2));

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum1 = vsum1.add(vx.mul(vy).reduceLanes(VectorOperators.ADD));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum1.add(vsum2).reduceLanes(VectorOperators.ADD);

		return sum;
	}

	private static float rv_dot_rv_d(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum1 = ZERO;
		FloatVector vsum2 = ZERO;
		FloatVector vsum3 = ZERO;
		FloatVector vsum4 = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum2 = vsum2.add(vx2.mul(vy2));
			vsum3 = vsum3.add(vx3.mul(vy3));
			vsum4 = vsum4.add(vx4.mul(vy4));

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum1 = vsum1.add(vx.mul(vy));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum1.add(vsum2).add(vsum3.add(vsum4)).reduceLanes(VectorOperators.ADD);

		return sum;
	}

	private static float rv_dot_rv_e(float x[], int xOffset, float y[], int yOffset, int count) {
		FloatVector vsum1 = ZERO;
		FloatVector vsum2 = ZERO;
		FloatVector vsum3 = ZERO;
		FloatVector vsum4 = ZERO;
		final boolean needLanes = count >= EPV;

		while (count >= EPVx4) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);
			final FloatVector vx3 = FloatVector.fromArray(PFS, x, xOffset + EPVx2);
			final FloatVector vy3 = FloatVector.fromArray(PFS, y, yOffset + EPVx2);
			final FloatVector vx4 = FloatVector.fromArray(PFS, x, xOffset + EPVx3);
			final FloatVector vy4 = FloatVector.fromArray(PFS, y, yOffset + EPVx3);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum2 = vsum2.add(vx2.mul(vy2));
			vsum3 = vsum3.add(vx3.mul(vy3));
			vsum4 = vsum4.add(vx4.mul(vy4));

			xOffset += EPVx4;
			yOffset += EPVx4;
			count -= EPVx4;
		}

		if (count >= EPVx2) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + EPV);

			vsum1 = vsum1.add(vx1.mul(vy1));
			vsum3 = vsum3.add(vx2.mul(vy2));

			xOffset += EPVx2;
			yOffset += EPVx2;
			count -= EPVx2;
		}

		if (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vsum1 = vsum1.add(vx.mul(vy));

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		if (needLanes)
			sum += vsum1.add(vsum2).add(vsum3.add(vsum4)).reduceLanes(VectorOperators.ADD);

		return sum;
	}
}
