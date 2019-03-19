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

import vectorapi.VO;
import vectorapi.VOVec;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 2)
@Threads(1)
@State(org.openjdk.jmh.annotations.Scope.Thread)
public class VectorBenchmarks {
    private final static int DATA_SIZE = 65536;

    @Param({"16", "1024", "65536"})
    public int callSize;

    private final static int MAX_OFFSET = 1;
    @Param({"0", "1"})
    public int startOffset;

    private float rvx[];
    private float rvy[];
    private float rvz[];

    private float cvx[];
    private float cvy[];
    private float cvz[];
    
    private float rsx;
    private float rsy;
    private float rsz;
    
    private float csx[];
    private float csy[];
    private float csz[];

    
    @Setup(Level.Trial)
    public void Setup() {
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
        csy = new float[] { (float)(Math.random() * 2.0 - 1.0), (float)(Math.random() * 2.0 - 1.0) };
    }


    @Benchmark
    public void VO_cv_abs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_abs(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_abs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_abs(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_cs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_cs(cvz, i, cvx, i, csy, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_cs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_cs(cvz, i, cvx, i, csy, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_cs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_cs_i(cvz, i, csx, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_cs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_cs_i(cvz, i, csx, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_cv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_cv(cvz, i, cvx, i, cvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_cv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_cv(cvz, i, cvx, i, cvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_cv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_cv_i(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_cv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_cv_i(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_rs_i(cvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_rs_i(cvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_rv(cvz, i, cvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_rv(cvz, i, cvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_add_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_add_rv_i(cvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_add_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_add_rv_i(cvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_arg() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_arg(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_arg() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_arg(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_argmul_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_argmul_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_argmul_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_argmul_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_cv_conj() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_conj(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_conj() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_conj(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_conj_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_conj_i(cvz, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_conj_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_conj_i(cvz, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_div_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_div_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_div_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_div_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_cv_div_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_div_rs_i(cvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_div_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_div_rs_i(cvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_cv_div_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_div_rv(cvz, i, cvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_div_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_div_rv(cvz, i, cvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_div_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_div_rv_i(cvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_div_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_div_rv_i(cvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_cs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_cs(cvz, i, cvx, i, csy, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_cs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_cs(cvz, i, cvx, i, csy, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_cs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_cs_i(cvz, i, csx, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_cs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_cs_i(cvz, i, csx, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_cv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_cv(cvz, i, cvx, i, cvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_cv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_cv(cvz, i, cvx, i, cvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_cv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_cv_i(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_cv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_cv_i(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_rs(cvz, i, cvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_rs_i(cvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_rs_i(cvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_rv(cvz, i, cvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_rv(cvz, i, cvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_mul_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_mul_rv_i(cvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_mul_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_mul_rv_i(cvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_p2r() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_p2r(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_p2r() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_p2r(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_p2r_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_p2r_i(cvz, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_p2r_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_p2r_i(cvz, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_r2p() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_r2p(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_r2p() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_r2p(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_r2p_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_r2p_i(cvz, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_r2p_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_r2p_i(cvz, i, callSize);
        }
    }

    @Benchmark
    public void VO_cv_sum() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.cv_sum(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_cv_sum() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.cv_sum(cvz, i, cvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rs_div_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rs_div_rv(rvz, i, rsx, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rs_div_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rs_div_rv(rvz, i, rsx, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_10log10() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_10log10(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_10log10() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_10log10(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_10log10_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_10log10_i(rvz, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_10log10_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_10log10_i(rvz, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_10log10_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_10log10_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_10log10_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_10log10_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_rv_10log10_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_10log10_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_10log10_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_10log10_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_rv_abs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_abs(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_abs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_abs(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_abs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_abs_i(rvz, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_abs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_abs_i(rvz, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_add_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_add_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_add_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_add_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_rv_add_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_add_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_add_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_add_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_rv_add_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_add_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_add_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_add_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_add_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_add_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_add_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_add_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_div_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_div_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_div_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_div_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_rv_div_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_div_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_div_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_div_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_rv_div_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_div_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_div_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_div_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_div_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_div_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_div_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_div_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_dot_cv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_dot_cv(csz, rvx, i, cvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_dot_cv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_dot_cv(csz, rvx, i, cvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_dot_rv(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VO.rv_dot_rv(rvx, i, rvy, i, callSize));
        }
    }

    @Benchmark
    public void VOVec_rv_dot_rv(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VOVec.rv_dot_rv(rvx, i, rvy, i, callSize));
        }
    }

    @Benchmark
    public void VO_rv_exp() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_exp(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_exp() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_exp(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_exp_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_exp_i(rvz, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_exp_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_exp_i(rvz, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_expi() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_expi(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_expi() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_expi(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_max(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VO.rv_max(rvx, i, callSize));
        }
    }

    @Benchmark
    public void VOVec_rv_max(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VOVec.rv_max(rvx, i, callSize));
        }
    }

    @Benchmark
    public void VO_rv_max_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_max_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_max_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_max_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_max_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_max_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_max_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_max_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_maxarg(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VO.rv_maxarg(rvx, i, callSize));
        }
    }

    @Benchmark
    public void VOVec_rv_maxarg(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VOVec.rv_maxarg(rvx, i, callSize));
        }
    }

    @Benchmark
    public void VO_rv_mul_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_mul_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_mul_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_mul_rs(rvz, i, rvx, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_rv_mul_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_mul_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_mul_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_mul_rs_i(rvz, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_rv_mul_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_mul_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_mul_rv() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_mul_rv(rvz, i, rvx, i, rvy, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_mul_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_mul_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_mul_rv_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_mul_rv_i(rvz, i, rvx, i, callSize);
        }
    }

    @Benchmark
    public void VO_rv_rs_lin_rv_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_rs_lin_rv_rs(rvz, i, rvx, i, rsx, rvy, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_rs_lin_rv_rs() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_rs_lin_rv_rs(rvz, i, rvx, i, rsx, rvy, i, rsy, callSize);
        }
    }

    @Benchmark
    public void VO_rv_rs_lin_rv_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VO.rv_rs_lin_rv_rs_i(rvz, i, rsz, rvx, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VOVec_rv_rs_lin_rv_rs_i() {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            VOVec.rv_rs_lin_rv_rs_i(rvz, i, rsz, rvx, i, rsx, callSize);
        }
    }

    @Benchmark
    public void VO_rv_sum(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VO.rv_sum(rvx, i, callSize));
        }
    }

    @Benchmark
    public void VOVec_rv_sum(Blackhole bh) {
        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {
            bh.consume(VOVec.rv_sum(rvx, i, callSize));
        }
    }
}