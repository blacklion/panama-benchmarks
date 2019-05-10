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
public class CVMax {
	private final static int SEED = 42; // Carefully selected, plucked by hands random number

    private final static int MAX_SIZE = 128;

    private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
    private final static int EPV = PFS.length();
    private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));
    private final static int EPV2 = PFS2.length();

    private final static VectorMask<Float> MASK_SECOND_HALF;

	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 : 0);
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_FIRST = VectorShuffle.shuffle(PFS, i -> (i < EPV2) ? i * 2 + 1 : 0);
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_RE_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV : 0);
	private final static VectorShuffle<Float> SHUFFLE_CV_TO_CV_PACK_IM_SECOND = VectorShuffle.shuffle(PFS, i -> (i >= EPV2) ? i * 2 - EPV + 1: 0);

    static {
        boolean[] sh = new boolean[EPV];
        Arrays.fill(sh, EPV / 2, sh.length, true);
        MASK_SECOND_HALF = VectorMask.fromArray(PFS, sh, 0);
	}

	private float x[];
    private float z[];

    @Setup(Level.Trial)
    public void Setup() {
		Random r = new Random(SEED);

		x = new float[MAX_SIZE * 2];
        z = new float[2];

        for (int i = 0; i < x.length; i++) {
            x[i] = r.nextFloat() * 2.0f - 1.0f;
        }
    }

	@Benchmark
	public void find_internal() { cv_max_0(z, x, 0, MAX_SIZE); }

    @Benchmark
	public void find_external() {
		cv_max_1(z, x, 0, MAX_SIZE);
	}

	@Benchmark
	public void find_array() {
		cv_max_3(z, x, 0, MAX_SIZE);
	}

	@Benchmark
	public void find_nv() {
		cv_max_4(z, x, 0, MAX_SIZE);
	}

	private static void cv_max_0(float z[], float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;

		while (count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + PFS.length());

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));
			float localMax = vxabs.maxLanes();
			if (max < localMax) {
				// Find it now
				for (int j = 0; j < EPV; j++) {
					if (vxabs.lane(j) == localMax) {
						i = xOffset + j * 2;
						break;
					}
				}
				max = localMax;
			}

			xOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (max < abs) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}

	private static void cv_max_1(float z[], float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		FloatVector vmax = null;
		int i = -1;
		xOffset <<= 1;

		while (count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + PFS.length());

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));
			float localMax = vxabs.maxLanes();
			if (max < localMax) {
				vmax = vxabs;
				i = xOffset;
				max = localMax;
			}

			xOffset += EPV * 2;
			count -= EPV;
		}

		// Find max in stored vector
		if (i >= 0) {
			for (int j = 0; j < EPV; j++) {
				if (vmax.lane(j) == max) {
					i += j * 2;
					break;
				}
			}
		}

		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (max < abs) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}

	private static void cv_max_3(float z[], float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;

		float aabs[] = new float[EPV];

		while (count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + PFS.length());

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));
			vxabs.intoArray(aabs, 0);
			for (int j = 0; j < EPV; j++) {
				if (max < aabs[j]) {
					max = aabs[j];
					i = xOffset + j;
				}
			}

			xOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (max < abs) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}

	private static void cv_max_4(float z[], float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (max < abs) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}
}
