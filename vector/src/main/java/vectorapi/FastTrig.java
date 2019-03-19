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

package vectorapi;

/**
 * @author Lev Serebryakov
 */
public final class FastTrig {
	public static final float _1PI2 =     (float)Math.PI / 2;
	public static final float _2PI2 =     (float)Math.PI;
	public static final float _3PI2 = 3 * (float)Math.PI / 2;
	public static final float _4PI2 = 2 * (float)Math.PI;

	// 256 elements is Ok for MSE ~2.82E-6 and max error of 0.002
	private static final int ATAN2_TABLES_SIZE = 256;
	// float is more cache-friendly and precision is the same
	private static final float ATAN2_HIGH[] = new float[ATAN2_TABLES_SIZE + 1];
	private static final float ATAN2_LOW[] = new float[ATAN2_TABLES_SIZE + 1];
	private static final float ATAN2_LOWEST_VALUE;

	static {
		for (int i = 0; i < ATAN2_TABLES_SIZE; i++) {
			// Small steps for first large one, it give order of magnitude advantage in MSE
			ATAN2_LOW[i] = (float)Math.atan((float)i / (float)((ATAN2_TABLES_SIZE - 1) * ATAN2_TABLES_SIZE));
			// Large step
			ATAN2_HIGH[i] = (float)Math.atan((float)i / (float)(ATAN2_TABLES_SIZE - 1));
		}
		ATAN2_LOWEST_VALUE = ATAN2_LOW[1];
		// To eliminate 1 if
		ATAN2_HIGH[ATAN2_TABLES_SIZE] = ATAN2_HIGH[ATAN2_TABLES_SIZE - 1];
		ATAN2_LOW[ATAN2_TABLES_SIZE] = ATAN2_LOW[ATAN2_TABLES_SIZE - 1];
	}

	// This implementation ~8x faster than Math.atan2()
	// on my i5
	public static float atan2(float y, float x) {
		// normalize to +/- 45 degree range
		float ay = Math.abs(y);
		float ax = Math.abs(x);
		// don't divide by zero!
		if (!((ay > 0.0) || (ax > 0.0)))
			return 0.0f;

		float rel;
		if (ay < ax)
			rel = ay / ax;
		else
			rel = ax / ay;

		float angle;
		if (rel < ATAN2_LOWEST_VALUE) {
			angle = rel;
		} else {
			rel *= ATAN2_TABLES_SIZE;
			// We will goes to first bin, use special table for it
			int index;
			if (rel < 1.5) {
				rel = rel * ATAN2_TABLES_SIZE - 0.5f;
				// Fix this mask, if you change ATAN2_TABLES_SIZE
				index = ((int)rel) & 0xff;
				rel -= index;
				angle = ATAN2_LOW[index] + (ATAN2_LOW[index + 1] - ATAN2_LOW[index]) * rel;
			} else {
				rel -= 0.5;
				// Fix this mask, if you change ATAN2_TABLES_SIZE
				index = ((int)rel) & 0xff;
				rel -= index;
				angle = ATAN2_HIGH[index] + (ATAN2_HIGH[index + 1] - ATAN2_HIGH[index]) * rel;
			}
		}

		if (ax > ay) { // [-45, 45] || [135, 225]
			if (x >= 0.0) { // [-45, 45]
				if (y < 0.0) angle = -angle; // [-45, 0], angle = -angle
			} else { // [135, 180] || [-180, -135] */
				if (y >= 0.0) angle = (float)_2PI2 - angle; // [135, 180], angle = PI - angle
				else angle -= _2PI2; // [-180, -135], angle = angle - PI
			}
		} else { // [45, 135] || [-135, -45]
			if (y >= 0.0) { // [45, 135]
				if (x >= 0.0) angle = _1PI2 - angle; // [45, 90], angle = PI/2 - angle
				else angle += _1PI2; // [90, 135], angle = PI/2 + angle
			} else { // [-135, -45]
				if (x >= 0.0) angle -= _1PI2; // [-90, -45], angle = angle - PI/2 */
				else angle = -(_1PI2 + angle); //  [-135, -90], angle = -PI/2 - angle */
			}
		}
		return angle;
	}

	// 1024 elements is Ok for MSE ~1.77E-6 and max error of 0.001
	private static final int COS_TABLE_SIZE = 1024;
	private static final int COS_TABLE_MASK = COS_TABLE_SIZE - 1;
	// float is more cache-friendly and precision is the same
	private static final float COS[] = new float[COS_TABLE_SIZE + 2];

	static {
		for (int i = 0; i < COS_TABLE_SIZE; i++)
			COS[i + 1] = (float)Math.cos(_1PI2 * i / (COS_TABLE_SIZE - 1));

		COS[0] = COS[1];
		COS[COS_TABLE_SIZE] = COS[COS_TABLE_SIZE - 1];
	}

	public static float cos(float x) {
		// Normalize x to [0, 2*PI], loops is much (2x) faster than
		// floating point module
		while (x > _4PI2)
			x -= _4PI2;
		while (x < 0)
			x += _4PI2;

		// Normalize to [0, PI/2]
		float smallX = x;
		while (smallX > _1PI2)
			smallX -= _1PI2;

		float alpha = smallX / _1PI2 * COS_TABLE_SIZE;
		int idx = ((int)alpha) & COS_TABLE_MASK;
		alpha -= idx;
		if (x <= _1PI2) {
			idx += 1;
			return COS[idx] + (COS[idx + 1] - COS[idx]) * alpha;
		} else if (x <= _2PI2) {
			idx = COS_TABLE_SIZE - idx + 1;
			return -(COS[idx] + (COS[idx - 1] - COS[idx]) * alpha);
		} else if (x <= _3PI2) {
			idx += 1;
			return -(COS[idx] + (COS[idx + 1] - COS[idx]) * alpha);
		} else {
			idx = COS_TABLE_SIZE - idx + 1;
			return COS[idx] + (COS[idx - 1] - COS[idx]) * alpha;
		}
	}

	public static float sin(float x) {
		// Normalize x to [0, 2*PI], loops is much (2x) faster than
		// floating point module
		while (x > _4PI2)
			x -= _4PI2;
		while (x < 0)
			x += _4PI2;

		// Normalize to [0, PI/2]
		float smallX = x;
		while (smallX > _1PI2)
			smallX -= _1PI2;

		float alpha = smallX / _1PI2 * COS_TABLE_SIZE;
		int idx = ((int)alpha) & COS_TABLE_MASK;
		alpha -= idx;
		if (x <= _1PI2) {
			idx = COS_TABLE_SIZE - idx + 1;
			return COS[idx] + (COS[idx - 1] - COS[idx]) * alpha;
		} else if (x <= _2PI2) {
			idx += 1;
			return COS[idx] + (COS[idx + 1] - COS[idx]) * alpha;
		} else if (x <= _3PI2) {
			idx = COS_TABLE_SIZE - idx + 1;
			return -(COS[idx] + (COS[idx - 1] - COS[idx]) * alpha);
		} else {
			idx += 1;
			return -(COS[idx] + (COS[idx + 1] - COS[idx]) * alpha);
		}
	}

	public static void sincos(float sc[], float x) {
		// Normalize x to [0, 2*PI], loops is much (2x) faster than
		// floating point module
		while (x > _4PI2)
			x -= _4PI2;
		while (x < 0)
			x += _4PI2;

		// Normalize to [0, PI/2]
		float smallX = x;
		while (smallX > _1PI2)
			smallX -= _1PI2;

		float alpha = smallX / _1PI2 * COS_TABLE_SIZE;
		int fidx = ((int)alpha) & COS_TABLE_MASK;
		int ridx = COS_TABLE_SIZE - fidx;
		alpha -= fidx;
		fidx += 1;
		ridx += 1;
		if (x <= _1PI2) {
			sc[0] = COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha;
			sc[1] = COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha;
		} else if (x <= _2PI2) {
			sc[0] = (COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
			sc[1] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
		} else if (x <= _3PI2) {
			sc[0] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
			sc[1] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
		} else {
			sc[0] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
			sc[1] = (COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
		}
	}

	public static void sincos(float sc[], int offset, float x) {
		// Normalize x to [0, 2*PI], loops is much (2x) faster than
		// floating point module
		while (x > _4PI2)
			x -= _4PI2;
		while (x < 0)
			x += _4PI2;

		// Normalize to [0, PI/2]
		float smallX = x;
		while (smallX > _1PI2)
			smallX -= _1PI2;

		float alpha = smallX / _1PI2 * COS_TABLE_SIZE;
		int fidx = ((int)alpha) & COS_TABLE_MASK;
		int ridx = COS_TABLE_SIZE - fidx;
		alpha -= fidx;
		fidx += 1;
		ridx += 1;
		if (x <= _1PI2) {
			sc[offset + 0] = COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha;
			sc[offset + 1] = COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha;
		} else if (x <= _2PI2) {
			sc[offset + 0] = (COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
			sc[offset + 1] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
		} else if (x <= _3PI2) {
			sc[offset + 0] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
			sc[offset + 1] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
		} else {
			sc[offset + 0] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
			sc[offset + 1] = (COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
		}
	}

	public static void cossin(float cs[], float x) {
		// Normalize x to [0, 2*PI], loops is much (2x) faster than
		// floating point module
		while (x > _4PI2)
			x -= _4PI2;
		while (x < 0)
			x += _4PI2;

		// Normalize to [0, PI/2]
		float smallX = x;
		while (smallX > _1PI2)
			smallX -= _1PI2;

		float alpha = smallX / _1PI2 * COS_TABLE_SIZE;
		int fidx = ((int)alpha) & COS_TABLE_MASK;
		int ridx = COS_TABLE_SIZE - fidx;
		alpha -= fidx;
		fidx += 1;
		ridx += 1;
		if (x <= _1PI2) {
			cs[0] = COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha;
			cs[1] = COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha;
		} else if (x <= _2PI2) {
			cs[0] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
			cs[1] = (COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
		} else if (x <= _3PI2) {
			cs[0] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
			cs[1] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
		} else {
			cs[0] = (COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
			cs[1] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
		}
	}

	public static void cossin(float cs[], int offset, float x) {
		// Normalize x to [0, 2*PI], loops is much (2x) faster than
		// floating point module
		while (x > _4PI2)
			x -= _4PI2;
		while (x < 0)
			x += _4PI2;

		// Normalize to [0, PI/2]
		float smallX = x;
		while (smallX > _1PI2)
			smallX -= _1PI2;

		float alpha = smallX / _1PI2 * COS_TABLE_SIZE;
		int fidx = ((int)alpha) & COS_TABLE_MASK;
		int ridx = COS_TABLE_SIZE - fidx;
		alpha -= fidx;
		fidx += 1;
		ridx += 1;
		if (x <= _1PI2) {
			cs[offset + 0] = COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha;
			cs[offset + 1] = COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha;
		} else if (x <= _2PI2) {
			cs[offset + 0] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
			cs[offset + 1] = (COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
		} else if (x <= _3PI2) {
			cs[offset + 0] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
			cs[offset + 1] = -(COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
		} else {
			cs[offset + 0] = (COS[ridx] + (COS[ridx - 1] - COS[ridx]) * alpha);
			cs[offset + 1] = -(COS[fidx] + (COS[fidx + 1] - COS[fidx]) * alpha);
		}
	}
}
