#! /usr/bin/perl -w
#***************************************************************************
# Copyright (c) 2019, Lev Serebryakov <lev@serebryakov.spb.ru>
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
# OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
# OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#***************************************************************************
use warnings;
use strict;

use Cwd qw(abs_path);
use FindBin;
use lib abs_path("$FindBin::Bin/../../main/perl");

use OpAnalyzer;

my $SKIPPED_OPS = {
  'sub'     => 'add',
  '20log10' => '10log10',
  'conjmul' => 'mul',
  'min'     => 'max',
  'minarg'  => 'maxarg'
};

die "Syntax: $0 <BaseImpl.java> <VectorImpl.java>\n" unless @ARGV == 2;

my $BASE = &OpAnalyzer::loadFile($ARGV[0], 1);
my $VEC  = &OpAnalyzer::loadFile($ARGV[1], 0);

# Check all methods
for my $name (sort keys %{$BASE}) {
	print STDERR "Can not find vectorized \"$name\"\n" unless exists $VEC->{$name};
	print STDERR "Return type mismatch for \"$name\": \"", $BASE->{'name'}, "\" vs \"", $VEC->{$name}, "\"\n" unless !$VEC->{$name} || $VEC->{$name} eq $BASE->{$name};
}

for my $name (sort keys %{$VEC}) {
	print STDERR "Can not find base $name\n" unless exists $BASE->{$name};
}

my $CODE_INDENT = "            ";

# Generate benchmark
print<<__HEADER;
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\\
!! THIS FILE IS GENERATED WITH genBenchmarks.pl SCRIPT. DO NOT EDIT! !!
\\!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
/*****************************************************************************
 * Copyright (c) 2019, Lev Serebryakov <lev\@serebryakov.spb.ru>
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

\@Fork(2)
\@Warmup(iterations = 5, time = 1)
\@Measurement(iterations = 10, time = 2)
\@Threads(1)
\@State(org.openjdk.jmh.annotations.Scope.Thread)
public class VectorBenchmarks {
    private final static int DATA_SIZE = 65536;

    \@Param({"16", "1024", "65536"})
    public int callSize;

    private final static int MAX_OFFSET = 1;
    \@Param({"0", "1"})
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

    
    \@Setup(Level.Trial)
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
        csz = new float[] { (float)(Math.random() * 2.0 - 1.0), (float)(Math.random() * 2.0 - 1.0) };
    }

__HEADER

for my $name (sort keys %{$VEC}) {
	next unless exists $BASE->{$name};

	# Parse this op
	my $op;
	eval { $op = &OpAnalyzer::parseOp($name, $VEC->{$name}); };
	if ($@) {
		print STDERR $@;
		next;
	}

	if (exists $SKIPPED_OPS->{$op->{'op'}}) {
		print STDERR "Skip \"$name\" as it is not interesting, because \"".$SKIPPED_OPS->{$op->{'op'}}."\"\n";
		next;
	}

	# Call generators
	if      ($op->{'type'} eq 'u' &&  $op->{'ip'}) {
		&generateBenchmark1i($op, 'VO');
		&generateBenchmark1i($op, 'VOVec');
	} elsif ($op->{'type'} eq 'u' && !$op->{'ip'}) {
		&generateBenchmark1o($op, 'VO');
		&generateBenchmark1o($op, 'VOVec');
	} elsif ($op->{'type'} eq 'b' &&  $op->{'ip'}) {
		&generateBenchmark2i($op, 'VO');
		&generateBenchmark2i($op, 'VOVec');
	} elsif ($op->{'type'} eq 'b' && !$op->{'ip'}) {
		&generateBenchmark2o($op, 'VO');
		&generateBenchmark2o($op, 'VOVec');
	} elsif ($op->{'type'} eq 'q' &&  $op->{'ip'}) {
		&generateBenchmark4i($op, 'VO');
		&generateBenchmark4i($op, 'VOVec');
	} elsif ($op->{'type'} eq 'q' && !$op->{'ip'}) {
		&generateBenchmark4o($op, 'VO');
		&generateBenchmark4o($op, 'VOVec');
	} else {
		print STDERR 'Unknown ', ($op->{'ip'} ? 'in-place' : 'out-of-place'), " operation '$name' type '", $op->{'type'}, "'\n";
	}
}

print "}";

exit 0;

sub generateBenchmark1i {
	my ($op, $imp) = (@_);

	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &generateArg($out, 'z', 'i', $op->{'name'}, 'first argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';
		
	&generateBenchmarkHeader($op->{'name'}, $imp, $out);
	print $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	&generateBenchmarkFooter();
}

sub generateBenchmark1o {
	my ($op, $imp) = (@_);
	
	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &generateArg($out,       'z', 'i', $op->{'name'}, 'output') unless $out eq 'rs' || $out eq 'int';
		push @args, &generateArg($op->{'l'}, 'x', 'i', $op->{'name'}, 'first argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($op->{'name'}, $imp, $out);
	if ($out eq 'rs' || $out eq 'int') {
		print $CODE_INDENT, "bh.consume($imp.", $op->{'name'}, '(', join(', ', @args), "));\n";
	} else {
		print $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	}
	&generateBenchmarkFooter();
}

sub generateBenchmark2i {
	my ($op, $imp) = (@_);
	
	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &generateArg($op->{'l'}, 'z', 'i', $op->{'name'}, 'first argument');
		push @args, &generateArg($op->{'r'}, 'x', 'i', $op->{'name'}, 'second argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($op->{'name'}, $imp, $out);
	print $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	&generateBenchmarkFooter();
}

sub generateBenchmark2o {
	my ($op, $imp) = (@_);
	
	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &generateArg($out,       'z', 'i', $op->{'name'}, 'output') unless $out eq 'rs' || $out eq 'int';
		push @args, &generateArg($op->{'l'}, 'x', 'i', $op->{'name'}, 'first argument');
		push @args, &generateArg($op->{'r'}, 'y', 'i', $op->{'name'}, 'second argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($op->{'name'}, $imp, $out);
	if ($out eq 'rs' || $out eq 'int') {
		print $CODE_INDENT, "bh.consume($imp.", $op->{'name'}, '(', join(', ', @args), "));\n";
	} else {
		print $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	}
	&generateBenchmarkFooter();
}

sub generateBenchmark4i {
	my ($op, $imp) = (@_);

	if ($op->{'op'} ne 'lin' || $op->{'l1'} ne 'rv' || $op->{'l2'} ne 'rs' || $op->{'r1'} ne 'rv' || $op->{'r2'} ne 'rs') {
		print STDERR "Can not generate benchmark for \"", $op->{'name'}, "\" yet\n";
		return;
	}

	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &generateArg($op->{'l1'}, 'z', 'i', $op->{'name'}, 'first argument');
		push @args, &generateArg($op->{'l2'}, 'z', 'i', $op->{'name'}, 'second argument');
		push @args, &generateArg($op->{'r1'}, 'x', 'i', $op->{'name'}, 'third argument');
		push @args, &generateArg($op->{'r2'}, 'x', 'i', $op->{'name'}, 'fourth argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($op->{'name'}, $imp, $out);
	print $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	&generateBenchmarkFooter();
}

sub generateBenchmark4o {
	my ($op, $imp) = (@_);

	if ($op->{'op'} ne 'lin' || $op->{'l1'} ne 'rv' || $op->{'l2'} ne 'rs' || $op->{'r1'} ne 'rv' || $op->{'r2'} ne 'rs') {
		print STDERR "Can not generate benchmark for \"", $op->{'name'}, "\" yet\n";
		return;
	}

	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &generateArg($out,        'z', 'i', $op->{'name'}, 'output') unless $out eq 'rs' || $out eq 'int';
		push @args, &generateArg($op->{'l1'}, 'x', 'i', $op->{'name'}, 'first argument');
		push @args, &generateArg($op->{'l2'}, 'x', 'i', $op->{'name'}, 'second argument');
		push @args, &generateArg($op->{'r1'}, 'y', 'i', $op->{'name'}, 'third argument');
		push @args, &generateArg($op->{'r2'}, 'y', 'i', $op->{'name'}, 'fourth argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($op->{'name'}, $imp, $out);
	if ($out eq 'rs' || $out eq 'int') {
		print $CODE_INDENT, "bh.consume($imp.", $op->{'name'}, '(', join(', ', @args), "));\n";
	} else {
		print $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	}
	&generateBenchmarkFooter();
}

sub generateBenchmarkHeader {
	my ($name, $imp, $out) = @_;
	print "\n";
	print "    \@Benchmark\n";
	if ($out eq 'rs' || $out eq 'int') {
		print "    public void ${imp}_${name}(Blackhole bh) {\n";
	} else {
		print "    public void ${imp}_${name}() {\n";
	}
	print "        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {\n";
}

sub generateBenchmarkFooter {
	print "        }\n";
	print "    }\n";
}

sub generateArg {
	my ($t, $sfx, $cnt, $name, $obj) = @_;
	if      ($t eq 'rs' || $t eq 'cs') {
		return ($t.$sfx);
	} elsif ($t eq 'rv' || $t eq 'cv') {
		return ($t.$sfx, $cnt);
	} else {
		die "Internal consistency error: Function \"$name\" has wrong $obj type \"$t\"\n";
	}
}
