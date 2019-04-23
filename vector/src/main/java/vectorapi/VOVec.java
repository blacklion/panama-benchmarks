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

package vectorapi;

import jdk.incubator.vector.*;

import java.util.Arrays;

/**
 * @author Lev Serebryakov
 * @noinspection CStyleArrayDeclaration
 */
@SuppressWarnings({ "PointlessArithmeticExpression", "UnusedDeclaration" })
public final class VOVec {
	//@TODO: Global idea: check extraction of complex multiply/division code
	//       to helper methods (and pray for inlining)
	/* Missing methods which make sense:
		cv_cs_lin_cv_cs
		cv_cs_lin_cv_cs_i
		cv_cs_lin_cv_rs
		cv_cs_lin_cv_rs_i
		cv_cs_lin_rv_cs
		cv_cs_lin_rv_cs_i
		cv_cs_lin_rv_rs
		cv_cs_lin_rv_rs_i
		cv_rs_lin_cv_rs
		cv_rs_lin_cv_rs_i
		cv_rs_lin_rv_cs
		cv_rs_lin_rv_cs_i
		cv_rs_lin_rv_rs
		cv_rs_lin_rv_rs_i
		rv_cs_lin_rv_cs
		rv_rs_lin_rv_cs

		And some one-complex-return function with "offset" result placement
	 */
	private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;
	private final static int EPV = PFS.length();
	private final static VectorSpecies<Float> PFS2 = VectorSpecies.of(Float.TYPE, VectorShape.forBitSize(PFS.bitSize() / 2));;
	private final static int EPV2 = EPV / 2;
	private final static VectorSpecies<Float> FS64 = FloatVector.SPECIES_64;
	private final static VectorMask<Float> MASK_C_RE;
	private final static VectorMask<Float> MASK_C_IM;
	private final static VectorMask<Float> MASK_SECOND_HALF;
	private final static FloatVector ZERO = FloatVector.zero(PFS);
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_RE;
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_BOTH;
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_RE_LOW;
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_IM_LOW;
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_RE_HIGH;
	private final static VectorShuffle<Float> SHUFFLE_RV_TO_CV_IM_HIGH;
	private final static VectorShuffle<Float> SHUFFLE_CS_TO_CV_SPREAD;
	private final static VectorShuffle<Float> SHUFFLE_CV_SWAP_RE_IM;
	private final static VectorShuffle<Float> SHUFFLE_CV_SPREAD_RE;
	private final static VectorShuffle<Float> SHUFFLE_CV_SPREAD_IM;

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
		MASK_C_RE = VectorMask.fromArray(PFS, alter, 0);
		MASK_C_IM = VectorMask.fromArray(PFS, alter, 1);

		boolean[] secondhalf = new boolean[EPV];
		Arrays.fill(secondhalf, PFS.length() / 2, secondhalf.length, true);
		MASK_SECOND_HALF = VectorMask.fromArray(PFS, secondhalf, 0);

		// [r0, r1, ...] -> [(r0, ?), (r1, ?), ...], take ? from last element for now
		SHUFFLE_RV_TO_CV_RE = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? (i / 2) : (EPV - 1));
		// [r0, r1, ...] -> [(r0, r0), (r1, r1), ...]
		SHUFFLE_RV_TO_CV_BOTH = VectorShuffle.shuffle(PFS, i -> i / 2);

		// [r0, r1, ..., r_len] -> [(r0, ?), (r1, ?), ... (r_{len/2}, ?)]
		SHUFFLE_RV_TO_CV_RE_LOW = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? (i / 2) : 0);
		// [r0, r1, ..., r_len] -> [(?, r0), (?, r1), ... (?, r_{len/2})]
		SHUFFLE_RV_TO_CV_IM_LOW = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? 0 : (i / 2));

		// [..., r_{len/2} ..., r_len] -> [(r_{len/2}, ?), ..., (r_len, ?)]
		SHUFFLE_RV_TO_CV_RE_HIGH = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? (i / 2 + EPV / 2) : 0);
		// [..., r_{len/2} ..., r_len] -> [(?, r_{len/2}), ..., (?, r_len)]
		SHUFFLE_RV_TO_CV_IM_HIGH = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? 0 : (i / 2 + EPV / 2));

		// [re, im] -> [(re, im), (re, im), (re, im), ...]
		SHUFFLE_CS_TO_CV_SPREAD = VectorShuffle.shuffle(PFS, i -> i % 2);

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

		// [(re0, im0), (re1, im1), ...] -> [(im0, re0), (im1, re1), ...]
		SHUFFLE_CV_SWAP_RE_IM = VectorShuffle.shuffle(PFS, i -> (i % 2 == 0) ? i + 1 : i - 1);
	}

	public static void rv_add_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.add(x).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] += x;
	}

	public static void rv_add_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vz.add(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] += x[xOffset++];
	}

	public static void cv_add_rs_i(float z[], int zOffset, float x, int count) {
		FloatVector vx = null;
		if (count >= EPV2)
			vx = FloatVector.zero(PFS).blend(x, MASK_C_RE);

		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			//@DONE: it is faster than add(x, MASK_C_RE)
			vz.add(vx).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] += x;
			zOffset += 2;
		}
	}

	public static void cv_add_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);
			vz.add(vx).intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset++];
			zOffset += 2;
		}
	}

	public static void cv_add_cs_i(float z[], int zOffset, float x[], int count) {
		FloatVector vx = null;
		//@DONE: It is faster than FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0)
		if (count>= EPV2)
				vx = FloatVector.fromArray(FS64, x, 0).reshape(PFS).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.add(vx).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[0];
			z[zOffset + 1] += x[1];
			zOffset += 2;
		}
	}

	public static void cv_add_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vz.add(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset + 0];
			z[zOffset + 1] += x[xOffset + 1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.add(y).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] + y;
	}

	public static void rv_add_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.add(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] + y[yOffset++];
	}

	public static void cv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		if (count >= EPV2)
			vy = FloatVector.zero(PFS).blend(y, MASK_C_RE);

		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: it is faster than add(y, MASK_C_RE)
			vx.add(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y;
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_add_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			final FloatVector vy = FloatVector.fromArray(PFS2, y, yOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);
			vx.add(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset++];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}

	}

	public static void cv_add_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		FloatVector vy = null;
		//@DONE: It is faster than FloatVector.fromArray(PFS, y, 0, LOAD_CS_TO_CV_SPREAD, 0)
		if (count >= EPV2)
			vy = FloatVector.fromArray(FS64, y, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.add(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[0];
			z[zOffset + 1] = x[xOffset + 1] + y[1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_add_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.add(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset + 0];
			z[zOffset + 1] = x[xOffset + 1] + y[yOffset + 1];
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_sub_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.sub(x).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] -= x;
	}

	public static void rv_sub_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vz.sub(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] -= x[xOffset++];
	}

	public static void cv_sub_rs_i(float z[], int zOffset, float x, int count) {
		FloatVector vx = null;
		if (count >= EPV2)
			vx = FloatVector.zero(PFS).blend(x, MASK_C_RE);

		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			//@DONE: it is faster than sub(x, MASK_C_RE)
			vz.sub(vx).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] -= x;
			zOffset += 2;
		}
	}

	public static void cv_sub_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);
			vz.sub(vx).intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] -= x[xOffset++];
			zOffset += 2;
		}
	}

	public static void cv_sub_cs_i(float z[], int zOffset, float x[], int count) {
		FloatVector vx = null;
		//@DONE: It is faster than FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0)
		if (count >= EPV2)
			vx = FloatVector.fromArray(FS64, x, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.sub(vx).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] -= x[0];
			z[zOffset + 1] -= x[1];
			zOffset += 2;
		}
	}

	public static void cv_sub_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vz.sub(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] -= x[xOffset + 0];
			z[zOffset + 1] -= x[xOffset + 1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_sub_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.sub(y).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] - y;
	}

	public static void rs_sub_rv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		FloatVector vx = null;
		if (count >= EPV)
			vx = FloatVector.broadcast(PFS, x);
			
		while (count >= EPV) {
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.sub(vy).intoArray(z, zOffset);

			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x - y[yOffset++];
	}

	public static void rv_sub_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.sub(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] - y[yOffset++];
	}

	public static void cv_sub_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		if (count >= EPV2)
			vy = FloatVector.zero(PFS).blend(y, MASK_C_RE);

		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: it is faster than add(x, MASK_C_RE)
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.sub(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y;
			z[zOffset + 1] = x[xOffset + 1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rs_sub_cv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		FloatVector vx = null;
		if (count >= EPV2)
			vx = FloatVector.broadcast(PFS, x).blend(FloatVector.zero(PFS), MASK_C_IM);

		zOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.sub(vy).intoArray(z, zOffset);

			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x - y[yOffset + 0];
			z[zOffset + 1] = -y[yOffset + 1];
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_sub_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			final FloatVector vy = FloatVector.fromArray(PFS2, y, yOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);
			vx.sub(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[yOffset++];
			z[zOffset + 1] = x[xOffset + 1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_sub_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.sub(vy).intoArray(z, zOffset);

			xOffset += EPV2;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] - y[yOffset + 0];
			z[zOffset + 1] = -y[yOffset + 1];
			xOffset += 1;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_sub_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		FloatVector vy = null;
		//@DONE: It is faster than FloatVector.fromArray(PFS, y, 0, LOAD_CS_TO_CV_SPREAD, 0)
		if (count >= EPV2)
			vy = FloatVector.fromArray(FS64, y, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.sub(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[0];
			z[zOffset + 1] = x[xOffset + 1] - y[1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cs_sub_cv(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
		FloatVector vx = null;
		//@DONE: It is faster than FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0)
		if (count >= EPV2)
			vx = FloatVector.fromArray(FS64, x, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.sub(vy).intoArray(z, zOffset);

			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[0] - y[yOffset + 0];
			z[zOffset + 1] = x[1] - y[yOffset + 1];
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_sub_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.sub(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[yOffset + 0];
			z[zOffset + 1] = x[xOffset + 1] - y[yOffset + 1];
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_mul_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(x).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] *= x;
	}

	public static void rv_mul_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] *= x[xOffset++];
	}

	public static void cv_mul_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(x).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] *= x;
			z[zOffset + 1] *= x;
			zOffset += 2;
		}
	}

	public static void cv_mul_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(vx).intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] *= x[xOffset];
			z[zOffset + 1] *= x[xOffset];
			xOffset += 1;
			zOffset += 2;
		}
	}

	public static void cv_mul_cs_i(float z[], int zOffset, float x[], int count) {
		FloatVector vxre = null, vxim = null;
		if (count >= EPV2) {
			// vxre is [(x.re, x.re), (x.re, x.re), ...]
			vxre = FloatVector.broadcast(PFS, x[0]);
			// vxim is [(x.im, x.im), (x.im, x.im), ...]
			vxim = FloatVector.broadcast(PFS, x[1]);
		}

		zOffset <<= 1;

		while (count >= EPV2) {
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// vmulxre [(z[0].re * x.re, z[0].im * x.re), (z[1].re * x.re, z[1].im * x.re), ...]
			final FloatVector vmulxre = vz.mul(vxre);
			// vmulxim [(z[0].re * x.im, z[0].im * x.im), (z[1].re * x.im, z[1].im * x.im), ...]
			final FloatVector vmulxim = vz.mul(vxim);
			// vmulximswap is [(z[0].im * x.im, z[0].re * x.im), (z[1].im * x.im, z[1].re * x.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre ([z[0].re * x.re - z[0].im * x.im, ?], ...)
			final FloatVector vrre = vmulxre.sub(vmulximswap);
			// vrim ([?, z[0].im * x.re + z[0].re * x.im], ...)
			final FloatVector vrim = vmulxre.add(vmulximswap);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1, k2;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[0];
			k1 = z[zOffset + 1] * x[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[0] + x[1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
		}
	}

	public static void cv_mul_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// vmulxre is [(z[0].re * x.re, z[0].im * x.re), (z[1].re * x.re, z[1].im * x.re), ...]
			final FloatVector vmulxre = vz.mul(vxre);

			// vmulxre is [(z[0].re * x.im, z[0].im * x.im), (z[1].re * x.im, z[1].im * x.im), ...]
			final FloatVector vmulxim = vz.mul(vxim);

			// vmulximswap is [(z[0].im * x.im, z[0].re * x.im), (z[1].im * x.im, z[1].re * x.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is ([z[0].re * x.re - z[0].im * x.im, ?], ...)
			final FloatVector vrre = vmulxre.sub(vmulximswap);
			// vrim is ([?, z[0].im * x.re + z[0].re * x.im], ...)
			final FloatVector vrim = vmulxre.add(vmulximswap);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1, k2;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] + x[xOffset + 1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_mul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		//@DONE: it is fater thab vx.mul(y)
		if (count >= EPV)
		    vy = FloatVector.broadcast(PFS, y);

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.mul(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * y;
	}

	public static void rv_mul_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);

			vx.mul(vy).intoArray(z, zOffset);
			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * y[yOffset++];
	}

	public static void cv_mul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		//@DONE: it is fater thab vx.mul(y)
		if (count >= EPV2)
		    vy = FloatVector.broadcast(PFS, y);

		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.mul(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y;
			z[zOffset + 1] = x[xOffset + 1] * y;
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_mul_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vy = FloatVector.fromArray(PFS2, y, yOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			vx.mul(vy).intoArray(z, zOffset);
			yOffset += EPV2;
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] * y[yOffset];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_mul_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		FloatVector vyre = null, vyim = null;
		if (count >= EPV2) {
			// vyre is [(y.re, y.re), (y.re, y.re), ...]
			vyre = FloatVector.broadcast(PFS, y[0]);
			// vyim is [(y.im, y.im), (y.im, y.im), ...]
			vyim = FloatVector.broadcast(PFS, y[1]);
		}

		zOffset <<= 1;
		xOffset <<= 1;
		while (count >= EPV2) {
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vmulyre [(x[0].re * y.re, x[0].im * y.re), (x[1].re * y.re, x[1].im * y.re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// vmulyim [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y.im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// vmulximswap [(x[0].im * y.im, x[0].re y x.im), (x[1].im * y.im, x[1].re * y.im), ...]
			final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// it is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulyre.sub(vmulximswap);
			// it is ([?, x[0].im * y.re + x[0].re * y.im], ...)
			final FloatVector vrim = vmulyre.add(vmulximswap);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[0];
			k1 = x[xOffset + 1] * y[1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[0] + y[1]) - k0 - k1;
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_mul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vmulyre is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// vmulyim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// vmulximswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
			final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulyre.sub(vmulximswap);
			// vrim is ([?, x[0].im * y.re + x[0].re * y.im], ...)
			final FloatVector vrim = vmulyre.add(vmulximswap);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_div_rs_i(float z[], int zOffset, float x, int count) {
		FloatVector vx = null;
		//@DONE: it is fater thab vz.mul(x)
		if (count >= EPV)
		    vx = FloatVector.broadcast(PFS, x);

		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(vx).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] /= x;
	}

	public static void rv_div_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] /= x[xOffset++];
	}

	public static void cv_div_rs_i(float z[], int zOffset, float x, int count) {
		FloatVector vx = null;
		//@DONE: it is fater thab vz.mul(x)
		if (count >= EPV2)
		    vx = FloatVector.broadcast(PFS, x);

		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(vx).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] /= x;
			z[zOffset + 1] /= x;
			zOffset += 2;
		}
	}

	public static void cv_div_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.div(vx).intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] /= x[xOffset];
			z[zOffset + 1] /= x[xOffset];
			xOffset += 1;
			zOffset += 2;
		}
	}

	public static void cv_div_cs_i(float z[], int zOffset, float x[], int count) {
		FloatVector vxre = null;
		FloatVector vxim = null;
		FloatVector vxsq = null;

		if (count >= EPV2) {
			// vxre is [(x.re, x.re), (x.re, x.re), ...]
			vxre = FloatVector.broadcast(PFS, x[0]);
			// vxim is [(x.im, x.im), (x.im, x.im), ...]
			vxim = FloatVector.broadcast(PFS, x[1]);
			// vxsq is [(x.re * x.re + x.im * x.im, x.re * x.re + x.im * x.im), ...]
			vxsq = vxre.mul(vxre).add(vxim.mul(vxim));
		}

		zOffset <<= 1;

		while (count >= EPV2) {
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// vmulxre is [(z[0].re * x.re, z[0].im * x.re), (z[1].re * x.re, z[1].im * x.re), ...]
			final FloatVector vmulxre = vz.mul(vxre);
			// vmulxm is [(z[0].re * x.im, z[0].im * x.im), (z[1].re * x.im, z[1].im * x.im), ...]
			final FloatVector vmulxim = vz.mul(vxim);
			// vmulximswap is [(z[0].im * x.im, z[0].re * x.im), (z[1].im * x.im, z[1].re * x.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is ([z[0].re * x.re + z[0].im * x.im, ?], ...)
			final FloatVector vrre = vmulxre.add(vmulximswap);
			// vrim is ([?, z[0].im * x.re - z[0].re * x.im], ...)
			final FloatVector vrim = vmulxre.sub(vmulximswap);

			// Divide, Blend together & save
			vrre.blend(vrim, MASK_C_IM).div(vxsq).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		float re, im, sq = 0;
		if (count > 0)
			sq = x[0] * x[0] + x[1] * x[1];
		while (count-- > 0) {
			re = (z[zOffset + 0] * x[0] + z[zOffset + 1] * x[1]) / sq;
			im = (z[zOffset + 1] * x[0] - z[zOffset + 0] * x[1]) / sq;
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
		}
	}

	public static void cv_div_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// vmulxre is [(z[0].re * x.re, z[0].im * x.re), (z[1].re * x.re, z[1].im * x.re), ...]
			final FloatVector vmulxre = vz.mul(vxre);
			// vmulxim is [(z[0].re * x.im, z[0].im * x.im), (z[1].re * x.im, z[1].im * x.im), ...]
			final FloatVector vmulxim = vz.mul(vxim);
			// vmulximswap is [(z[0].im * x.im, z[0].re * x.im), (z[1].im * x.im, z[1].re * x.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// vxsq is [(x[0].re * x[0].re + x[0].im * x[0].im, x[0].re * x[0].re + x[0].im * x[0].im), (x[1].re * x[1].re + x[1].im * x[1].im, x[1].re * x[1].re + x[1].im * x[1].im), ...]
			final FloatVector vxsq = vxre.mul(vxre).add(vxim.mul(vxim));

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is ([z[0].re * x[0].re - z[0].im * x[0].im, ?], ...)
			final FloatVector vrre = vmulxre.add(vmulximswap);
			// vrim is ([?, z[0].im * x.re - z[0].re * x.im], ...)
			final FloatVector vrim = vmulxre.sub(vmulximswap);

			// Divide, Blend together & save
			vrre.blend(vrim, MASK_C_IM).div(vxsq).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float re, im, sq;
		while (count-- > 0) {
			sq = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			re = (z[zOffset + 0] * x[xOffset + 0] + z[zOffset + 1] * x[xOffset + 1]) / sq;
			im = (z[zOffset + 1] * x[xOffset + 0] - z[zOffset + 0] * x[xOffset + 1]) / sq;
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void rv_div_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		//@DONE: it is fater thab vx.div(y)
		if (count >= EPV)
		    vy = FloatVector.broadcast(PFS, y);

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.div(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] / y;
	}

	public static void rs_div_rv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		FloatVector vx = null;
		if (count >= EPV)
			vx = FloatVector.broadcast(PFS, x);

		while (count >= EPV) {
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.div(vy).intoArray(z, zOffset);

			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x / y[yOffset++];
	}

	public static void rv_div_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.div(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = x[xOffset++] / y[yOffset++];
	}

	public static void cv_div_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		//@DONE: it is fater thab vx.div(y)
		if (count >= EPV2)
		    vy = FloatVector.broadcast(PFS, y);

		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.div(vy).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y;
			z[zOffset + 1] = x[xOffset + 1] / y;
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rs_div_cv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		FloatVector vx = null;
		if (count >= EPV2) {
			// vx is [(x[0], 0), (x[1], 0), ...]
			vx = FloatVector.broadcast(PFS, x).blend(0.0f, MASK_C_IM);
		}

		zOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vmulxre is [(x * y[0].re, 0), (x * y[1].re, 0), ...]
			final FloatVector vmulxre = vx.mul(vyre);
			// vmulxim is [(x * y[0].im, 0), (x * y[1].im, 0), ...]
			final FloatVector vmulxim = vx.mul(vyim);
			// vmulximswap is [(0, x * y[0].im), (0, x * y[1].im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// vysq is [(y[0].re * y[0].re + y[0].im * y[0].im, y[0].re * y[0].re + y[0].im * y[0].im), (y[1].re * y[1].re + y[1].im * y[1].im, y[1].re * y[1].re + y[1].im * y[1].im), ...]
			final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

			// Now blend real parts and negated imaginary parts and divide by abs(y)^2
			final FloatVector vr = vmulxre.blend(vmulximswap.neg(), MASK_C_IM).div(vysq);
			// Save
			vr.intoArray(z, zOffset);

			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float sq;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = x * y[yOffset + 0] / sq;
			z[zOffset + 1] = -x * y[yOffset + 1] / sq;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_div_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vy = FloatVector.fromArray(PFS2, y, yOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			vx.div(vy).intoArray(z, zOffset);

			yOffset += EPV2;
			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] / y[yOffset];
			xOffset += 2;
			yOffset += 1;
			zOffset += 2;
		}
	}

	public static void rv_div_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			// vx is [(x[0], 0), (x[1], 0), ...]
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);

			// vmulxre is [(x[0] * y[0].re, 0), (x[1] * y[1].re, 0), ...]
			final FloatVector vmulxre = vx.mul(vyre);
			// vmulxim is [(x[0] * y[0].im, 0), (x[1] * y[1].im, 0), ...]
			final FloatVector vmulxim = vx.mul(vyim);
			// vmulximswap is [(0, x[0] * y[0].im), (0, x[1] * y[1].im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// vysq is [(y[0].re * y[0].re + y[0].im * y[0].im, y[0].re * y[0].re + y[0].im * y[0].im), (y[1].re * y[1].re + y[1].im * y[1].im, y[1].re * y[1].re + y[1].im * y[1].im), ...]
			final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

			// Now blend real parts and negated imaginary parts and divide by abs(y)^2
			final FloatVector vr = vmulxre.blend(vmulximswap.neg(), MASK_C_IM).div(vysq);
			// Save
			vr.intoArray(z, zOffset);

			xOffset += EPV2;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float sq;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = x[xOffset] * y[yOffset + 0] / sq;
			z[zOffset + 1] = -x[xOffset] * y[yOffset + 1] / sq;
			xOffset += 1;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_div_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		FloatVector vyre = null;
		FloatVector vyim = null;
		FloatVector vysq = null;

		if (count >= EPV2) {
			// vyre is [(y.re, y.re), (y.re, y.re), ...]
			vyre = FloatVector.broadcast(PFS, y[0]);
			// vyim is [(y.im, y.im), (y.im, y.im), ...]
			vyim = FloatVector.broadcast(PFS, y[1]);
			// vysq is [(y.re * y.re + y.im * y.im, y.re * y.re + y.im * y.im), (y.re * y.re + y.im * y.im, y.re * y.re + y.im * y.im), ...]
			vysq = vyre.mul(vyre).add(vyim.mul(vyim));
		}

		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vmulxre is [(x[0].re * y.re, x[0].im * y.re), (x[1].re * y.re, x[1].im * y.re), ...]
			final FloatVector vmulxre = vx.mul(vyre);
			// vmulxim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y.im), ...]
			final FloatVector vmulxim = vx.mul(vyim);
			// vmulximswap is [(x[0].im * y.im, x[0].re * y.im), (x[1].im * y.im, x[1].re * y.im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is ([x[0].re * y.re + x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulxre.add(vmulximswap);
			// vrim is ([?, x[0].im * y.re - x[0].re * y.im], ...)
			final FloatVector vrim = vmulxre.sub(vmulximswap);

			// Divide, Blend together & save
			vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float sq = 0;
		if (count > 0)
			sq = y[0] * y[0] + y[1] * y[1];
		while (count-- > 0) {
			z[zOffset + 0] = (x[xOffset + 0] * y[0] + x[xOffset + 1] * y[1]) / sq;
			z[zOffset + 1] = (x[xOffset + 1] * y[0] - x[xOffset + 0] * y[1]) / sq;
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cs_div_cv(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
		FloatVector vx = null;
		//@DONE: It is faster than FloatVector.fromArray(PFS, x, 0, LOAD_CS_TO_CV_SPREAD, 0)
		if (count >= EPV2)
			vx = FloatVector.fromArray(FS64, x, 0).reshape(PFS).rearrange(SHUFFLE_CS_TO_CV_SPREAD);

		zOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vmulxre [(x.re * y[0].re, x.im * y[0].re), (x.re * y[1].re, x.im * y[1].re), ...]
			final FloatVector vmulxre = vx.mul(vyre);
			// vmulxim [(x.re * y[0].im, x.im * y[0].im), (x.re * y[1].im, x.im * y[1].im), ...]
			final FloatVector vmulxim = vx.mul(vyim);
			// vmulximswap is [(x.im * y[0].im, x.re * y[0].im), (x.im * y[1].im, x.re * y[1].im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// Get abs to divide
			final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is [(x.re * y[0].re + x.im * y[0].im, ?), (x.re * y[1].re + x.im * y[1].im, ?), ...]
			final FloatVector vrre = vmulxre.add(vmulximswap);
			// vrim it is [(?, x.im * y[0].re - x.re * y[0].im), (?, x.im * y[1].re - x.re * y[1].im), ...]
			final FloatVector vrim = vmulxre.sub(vmulximswap);

			// Divide, Blend together & save
			vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float sq;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
			z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_div_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vmulxre [(x[0].re * y[0].re, x[0].im * y[0].re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
			final FloatVector vmulxre = vx.mul(vyre);
			// vmulxim [(x[0].re * y[0].im, x[0].im * y[0].im), (x[1].re * y[1].im, x[1].im * y[1].im), ...]
			final FloatVector vmulxim = vx.mul(vyim);
			// vmulximswap is [(x[0].im * y[0].im, x[0].re * y[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			// vysq is [(y[0].re * y[0].re + y[0].im * y[0].im, y[0].re * y[0].re + y[0].im * y[0].im), (y[1].re * y[1].re + y[1].im * y[1].im, y[1].re * y[1].re + y[1].im * y[1].im), ...]
			final FloatVector vysq = vyre.mul(vyre).add(vyim.mul(vyim));

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre is [(x[0].re * y[0].re + x[0].im * y[0].im, ?), (x[1].re * y[1].re + x[1].im * y[1].im, ?), ...]
			final FloatVector vrre = vmulxre.add(vmulximswap);
			// vrim it is [(?, x[0].im * y[0].re - x[0].re * y[0].im), (?, x[1].im * y[1].re - x[1].re * y[1].im), ...]
			final FloatVector vrim = vmulxre.sub(vmulximswap);

			// Divide, Blend together & save
			vrre.blend(vrim, MASK_C_IM).div(vysq).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float sq;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = (x[xOffset + 0] * y[yOffset + 0] + x[xOffset + 1] * y[yOffset + 1]) / sq;
			z[zOffset + 1] = (x[xOffset + 1] * y[yOffset + 0] - x[xOffset + 0] * y[yOffset + 1]) / sq;
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_conjmul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.mul(vy.neg(MASK_C_IM)).intoArray(z, zOffset);

			xOffset += EPV2;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * y[yOffset + 0];
			z[zOffset + 1] = -x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_conjmul_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// vmulxre is [(z[0].re * x[0].re, z[0].im * x[0].re), (z[1].re * x[0].re, z[1].im * x[0].re), ...]
			final FloatVector vmulxre = vz.mul(vxre);
			// vmulxim is [(z[0].re * x[0].im, z[0].im * x[0].im), (z[1].re * x[1].im, z[1].im * x[1].im), ...]
			final FloatVector vmulxim = vz.mul(vxim);
			// vmulximswap is [(z[0].im * x[0].im, z[0].re * x[0].im), (z[1].im * x[1].im, z[1].re * x[1].im), ...]
			final FloatVector vmulximswap = vmulxim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre it is [(z[0].re * x[0].re + z[0].im * x[0].im, ?), (z[1].re * x[1].re + z[1].im * x[1].im, ?)]
			final FloatVector vrre = vmulxre.add(vmulximswap);
			// vrim it is [(?, z[0].im * x[0].re - z[0].re * x[0].im), (?, z[1].im * x[1].re - z[1].re * x[1].im), ...]
			final FloatVector vrim = vmulxre.sub(vmulximswap);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1, k2;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] - x[xOffset + 1]);
			z[zOffset + 0] = k0 + k1;
			z[zOffset + 1] = k2 - k0 + k1;
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_conjmul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// Load x
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vmuly is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// vmuly is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// vmulyswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
			final FloatVector vmulyimswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: Looks like sub(vmulximswap, MASK_C_RE) and add(vmulximswap, MASK_C_IM) is slower
			// vrre it is [(x[0].re * y[0].re + x[0].im * y[0].im, ?), (x[1].re * y[1].re + x[1].im * y[1].im, ?)]
			final FloatVector vrre = vmulyre.add(vmulyimswap);
			// vrim it is [(?, x[0].im * y[0].re - x[0].re * y[0].im), (?, x[1].im * y[1].re - x[1].re * y[1].im), ...]
			final FloatVector vrim = vmulyre.sub(vmulyimswap);

			// Blend together & save
			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 + k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] - y[yOffset + 1]) - k0 + k1;
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_conj_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.neg(MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		zOffset++;
		while (count-- > 0) {
			z[zOffset] = -z[zOffset]; // + 1
			zOffset += 2;
		}
	}

	public static void cv_conj(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.neg(MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0];
			z[zOffset + 1] = -x[xOffset + 1];
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void rv_expi(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count >= EPV) {
			//@DONE: check, do we need pack and process twice elements, and save result twice
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_RV_TO_CV_BOTH, 0);
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vzre = vx.cos();
			final FloatVector vzim = vx.sin();
			// And now we should combine TWO z vectors from re/im, as they are packed without empty slots
			vzre.rearrange(SHUFFLE_RV_TO_CV_RE_LOW).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_LOW), MASK_C_IM).intoArray(z, zOffset);
			vzre.rearrange(SHUFFLE_RV_TO_CV_RE_HIGH).blend(vzim.rearrange(SHUFFLE_RV_TO_CV_IM_HIGH), MASK_C_IM).intoArray(z, zOffset + EPV);

			// We stored twice as much complex numbers
			xOffset += EPV;
			zOffset += EPV * 2;
			count -= EPV;
		}
		// If we have half-vector
		if (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset);
			final FloatVector vzre = vx.cos().reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			final FloatVector vzim = vx.sin().reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_BOTH);
			vzre.blend(vzim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = (float) Math.cos(x[xOffset]);
			z[zOffset + 1] = (float) Math.sin(x[xOffset]);
			xOffset += 1;
			zOffset += 2;
		}
	}

	public static void rv_exp_i(float z[], int zOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.exp().intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = (float)Math.exp(z[zOffset]);
			zOffset++;
		}
	}

	public static void cv_exp_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		// To process
		while (count >= EPV) {
			//@DONE: one load & two reshuffles are faster
			//@DONE: it is better than process one vector
			final FloatVector vz1 = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector vz2 = FloatVector.fromArray(PFS, z, zOffset + EPV);

			// Ger vz1re/vz1im till vz2 is loading
			final FloatVector vz1re = vz1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vz1im = vz1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			// Ger vz2re/vz2im
			final FloatVector vz2re = vz2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vz2im = vz2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			// Combine them
			final FloatVector vzreexp = vz1re.blend(vz2re, MASK_SECOND_HALF).exp();
			final FloatVector vzim = vz1im.blend(vz2im, MASK_SECOND_HALF);

			final FloatVector vrre = vzreexp.mul(vzim.cos());
			final FloatVector vrim = vzreexp.mul(vzim.sin());

			// And combine & store twice
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

			// We loaded and stored twice as many numbers
			zOffset += EPV * 2;
			count -= EPV;
		}

		//@DONE: Don't process obe PFS-sized vector, it is too expensive to setup for only one

		while (count-- > 0) {
			float g = (float) Math.exp(z[zOffset + 0]);
			z[zOffset + 0] = g * (float) Math.cos(z[zOffset + 1]);
			z[zOffset + 1] = g * (float) Math.sin(z[zOffset + 1]);
			zOffset += 2;
		}
	}

	public static void rv_exp(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.exp().intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = (float)Math.exp(x[xOffset++]);
	}

	public static void cv_exp(float z[], int zOffset, float x[], int xOffset, int count) {
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

		//@DONE: Don't process obe PFS-sized vector, it is too expensive to setup for only one

		while (count-- > 0) {
			float g = (float) Math.exp(x[xOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(x[xOffset + 1]);
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_im(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while (count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_IM, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);

			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);
			vxim.intoArray(z, zOffset);

			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		xOffset += 1;
		while (count-- > 0) {
			z[zOffset++] = x[xOffset]; // + 1
			xOffset += 2;
		}
	}

	public static void cv_re(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while (count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_RE, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + EPV);

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			vxre.intoArray(z, zOffset);

			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = x[xOffset];
			xOffset += 2;
		}
	}

	public static void cv_abs(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + PFS.length());

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			vxre.hypot(vxim).intoArray(z, zOffset);
			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			xOffset += 2;
		}
	}

	public static void cv_arg(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + PFS.length());

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			vxim.atan2(vxre).intoArray(z, zOffset);
			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void cv_argmul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		FloatVector vy = null;
		//@DONE: it is fater thab ...mul(y)
		if (count >= EPV)
		    vy = FloatVector.broadcast(PFS, y);

		xOffset <<= 1;

		while(count >= EPV) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vx1 = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vx2 = FloatVector.fromArray(PFS, x, xOffset + PFS.length());

			final FloatVector vx1re = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vx1im = vx1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vx2re = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vx2im = vx2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vxre = vx1re.blend(vx2re, MASK_SECOND_HALF);
			final FloatVector vxim = vx1im.blend(vx2im, MASK_SECOND_HALF);

			vxim.atan2(vxre).mul(vy).intoArray(z, zOffset);

			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset++] = y * (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void rv_abs_i(float z[], int zOffset, int count) {
		while(count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = Math.abs(z[zOffset]);
			zOffset += 1;
		}
	}

	public static void rv_abs(float z[], int zOffset, float x[], int xOffset, int count) {
		while(count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = Math.abs(x[xOffset++]);
	}

	public static void rv_cvt(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count >= EPV2) {
			//@DONE: It is faster than FloatVector.fromArray(PFS, x, xOffset, MASK_C_RE, LOAD_RV_TO_CV_RE, 0);
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE);
			vx.intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset];
			z[zOffset + 1] = 0.0f;
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_r2p_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			// vzreezp is [(z[0].re, z[0].re), (z[1].re, z[1].re), ...]
			final FloatVector vzre = vz.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vzim is [(z[0].im, z[0].im), (z[1].im, z[1].im), ...]
			final FloatVector vzim = vz.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: Masks are insanely expensive here
			final FloatVector vrre = vzre.hypot(vzim);
			final FloatVector vrim = vzim.atan2(vzre);

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		float abs, arg;
		while (count-- > 0) {
			abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
			arg = (float)Math.atan2(z[zOffset + 1], z[zOffset + 0]);
			z[zOffset + 0] = abs;
			z[zOffset + 1] = arg;
			zOffset += 2;
		}
	}

	public static void cv_r2p(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxreexp is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: Masks are insanely expensive here
			final FloatVector vrre = vxre.hypot(vxim);
			final FloatVector vrim = vxim.atan2(vxre);

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			z[zOffset + 1] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_p2r_i(float z[], int zOffset, int count) {
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			//@TODO: check, do we need pack and process twice elements, and save result twice
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			// vzreezp is [(z[0].re, z[0].re), (z[1].re, z[1].re), ...]
			final FloatVector vzre = vz.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vzim is [(z[0].im, z[0].im), (z[1].im, z[1].im), ...]
			final FloatVector vzim = vz.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: .cos(MASK_C_IM)/.sin(MASK_C_RE) is much slower
			final FloatVector vrre = vzre.mul(vzim.cos());
			final FloatVector vrim = vzre.mul(vzim.sin());

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV2;
		}

		float re, im;
		while (count-- > 0) {
			re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
			im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
		}
	}

	public static void cv_p2r(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			//@TODO: check, do we need pack and process twice elements, and save result twice
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vxreexp is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			//@DONE: .cos(MASK_C_IM)/.sin(MASK_C_RE) is much slower
			final FloatVector vrre = vxre.mul(vxim.cos());
			final FloatVector vrim = vxre.mul(vxim.sin());

			vrre.blend(vrim, MASK_C_IM).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = x[xOffset + 0] * (float)Math.sin(x[xOffset + 1]);
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static float rv_sum(float x[], int xOffset, int count) {
		float sum = 0.0f;

		while (count >= EPV) {
			sum += FloatVector.fromArray(PFS, x, xOffset).addAll();

			xOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++];
		return sum;
	}

	public static void cv_sum(float z[], float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@TODO Check, can we pack and process twice elements, and save result twice
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			re += vx.addAll(MASK_C_RE);
			im += vx.addAll(MASK_C_IM);

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

	public static void cv_sum(float z[], int zOffset, float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@TODO Check, can we pack and process twice elements, and save result twice
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			re += vx.addAll(MASK_C_RE);
			im += vx.addAll(MASK_C_IM);

			xOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static float rv_dot_rv(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			sum += vx.mul(vy).addAll();

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];

		return sum;
	}

	public static void rv_dot_cv(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		yOffset <<= 1;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + PFS.length());

			final FloatVector vy1re = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vy1im = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vy2re = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vy2im = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vyre = vy1re.blend(vy2re, MASK_SECOND_HALF);
			final FloatVector vyim = vy1im.blend(vy2im, MASK_SECOND_HALF);

			re += vx.mul(vyre).addAll();
			im += vx.mul(vyim).addAll();

			xOffset += EPV;
			yOffset += EPV * 2; // We load twice as much complex numbers
			count -= EPV;
		}

		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			yOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void rv_dot_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			//@DONE: It is faster than FloatVector.fromArray(PFS, y, yOffset, LOAD_CV_TO_CV_PACK_{RE|IM}, 0)
			final FloatVector vy1 = FloatVector.fromArray(PFS, y, yOffset);
			final FloatVector vy2 = FloatVector.fromArray(PFS, y, yOffset + PFS.length());

			final FloatVector vy1re = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_FIRST);
			final FloatVector vy1im = vy1.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_FIRST);

			final FloatVector vy2re = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_RE_SECOND);
			final FloatVector vy2im = vy2.rearrange(SHUFFLE_CV_TO_CV_PACK_IM_SECOND);

			final FloatVector vyre = vy1re.blend(vy2re, MASK_SECOND_HALF);
			final FloatVector vyim = vy1im.blend(vy2im, MASK_SECOND_HALF);

			re += vx.mul(vyre).addAll();
			im += vx.mul(vyim).addAll();

			xOffset += EPV;
			yOffset += EPV * 2; // We load twice as much complex numbers
			count -= EPV;
		}

		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			yOffset += 2;
		}
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void cv_dot_cv(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;
		yOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vmulyre is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// vmulyim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// vmulximswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
			final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: vmulyre.sub(vmulximswap, MASK_C_RE) / vmulyre.add(vmulximswap, MASK_C_IM) are slower
			// vrre is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulyre.sub(vmulximswap);
			// vrim is ([?, x[0].im * y.re + x[0].re * y.im], ...)
			final FloatVector vrim = vmulyre.add(vmulximswap);

			re += vrre.addAll(MASK_C_RE);
			im += vrim.addAll(MASK_C_IM);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			re += k0 - k1;
			im += (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
			xOffset += 2;
			yOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_dot_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vmulyre is [(x[0].re * y[0].re, x[0].im * y.re), (x[1].re * y[1].re, x[1].im * y[1].re), ...]
			final FloatVector vmulyre = vx.mul(vyre);
			// vmulyim is [(x[0].re * y.im, x[0].im * y.im), (x[1].re * y.im, x[1].im * y[1].im), ...]
			final FloatVector vmulyim = vx.mul(vyim);
			// vmulximswap is [(x[0].im * y[0].im, x[0].re * x[0].im), (x[1].im * y[1].im, x[1].re * y[1].im), ...]
			final FloatVector vmulximswap = vmulyim.rearrange(SHUFFLE_CV_SWAP_RE_IM);

			//@DONE: vmulyre.sub(vmulximswap, MASK_C_RE) / vmulyre.add(vmulximswap, MASK_C_IM) are slower
			// vrre is ([x[0].re * y.re - x[0].im * y.im, ?], ...)
			final FloatVector vrre = vmulyre.sub(vmulximswap);
			// vrim is ([?, x[0].im * y.re + x[0].re * y.im], ...)
			final FloatVector vrim = vmulyre.add(vmulximswap);

			re += vrre.addAll(MASK_C_RE);
			im += vrim.addAll(MASK_C_IM);

			xOffset += EPV;
			yOffset += EPV;
			count -= EPV2;
		}

		float k0, k1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			re += k0 - k1;
			im += (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
			xOffset += 2;
			yOffset += 2;
		}
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void rv_cpy(float z[], int zOffset, float x[], int xOffset, int count) {
		// Just for fun: maybe, it is faster than System.arraycopy()? :-)
		while (count >= EPV) {
			FloatVector.fromArray(PFS, x, xOffset).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}
		System.arraycopy(x, xOffset, z, zOffset, count);
	}

	public static void cv_cpy(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		zOffset <<= 1;

		// Just for fun: maybe, it is faster than System.arraycopy()? :-)
		while (count >= EPV2) {
			FloatVector.fromArray(PFS, x, xOffset).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}
		System.arraycopy(x, xOffset, z, zOffset, count * 2);
	}

	public static float rv_max(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;

		while (count >= EPV) {
			float localMax = FloatVector.fromArray(PFS, x, xOffset).maxAll();
			if (max < localMax)
				max = localMax;

			xOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			if (max < x[xOffset])
				max = x[xOffset];
			xOffset += 1;
		}
		return max;
	}

	public static void rv_max_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.max(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			if (x[xOffset] > z[zOffset])
				z[zOffset] = x[xOffset];
			xOffset += 1;
			zOffset += 1;
		}
	}

	public static void rv_max_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.max(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = x[xOffset] > y[yOffset] ? x[xOffset] : y[yOffset];
			xOffset += 1;
			yOffset += 1;
			zOffset += 1;
		}
	}

	public static void cv_max(float z[], float x[], int xOffset, int count) {
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
			float localMax = vxabs.maxAll();
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

	public static void cv_max(float z[], int zOffset, float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		zOffset <<= 1;

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
			float localMax = vxabs.maxAll();
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
		z[zOffset + 0] = x[i + 0];
		z[zOffset + 1] = x[i + 1];
	}

	public static void cv_max_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vzre is [(z[0].re, z[0].re), (z[1].re, z[1].re), ...]
			final FloatVector vzre = vz.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vzim is [(z[0].im, z[0].im), (z[1].im, z[1].im), ...]
			final FloatVector vzim = vz.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			final FloatVector vzabs = vzre.mul(vzre).add(vzim.mul(vzim));
			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));

			VectorMask<Float> xGz = vxabs.greaterThan(vzabs);
			vx.intoArray(z, zOffset, xGz);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) > (z[zOffset + 0] * z[zOffset + 0] + z[zOffset + 1] * z[zOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			}
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_max_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);

			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));
			final FloatVector vyabs = vyre.mul(vyre).add(vyim.mul(vyim));

			VectorMask<Float> xGy = vxabs.greaterThan(vyabs);
			vy.blend(vx, xGy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) > (y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			} else {
				z[zOffset + 0] = y[yOffset + 0];
				z[zOffset + 1] = y[yOffset + 1];
			}
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static float rv_min(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;

		while (count >= EPV) {
			float localMin = FloatVector.fromArray(PFS, x, xOffset).minAll();
			if (min > localMin)
				min = localMin;
			xOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			if (min > x[xOffset])
				min = x[xOffset];
			xOffset += 1;
		}
		return min;
	}

	public static void rv_min_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.min(vx).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			if (x[xOffset] < z[zOffset])
				z[zOffset] = x[xOffset];
			xOffset += 1;
			zOffset += 1;
		}
	}

	public static void rv_min_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);
			vx.min(vy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = x[xOffset] < y[yOffset] ? x[xOffset] : y[yOffset];
			xOffset += 1;
			yOffset += 1;
			zOffset += 1;
		}
	}

	public static void cv_min(float z[], float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
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
			float localMin = vxabs.minAll();
			if (min > localMin) {
				// Find it/ now
				for (int j = 0; j < EPV; j++) {
					if (vxabs.lane(j) == localMin) {
						i = xOffset + j * 2;
						break;
					}
				}
				min = localMin;
			}

			xOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (min > abs) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}

	public static void cv_min(float z[], int zOffset, float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		zOffset <<= 1;

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
			float localMin = vxabs.minAll();
			if (min > localMin) {
				// Find it/ now
				for (int j = 0; j < EPV; j++) {
					if (vxabs.lane(j) == localMin) {
						i = xOffset + j * 2;
						break;
					}
				}
				min = localMin;
			}

			xOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (min > abs) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[zOffset + 0] = x[i + 0];
		z[zOffset + 1] = x[i + 1];
	}

	public static void cv_min_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vz is [(z[0].re, z[0].im), (z[1].re, z[1].im), ...]
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);

			// vzre is [(z[0].re, z[0].re), (z[1].re, z[1].re), ...]
			final FloatVector vzre = vz.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vzim is [(z[0].im, z[0].im), (z[1].im, z[1].im), ...]
			final FloatVector vzim = vz.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			final FloatVector vzabs = vzre.mul(vzre).add(vzim.mul(vzim));
			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));

			VectorMask<Float> xLz = vxabs.lessThan(vzabs);
			vx.intoArray(z, zOffset, xLz);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) < (z[zOffset + 0] * z[zOffset + 0] + z[zOffset + 1] * z[zOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			}
			xOffset += 2;
			zOffset += 2;
		}
	}

	public static void cv_min_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		xOffset <<= 1;
		yOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			//@DONE: one load & two reshuffles are faster
			// vx is [(x[0].re, x[0].im), (x[1].re, x[1].im), ...]
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			// vy is [(y[0].re, y[0].im), (y[1].re, y[1].im), ...]
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);

			// vxre is [(x[0].re, x[0].re), (x[1].re, x[1].re), ...]
			final FloatVector vxre = vx.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vxim is [(x[0].im, x[0].im), (x[1].im, x[1].im), ...]
			final FloatVector vxim = vx.rearrange(SHUFFLE_CV_SPREAD_IM);

			// vyre is [(y[0].re, y[0].re), (y[1].re, y[1].re), ...]
			final FloatVector vyre = vy.rearrange(SHUFFLE_CV_SPREAD_RE);
			// vyim is [(y[0].im, y[0].im), (y[1].im, y[1].im), ...]
			final FloatVector vyim = vy.rearrange(SHUFFLE_CV_SPREAD_IM);

			final FloatVector vxabs = vxre.mul(vxre).add(vxim.mul(vxim));
			final FloatVector vyabs = vyre.mul(vyre).add(vyim.mul(vyim));

			VectorMask<Float> xLy = vxabs.lessThan(vyabs);
			vy.blend(vx, xLy).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) < (y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			} else {
				z[zOffset + 0] = y[yOffset + 0];
				z[zOffset + 1] = y[yOffset + 1];
			}
			xOffset += 2;
			yOffset += 2;
			zOffset += 2;
		}
	}

	public static int rv_maxarg(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;

		while (count >= EPV) {
			float localMax = FloatVector.fromArray(PFS, x, xOffset).maxAll();
			if (max < localMax) {
				max = localMax;
				i = xOffset;
			}
			xOffset += EPV;
			count -= EPV;
		}

		// Find max in vector
		if (i >= 0) {
			int i2 = i;
			for (int j = i; j < i + EPV; j++) {
				// We could compare here, as max got from here
				if (max == x[j]) {
					i2 = j;
					break;
				}
			}
			i = i2;
		}

		while (count-- > 0) {
			if (max < x[xOffset]) {
				max = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
		}
		return i;
	}

	public static int cv_maxarg(float x[], int xOffset, int count) {
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
			float localMax = vxabs.maxAll();

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

		return i >> 1;
	}

	public static int rv_minarg(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;

		while (count >= EPV) {
			float localMin = FloatVector.fromArray(PFS, x, xOffset).minAll();
			if (min > localMin) {
				min = localMin;
				i = xOffset;
			}

			xOffset += EPV;
			count -= EPV;
		}

		// Find min in vector
		if (i >= 0) {
			int i2 = i;
			for (int j = i; j < i + EPV; j++) {
				// We could compare here, as min got from here
				if (min == x[j]) {
					i2 = j;
					break;
				}
			}
			i = i2;
		}

		while (count-- > 0) {
			if (min > x[xOffset]) {
				min = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
		}
		return i;
	}

	public static int cv_minarg(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
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
			float localMin = vxabs.minAll();

			if (min > localMin) {
				// Find it now
				for (int j = 0; j < EPV; j++) {
					if (vxabs.lane(j) == localMin) {
						i = xOffset + j * 2;
						break;
					}
				}
				min = localMin;
			}

			xOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (min > abs) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
		}

		return i >> 1;
	}

	public static void rv_rs_lin_rv_rs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		FloatVector va1 = null;
		FloatVector va2 = null;
		//@DONE: it is fater thab ...mul(a1)
		if (count >= EPV) {
			va1 = FloatVector.broadcast(PFS, a1);
			va2 = FloatVector.broadcast(PFS, a2);
		}

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.mul(va1).add(vx.mul(va2)).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = z[zOffset] * a1 + x[xOffset] * a2;
			xOffset += 1;
			zOffset += 1;
		}
	}

	public static void rv_rs_lin_rv_rs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
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

	public static void rv_rs_lin_rv_cs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2[], int count) {
		FloatVector va1 = null;
		FloatVector va2re = null;
		FloatVector va2im = null;
		if (count >= EPV) {
			va1 = FloatVector.broadcast(PFS, a1);
			va2re = FloatVector.broadcast(PFS, a2[0]);
			va2im = FloatVector.broadcast(PFS, a2[1]);
		}

		zOffset <<= 1;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);

			final FloatVector vrre = vx.mul(va1).add(vy.mul(va2re));
			final FloatVector vrim = vy.mul(va2im);

			// Store twice
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * a1 + y[yOffset] * a2[0];
			z[zOffset + 1] = y[yOffset] * a2[1];
			xOffset += 1;
			yOffset += 1;
			zOffset += 2;
		}
	}

	public static void rv_cs_lin_rv_cs(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		FloatVector va1re = null;
		FloatVector va1im = null;
		FloatVector va2re = null;
		FloatVector va2im = null;
		if (count >= EPV) {
			va1re = FloatVector.broadcast(PFS, a1[0]);
			va1im = FloatVector.broadcast(PFS, a1[1]);
			va2re = FloatVector.broadcast(PFS, a2[0]);
			va2im = FloatVector.broadcast(PFS, a2[1]);
		}

		zOffset <<= 1;

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS, y, yOffset);

			final FloatVector vrre = vx.mul(va1re).add(vy.mul(va2re));
			final FloatVector vrim = vx.mul(va1im).add(vy.mul(va2im));

			// Store twice
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_FIRST).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_FIRST), MASK_C_IM).intoArray(z, zOffset);
			vrre.rearrange(SHUFFLE_CV_TO_CV_UNPACK_RE_SECOND).blend(vrim.rearrange(SHUFFLE_CV_TO_CV_UNPACK_IM_SECOND), MASK_C_IM).intoArray(z, zOffset + EPV);

			xOffset += EPV;
			yOffset += EPV;
			zOffset += EPV * 2;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * a1[0] + y[yOffset] * a2[0];
			z[zOffset + 1] = x[xOffset] * a1[1] + y[yOffset] * a2[1];
			xOffset += 1;
			yOffset += 1;
			zOffset += 2;
		}
	}

	public static void cv_rs_lin_rv_rs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		FloatVector va1 = null;
		FloatVector va2 = null;
		if (count >= EPV2) {
			va1 = FloatVector.broadcast(PFS, a1);
			va2 = FloatVector.broadcast(PFS2, a2);
		}

		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS2, x, xOffset);
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);

			// Rearrange of vx gives zeroes in im-parts and it could be added without any masks or blends
			vz.mul(va1).add(vx.mul(va2).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE)).intoArray(z, zOffset);

			xOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset] * a2;
			z[zOffset + 1] = z[zOffset + 1] * a1;
			xOffset += 1;
			zOffset += 2;
		}
	}

	public static void cv_rs_lin_rv_rs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		FloatVector va1 = null;
		FloatVector va2 = null;
		if (count >= EPV2) {
			va1 = FloatVector.broadcast(PFS, a1);
			va2 = FloatVector.broadcast(PFS2, a2);
		}

		xOffset <<= 1;
		zOffset <<= 1;

		while (count >= EPV2) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			final FloatVector vy = FloatVector.fromArray(PFS2, y, yOffset);

			// Rearrange of vy gives zeroes in im-parts and it could be added without any masks or blends
			vx.mul(va1).add(vy.mul(va2).reshape(PFS).rearrange(SHUFFLE_RV_TO_CV_RE)).intoArray(z, zOffset);

			xOffset += EPV;
			yOffset += EPV2;
			zOffset += EPV;
			count -= EPV2;
		}

		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset] * a2;
			z[zOffset + 1] = x[xOffset + 1] * a1;
			xOffset += 2;
			yOffset += 1;
			zOffset += 2;
		}
	}

	public static void rv_10log10_i(float z[], int zOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().log10().mul(10.0f).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
		}
	}

	public static void rv_10log10(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().log10().mul(10.0f).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = 10 * (float)Math.log10(Math.abs(x[xOffset++]) + Float.MIN_NORMAL);
	}

	public static void rv_10log10_rs_i(float z[], int zOffset, float base, int count) {
		base = 10 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);

		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().log10().mul(10.0f).sub(base).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
		}
	}

	public static void rv_10log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().log10().mul(10.0f).sub(base).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = 10 * (float)Math.log10(Math.abs(x[xOffset++]) + Float.MIN_NORMAL) - base;
	}
	
	public static void cv_10log10(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
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

			vxabs.log10().mul(5.0f).intoArray(z, zOffset);

			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 5 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL);
			xOffset += 2;
			zOffset += 1;
		}
	}

	public static void cv_10log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);
		xOffset <<= 1;

		while(count >= EPV) {
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

			vxabs.log10().mul(5.0f).sub(base).intoArray(z, zOffset);

			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 5 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL) - base;
			xOffset += 2;
			zOffset += 1;
		}
	}

	public static void rv_20log10_i(float z[], int zOffset, int count) {
		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().log10().mul(20.0f).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10(Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
		}
	}

	public static void rv_20log10(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().log10().mul(20.0f).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = 20 * (float)Math.log10(Math.abs(x[xOffset++]) + Float.MIN_NORMAL);
	}

	public static void rv_20log10_rs_i(float z[], int zOffset, float base, int count) {
		base = 20 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);

		while (count >= EPV) {
			final FloatVector vz = FloatVector.fromArray(PFS, z, zOffset);
			vz.abs().log10().mul(20.0f).sub(base).intoArray(z, zOffset);

			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10(Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
		}
	}

	public static void rv_20log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 20 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);

		while (count >= EPV) {
			final FloatVector vx = FloatVector.fromArray(PFS, x, xOffset);
			vx.abs().log10().mul(20.0f).sub(base).intoArray(z, zOffset);

			xOffset += EPV;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0)
			z[zOffset++] = 20 * (float)Math.log10(Math.abs(x[xOffset++]) + Float.MIN_NORMAL) - base;
	}

	public static void cv_20log10(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;

		while(count >= EPV) {
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

			vxabs.log10().mul(10.0f).intoArray(z, zOffset);

			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL);
			xOffset += 2;
			zOffset += 1;
		}
	}

	public static void cv_20log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 20 * (float)Math.log10(Math.abs(base) + Float.MIN_NORMAL);
		xOffset <<= 1;

		while(count >= EPV) {
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

			vxabs.log10().mul(10.0f).sub(base).intoArray(z, zOffset);

			// We load twice as much complex numbers
			xOffset += EPV * 2;
			zOffset += EPV;
			count -= EPV;
		}

		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL) - base;
			xOffset += 2;
			zOffset += 1;
		}
	}
}
