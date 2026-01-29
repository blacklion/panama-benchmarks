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

package vector.micro;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

/** @noinspection CStyleArrayDeclaration */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class MaskedArithmeticOps {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();

	private final static VectorMask<Float> MASK_C_RE;
	private final static VectorMask<Float> MASK_C_IM;

	static {
		boolean[] alter = new boolean[EPV + 1];
		alter[0] = true;
		for (int i = 1; i < alter.length; i++)
			alter[i] = !alter[i - 1];
		MASK_C_RE = VectorMask.fromArray(PFS, alter, 0);
		MASK_C_IM = VectorMask.fromArray(PFS, alter, 1);
	}

	private FloatVector vx;
	private FloatVector vy;

	@Setup(Level.Trial)
	public void Setup() {
		Random r = new Random(SEED);

		float x[] = new float[EPV * 2];
		float y[] = new float[EPV * 2];

		for (int i = 0; i < x.length; i++) {
			x[i] = r.nextFloat() * 2.0f - 1.0f;
			y[i] = r.nextFloat() * 2.0f - 1.0f;
		}
		vx = FloatVector.fromArray(PFS, x, 0);
		vy = FloatVector.fromArray(PFS, y, 0);
	}


	@Benchmark
	public void addsubWithMask(Blackhole bh) {
		bh.consume(vx.add(vy, MASK_C_RE).blend(vx.sub(vy, MASK_C_IM), MASK_C_IM));
	}

	@Benchmark
	public void addsub(Blackhole bh) {
		bh.consume(vx.add(vy).blend(vx.sub(vy), MASK_C_IM));
	}

	@Benchmark
	public void cossinWithMask(Blackhole bh) {
		bh.consume(vx.lanewise(VectorOperators.COS, MASK_C_RE).blend(vx.lanewise(VectorOperators.SIN, MASK_C_IM), MASK_C_IM));
	}

	@Benchmark
	public void cossin(Blackhole bh) {
		bh.consume(vx.lanewise(VectorOperators.COS).blend(vx.lanewise(VectorOperators.SIN), MASK_C_IM));
	}

	@Benchmark
	public void hypotatan2WithMask(Blackhole bh) {
		bh.consume(vx.lanewise(VectorOperators.HYPOT, vy, MASK_C_RE).blend(vx.lanewise(VectorOperators.ATAN2, vy, MASK_C_IM), MASK_C_IM));
	}

	@Benchmark
	public void hypotatan2(Blackhole bh) {
		bh.consume(vx.lanewise(VectorOperators.HYPOT, vy).blend(vx.lanewise(VectorOperators.ATAN2, vy), MASK_C_IM));
	}
}
