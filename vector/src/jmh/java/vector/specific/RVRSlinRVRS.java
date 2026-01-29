/*****************************************************************************
 * Copyright (c) 2019-2026, Lev Serebryakov <lev@blacklion.dev>
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

import java.util.Random;

/** @noinspection CStyleArrayDeclaration, SameParameterValue */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class RVRSlinRVRS {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();

	private float x[];
	private float y[];
	private float z[];
	private float a1, a2;
	/** @noinspection unused */
	@Param({"128"})
	private int count;

	@Setup
	public void Setup() {
		Random r = new Random(SEED);

		x = new float[count];
		y = new float[count];
		z = new float[count];

		for (int i = 0; i < y.length; i++) {
			x[i] = r.nextFloat() * 2.0f - 1.0f;
			y[i] = r.nextFloat() * 2.0f - 1.0f;
		}
		a1 = r.nextFloat() * 2.0f - 1.0f;
		a2 = r.nextFloat() * 2.0f - 1.0f;
	}

	@Benchmark
	public void nv() { rv_rs_lin_rv_rs_0(z, 0, x, 0, a1, y, 0, a2, count); }

	@Benchmark
	public void naive() { rv_rs_lin_rv_rs_1(z, 0, x, 0, a1, y, 0, a2, count); }

	@Benchmark
	public void fma() { rv_rs_lin_rv_rs_2(z, 0, x, 0, a1, y, 0, a2, count); }

	private static void rv_rs_lin_rv_rs_0(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * a1 + y[yOffset++] * a2;
	}

	private static void rv_rs_lin_rv_rs_1(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		FloatVector va1 = null;
		FloatVector va2 = null;
		//@DONE: it is fater thab ...mul(a1)
		if (count >= EPV) {
			va1 = FloatVector.broadcast(PFS, a1);
			va2 = FloatVector.broadcast(PFS, a2);
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.mul(va1).add(vy.mul(va2)).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * a1 + y[yOffset++] * a2;
	}

	private static void rv_rs_lin_rv_rs_2(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		FloatVector va1 = null;
		FloatVector va2 = null;
		//@DONE: it is fater thab ...mul(a1)
		if (count >= EPV) {
			va1 = FloatVector.broadcast(PFS, a1);
			va2 = FloatVector.broadcast(PFS, a2);
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.fma(va1, vy.mul(va2)).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * a1 + y[yOffset++] * a2;
	}
}
