/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\
!! THIS FILE IS GENERATED WITH genTests.pl SCRIPT. DO NOT EDIT! !!
\!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
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

import vectorapi.VO;
import vectorapi.VOVec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lev Serebryakov
 * @noinspection CStyleArrayDeclaration, WeakerAccess
 */
public class VectorTests {
    private static final float EPSILON = 0.0001f;
    private static final float EPSILON_APPROX = 0.0001f;

    private static final int DATA_SIZE = 65536;
    private static final int MAX_OFFSET = 1;

    private final static VectorSpecies<Float> PFS = FloatVector.SPECIES_PREFERRED;

    static Stream<Arguments> params() {
        ArrayList<Arguments> rv = new ArrayList<>();
        rv.add(Arguments.of(1, 0));
        rv.add(Arguments.of(1, 1));
        rv.add(Arguments.of(PFS.length() / 2 - 1, 0));
        rv.add(Arguments.of(PFS.length() / 2 - 1, 1));
        rv.add(Arguments.of(PFS.length() / 2, 0));
        rv.add(Arguments.of(PFS.length() / 2, 1));
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
        rv.add(Arguments.of(DATA_SIZE, 0));
        rv.add(Arguments.of(DATA_SIZE, 1));
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


    @ParameterizedTest(name = "cs_div_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cs_div_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cs_div_cv(cvz1, 0, csx, cvy, offset, size);
        VOVec.cs_div_cv(cvz2, 0, csx, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cs_sub_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cs_sub_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cs_sub_cv(cvz1, 0, csx, cvy, offset, size);
        VOVec.cs_sub_cv(cvz2, 0, csx, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_10log10({0}, {1})")
    @MethodSource("params")
    public void Test_cv_10log10(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_10log10(cvz1, 0, cvx, offset, size);
        VOVec.cv_10log10(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "cv_10log10_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_10log10_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_10log10_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_10log10_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "cv_20log10({0}, {1})")
    @MethodSource("params")
    public void Test_cv_20log10(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_20log10(cvz1, 0, cvx, offset, size);
        VOVec.cv_20log10(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "cv_20log10_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_20log10_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_20log10_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_20log10_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON_APPROX);
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

    @ParameterizedTest(name = "cv_conjmul_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_conjmul_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_conjmul_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_conjmul_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_conjmul_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_conjmul_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_conjmul_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_conjmul_cv_i(cvz2, offset, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_cpy({0}, {1})")
    @MethodSource("params")
    public void Test_cv_cpy(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_cpy(cvz1, 0, cvx, offset, size);
        VOVec.cv_cpy(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_cs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_cs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_div_cs(cvz1, 0, cvx, offset, csy, size);
        VOVec.cv_div_cs(cvz2, 0, cvx, offset, csy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_cs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_cs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_div_cs_i(cvz1, offset, csx, size);
        VOVec.cv_div_cs_i(cvz2, offset, csx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_div_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_div_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_div_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_div_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_div_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_div_cv_i(cvz2, offset, cvx, offset, size);
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

    @ParameterizedTest(name = "cv_dot_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_dot_cv(int size, int offset) {
        float csz1[] = new float[2];
        float csz2[] = new float[2];
        VO.cv_dot_cv(csz1, cvx, offset, cvy, offset, size);
        VOVec.cv_dot_cv(csz2, cvx, offset, cvy, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON * size);
    }

    @ParameterizedTest(name = "cv_dot_cv_zoffset({0}, {1})")
    @MethodSource("params")
    public void Test_cv_dot_cv_zoffset(int size, int offset) {
        float csz1[] = new float[6];
        float csz2[] = new float[6];
        VO.cv_dot_cv(csz1, 1, cvx, offset, cvy, offset, size);
        VOVec.cv_dot_cv(csz2, 1, cvx, offset, cvy, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON * size);
    }

    @ParameterizedTest(name = "cv_exp({0}, {1})")
    @MethodSource("params")
    public void Test_cv_exp(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_exp(cvz1, 0, cvx, offset, size);
        VOVec.cv_exp(cvz2, 0, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_exp_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_exp_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);
        VO.cv_exp_i(cvz1, offset, size);
        VOVec.cv_exp_i(cvz2, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_im({0}, {1})")
    @MethodSource("params")
    public void Test_cv_im(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.cv_im(rvz1, 0, cvx, offset, size);
        VOVec.cv_im(rvz2, 0, cvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_max({0}, {1})")
    @MethodSource("params")
    public void Test_cv_max(int size, int offset) {
        float csz1[] = new float[2];
        float csz2[] = new float[2];
        VO.cv_max(csz1, cvx, offset, size);
        VOVec.cv_max(csz2, cvx, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_max_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_max_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_max_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_max_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_max_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_max_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_max_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_max_cv_i(cvz2, offset, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_maxarg({0}, {1})")
    @MethodSource("params")
    public void Test_cv_maxarg(int size, int offset) {
        int intz1 = VO.cv_maxarg(cvx, offset, size);
        int intz2 = VOVec.cv_maxarg(cvx, offset, size);
        assertEquals(intz1, intz2);
    }

    @ParameterizedTest(name = "cv_min({0}, {1})")
    @MethodSource("params")
    public void Test_cv_min(int size, int offset) {
        float csz1[] = new float[2];
        float csz2[] = new float[2];
        VO.cv_min(csz1, cvx, offset, size);
        VOVec.cv_min(csz2, cvx, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_min_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_min_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_min_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_min_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_min_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_min_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_min_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_min_cv_i(cvz2, offset, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_minarg({0}, {1})")
    @MethodSource("params")
    public void Test_cv_minarg(int size, int offset) {
        int intz1 = VO.cv_minarg(cvx, offset, size);
        int intz2 = VOVec.cv_minarg(cvx, offset, size);
        assertEquals(intz1, intz2);
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

    @ParameterizedTest(name = "cv_re({0}, {1})")
    @MethodSource("params")
    public void Test_cv_re(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.cv_re(rvz1, 0, cvx, offset, size);
        VOVec.cv_re(rvz2, 0, cvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_rs_lin_rv_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_rs_lin_rv_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_rs_lin_rv_rs(cvz1, 0, cvx, offset, rsx, rvy, offset, rsy, size);
        VOVec.cv_rs_lin_rv_rs(cvz2, 0, cvx, offset, rsx, rvy, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_rs_lin_rv_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_rs_lin_rv_rs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);
        VO.cv_rs_lin_rv_rs_i(cvz1, offset, rsz, rvx, offset, rsx, size);
        VOVec.cv_rs_lin_rv_rs_i(cvz2, offset, rsz, rvx, offset, rsx, size);
        assertArrayEquals(cvz2, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_cs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_cs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_sub_cs(cvz1, 0, cvx, offset, csy, size);
        VOVec.cv_sub_cs(cvz2, 0, cvx, offset, csy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_cs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_cs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_sub_cs_i(cvz1, offset, csx, size);
        VOVec.cv_sub_cs_i(cvz2, offset, csx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_cv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_sub_cv(cvz1, 0, cvx, offset, cvy, offset, size);
        VOVec.cv_sub_cv(cvz2, 0, cvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_cv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_cv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_sub_cv_i(cvz1, offset, cvx, offset, size);
        VOVec.cv_sub_cv_i(cvz2, offset, cvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_rs({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_rs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_sub_rs(cvz1, 0, cvx, offset, rsy, size);
        VOVec.cv_sub_rs(cvz2, 0, cvx, offset, rsy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_rs_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_sub_rs_i(cvz1, offset, rsx, size);
        VOVec.cv_sub_rs_i(cvz2, offset, rsx, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_rv({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_rv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.cv_sub_rv(cvz1, 0, cvx, offset, rvy, offset, size);
        VOVec.cv_sub_rv(cvz2, 0, cvx, offset, rvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sub_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sub_rv_i(int size, int offset) {
        float cvz1[] = Arrays.copyOf(cvz, cvz.length);
        float cvz2[] = Arrays.copyOf(cvz, cvz.length);

        VO.cv_sub_rv_i(cvz1, offset, rvx, offset, size);
        VOVec.cv_sub_rv_i(cvz2, offset, rvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "cv_sum({0}, {1})")
    @MethodSource("params")
    public void Test_cv_sum(int size, int offset) {
        float csz1[] = new float[2];
        float csz2[] = new float[2];
        VO.cv_sum(csz1, cvx, offset, size);
        VOVec.cv_sum(csz2, cvx, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON * size);
    }

    @ParameterizedTest(name = "rs_div_cv({0}, {1})")
    @MethodSource("params")
    public void Test_rs_div_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rs_div_cv(cvz1, 0, rsx, cvy, offset, size);
        VOVec.rs_div_cv(cvz2, 0, rsx, cvy, offset, size);
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

    @ParameterizedTest(name = "rs_sub_cv({0}, {1})")
    @MethodSource("params")
    public void Test_rs_sub_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rs_sub_cv(cvz1, 0, rsx, cvy, offset, size);
        VOVec.rs_sub_cv(cvz2, 0, rsx, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "rs_sub_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rs_sub_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rs_sub_rv(rvz1, 0, rsx, rvy, offset, size);
        VOVec.rs_sub_rv(rvz2, 0, rsx, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_10log10({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_10log10(rvz1, 0, rvx, offset, size);
        VOVec.rv_10log10(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_10log10_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);
        VO.rv_10log10_i(rvz1, offset, size);
        VOVec.rv_10log10_i(rvz2, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_10log10_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_10log10_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_10log10_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_10log10_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_10log10_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_10log10_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_10log10_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_20log10({0}, {1})")
    @MethodSource("params")
    public void Test_rv_20log10(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_20log10(rvz1, 0, rvx, offset, size);
        VOVec.rv_20log10(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_20log10_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_20log10_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);
        VO.rv_20log10_i(rvz1, offset, size);
        VOVec.rv_20log10_i(rvz2, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_20log10_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_20log10_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_20log10_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_20log10_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
    }

    @ParameterizedTest(name = "rv_20log10_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_20log10_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_20log10_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_20log10_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON_APPROX);
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

    @ParameterizedTest(name = "rv_conjmul_cv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_conjmul_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_conjmul_cv(cvz1, 0, rvx, offset, cvy, offset, size);
        VOVec.rv_conjmul_cv(cvz2, 0, rvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_cpy({0}, {1})")
    @MethodSource("params")
    public void Test_rv_cpy(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_cpy(rvz1, 0, rvx, offset, size);
        VOVec.rv_cpy(rvz2, 0, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_cs_lin_rv_cs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_cs_lin_rv_cs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_cs_lin_rv_cs(cvz1, 0, rvx, offset, csx, rvy, offset, csy, size);
        VOVec.rv_cs_lin_rv_cs(cvz2, 0, rvx, offset, csx, rvy, offset, csy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_cvt({0}, {1})")
    @MethodSource("params")
    public void Test_rv_cvt(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_cvt(cvz1, 0, rvx, offset, size);
        VOVec.rv_cvt(cvz2, 0, rvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_div_cv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_div_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_div_cv(cvz1, 0, rvx, offset, cvy, offset, size);
        VOVec.rv_div_cv(cvz2, 0, rvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
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
        assertArrayEquals(csz1, csz2, EPSILON * size);
    }

    @ParameterizedTest(name = "rv_dot_cv_zoffset({0}, {1})")
    @MethodSource("params")
    public void Test_rv_dot_cv_zoffset(int size, int offset) {
        float csz1[] = new float[6];
        float csz2[] = new float[6];
        VO.rv_dot_cv(csz1, 1, rvx, offset, cvy, offset, size);
        VOVec.rv_dot_cv(csz2, 1, rvx, offset, cvy, offset, size);
        assertArrayEquals(csz1, csz2, EPSILON * size);
    }

    @ParameterizedTest(name = "rv_dot_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_dot_rv(int size, int offset) {
        float rsz1 = VO.rv_dot_rv(rvx, offset, rvy, offset, size);
        float rsz2 = VOVec.rv_dot_rv(rvx, offset, rvy, offset, size);
        assertEquals(rsz1, rsz2, EPSILON * size);
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
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_expi(cvz1, 0, rvx, offset, size);
        VOVec.rv_expi(cvz2, 0, rvx, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
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
        int intz1 = VO.rv_maxarg(rvx, offset, size);
        int intz2 = VOVec.rv_maxarg(rvx, offset, size);
        assertEquals(intz1, intz2);
    }

    @ParameterizedTest(name = "rv_min({0}, {1})")
    @MethodSource("params")
    public void Test_rv_min(int size, int offset) {
        float rsz1 = VO.rv_min(rvx, offset, size);
        float rsz2 = VOVec.rv_min(rvx, offset, size);
        assertEquals(rsz1, rsz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_min_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_min_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_min_rv(rvz1, 0, rvx, offset, rvy, offset, size);
        VOVec.rv_min_rv(rvz2, 0, rvx, offset, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_min_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_min_rv_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_min_rv_i(rvz1, offset, rvx, offset, size);
        VOVec.rv_min_rv_i(rvz2, offset, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_minarg({0}, {1})")
    @MethodSource("params")
    public void Test_rv_minarg(int size, int offset) {
        int intz1 = VO.rv_minarg(rvx, offset, size);
        int intz2 = VOVec.rv_minarg(rvx, offset, size);
        assertEquals(intz1, intz2);
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

    @ParameterizedTest(name = "rv_rs_lin_rv_cs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_rs_lin_rv_cs(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_rs_lin_rv_cs(cvz1, 0, rvx, offset, rsx, rvy, offset, csy, size);
        VOVec.rv_rs_lin_rv_cs(cvz2, 0, rvx, offset, rsx, rvy, offset, csy, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
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

    @ParameterizedTest(name = "rv_sub_cv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sub_cv(int size, int offset) {
        float cvz1[] = new float[cvz.length];
        float cvz2[] = new float[cvz.length];
        VO.rv_sub_cv(cvz1, 0, rvx, offset, cvy, offset, size);
        VOVec.rv_sub_cv(cvz2, 0, rvx, offset, cvy, offset, size);
        assertArrayEquals(cvz1, cvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_sub_rs({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sub_rs(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_sub_rs(rvz1, 0, rvx, offset, rsy, size);
        VOVec.rv_sub_rs(rvz2, 0, rvx, offset, rsy, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_sub_rs_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sub_rs_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_sub_rs_i(rvz1, offset, rsx, size);
        VOVec.rv_sub_rs_i(rvz2, offset, rsx, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_sub_rv({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sub_rv(int size, int offset) {
        float rvz1[] = new float[rvz.length];
        float rvz2[] = new float[rvz.length];
        VO.rv_sub_rv(rvz1, 0, rvx, offset, rvy, offset, size);
        VOVec.rv_sub_rv(rvz2, 0, rvx, offset, rvy, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_sub_rv_i({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sub_rv_i(int size, int offset) {
        float rvz1[] = Arrays.copyOf(rvz, rvz.length);
        float rvz2[] = Arrays.copyOf(rvz, rvz.length);

        VO.rv_sub_rv_i(rvz1, offset, rvx, offset, size);
        VOVec.rv_sub_rv_i(rvz2, offset, rvx, offset, size);
        assertArrayEquals(rvz1, rvz2, EPSILON);
    }

    @ParameterizedTest(name = "rv_sum({0}, {1})")
    @MethodSource("params")
    public void Test_rv_sum(int size, int offset) {
        float rsz1 = VO.rv_sum(rvx, offset, size);
        float rsz2 = VOVec.rv_sum(rvx, offset, size);
        assertEquals(rsz1, rsz2, EPSILON * size);
    }
}