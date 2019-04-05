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
my $GEN = {};

# Check all methods
for my $name (sort keys %{$BASE}) {
	print STDERR "Can not find vectorized \"$name\"\n" unless exists $VEC->{$name};
	print STDERR "Return type mismatch for \"$name\": \"", $BASE->{'name'}, "\" vs \"", $VEC->{$name}, "\"\n" unless !$VEC->{$name} || $VEC->{$name} eq $BASE->{$name};
}

for my $name (sort keys %{$VEC}) {
	print STDERR "Can not find base \"$name\"\n" unless exists $BASE->{$name};
}

my $CODE_INDENT = "            ";

for my $mode ('OutOfPlace', 'InPlaceR', 'InPlaceC') {
	my $className = 'VectorBenchmarks'.$mode;
	my $FH;
	open($FH, '>', $className.'.java') or die "Can not open output file \"$className.java\"\n";
	&generateBenchmarks($FH, $className, $mode);
	close($FH);
}

for my $name (sort keys %{$VEC}) {
	print STDERR "Can not generated \"$name\"\n" unless exists $GEN->{$name};
}

exit 0;

sub generateBenchmarks {
	my ($FH, $class, $mode) = @_;
	&generateHeader($FH, $class);

	if      ($mode eq 'InPlaceR') {
		&generateIterSetup($FH, 'r');
	} elsif ($mode eq 'InPlaceC') {
		&generateIterSetup($FH, 'c');
	} elsif ($mode eq 'OutOfPlace') {
		# Do nothing
	} else {
		die "Internal error: unknown mode $mode\n";
	}

	# Generate all not filtered benchmarks
	for my $name (sort keys %{$VEC}) {
		next unless exists $BASE->{$name};

		# Parse this op
		my $op;
		my $out;
		eval {
			$op = &OpAnalyzer::parseOp($name, $VEC->{$name});
			$out = &OpAnalyzer::getOutType($op);
		};
		if ($@) {
			print STDERR $@;
			next;
		}

		# Filter out by mode
		if      ($mode eq 'InPlaceR') {
			next unless $op->{'ip'} && $out eq 'rv';
		} elsif ($mode eq 'InPlaceC') {
			next unless $op->{'ip'} && $out eq 'cv';
		} elsif ($mode eq 'OutOfPlace') {
			next unless !$op->{'ip'};
		}

		# Next skip count as generated
		$GEN->{$name} = 1;

		if (exists $SKIPPED_OPS->{$op->{'op'}}) {
			print STDERR "Skip \"$name\" as it is not interesting, because \"".$SKIPPED_OPS->{$op->{'op'}}."\"\n";
			next;
		}

		# Call generators
		if      ($op->{'type'} eq 'u' &&  $op->{'ip'}) {
			&generateBenchmark1i($FH, $op, 'VO');
			&generateBenchmark1i($FH, $op, 'VOVec');
		} elsif ($op->{'type'} eq 'u' && !$op->{'ip'}) {
			&generateBenchmark1o($FH, $op, 'VO');
			&generateBenchmark1o($FH, $op, 'VOVec');
		} elsif ($op->{'type'} eq 'b' &&  $op->{'ip'}) {
			&generateBenchmark2i($FH, $op, 'VO');
			&generateBenchmark2i($FH, $op, 'VOVec');
		} elsif ($op->{'type'} eq 'b' && !$op->{'ip'}) {
			&generateBenchmark2o($FH, $op, 'VO');
			&generateBenchmark2o($FH, $op, 'VOVec');
		} elsif ($op->{'type'} eq 'q' &&  $op->{'ip'}) {
			&generateBenchmark4i($FH, $op, 'VO');
			&generateBenchmark4i($FH, $op, 'VOVec');
		} elsif ($op->{'type'} eq 'q' && !$op->{'ip'}) {
			&generateBenchmark4o($FH, $op, 'VO');
			&generateBenchmark4o($FH, $op, 'VOVec');
		} else {
			print STDERR 'Unknown ', ($op->{'ip'} ? 'in-place' : 'out-of-place'), " operation '$name' type '", $op->{'type'}, "'\n";
		}
	}

	print $FH "}";
}


sub generateHeader {
	my ($FH, $class) = @_;

	# Generate file header
	my $header =<<__HEADER;
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

import java.util.Random;

\@Fork(2)
\@Warmup(iterations = 5, time = 2)
\@Measurement(iterations = 10, time = 5)
\@Threads(1)
\@State(org.openjdk.jmh.annotations.Scope.Thread)
public class $class {
    private final static int SEED = 42; // Carefully selected, plucked by hands random number

    private final static int DATA_SIZE = 65536;

    \@Param({"16", "1024", "65536"})
    public int callSize;

    private final static int MAX_OFFSET = 1;
    \@Param({"0", "1"})
    public int startOffset;

    private float rvx[];
    private float rvy[];
    private float rvz[];
    private float rvd[];

    private float cvx[];
    private float cvy[];
    private float cvz[];
    private float cvd[];
    
    private float rsx;
    private float rsy;
    private float rsz;
    
    private float csx[];
    private float csy[];
    private float csz[];

    
    \@Setup(Level.Trial)
    public void Setup() {
        Random r = new Random(SEED);

        rvx = new float[DATA_SIZE + MAX_OFFSET];
        rvy = new float[DATA_SIZE + MAX_OFFSET];
        rvz = new float[DATA_SIZE + MAX_OFFSET];
        rvd = new float[DATA_SIZE + MAX_OFFSET];
        for (int i = 0; i < rvx.length; i++) {
            rvx[i] = r.nextFloat() * 2.0f - 1.0f;
            rvy[i] = r.nextFloat() * 2.0f - 1.0f;
            rvd[i] = rvz[i] = r.nextFloat() * 2.0f - 1.0f;
        }

        cvx = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        cvy = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        cvz = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        cvd = new float[(DATA_SIZE + MAX_OFFSET) * 2];
        for (int i = 0; i < cvx.length; i++) {
            cvx[i] = r.nextFloat() * 2.0f - 1.0f;
            cvy[i] = r.nextFloat() * 2.0f - 1.0f;
            cvd[i] = cvz[i] = r.nextFloat() * 2.0f - 1.0f;
        }
        
        rsx = r.nextFloat() * 2.0f - 1.0f;
        rsy = r.nextFloat() * 2.0f - 1.0f;
        rsz = r.nextFloat() * 2.0f - 1.0f;
        
        csx = new float[] { r.nextFloat() * 2.0f - 1.0f, r.nextFloat() * 2.0f - 1.0f };
        csy = new float[] { r.nextFloat() * 2.0f - 1.0f, r.nextFloat() * 2.0f - 1.0f };
        csz = new float[] { r.nextFloat() * 2.0f - 1.0f, r.nextFloat() * 2.0f - 1.0f };
    }

__HEADER
	print $FH $header;
}

sub generateIterSetup {
	my ($FH, $type) = @_;
	my $setup = <<__SETUP;
    \@Setup(Level.Invocation)
    public void SetupInPlaceData() {
        System.arraycopy(${type}vd, 0, ${type}vz, 0, ${type}vd.length);
    }

__SETUP
	print $FH $setup;
}

sub generateBenchmark1i {
	my ($FH, $op, $imp) = (@_);

	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &OpAnalyzer::generateArg($out, 'z', 'i', $op->{'name'}, 'first argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';
		
	&generateBenchmarkHeader($FH, $op->{'name'}, $imp, $out);
	print $FH $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	&generateBenchmarkFooter($FH);
}

sub generateBenchmark1o {
	my ($FH, $op, $imp) = (@_);
	
	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &OpAnalyzer::generateArg($out,       'z', 'i', $op->{'name'}, 'output') unless $out eq 'rs' || $out eq 'int';
		push @args, &OpAnalyzer::generateArg($op->{'l'}, 'x', 'i', $op->{'name'}, 'first argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($FH, $op->{'name'}, $imp, $out);
	if ($out eq 'rs' || $out eq 'int') {
		print $FH $CODE_INDENT, "bh.consume($imp.", $op->{'name'}, '(', join(', ', @args), "));\n";
	} else {
		print $FH $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	}
	&generateBenchmarkFooter($FH);
}

sub generateBenchmark2i {
	my ($FH, $op, $imp) = (@_);
	
	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &OpAnalyzer::generateArg($op->{'l'}, 'z', 'i', $op->{'name'}, 'first argument');
		push @args, &OpAnalyzer::generateArg($op->{'r'}, 'x', 'i', $op->{'name'}, 'second argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($FH, $op->{'name'}, $imp, $out);
	print $FH $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	&generateBenchmarkFooter($FH);
}

sub generateBenchmark2o {
	my ($FH, $op, $imp) = (@_);
	
	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &OpAnalyzer::generateArg($out,       'z', 'i', $op->{'name'}, 'output') unless $out eq 'rs' || $out eq 'int';
		push @args, &OpAnalyzer::generateArg($op->{'l'}, 'x', 'i', $op->{'name'}, 'first argument');
		push @args, &OpAnalyzer::generateArg($op->{'r'}, 'y', 'i', $op->{'name'}, 'second argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($FH, $op->{'name'}, $imp, $out);
	if ($out eq 'rs' || $out eq 'int') {
		print $FH $CODE_INDENT, "bh.consume($imp.", $op->{'name'}, '(', join(', ', @args), "));\n";
	} else {
		print $FH $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	}
	&generateBenchmarkFooter($FH);
}

sub generateBenchmark4i {
	my ($FH, $op, $imp) = (@_);

	if ($op->{'op'} ne 'lin') {
		print STDERR "Can not generate benchmark for \"", $op->{'name'}, "\" yet\n";
		return;
	}

	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &OpAnalyzer::generateArg($op->{'l1'}, 'z', 'i', $op->{'name'}, 'first argument');
		push @args, &OpAnalyzer::generateArg($op->{'l2'}, 'z', 'i', $op->{'name'}, 'second argument');
		push @args, &OpAnalyzer::generateArg($op->{'r1'}, 'x', 'i', $op->{'name'}, 'third argument');
		push @args, &OpAnalyzer::generateArg($op->{'r2'}, 'x', 'i', $op->{'name'}, 'fourth argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($FH, $op->{'name'}, $imp, $out);
	print $FH $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	&generateBenchmarkFooter($FH);
}

sub generateBenchmark4o {
	my ($FH, $op, $imp) = (@_);

	if ($op->{'op'} ne 'lin') {
		print STDERR "Can not generate benchmark for \"", $op->{'name'}, "\" yet\n";
		return;
	}

	my $out;
	my @args = ();
	eval {
		$out = &OpAnalyzer::getOutType($op);
		push @args, &OpAnalyzer::generateArg($out,        'z', 'i', $op->{'name'}, 'output') unless $out eq 'rs' || $out eq 'int';
		push @args, &OpAnalyzer::generateArg($op->{'l1'}, 'x', 'i', $op->{'name'}, 'first argument');
		push @args, &OpAnalyzer::generateArg($op->{'l2'}, 'x', 'i', $op->{'name'}, 'second argument');
		push @args, &OpAnalyzer::generateArg($op->{'r1'}, 'y', 'i', $op->{'name'}, 'third argument');
		push @args, &OpAnalyzer::generateArg($op->{'r2'}, 'y', 'i', $op->{'name'}, 'fourth argument');
	};
	if ($@) {
		print STDERR $@;
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($FH, $op->{'name'}, $imp, $out);
	if ($out eq 'rs' || $out eq 'int') {
		print $FH $CODE_INDENT, "bh.consume($imp.", $op->{'name'}, '(', join(', ', @args), "));\n";
	} else {
		print $FH $CODE_INDENT, "$imp.", $op->{'name'}, '(', join(', ', @args), ");\n";
	}
	&generateBenchmarkFooter($FH);
}

sub generateBenchmarkHeader {
	my ($FH, $name, $imp, $out) = @_;
	print $FH "\n";
	print $FH "    \@Benchmark\n";
	if ($out eq 'rs' || $out eq 'int') {
		print $FH "    public void ${imp}_${name}(Blackhole bh) {\n";
	} else {
		print $FH "    public void ${imp}_${name}() {\n";
	}
	print $FH "        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {\n";
}

sub generateBenchmarkFooter {
	my ($FH) = @_;
	print $FH "        }\n";
	print $FH "    }\n";
}
