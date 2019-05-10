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

/** @noinspection CStyleArrayDeclaration, WeakerAccess, PointlessArithmeticExpression */
@Fork(2)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(Scope.Thread)
public class CVSum {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

    private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
    private final static int EPV = PFS.length();
    private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));
    private final static int EPV2 = PFS2.length();

	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_FIRST;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_FIRST;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_SECOND;
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_SECOND;
	private final static VectorMask<Float> MASK_SECOND_HALF;

    static {
		boolean[] secondhalf = new boolean[EPV];
		Arrays.fill(secondhalf, PFS.length() / 2, secondhalf.length, true);
		MASK_SECOND_HALF = VectorMask.fromArray(PFS, secondhalf, 0);

		// [(re0, im0), (re1, im1), ...] -> [re0, re1, ..., re_len, ?, ...]
		SHUFFLE_CV_TO_CV_PACK_RE_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 : 0);
		// [(re0, im0), (re1, im1), ...] -> [im0, im1, ..., im_len, ?, ...]
		SHUFFLE_CV_TO_CV_PACK_IM_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 + 1 : 0);
		// [(re0, im0), (re1, im1), ...] -> [?, ..., re0, re1, ..., re_len]
		SHUFFLE_CV_TO_CV_PACK_RE_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV : 0);
		// [(re0, im0), (re1, im1), ...] -> [?, ..., im0, im1, ..., im_len]
		SHUFFLE_CV_TO_CV_PACK_IM_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV + 1 : 0);
	}

	@Param({"128"})
	private int count;

	private float x[];
    private float z[];

    @Setup(Level.Trial)
    public void Setup() {
		Random r = new Random(SEED);

		x = new float[count * 2];
        z = new float[2];

        for (int i = 0; i < x.length; i++) {
            x[i] = r.nextFloat() * 2.0f - 1.0f;
        }
    }

	@Benchmark
	public void nv() { cv_sum_0(z, x, 0, count); }

	@Benchmark
	public void reshape_add_lanes() { cv_sum_1(z, x, 0, count); }

	@Benchmark
	public void blend_add_lanes() { cv_sum_2(z, x, 0, count); }

	private static void cv_sum_0(float z[], float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;
		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	private static void cv_sum_1(float z[], float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@TODO Check, can we pack and process twice elements, and save result twice
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// It is faster than addLanes(MASK)
			re += vx.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST).reshape(PFS2).addLanes();
			im += vx.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST).reshape(PFS2).addLanes();

			xOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	private static void cv_sum_2(float z[], float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;

		while (count >= EPV) {
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			re += vxim.addLanes();
			im += vxre.addLanes();

			xOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}
}
