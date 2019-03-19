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

import jdk.incubator.vector.FloatVector;
import vectorapi.VO;
import vectorapi.VOVec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lev Serebryakov
 * @noinspection CStyleArrayDeclaration, WeakerAccess
 */
public class VectorTests {
    // We can not have 1E-7 EPSILON on horizontal operations (!)
    private static final float EPSILON = 0.000001f;

    private static final int DATA_SIZE = 65536;
    private static final int MAX_OFFSET = 63;

    private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();

    static Stream<Arguments> params() {
        ArrayList<Arguments> rv = new ArrayList<>();
        rv.add(Arguments.of(1, 0));
        rv.add(Arguments.of(1, 1));
        rv.add(Arguments.of(PFS.length() - 1, 0));
        rv.add(Arguments.of(PFS.length() - 1, 1));
        rv.add(Arguments.of(PFS.length(), 0));
        rv.add(Arguments.of(PFS.length(), 1));
        rv.add(Arguments.of(PFS.length() + 1, 0));
        rv.add(Arguments.of(PFS.length() + 1, 1));
        rv.add(Arguments.of(PFS.length() * 2, 0));
        rv.add(Arguments.of(PFS.length() * 2, 1));
        rv.add(Arguments.of(PFS.length() * 2 + 1, 0));
        rv.add(Arguments.of(PFS.length() * 2 + 1, 1));
        return rv.stream();
    }

    private static float rvx[];
    private static float rvy[];
    private static float rvz[];

    private static float cvx[];
    private static float cvy[];
    private static float cvz[];
    
    private static float rsx;
    private static float rsy;
    private static float rsz;
    
    private static float csx[];
    private static float csy[];
    
    @BeforeAll
    public static void Setup() {
        rvx = new float[DATA_SIZE + MAX_OFFSET];
        rvy = new float[DATA_SIZE + MAX_OFFSET];
        rvz = new float[DATA_SIZE + MAX_OFFSET];
        for (int i = 0; i < rvx.length; i++) {
            rvx[i] = (float)(Math.random() * 2.0 - 1.0);
            rvy[i] = (float)(Math.random() * 2.0 - 1.0);
            rvz[i] = (float)(Math.random() * 2.0 - 1.0);
        }

        cvx = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        cvy = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        cvz = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        for (int i = 0; i < cvx.length; i++) {
            cvx[i] = (float)(Math.random() * 2.0 - 1.0);
            cvy[i] = (float)(Math.random() * 2.0 - 1.0);
            cvz[i] = (float)(Math.random() * 2.0 - 1.0);
        }
        
        rsx = (float)(Math.random() * 2.0 - 1.0);
        rsy = (float)(Math.random() * 2.0 - 1.0);
        rsz = (float)(Math.random() * 2.0 - 1.0);
        
        csx = new float[] { (float)(Math.random() * 2.0 - 1.0), (float)(Math.random() * 2.0 - 1.0) };
        csy = new float[] { (float)(Math.random() * 2.0 - 1.0), (float)(Math.random() * 2.0 - 1.0) };
    }


    @ParameterizedTest(name = "cv_abs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_abs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_abs(cvz1, 0, cvx, offset, size);
        VOVec.cv_abs(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_cs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_cs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_add_cs(cvz1, 0, cvx, offset, csy, size);
        VOVec.cv_add_cs(cvz2, 0, cvx, offset, csy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_cs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_cs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_add_cs_i(cvz1, offset, csx, size);
        VOVec.cv_add_cs_i(cvz2, offset, csx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_add_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_add_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_add_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_add_cv_i(cvz2, offset, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_add_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_add_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_rs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_add_rs_i(cvz1, offset, rsx, size);
        VOVec.cv_add_rs_i(cvz2, offset, rsx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_rv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_rv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_add_rv(cvz1, 0, cvx, offset, rvy, offset, size);
        VOVec.cv_add_rv(cvz2, 0, cvx, offset, rvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_add_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_add_rv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_add_rv_i(cvz1, offset, rvx, offset, size);
        VOVec.cv_add_rv_i(cvz2, offset, rvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_arg({0}, {1})")
    @MethodSource("params")
    public void Test_cv_arg(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_arg(cvz1, 0, cvx, offset, size);
        VOVec.cv_arg(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_argmul_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_argmul_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_argmul_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_argmul_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_conj({0}, {1})")
    @MethodSource("params")
    public void Test_cv_conj(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_conj(cvz1, 0, cvx, offset, size);
        VOVec.cv_conj(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_conj_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_conj_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);
        VO.cv_conj_i(cvz1, offset, size);
        VOVec.cv_conj_i(cvz2, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_div_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_div_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_rs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_div_rs_i(cvz1, offset, rsx, size);
        VOVec.cv_div_rs_i(cvz2, offset, rsx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_rv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_rv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_div_rv(cvz1, 0, cvx, offset, rvy, offset, size);
        VOVec.cv_div_rv(cvz2, 0, cvx, offset, rvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_rv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_div_rv_i(cvz1, offset, rvx, offset, size);
        VOVec.cv_div_rv_i(cvz2, offset, rvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_cs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_cs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_mul_cs(cvz1, 0, cvx, offset, csy, size);
        VOVec.cv_mul_cs(cvz2, 0, cvx, offset, csy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_cs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_cs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_mul_cs_i(cvz1, offset, csx, size);
        VOVec.cv_mul_cs_i(cvz2, offset, csx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_mul_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_mul_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_mul_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_mul_cv_i(cvz2, offset, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_mul_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_mul_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_rs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_mul_rs_i(cvz1, offset, rsx, size);
        VOVec.cv_mul_rs_i(cvz2, offset, rsx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_rv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_rv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_mul_rv(cvz1, 0, cvx, offset, rvy, offset, size);
        VOVec.cv_mul_rv(cvz2, 0, cvx, offset, rvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_mul_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_mul_rv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_mul_rv_i(cvz1, offset, rvx, offset, size);
        VOVec.cv_mul_rv_i(cvz2, offset, rvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_p2r({0}, {1})")
    @MethodSource("params")
    public void Test_cv_p2r(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_p2r(cvz1, 0, cvx, offset, size);
        VOVec.cv_p2r(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_p2r_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_p2r_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);
        VO.cv_p2r_i(cvz1, offset, size);
        VOVec.cv_p2r_i(cvz2, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_r2p({0}, {1})")
    @MethodSource("params")
    public void Test_cv_r2p(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_r2p(cvz1, 0, cvx, offset, size);
        VOVec.cv_r2p(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_r2p_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_r2p_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);
        VO.cv_r2p_i(cvz1, offset, size);
        VOVec.cv_r2p_i(cvz2, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sum({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sum(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_sum(cvz1, 0, cvx, offset, size);
        VOVec.cv_sum(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "rs_div_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rs_div_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rs_div_rv(rvz1, 0, rsx, rvy, offset, size);
        VOVec.rs_div_rv(rvz2, 0, rsx, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_10log10({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_10log10(rvz1, 0, rvx, offset, size);
        VOVec.rv_10log10(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_10log10_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);
        VO.rv_10log10_i(rvz1, offset, size);
        VOVec.rv_10log10_i(rvz2, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_10log10_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_10log10_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_10log10_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_10log10_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_10log10_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_10log10_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_abs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_abs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_abs(rvz1, 0, rvx, offset, size);
        VOVec.rv_abs(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_abs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_abs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);
        VO.rv_abs_i(rvz1, offset, size);
        VOVec.rv_abs_i(rvz2, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_add_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_add_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_add_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_add_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_add_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_add_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_add_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_add_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_add_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_add_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_add_rv(rvz1, 0, rvx, offset, rvy, offset, size);
        VOVec.rv_add_rv(rvz2, 0, rvx, offset, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_add_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_add_rv_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_add_rv_i(rvz1, offset, rvx, offset, size);
        VOVec.rv_add_rv_i(rvz2, offset, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_div_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_div_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_div_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_div_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_div_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_div_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_div_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_div_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_div_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_div_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_div_rv(rvz1, 0, rvx, offset, rvy, offset, size);
        VOVec.rv_div_rv(rvz2, 0, rvx, offset, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_div_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_div_rv_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_div_rv_i(rvz1, offset, rvx, offset, size);
        VOVec.rv_div_rv_i(rvz2, offset, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_dot_cv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_dot_cv(int size, int offset) {
        float csz1[] = new float[2];
        float csz2[] = new float[2];
        VO.rv_dot_cv(csz1, rvx, offset, cvy, offset, size);
        VOVec.rv_dot_cv(csz2, rvx, offset, cvy, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_dot_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_dot_rv(int size, int offset) {
        float rsz1 = VO.rv_dot_rv(rvx, offset, rvy, offset, size);
        float rsz2 = VOVec.rv_dot_rv(rvx, offset, rvy, offset, size);
        assertEquals(rsz1, rsz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_exp({0}, {1})")
    @MethodSource("params")
    public void Test_rv_exp(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_exp(rvz1, 0, rvx, offset, size);
        VOVec.rv_exp(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_exp_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_exp_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);
        VO.rv_exp_i(rvz1, offset, size);
        VOVec.rv_exp_i(rvz2, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_expi({0}, {1})")
    @MethodSource("params")
    public void Test_rv_expi(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_expi(rvz1, 0, rvx, offset, size);
        VOVec.rv_expi(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_max({0}, {1})")
    @MethodSource("params")
    public void Test_rv_max(int size, int offset) {
        float rsz1 = VO.rv_max(rvx, offset, size);
        float rsz2 = VOVec.rv_max(rvx, offset, size);
        assertEquals(rsz1, rsz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_max_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_max_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_max_rv(rvz1, 0, rvx, offset, rvy, offset, size);
        VOVec.rv_max_rv(rvz2, 0, rvx, offset, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_max_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_max_rv_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_max_rv_i(rvz1, offset, rvx, offset, size);
        VOVec.rv_max_rv_i(rvz2, offset, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_maxarg({0}, {1})")
    @MethodSource("params")
    public void Test_rv_maxarg(int size, int offset) {
        int rsz1 = VO.rv_maxarg(rvx, offset, size);
        int rsz2 = VOVec.rv_maxarg(rvx, offset, size);
        assertEquals(rsz1, rsz2);
    }

    @ParameterizedTest(name = "rv_mul_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_mul_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_mul_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_mul_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_mul_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_mul_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_mul_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_mul_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_mul_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_mul_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_mul_rv(rvz1, 0, rvx, offset, rvy, offset, size);
        VOVec.rv_mul_rv(rvz2, 0, rvx, offset, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_mul_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_mul_rv_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_mul_rv_i(rvz1, offset, rvx, offset, size);
        VOVec.rv_mul_rv_i(rvz2, offset, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_rs_lin_rv_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_rs_lin_rv_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_rs_lin_rv_rs(rvz1, 0, rvx, offset, rsx, rvy, offset, rsy, size);
        VOVec.rv_rs_lin_rv_rs(rvz2, 0, rvx, offset, rsx, rvy, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_rs_lin_rv_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_rs_lin_rv_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);
        VO.rv_rs_lin_rv_rs_i(rvz1, offset, rsz, rvx, offset, rsx, size);
        VOVec.rv_rs_lin_rv_rs_i(rvz2, offset, rsz, rvx, offset, rsx, size);
        assertArrayEquals(rvz2, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_sum({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sum(int size, int offset) {
        float rsz1 = VO.rv_sum(rvx, offset, size);
        float rsz2 = VOVec.rv_sum(rvx, offset, size);
        assertEquals(rsz1, rsz2, EPSILON);
    }
}