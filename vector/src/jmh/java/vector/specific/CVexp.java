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

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.Random;

/** @noinspection PointlessArithmeticExpression, CStyleArrayDeclaration, SameParameterValue */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class CVexp {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();
	private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));
	private final static int EPV2 = PFS2.length();

	private final static VectorMask<Float> MASK_C_IM;
	private final static VectorMask<Float> MASK_SECOND_HALF;

	private final static VectorShuffle<Float> SHUFFLE_CV_SPREAD_IM;
	private final static VectorShuffle<Float> SHUFFLE_CV_SPREAD_RE;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_FIRST;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_FIRST;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_SECOND;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_SECOND;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND;

	static {
		boolean[] alter = new boolean[EPV + 1];
		alter[0] = true;
		for (int i = 1; i < alter.length; i++)
			alter[i] = !alter[i - 1];
		MASK_C_IM = VectorMask.fromArray(PFS, alter, 1);

		boolean[] sh = new boolean[EPV];
		Arrays.fill(sh, EPV / 2, sh.length, true);
		MASK_SECOND_HALF = VectorMask.fromArray(PFS, sh, 0);

		// [(re0, im0), (re1, im1), ...] -> [(re0, re0), (re1, re1), ...]
		SHUFFLE_CV_SPREAD_RE = VectorShuffle.shuffle(PFS, i -> i - i % 2);
		// [(re0, im0), (re1, im1), ...] -> [(im0, im0), (im1, im1), ...]
		SHUFFLE_CV_SPREAD_IM = VectorShuffle.shuffle(PFS, i -> i - i % 2 + 1);
		// [(re0, im0), (re1, im1), ...] -> [re0, re1, ..., re_len, ?, ...]
		SHUFFLE_CV_TO_CV_PACK_RE_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 : 0);
		// [(re0, im0), (re1, im1), ...] -> [im0, im1, ..., im_len, ?, ...]
		SHUFFLE_CV_TO_CV_PACK_IM_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 + 1 : 0);
		// [(re0, im0), (re1, im1), ...] -> [?, ..., re0, re1, ..., re_len]
		SHUFFLE_CV_TO_CV_PACK_RE_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV : 0);
		// [(re0, im0), (re1, im1), ...] -> [?, ..., im0, im1, ..., im_len]
		SHUFFLE_CV_TO_CV_PACK_IM_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV + 1 : 0);
		// [re0, re1, re2, ...] -> [(re0, ?), (re1, ?), ..., (re_{len/2}, ?)]
		SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? i / 2 : 0);
		// [im0, im1, im2, ...] -> [(?, im0), (?, im1), ..., (?, im_{len/2})]
		SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST = VectorShuffle.shuffle(PFS, i -> (i % 2 == 1) ? i / 2 : 0);
		// [..., re_{len/2}, ..., re_len] -> [(re_{len/2}, ?), ..., (re_len, ?)]
		SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? i / 2 + EPV2 : 0);
		// [..., im_{len/2}, ..., im_len] -> [(?, im_{len/2}), ..., (?, im_len)]
		SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND = VectorShuffle.shuffle(PFS, i -> (i % 2 == 1) ? i / 2 + EPV2 : 0);
	}

	private float x[];
	private float z[];
	/** @noinspection unused */
	@Param({"128"})
	private int count;

	@Setup(Level.Trial)
	public void Setup() {
		Random r = new Random(SEED);

		x = new float[count * 2];
		z = new float[count * 2];

		for (int i = 0; i < x.length; i++) {
			x[i] = r.nextFloat() * 2.0f - 1.0f;
		}
	}

	@Benchmark
	public void nv() { cv_exp_0(z, 0, x, 0, count); }

	@Benchmark
	public void epv2() { cv_exp_1(z, 0, x, 0, count); }

	@Benchmark
	public void epv() { cv_exp_2(z, 0, x, 0, count); }

	@Benchmark
	public void epv_epv2() { cv_exp_3(z, 0, x, 0, count); }

	private static void cv_exp_0(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			float g = (float)Math.exp(x[xOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(x[xOffset + 1]);
			zOffset += 2;
			xOffset += 2;
		}
	}

	private static void cv_exp_1(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			//@TODO: check, do we need pack and process twice elements, and save result twice
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxreexp is [(exp(x[0].re), exp(x[0].re)), (exp(x[1].re), exp(x[1].re)), ...]
			final FloatVector vxreexp = vx.rearrange(SHUFFLE_CV_SPREAD_RE).exp();
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: .cos(MASK_C_IM)/.sin(MASK_C_RE) is much slower
			final FloatVector vrre = vxreexp.mul(vxim.cos());
			final FloatVector vrim = vxreexp.mul(vxim.sin());

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			float g = (float)Math.exp(x[xOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(x[xOffset + 1]);
			xOffset += 2;
			zOffset += 2;
		}
	}

	private static void cv_exp_2(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		// To process
		while (count >= EPV) {
			//@DONE: one load & two reshuffles are faster
			//@DONE: it is better than process one vector
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);

			// Ger vx1re/vx1im till vx2 is loading
			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			// Ger vx2re/vx2im
			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			// Combine them
			final FloatVector vxreexp = vx1re.blend(vx2re, MASK_SECOND_HALF).exp();
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			final FloatVector vrre = vxreexp.mul(vxim.cos());
			final FloatVector vrim = vxreexp.mul(vxim.sin());

			// And combine & store twice
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

			// We loaded and stored twice as many numbers
			xOffset += EPV * 2;
			zOffset += EPV * 2;
			count -= EPV;
		}

		//@DONE: Don't process obe PFS-countd vector, it is too expensive to setup for only one

		while (count-- > 0) {
			float g = (float)Math.exp(x[xOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(x[xOffset + 1]);
			xOffset += 2;
			zOffset += 2;
		}
	}

	private static void cv_exp_3(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		// To process
		while (count >= EPV) {
			//@DONE: one load & two reshuffles are faster
			//@DONE: it is better than process one vector
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);

			// Ger vx1re/vx1im till vx2 is loading
			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			// Ger vx2re/vx2im
			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			// Combine them
			final FloatVector vxreexp = vx1re.blend(vx2re, MASK_SECOND_HALF).exp();
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			final FloatVector vrre = vxreexp.mul(vxim.cos());
			final FloatVector vrim = vxreexp.mul(vxim.sin());

			// And combine & store twice
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

			// We loaded and stored twice as many numbers
			xOffset += EPV * 2;
			zOffset += EPV * 2;
			count -= EPV;
		}

		if (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			//@TODO: check, do we need pack and process twice elements, and save result twice
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxreexp is [(exp(x[0].re), exp(x[0].re)), (exp(x[1].re), exp(x[1].re)), ...]
			final FloatVector vxreexp = vx.rearrange(SHUFFLE_CV_SPREAD_RE).exp();
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: .cos(MASK_C_IM)/.sin(MASK_C_RE) is much slower
			final FloatVector vrre = vxreexp.mul(vxim.cos());
			final FloatVector vrim = vxreexp.mul(vxim.sin());

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			float g = (float)Math.exp(x[xOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(x[xOffset + 1]);
			xOffset += 2;
			zOffset += 2;
		}
	}
}
