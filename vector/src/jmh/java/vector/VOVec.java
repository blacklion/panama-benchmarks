/*****************************************************************************
 * Copyright (c) 2014, Lev Serebryakov <lev@serebryakov.spb.ru>
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
import jdk.incubator.vector.Vector;

/**
 * @author Lev Serebryakov
 */
@SuppressWarnings({ "PointlessArithmeticExpression", "UnusedDeclaration" })
public final class VOVec {
	private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();
	private final static int EPV = PFS.length();
	private final static int EPV2 = EPV / 2;
	private final static Vector.Mask<Float> MASK_C_RE;
	private final static Vector.Mask<Float> MASK_C_IM;
	private final static FloatVector ZERO = PFS.zero();

	static {
		boolean[] alter = new boolean[EPV + 1];
		alter[0] = true;
		for (int i = 1; i < alter.length; i++)
			alter[i] = !alter[i-1];
		MASK_C_RE = FloatVector.maskFromArray(PFS, alter, 0);
		MASK_C_IM = FloatVector.maskFromArray(PFS, alter, 1);
	}

	public static void rv_add_rs_i(float z[], int zOffset, float x, int count) {
		while (count >= EPV) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			fz.add(x).intoArray(z, zOffset);;
			count -= EPV;
			zOffset += EPV;
		}
		while (count-- > 0)
			z[zOffset++] += x;
	}

	public static void rv_add_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count >= EPV) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fz.add(fx).intoArray(z, zOffset);
			count -= EPV;
			zOffset += EPV;
		}
		while (count-- > 0)
			z[zOffset++] += x[xOffset++];
	}

	public static void cv_add_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count >= EPV) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			fz.add(x, MASK_C_RE).intoArray(z, zOffset);
			count -= EPV2;
			zOffset += EPV;
		}
		while (count-- > 0) {
			z[zOffset + 0] += x;
			zOffset += 2;
		}
	}

	public static void cv_add_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count >= EPV) {
			final FloatVector fz = FloatVector.fromArray(PFS, z, zOffset);
			final FloatVector fx = FloatVector.fromArray(PFS, x, xOffset);
			fz.add(fx, MASK_C_RE).intoArray(z, zOffset);
			count -= EPV;
			xOffset += EPV2;
			zOffset += EPV;
			FloatVector.
		}
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset++];
			zOffset += 2;
		}
	}

	public static void cv_add_cs_i(float z[], int zOffset, float x[], int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] += x[0];
			z[zOffset + 1] += x[1];
			zOffset += 2;
		}
	}

	public static void cv_add_cs_iw(float z[], int zOffset, float x[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			z[zOffset + 0] += x[0];
			z[zOffset + 1] += x[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_add_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset + 0];
			z[zOffset + 1] += x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_add_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] += x[xOffset + 0];
			z[zOffset + 1] += x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] + y;
	}

	public static void rv_add_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] + y;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_add_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] + y[yOffset++];
	}

	public static void rv_add_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] + y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_add_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y;
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_add_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y;
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_add_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset++];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_add_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_add_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[0];
			z[zOffset + 1] = x[xOffset + 1] + y[1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_add_cs_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[0];
			z[zOffset + 1] = x[xOffset + 1] + y[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_add_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset + 0];
			z[zOffset + 1] = x[xOffset + 1] + y[yOffset + 1];
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_add_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] + y[yOffset + 0];
			z[zOffset + 1] = x[xOffset + 1] + y[yOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_sub_rs_i(float z[], int zOffset, float x, int count) {
		while (count-- > 0)
			z[zOffset++] -= x;
	}

	public static void rv_sub_rs_iw(float z[], int zOffset, float x, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] -= x;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_sub_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] -= x[xOffset++];
	}

	public static void rv_sub_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] -= x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_sub_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] -= x;
			zOffset += 2;
		}
	}

	public static void cv_sub_rs_iw(float z[], int zOffset, float x, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			z[zOffset + 0] -= x;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_sub_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] -= x[xOffset++];
			zOffset += 2;
		}
	}

	public static void cv_sub_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] -= x[xOffset];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_sub_cs_i(float z[], int zOffset, float x[], int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] -= x[0];
			z[zOffset + 1] -= x[1];
			zOffset += 2;
		}
	}

	public static void cv_sub_cs_iw(float z[], int zOffset, float x[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			z[zOffset + 0] -= x[0];
			z[zOffset + 1] -= x[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_sub_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] -= x[xOffset + 0];
			z[zOffset + 1] -= x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_sub_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] -= x[xOffset + 0];
			z[zOffset + 1] -= x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_sub_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] - y;
	}

	public static void rv_sub_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] - y;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rs_sub_rv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = x - y[yOffset++];
	}

	public static void rs_sub_rv_w(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x - y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_sub_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] - y[yOffset++];
	}

	public static void rv_sub_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] - y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_sub_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y;
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_sub_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y;
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rs_sub_cv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x - y[yOffset + 0];
			z[zOffset + 1] = -y[yOffset + 1];
			zOffset += 2;
			yOffset += 2;
		}
	}

	public static void rs_sub_cv_w(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x - y[yOffset + 0];
			z[zOffset + 1] = -y[yOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_sub_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[yOffset++];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_sub_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[yOffset];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_sub_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] - y[yOffset + 0];
			z[zOffset + 1] = -y[yOffset + 1];
			zOffset += 2;
			xOffset += 1;
			yOffset += 2;
		}
	}

	public static void rv_sub_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] - y[yOffset + 0];
			z[zOffset + 1] = -y[yOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_sub_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[0];
			z[zOffset + 1] = x[xOffset + 1] - y[1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_sub_cs_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[0];
			z[zOffset + 1] = x[xOffset + 1] - y[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cs_sub_cv(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[0] - y[yOffset + 0];
			z[zOffset + 1] = x[1] - y[yOffset + 1];
			zOffset += 2;
			yOffset += 2;
		}
	}

	public static void cs_sub_cv_w(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[0] - y[yOffset + 0];
			z[zOffset + 1] = x[1] - y[yOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_sub_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[yOffset + 0];
			z[zOffset + 1] = x[xOffset + 1] - y[yOffset + 1];
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_sub_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] - y[yOffset + 0];
			z[zOffset + 1] = x[xOffset + 1] - y[yOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_mul_rs_i(float z[], int zOffset, float x, int count) {
		while (count-- > 0)
			z[zOffset++] *= x;
	}

	public static void rv_mul_rs_iw(float z[], int zOffset, float x, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] *= x;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_mul_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] *= x[xOffset++];
	}

	public static void rv_mul_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] *= x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_mul_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] *= x;
			z[zOffset + 1] *= x;
			zOffset += 2;
		}
	}

	public static void cv_mul_rs_iw(float z[], int zOffset, float x, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			z[zOffset + 0] *= x;
			z[zOffset + 1] *= x;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_mul_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] *= x[xOffset];
			z[zOffset + 1] *= x[xOffset];
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_mul_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] *= x[xOffset];
			z[zOffset + 1] *= x[xOffset];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_mul_cs_i(float z[], int zOffset, float x[], int count) {
		float k0, k1, k2;
		zOffset <<= 1;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[0];
			k1 = z[zOffset + 1] * x[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[0] + x[1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
		}
	}

	public static void cv_mul_cs_iw(float z[], int zOffset, float x[], int count) {
		float k0, k1, k2;
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[0];
			k1 = z[zOffset + 1] * x[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[0] + x[1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_mul_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		float k0, k1, k2;
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] + x[xOffset + 1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_mul_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		float k0, k1, k2;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] + x[xOffset + 1]);
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_mul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * y;
	}

	public static void rv_mul_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] * y;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_mul_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * y[yOffset++];
	}

	public static void rv_mul_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] * y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_mul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y;
			z[zOffset + 1] = x[xOffset + 1] * y;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_mul_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y;
			z[zOffset + 1] = x[xOffset + 1] * y;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_mul_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] * y[yOffset];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_mul_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] * y[yOffset];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_mul_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float k0, k1;
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[0];
			k1 = x[xOffset + 1] * y[1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[0] + y[1]) - k0 - k1;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_mul_cs_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float k0, k1;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[0];
			k1 = x[xOffset + 1] * y[1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[0] + y[1]) - k0 - k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_mul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float k0, k1;
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_mul_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float k0, k1;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 - k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] + y[yOffset + 1]) - k0 - k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_div_rs_i(float z[], int zOffset, float x, int count) {
		while (count-- > 0)
			z[zOffset++] /= x;
	}

	public static void rv_div_rs_iw(float z[], int zOffset, float x, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] /= x;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_div_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] /= x[xOffset++];
	}

	public static void rv_div_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] /= x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_div_rs_i(float z[], int zOffset, float x, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] /= x;
			z[zOffset + 1] /= x;
			zOffset += 2;
		}
	}

	public static void cv_div_rs_iw(float z[], int zOffset, float x, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			z[zOffset + 0] /= x;
			z[zOffset + 1] /= x;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_div_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] /= x[xOffset];
			z[zOffset + 1] /= x[xOffset];
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_div_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] /= x[xOffset];
			z[zOffset + 1] /= x[xOffset];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_div_cs_i(float z[], int zOffset, float x[], int count) {
		float re, im, sq;
		zOffset <<= 1;
		sq = x[0] * x[0] + x[1] * x[1];
		while (count-- > 0) {
			re = (z[zOffset + 0] * x[0] + z[zOffset + 1] * x[1]) / sq;
			im = (z[zOffset + 1] * x[0] - z[zOffset + 0] * x[1]) / sq;
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
		}
	}

	public static void cv_div_cs_iw(float z[], int zOffset, float x[], int count) {
		float re, im, sq;
		zOffset = preWrap(zOffset << 1, z.length);
		sq = x[0] * x[0] + x[1] * x[1];
		while (count-- > 0) {
			re = (z[zOffset + 0] * x[0] + z[zOffset + 1] * x[1]) / sq;
			im = (z[zOffset + 1] * x[0] - z[zOffset + 0] * x[1]) / sq;
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_div_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		float re, im, sq;
		zOffset <<= 1;
		xOffset <<= 1;
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

	public static void cv_div_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		float re, im, sq;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			sq = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			re = (z[zOffset + 0] * x[xOffset + 0] + z[zOffset + 1] * x[xOffset + 1]) / sq;
			im = (z[zOffset + 1] * x[xOffset + 0] - z[zOffset + 0] * x[xOffset + 1]) / sq;
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_div_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] / y;
	}

	public static void rv_div_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] / y;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rs_div_rv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = x / y[yOffset++];
	}

	public static void rs_div_rv_w(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x / y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_div_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] / y[yOffset++];
	}

	public static void rv_div_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] / y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_div_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y;
			z[zOffset + 1] = x[xOffset + 1] / y;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_div_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y;
			z[zOffset + 1] = x[xOffset + 1] / y;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rs_div_cv(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		float sq;
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = x * y[yOffset + 0] / sq;
			z[zOffset + 1] = -x * y[yOffset + 1] / sq;
			zOffset += 2;
			yOffset += 2;
		}
	}

	public static void rs_div_cv_w(float z[], int zOffset, float x, float y[], int yOffset, int count) {
		float sq;
		zOffset = preWrap(zOffset << 1, z.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = x * y[yOffset + 0] / sq;
			z[zOffset + 1] = -x * y[yOffset + 1] / sq;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_div_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] / y[yOffset];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_div_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] / y[yOffset];
			z[zOffset + 1] = x[xOffset + 1] / y[yOffset];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_div_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float sq;
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = x[xOffset] * y[yOffset + 0] / sq;
			z[zOffset + 1] = -x[xOffset] * y[yOffset + 1] / sq;
			zOffset += 2;
			xOffset += 1;
			yOffset += 2;
		}
	}

	public static void rv_div_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float sq;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = x[xOffset] * y[yOffset + 0] / sq;
			z[zOffset + 1] = -x[xOffset] * y[yOffset + 1] / sq;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_div_cs(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float sq;
		zOffset <<= 1;
		xOffset <<= 1;
		sq = y[0] * y[0] + y[1] * y[1];
		while (count-- > 0) {
			z[zOffset + 0] = (x[xOffset + 0] * y[0] + x[xOffset + 1] * y[1]) / sq;
			z[zOffset + 1] = (x[xOffset + 1] * y[0] - x[xOffset + 0] * y[1]) / sq;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_div_cs_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float sq;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		sq = y[0] * y[0] + y[1] * y[1];
		while (count-- > 0) {
			z[zOffset + 0] = (x[xOffset + 0] * y[0] + x[xOffset + 1] * y[1]) / sq;
			z[zOffset + 1] = (x[xOffset + 1] * y[0] - x[xOffset + 0] * y[1]) / sq;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cs_div_cv(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
		float sq;
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
			z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
			zOffset += 2;
			yOffset += 2;
		}
	}

	public static void cs_div_cv_w(float z[], int zOffset, float x[], float y[], int yOffset, int count) {
		float sq;
		zOffset = preWrap(zOffset << 1, z.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = (x[0] * y[yOffset + 0] + x[1] * y[yOffset + 1]) / sq;
			z[zOffset + 1] = (x[1] * y[yOffset + 0] - x[0] * y[yOffset + 1]) / sq;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_div_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float sq;
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = (x[xOffset + 0] * y[yOffset + 0] + x[xOffset + 1] * y[yOffset + 1]) / sq;
			z[zOffset + 1] = (x[xOffset + 1] * y[yOffset + 0] - x[xOffset + 0] * y[yOffset + 1]) / sq;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_div_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float sq;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			sq = y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = (x[xOffset + 0] * y[yOffset + 0] + x[xOffset + 1] * y[yOffset + 1]) / sq;
			z[zOffset + 1] = (x[xOffset + 1] * y[yOffset + 0] - x[xOffset + 0] * y[yOffset + 1]) / sq;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_conjmul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * y[yOffset + 0];
			z[zOffset + 1] = -x[xOffset] * y[yOffset + 1];
			zOffset += 2;
			xOffset += 1;
			yOffset += 2;
		}
	}

	public static void rv_conjmul_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * y[yOffset + 0];
			z[zOffset + 1] = -x[xOffset] * y[yOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_conjmul_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		float k0, k1, k2;
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] - x[xOffset + 1]);
			z[zOffset + 0] = k0 + k1;
			z[zOffset + 1] = k2 - k0 + k1;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_conjmul_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		float k0, k1, k2;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			k0 = z[zOffset + 0] * x[xOffset + 0];
			k1 = z[zOffset + 1] * x[xOffset + 1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * (x[xOffset + 0] - x[xOffset + 1]);
			z[zOffset + 0] = k0 + k1;
			z[zOffset + 1] = k2 - k0 + k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_conjmul_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float k0, k1;
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 + k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] - y[yOffset + 1]) - k0 + k1;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_conjmul_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float k0, k1;
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			k0 = x[xOffset + 0] * y[yOffset + 0];
			k1 = x[xOffset + 1] * y[yOffset + 1];
			z[zOffset + 0] = k0 + k1;
			z[zOffset + 1] = (x[xOffset + 0] + x[xOffset + 1]) * (y[yOffset + 0] - y[yOffset + 1]) - k0 + k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_conj_i(float z[], int zOffset, int count) {
		zOffset = (zOffset << 1) + 1;
		while (count-- > 0) {
			z[zOffset] = -z[zOffset]; // + 1
			zOffset += 2;
		}
	}

	public static void cv_conj_iw(float z[], int zOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length) + 1;
		while (count-- > 0) {
			z[zOffset] = -z[zOffset]; // + 1
			zOffset += 2;
			if (zOffset > z.length) zOffset = 1;
		}
	}

	public static void cv_conj(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0];
			z[zOffset + 1] = -x[xOffset + 1];
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_conj_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0];
			z[zOffset + 1] = -x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_expi(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.cos(x[xOffset]);
			z[zOffset + 1] = (float)Math.sin(x[xOffset]);
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void rv_expi_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.cos(x[xOffset]);
			z[zOffset + 1] = (float)Math.sin(x[xOffset]);
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_exp_i(float z[], int zOffset, int count) {
		while (count-- > 0) {
			z[zOffset] = (float)Math.exp(z[zOffset]);
			zOffset++;
		}
	}

	public static void rv_exp_iw(float z[], int zOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] = (float)Math.exp(z[zOffset]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_exp_i(float z[], int zOffset, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			float g = (float)Math.exp(z[zOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(z[zOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(z[zOffset + 1]);
			zOffset += 2;
		}
	}

	public static void cv_exp_iw(float z[], int zOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			float g = (float)Math.exp(z[zOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(z[zOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(z[zOffset + 1]);
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_exp(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = (float)Math.exp(x[xOffset++]);
	}

	public static void rv_exp_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = (float)Math.exp(x[xOffset]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_exp(float z[], int zOffset, float x[], int xOffset, int count) {
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

	public static void cv_exp_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float g = (float)Math.exp(x[xOffset + 0]);
			z[zOffset + 0] = g * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = g * (float)Math.sin(x[xOffset + 1]);
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_im(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		xOffset += 1;
		while (count-- > 0) {
			z[zOffset++] = x[xOffset]; // + 1
			xOffset += 2;
		}
	}

	public static void cv_im_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length) + 1;
		while (count-- > 0) {
			z[zOffset] = x[xOffset]; // + 1
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length + 1) xOffset = 1;
		}
	}

	public static void cv_re(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset++] = x[xOffset];
			xOffset += 2;
		}
	}

	public static void cv_re_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_abs(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset++] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			xOffset += 2;
		}
	}

	public static void cv_abs_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_arg(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset++] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void cv_arg_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_arg_f(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset++] = FastTrig.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void cv_arg_fw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = FastTrig.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_argmul_rs(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset++] = y * (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void cv_argmul_rs_w(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = y * (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_argmul_rs_f(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset++] = y * FastTrig.atan2(x[xOffset + 1], x[xOffset + 0]);
			xOffset += 2;
		}
	}

	public static void cv_argmul_rs_fw(float z[], int zOffset, float x[], int xOffset, float y, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = y * FastTrig.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_abs_i(float z[], int zOffset, int count) {
		while (count-- > 0) {
			z[zOffset] = (float)Math.abs(z[zOffset]);
			zOffset += 1;
		}
	}

	public static void rv_abs_iw(float z[], int zOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] = (float)Math.abs(z[zOffset]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_abs(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = (float)Math.abs(x[xOffset++]);
	}

	public static void rv_abs_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = (float)Math.abs(x[xOffset]);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_cvt(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset];
			z[zOffset + 1] = 0.0f;
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void rv_cvt_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset];
			z[zOffset + 1] = 0.0f;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_r2p_i(float z[], int zOffset, int count) {
		float abs, arg;
		zOffset <<= 1;
		while (count-- > 0) {
			abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
			arg = (float)Math.atan2(z[zOffset + 1], z[zOffset + 0]);
			z[zOffset + 0] = abs;
			z[zOffset + 1] = arg;
			zOffset += 2;
		}
	}

	public static void cv_r2p_iw(float z[], int zOffset, int count) {
		float abs, arg;
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
			arg = (float)Math.atan2(z[zOffset + 1], z[zOffset + 0]);
			z[zOffset + 0] = abs;
			z[zOffset + 1] = arg;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_r2p_if(float z[], int zOffset, int count) {
		float abs, arg;
		zOffset <<= 1;
		while (count-- > 0) {
			abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
			arg = FastTrig.atan2(z[zOffset + 1], z[zOffset + 0]);
			z[zOffset + 0] = abs;
			z[zOffset + 1] = arg;
			zOffset += 2;
		}
	}

	public static void cv_r2p_ifw(float z[], int zOffset, int count) {
		float abs, arg;
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			abs = (float)Math.hypot(z[zOffset + 0], z[zOffset + 1]);
			arg = FastTrig.atan2(z[zOffset + 1], z[zOffset + 0]);
			z[zOffset + 0] = abs;
			z[zOffset + 1] = arg;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_r2p(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			z[zOffset + 1] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_r2p_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			z[zOffset + 1] = (float)Math.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_r2p_f(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			z[zOffset + 1] = FastTrig.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_r2p_fw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = (float)Math.hypot(x[xOffset + 0], x[xOffset + 1]);
			z[zOffset + 1] = FastTrig.atan2(x[xOffset + 1], x[xOffset + 0]);
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_p2r_i(float z[], int zOffset, int count) {
		float re, im;
		zOffset <<= 1;
		while (count-- > 0) {
			re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
			im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
		}
	}

	public static void cv_p2r_iw(float z[], int zOffset, int count) {
		float re, im;
		zOffset = preWrap(zOffset << 1, z.length);
		while (count-- > 0) {
			re = z[zOffset + 0] * (float)Math.cos(z[zOffset + 1]);
			im = z[zOffset + 0] * (float)Math.sin(z[zOffset + 1]);
			z[zOffset + 0] = re;
			z[zOffset + 1] = im;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void cv_p2r(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = x[xOffset + 0] * (float)Math.sin(x[xOffset + 1]);
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_p2r_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * (float)Math.cos(x[xOffset + 1]);
			z[zOffset + 1] = x[xOffset + 0] * (float)Math.sin(x[xOffset + 1]);
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static float rv_sum(float x[], int xOffset, int count) {
		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++];
		return sum;
	}

	public static float rv_sum_w(float x[], int xOffset, int count) {
		float sum = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			sum += x[xOffset];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
		return sum;
	}

	public static void cv_sum(float z[], float x[], int xOffset, int count) {
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

	public static void cv_sum(float z[], int zOffset, float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;
		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
		}
		z[(zOffset << 1) + 0] = re;
		z[(zOffset << 1) + 1] = im;
	}

	public static void cv_sum_w(float z[], float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_sum_w(float z[], int zOffset, float x[], int xOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			re += x[xOffset + 0];
			im += x[xOffset + 1];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		zOffset <<= 1;
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static float rv_dot_rv(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;
		while (count-- > 0)
			sum += x[xOffset++] * y[yOffset++];
		return sum;
	}

	public static float rv_dot_rv_w(float x[], int xOffset, float y[], int count) {
		float sum = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			sum += x[xOffset] * y[yOffset++];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
		return sum;
	}

	public static float rv_dot_rv_w(float x[], int xOffset, float y[], int yOffset, int count) {
		float sum = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			sum += x[xOffset] * y[yOffset];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
		return sum;
	}

	public static void rv_dot_cv(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		yOffset <<= 1;
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
		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			yOffset += 2;
		}
		z[(zOffset << 1) + 0] = re;
		z[(zOffset << 1) + 1] = im;
	}

	public static void rv_dot_cv_w(float z[], float x[], int xOffset, float y[], int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void rv_dot_cv_w(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void rv_dot_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
		}
		zOffset <<= 1;
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void rv_dot_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			re += x[xOffset] * y[yOffset + 0];
			im += x[xOffset] * y[yOffset + 1];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
		zOffset <<= 1;
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void cv_dot_rv_w(float z[], float x[], int xOffset, float y[], int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset];
			im += x[xOffset + 1] * y[yOffset];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_dot_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset];
			im += x[xOffset + 1] * y[yOffset];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
		}
		zOffset <<= 1;
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void cv_dot_cv(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset + 0] - x[xOffset + 1] * y[yOffset + 1];
			im += x[xOffset + 1] * y[yOffset + 0] + x[xOffset + 0] * y[yOffset + 1];
			xOffset += 2;
			yOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_dot_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset + 0] - x[xOffset + 1] * y[yOffset + 1];
			im += x[xOffset + 1] * y[yOffset + 0] + x[xOffset + 0] * y[yOffset + 1];
			xOffset += 2;
			yOffset += 2;
		}
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void cv_dot_cv_w(float z[], float x[], int xOffset, float y[], int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset + 0] - x[xOffset + 1] * y[yOffset + 1];
			im += x[xOffset + 1] * y[yOffset + 0] + x[xOffset + 0] * y[yOffset + 1];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_dot_cv_w(float z[], float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset + 0] - x[xOffset + 1] * y[yOffset + 1];
			im += x[xOffset + 1] * y[yOffset + 0] + x[xOffset + 0] * y[yOffset + 1];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
		z[0] = re;
		z[1] = im;
	}

	public static void cv_dot_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		int yOffset = 0;
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset + 0] - x[xOffset + 1] * y[yOffset + 1];
			im += x[xOffset + 1] * y[yOffset + 0] + x[xOffset + 0] * y[yOffset + 1];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
		}
		zOffset <<= 1;
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void cv_dot_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		float re = 0.0f;
		float im = 0.0f;
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			re += x[xOffset + 0] * y[yOffset + 0] - x[xOffset + 1] * y[yOffset + 1];
			im += x[xOffset + 1] * y[yOffset + 0] + x[xOffset + 0] * y[yOffset + 1];
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
		zOffset <<= 1;
		z[zOffset + 0] = re;
		z[zOffset + 1] = im;
	}

	public static void rv_cpy(float z[], int zOffset, float x[], int xOffset, int count) {
		System.arraycopy(x, xOffset, z, zOffset, count);
	}

	public static void rv_cpy_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count > 0) {
			// How much could we copy?
			int work = Math.min(count, Math.min(z.length - zOffset, x.length - xOffset));
			System.arraycopy(x, xOffset, z, zOffset, work);
			xOffset += work;
			if (xOffset == x.length) xOffset = 0;
			zOffset += work;
			if (zOffset == z.length) zOffset = 0;
			count -= work;
		}
	}

	public static void cv_cpy(float z[], int zOffset, float x[], int xOffset, int count) {
		System.arraycopy(x, xOffset << 1, z, zOffset << 1, count * 2);
	}

	public static void cv_cpy_w(float z[], int zOffset, float x[], int xOffset, int count) {
		rv_cpy_w(z, zOffset << 1, x, xOffset << 1, count << 1);
	}

	public static void rv_rev_i(float z[], int zOffset, int count) {
		float t;
		int bOffset = zOffset + count - 1;
		while (zOffset < bOffset) {
			t = z[zOffset];
			z[zOffset] = z[bOffset];
			z[bOffset] = t;
			zOffset++;
			bOffset--;
		}
	}

	public static void rv_rev_iw(float z[], int zOffset, int count) {
		float t;
		zOffset = preWrap(zOffset, z.length);
		int bOffset = preWrap(zOffset + count - 1, z.length);
		while (zOffset < bOffset) {
			t = z[zOffset];
			z[zOffset] = z[bOffset];
			z[bOffset] = t;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			bOffset -= 1;
			if (bOffset < 0) bOffset += z.length;
		}
	}

	public static void cv_rev_i(float z[], int zOffset, int count) {
		float t;
		int bOffset = zOffset + count - 1;
		zOffset <<= 1;
		bOffset <<= 1;
		while (zOffset < bOffset) {
			t = z[zOffset + 0];
			z[zOffset + 0] = z[bOffset + 0];
			z[bOffset + 0] = t;
			t = z[zOffset + 1];
			z[zOffset + 1] = z[bOffset + 1];
			z[bOffset + 1] = t;
			zOffset += 2;
			bOffset -= 2;
		}
	}

	public static void cv_rev_iw(float z[], int zOffset, int count) {
		float t;
		zOffset = preWrap(zOffset << 1, z.length);
		int bOffset = preWrap(zOffset + count * 2 - 2, z.length);
		while (zOffset < bOffset) {
			t = z[zOffset + 0];
			z[zOffset + 0] = z[bOffset + 0];
			z[bOffset + 0] = t;
			t = z[zOffset + 1];
			z[zOffset + 1] = z[bOffset + 1];
			z[bOffset + 1] = t;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			bOffset -= 2;
			if (bOffset < 0) bOffset += z.length;
		}
	}

	public static void rv_rev(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset = xOffset + count - 1;
		while (count-- > 0)
			z[zOffset++] = x[xOffset--];
	}

	public static void rv_rev_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset + count - 1, x.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset -= 1;
			if (xOffset < 0) xOffset += x.length;
		}
	}

	public static void cv_rev(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset = xOffset + count - 1;
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			xOffset -= 2;
		}
	}

	public static void cv_rev_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap((xOffset + count - 1) << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0];
			z[zOffset + 1] = x[xOffset + 1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset -= 2;
			if (xOffset < 0) xOffset += x.length;
		}
	}

	public static float rv_max(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		while (count-- > 0) {
			if (Float.compare(max, x[xOffset]) < 0)
				max = x[xOffset];
			xOffset += 1;
		}
		return max;
	}

	public static float rv_max_w(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			if (Float.compare(max, x[xOffset]) < 0)
				max = x[xOffset];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
		return max;
	}

	public static void rv_max_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0) {
			if (x[xOffset] > z[zOffset])
				z[zOffset] = x[xOffset];
			zOffset += 1;
			xOffset += 1;
		}
	}

	public static void rv_max_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			if (x[xOffset] > z[zOffset])
				z[zOffset] = x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_max_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count-- > 0) {
			z[zOffset] = x[xOffset] > y[yOffset] ? x[xOffset] : y[yOffset];
			zOffset += 1;
			xOffset += 1;
			yOffset += 1;
		}
	}

	public static void rv_max_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] > y[yOffset] ? x[xOffset] : y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_max(float z[], float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(max, abs) < 0) {
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
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(max, abs) < 0) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[(zOffset << 1) + 0] = x[i + 0];
		z[(zOffset << 1) + 1] = x[i + 1];
	}

	public static void cv_max_w(float z[], float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(max, abs) < 0) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}

	public static void cv_max_w(float z[], int zOffset, float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(max, abs) < 0) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		zOffset <<= 1;
		z[zOffset + 0] = x[i + 0];
		z[zOffset + 1] = x[i + 1];
	}

	public static void cv_max_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) > (z[zOffset + 0] * z[zOffset + 0] + z[zOffset + 1] * z[zOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			}
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_max_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) > (z[zOffset + 0] * z[zOffset + 0] + z[zOffset + 1] * z[zOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			}
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_max_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) > (y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			} else {
				z[zOffset + 0] = y[yOffset + 0];
				z[zOffset + 1] = y[yOffset + 1];
			}
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_max_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) > (y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			} else {
				z[zOffset + 0] = y[yOffset + 0];
				z[zOffset + 1] = y[yOffset + 1];
			}
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static float rv_min(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		while (count-- > 0) {
			if (Float.compare(min, x[xOffset]) > 0)
				min = x[xOffset];
			xOffset += 1;
		}
		return min;
	}

	public static float rv_min_w(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			if (Float.compare(min, x[xOffset]) > 0)
				min = x[xOffset];
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
		return min;
	}

	public static void rv_min_rv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0) {
			if (x[xOffset] < z[zOffset])
				z[zOffset] = x[xOffset];
			zOffset += 1;
			xOffset += 1;
		}
	}

	public static void rv_min_rv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			if (x[xOffset] < z[zOffset])
				z[zOffset] = x[xOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_min_rv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		while (count-- > 0) {
			z[zOffset] = x[xOffset] < y[yOffset] ? x[xOffset] : y[yOffset];
			zOffset += 1;
			xOffset += 1;
			yOffset += 1;
		}
	}

	public static void rv_min_rv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] < y[yOffset] ? x[xOffset] : y[yOffset];
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_min(float z[], float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(min, abs) > 0) {
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
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(min, abs) > 0) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		z[(zOffset << 1) + 0] = x[i + 0];
		z[(zOffset << 1) + 1] = x[i + 1];
	}

	public static void cv_min_w(float z[], float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(min, abs) > 0) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		z[0] = x[i + 0];
		z[1] = x[i + 1];
	}

	public static void cv_min_w(float z[], int zOffset, float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float abs = (float)Math.abs(x[xOffset + 0]) + (float)Math.abs(x[xOffset + 1]);
			if (Float.compare(min, abs) > 0) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		zOffset <<= 1;
		z[zOffset + 0] = x[i + 0];
		z[zOffset + 1] = x[i + 1];
	}

	public static void cv_min_cv_i(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) < (z[zOffset + 0] * z[zOffset + 0] + z[zOffset + 1] * z[zOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			}
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_min_cv_iw(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) < (z[zOffset + 0] * z[zOffset + 0] + z[zOffset + 1] * z[zOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			}
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_min_cv(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) < (y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			} else {
				z[zOffset + 0] = y[yOffset + 0];
				z[zOffset + 1] = y[yOffset + 1];
			}
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_min_cv_w(float z[], int zOffset, float x[], int xOffset, float y[], int yOffset, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			if ((x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1]) < (y[yOffset + 0] * y[yOffset + 0] + y[yOffset + 1] * y[yOffset + 1])) {
				z[zOffset + 0] = x[xOffset + 0];
				z[zOffset + 1] = x[xOffset + 1];
			} else {
				z[zOffset + 0] = y[yOffset + 0];
				z[zOffset + 1] = y[yOffset + 1];
			}
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static int rv_maxarg(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		while (count-- > 0) {
			if (Float.compare(max, x[xOffset]) < 0) {
				max = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
		}
		return i;
	}

	public static int rv_maxarg_w(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			if (Float.compare(max, x[xOffset]) < 0) {
				max = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
		return i;
	}

	public static int cv_maxarg(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (Float.compare(max, abs) < 0) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		return i >> 1;
	}

	public static int cv_maxarg_w(float x[], int xOffset, int count) {
		float max = Float.NEGATIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (Float.compare(max, abs) < 0) {
				max = abs;
				i = xOffset;
			}
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		return i >> 1;
	}

	public static int rv_minarg(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		while (count-- > 0) {
			if (Float.compare(min, x[xOffset]) > 0) {
				min = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
		}
		return i;
	}

	public static int rv_minarg_w(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			if (Float.compare(min, x[xOffset]) > 0) {
				min = x[xOffset];
				i = xOffset;
			}
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
		return i;
	}

	public static int cv_minarg(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset <<= 1;
		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (Float.compare(min, abs) > 0) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
		}
		return i >> 1;
	}

	public static int cv_minarg_w(float x[], int xOffset, int count) {
		float min = Float.POSITIVE_INFINITY;
		int i = -1;
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			float abs = x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1];
			if (Float.compare(min, abs) > 0) {
				min = abs;
				i = xOffset;
			}
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
		return i >> 1;
	}

	public static void rv_rs_lin_rv_rs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		while (count-- > 0) {
			z[zOffset] = z[zOffset] * a1 + x[xOffset] * a2;
			zOffset += 1;
			xOffset += 1;
		}
	}

	public static void rv_rs_lin_rv_rs_iw(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = z[zOffset] * a1 + x[xOffset] * a2;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_rs_lin_rv_rs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		while (count-- > 0)
			z[zOffset++] = x[xOffset++] * a1 + y[yOffset++] * a2;
	}

	public static void rv_rs_lin_rv_rs_w(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset] = x[xOffset] * a1 + y[yOffset] * a2;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_rs_lin_rv_cs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2[], int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * a1 + y[yOffset] * a2[0];
			z[zOffset + 1] = y[yOffset] * a2[1];
			zOffset += 2;
			xOffset += 1;
			yOffset += 1;
		}
	}

	public static void rv_rs_lin_rv_cs_w(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * a1 + y[yOffset] * a2[0];
			z[zOffset + 1] = y[yOffset] * a2[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_cs_lin_rv_cs(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * a1[0] + y[yOffset] * a2[0];
			z[zOffset + 1] = x[xOffset] * a1[1] + y[yOffset] * a2[1];
			zOffset += 2;
			xOffset += 1;
			yOffset += 1;
		}
	}

	public static void rv_cs_lin_rv_cs_w(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset] * a1[0] + y[yOffset] * a2[0];
			z[zOffset + 1] = x[xOffset] * a1[1] + y[yOffset] * a2[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_rs_lin_rv_rs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset] * a2;
			z[zOffset + 1] = z[zOffset + 1] * a1;
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_rs_lin_rv_rs_iw(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset] * a2;
			z[zOffset + 1] = z[zOffset + 1] * a1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_rs_lin_rv_rs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset] * a2;
			z[zOffset + 1] = x[xOffset + 1] * a1;
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_rs_lin_rv_rs_w(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset] * a2;
			z[zOffset + 1] = x[xOffset + 1] * a1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_cs_lin_rv_rs_i(float z[], int zOffset, float a1[], float x[], int xOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset <<= 1;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * a1[0];
			k1 = z[zOffset + 1] * a1[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + x[xOffset] * a2;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_cs_lin_rv_rs_iw(float z[], int zOffset, float a1[], float x[], int xOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			k0 = z[zOffset + 0] * a1[0];
			k1 = z[zOffset + 1] * a1[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + x[xOffset] * a2;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_cs_lin_rv_rs(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * a1[0];
			k1 = x[xOffset + 1] * a1[1];
			k2 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + y[yOffset] * a2;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_cs_lin_rv_rs_w(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			k0 = x[xOffset + 0] * a1[0];
			k1 = x[xOffset + 1] * a1[1];
			k2 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + y[yOffset] * a2;
			z[zOffset + 1] = k2 - k0 - k1;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_rs_lin_rv_cs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2[], int count) {
		zOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset] * a2[0];
			z[zOffset + 1] = z[zOffset + 1] * a1 + x[xOffset] * a2[1];
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_rs_lin_rv_cs_iw(float z[], int zOffset, float a1, float x[], int xOffset, float a2[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset] * a2[0];
			z[zOffset + 1] = z[zOffset + 1] * a1 + x[xOffset] * a2[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_rs_lin_rv_cs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2[], int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset] * a2[0];
			z[zOffset + 1] = x[xOffset + 1] * a1 + y[yOffset] * a2[1];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_rs_lin_rv_cs_w(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2[], int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset] * a2[0];
			z[zOffset + 1] = x[xOffset + 1] * a1 + y[yOffset] * a2[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_cs_lin_rv_cs_i(float z[], int zOffset, float a1[], float x[], int xOffset, float a2[], int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset <<= 1;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * a1[0];
			k1 = z[zOffset + 1] * a1[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + x[xOffset] * a2[0];
			z[zOffset + 1] = k2 - k0 - k1 + x[xOffset] * a2[1];
			zOffset += 2;
			xOffset += 1;
		}
	}

	public static void cv_cs_lin_rv_cs_iw(float z[], int zOffset, float a1[], float x[], int xOffset, float a2[], int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			k0 = z[zOffset + 0] * a1[0];
			k1 = z[zOffset + 1] * a1[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + x[xOffset] * a2[0];
			z[zOffset + 1] = k2 - k0 - k1 + x[xOffset] * a2[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_cs_lin_rv_cs(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * a1[0];
			k1 = x[xOffset + 1] * a1[1];
			k2 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + y[yOffset] * a2[0];
			z[zOffset + 1] = k2 - k0 - k1 + y[yOffset] * a2[1];
			zOffset += 2;
			xOffset += 2;
			yOffset += 1;
		}
	}

	public static void cv_cs_lin_rv_cs_w(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset, y.length);
		while (count-- > 0) {
			k0 = x[xOffset + 0] * a1[0];
			k1 = x[xOffset + 1] * a1[1];
			k2 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + y[yOffset] * a2[0];
			z[zOffset + 1] = k2 - k0 - k1 + y[yOffset] * a2[1];
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 1;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_rs_lin_cv_rs_i(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset + 0] * a2;
			z[zOffset + 1] = z[zOffset + 1] * a1 + x[xOffset + 1] * a2;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_rs_lin_cv_rs_iw(float z[], int zOffset, float a1, float x[], int xOffset, float a2, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset + 0] = z[zOffset + 0] * a1 + x[xOffset + 0] * a2;
			z[zOffset + 1] = z[zOffset + 1] * a1 + x[xOffset + 1] * a2;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_rs_lin_cv_rs(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset + 0] * a2;
			z[zOffset + 1] = x[xOffset + 1] * a1 + y[yOffset + 1] * a2;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_rs_lin_cv_rs_w(float z[], int zOffset, float x[], int xOffset, float a1, float y[], int yOffset, float a2, int count) {
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			z[zOffset + 0] = x[xOffset + 0] * a1 + y[yOffset + 0] * a2;
			z[zOffset + 1] = x[xOffset + 1] * a1 + y[yOffset + 1] * a2;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_cs_lin_cv_rs_i(float z[], int zOffset, float a1[], float x[], int xOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0 = z[zOffset + 0] * a1[0];
			k1 = z[zOffset + 1] * a1[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + x[xOffset + 0] * a2;
			z[zOffset + 1] = k2 - k0 - k1 + x[xOffset + 1] * a2;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_cs_lin_cv_rs_iw(float z[], int zOffset, float a1[], float x[], int xOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			k0 = z[zOffset + 0] * a1[0];
			k1 = z[zOffset + 1] * a1[1];
			k2 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + x[xOffset + 0] * a2;
			z[zOffset + 1] = k2 - k0 - k1 + x[xOffset + 1] * a2;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_cs_lin_cv_rs(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			k0 = x[xOffset + 0] * a1[0];
			k1 = x[xOffset + 1] * a1[1];
			k2 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + y[yOffset + 0] * a2;
			z[zOffset + 1] = k2 - k0 - k1 + y[yOffset + 1] * a2;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_cs_lin_cv_rs_w(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2, int count) {
		float k0, k1, k2;
		float a1s = a1[0] + a1[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			k0 = x[xOffset + 0] * a1[0];
			k1 = x[xOffset + 1] * a1[1];
			k2 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			z[zOffset + 0] = k0 - k1 + y[yOffset + 0] * a2;
			z[zOffset + 1] = k2 - k0 - k1 + y[yOffset + 1] * a2;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void cv_cs_lin_cv_cs_i(float z[], int zOffset, float a1[], float x[], int xOffset, float a2[], int count) {
		float k0_1, k1_1, k2_1;
		float a1s = a1[0] + a1[1];
		float k0_2, k1_2, k2_2;
		float a2s = a2[0] + a2[1];
		zOffset <<= 1;
		xOffset <<= 1;
		while (count-- > 0) {
			k0_1 = z[zOffset + 0] * a1[0];
			k1_1 = z[zOffset + 1] * a1[1];
			k2_1 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			k0_2 = x[xOffset + 0] * a2[0];
			k1_2 = x[xOffset + 1] * a2[1];
			k2_2 = (x[xOffset + 0] + x[xOffset + 1]) * a2s;
			z[zOffset + 0] = k0_1 - k1_1 + k0_2 - k1_2;
			z[zOffset + 1] = k2_1 - k0_1 - k1_1 + k2_2 - k0_2 - k1_2;
			zOffset += 2;
			xOffset += 2;
		}
	}

	public static void cv_cs_lin_cv_cs_iw(float z[], int zOffset, float a1[], float x[], int xOffset, float a2[], int count) {
		float k0_1, k1_1, k2_1;
		float a1s = a1[0] + a1[1];
		float k0_2, k1_2, k2_2;
		float a2s = a2[0] + a2[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			k0_1 = z[zOffset + 0] * a1[0];
			k1_1 = z[zOffset + 1] * a1[1];
			k2_1 = (z[zOffset + 0] + z[zOffset + 1]) * a1s;
			k0_2 = x[xOffset + 0] * a2[0];
			k1_2 = x[xOffset + 1] * a2[1];
			k2_2 = (x[xOffset + 0] + x[xOffset + 1]) * a2s;
			z[zOffset + 0] = k0_1 - k1_1 + k0_2 - k1_2;
			z[zOffset + 1] = k2_1 - k0_1 - k1_1 + k2_2 - k0_2 - k1_2;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_cs_lin_cv_cs(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		float k0_1, k1_1, k2_1;
		float a1s = a1[0] + a1[1];
		float k0_2, k1_2, k2_2;
		float a2s = a2[0] + a2[1];
		zOffset <<= 1;
		xOffset <<= 1;
		yOffset <<= 1;
		while (count-- > 0) {
			k0_1 = x[xOffset + 0] * a1[0];
			k1_1 = x[xOffset + 1] * a1[1];
			k2_1 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			k0_2 = y[yOffset + 0] * a2[0];
			k1_2 = y[yOffset + 1] * a2[1];
			k2_2 = (y[yOffset + 0] + y[yOffset + 1]) * a2s;
			z[zOffset + 0] = k0_1 - k1_1 + k0_2 - k1_2;
			z[zOffset + 1] = k2_1 - k0_1 - k1_1 + k2_2 - k0_2 - k1_2;
			zOffset += 2;
			xOffset += 2;
			yOffset += 2;
		}
	}

	public static void cv_cs_lin_cv_cs_w(float z[], int zOffset, float x[], int xOffset, float a1[], float y[], int yOffset, float a2[], int count) {
		float k0_1, k1_1, k2_1;
		float a1s = a1[0] + a1[1];
		float k0_2, k1_2, k2_2;
		float a2s = a2[0] + a2[1];
		zOffset = preWrap(zOffset << 1, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		yOffset = preWrap(yOffset << 1, y.length);
		while (count-- > 0) {
			k0_1 = x[xOffset + 0] * a1[0];
			k1_1 = x[xOffset + 1] * a1[1];
			k2_1 = (x[xOffset + 0] + x[xOffset + 1]) * a1s;
			k0_2 = y[yOffset + 0] * a2[0];
			k1_2 = y[yOffset + 1] * a2[1];
			k2_2 = (y[yOffset + 0] + y[yOffset + 1]) * a2s;
			z[zOffset + 0] = k0_1 - k1_1 + k0_2 - k1_2;
			z[zOffset + 1] = k2_1 - k0_1 - k1_1 + k2_2 - k0_2 - k1_2;
			zOffset += 2;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
			yOffset += 2;
			if (yOffset == y.length) yOffset = 0;
		}
	}

	public static void rv_10log10_i(float z[], int zOffset, int count) {
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
		}
	}

	public static void rv_10log10_iw(float z[], int zOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_10log10(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = 10 * (float)Math.log10((float)Math.abs(x[xOffset++]) + Float.MIN_NORMAL);
	}

	public static void rv_10log10_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10((float)Math.abs(x[xOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_10log10_rs_i(float z[], int zOffset, float base, int count) {
		base = 10 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
		}
	}

	public static void rv_10log10_rs_iw(float z[], int zOffset, float base, int count) {
		base = 10 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_10log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		while (count-- > 0)
			z[zOffset++] = 10 * (float)Math.log10((float)Math.abs(x[xOffset++]) + Float.MIN_NORMAL) - base;
	}

	public static void rv_10log10_rs_w(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10((float)Math.abs(x[xOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_10log10(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset] = 5 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL);
			zOffset += 1;
			xOffset += 2;
		}
	}

	public static void cv_10log10_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = 5 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_10log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset] = 5 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL) - base;
			zOffset += 1;
			xOffset += 2;
		}
	}

	public static void cv_10log10_rs_w(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 10 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = 5 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL) - base;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_20log10_i(float z[], int zOffset, int count) {
		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
		}
	}

	public static void rv_20log10_iw(float z[], int zOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_20log10(float z[], int zOffset, float x[], int xOffset, int count) {
		while (count-- > 0)
			z[zOffset++] = 20 * (float)Math.log10((float)Math.abs(x[xOffset++]) + Float.MIN_NORMAL);
	}

	public static void rv_20log10_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10((float)Math.abs(x[xOffset]) + Float.MIN_NORMAL);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void rv_20log10_rs_i(float z[], int zOffset, float base, int count) {
		base = 20 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
		}
	}

	public static void rv_20log10_rs_iw(float z[], int zOffset, float base, int count) {
		base = 20 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		zOffset = preWrap(zOffset, z.length);
		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10((float)Math.abs(z[zOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
		}
	}

	public static void rv_20log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 20 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		while (count-- > 0)
			z[zOffset++] = 20 * (float)Math.log10((float)Math.abs(x[xOffset++]) + Float.MIN_NORMAL) - base;
	}

	public static void rv_20log10_rs_w(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 20 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset, x.length);
		while (count-- > 0) {
			z[zOffset] = 20 * (float)Math.log10((float)Math.abs(x[xOffset]) + Float.MIN_NORMAL) - base;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 1;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_20log10(float z[], int zOffset, float x[], int xOffset, int count) {
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL);
			zOffset += 1;
			xOffset += 2;
		}
	}

	public static void cv_20log10_w(float z[], int zOffset, float x[], int xOffset, int count) {
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL);
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	public static void cv_20log10_rs(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 20 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		xOffset <<= 1;
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL) - base;
			zOffset += 1;
			xOffset += 2;
		}
	}

	public static void cv_20log10_rs_w(float z[], int zOffset, float x[], int xOffset, float base, int count) {
		base = 20 * (float)Math.log10((float)Math.abs(base) + Float.MIN_NORMAL);
		zOffset = preWrap(zOffset, z.length);
		xOffset = preWrap(xOffset << 1, x.length);
		while (count-- > 0) {
			z[zOffset] = 10 * (float)Math.log10(x[xOffset + 0] * x[xOffset + 0] + x[xOffset + 1] * x[xOffset + 1] + Float.MIN_NORMAL) - base;
			zOffset += 1;
			if (zOffset == z.length) zOffset = 0;
			xOffset += 2;
			if (xOffset == x.length) xOffset = 0;
		}
	}

	private static int preWrap(int i, int length) {
		return (i < 0) ?
				(i % length + length) :
				((i >= length) ?
						(i % length) :
						(i));
	}
}
