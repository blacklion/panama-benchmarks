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

package vector.micro;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

/** @noinspection CStyleArrayDeclaration, SameParameterValue */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class LoadRVtoCVRE {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();
	private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.vectorBitSize() / 2));


	private final static VectorMask<Float> MASK_C_RE;
	private final static VectorMask<Float> MASK_C_IM;

	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_RE;
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_RE_ZERO;
	private final static int[] LOAD_RV_TO_CV_RE;

	private final static FloatVector ZERO = FloatVector.zero(PFS);

	static {
		boolean[] alter = new boolean[EPV + 1];
		alter[0] = true;
		for (int i = 1; i < alter.length; i++)
			alter[i] = !alter[i - 1];
		MASK_C_RE = VectorMask.fromArray(PFS, alter, 0);
		MASK_C_IM = VectorMask.fromArray(PFS, alter, 1);

		SHUFFLE_RV_TO_CV_RE = VectorShuffle.fromOp(PFS, i -> i / 2);
		SHUFFLE_RV_TO_CV_RE_ZERO = VectorShuffle.fromOp(PFS, i -> (i % 2 == 0) ? (i / 2) : (EPV - 1));

		LOAD_RV_TO_CV_RE = SHUFFLE_RV_TO_CV_RE.toArray();
	}

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
	public void load_with_spread(Blackhole bh) {
		bh.consume(FloatVector.fromArray(PFS, x, 0, LOAD_RV_TO_CV_RE, 0));
	}

	@Benchmark
	public void load_simple_shuffle_blend(Blackhole bh) {
		final FloatVector vr = FloatVector.fromArray(PFS2, x, 0);
		bh.consume(vr.reinterpretShape(PFS, 0).reinterpretAsFloats().rearrange(SHUFFLE_RV_TO_CV_RE).blend(ZERO, MASK_C_IM));
	}

	@Benchmark
	public void load_simple_shuffle(Blackhole bh) {
		final FloatVector vr = FloatVector.fromArray(PFS2, x, 0);
		bh.consume(vr.reinterpretShape(PFS, 0).reinterpretAsFloats().rearrange(SHUFFLE_RV_TO_CV_RE_ZERO));
	}
}
