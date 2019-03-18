#! /usr/bin/perl -w
use warnings;
use strict;

die "Syntax: $0 <BaseImpl.java> <VectorImpl.java>\n" unless @ARGV == 2;

my $BASE = &loadFile($ARGV[0], 1);
my $VEC  = &loadFile($ARGV[1], 0);

# Check all methods
for my $name (sort keys %{$BASE}) {
	print STDERR "Can not find vectorized $name\n" unless exists $VEC->{$name};
}

for my $name (sort keys %{$VEC}) {
	print STDERR "Can not find base $name\n" unless exists $BASE->{$name};
}

# Generate benchmark
print<<__HEADER;
/*****************************************************************************
 * Copyright (c) 2014, Lev Serebryakov <lev\@serebryakov.spb.ru>
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

\@Fork(2)
\@Warmup(iterations = 5, time = 1)
\@Measurement(iterations = 10, time = 2)
\@Threads(1)
\@State(org.openjdk.jmh.annotations.Scope.Thread)
public class VectorBenchmarks {
    private final static int MAX_SIZE = 65536;
    \@Param({"16", "1024", "65536"})
    public int size;

    private final static int MAX_OFFSET = 1;
    \@Param({"0", "1"})
    public int startOffsets;

    private float rx[];
    private float ry[];
    private float rz[];

    private float cx[];
    private float cy[];
    private float cz[];

    \@Setup(Level.Trial)
    public void Setup() {
        rx = new float[MAX_SIZE + MAX_OFFSET];
        ry = new float[MAX_SIZE + MAX_OFFSET];
        rz = new float[MAX_SIZE + MAX_OFFSET];
        for (int i = 0; i < rx.length; i++) {
            rx[i] = (float)(Math.random() * 2.0 - 1.0);
            ry[i] = (float)(Math.random() * 2.0 - 1.0);
            rz[i] = (float)(Math.random() * 2.0 - 1.0);
        }

        cx = new float[(MAX_SIZE + MAX_OFFSET) * 2];
        cy = new float[(MAX_SIZE + MAX_OFFSET) * 2];
        cz = new float[(MAX_SIZE + MAX_OFFSET) * 2];
        for (int i = 0; i < cx.length; i++) {
            cx[i] = (float)(Math.random() * 2.0 - 1.0);
            cy[i] = (float)(Math.random() * 2.0 - 1.0);
            cz[i] = (float)(Math.random() * 2.0 - 1.0);
        }
    }

__HEADER

for my $name (sort keys %{$VEC}) {
	next unless exists $BASE->{$name};

	# Parse name
	my $inplace = $name =~ /_i$/;
	if      ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]+)_(rs|rv|cs|cv)(_i)?$/) {
		my $l  = $1;
		my $op = $2;
		my $r  = $3;

		if ($inplace) {
			&generateBenchmark2i($name, $l, $op, $r, 'VO');
			&generateBenchmark2i($name, $l, $op, $r, 'VOVec');
		} else {
			&generateBenchmark2o($name, $l, $op, $r, 'VO');
			&generateBenchmark2o($name, $l, $op, $r, 'VOVec');
		}
	} elsif ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{3,})(_i)?$/) {
		my $l  = $1;
		my $op = $2;

		if ($inplace) {
			&generateBenchmark1i($name, $l, $op, 'VO');
			&generateBenchmark1i($name, $l, $op, 'VOVec');
		} else {
			&generateBenchmark1o($name, $l, $op, 'VO');
			&generateBenchmark1o($name, $l, $op, 'VOVec');
		}
	} elsif ($name =~ /^rv_rs_lin_rv_rs(_i)?$/) {
	} else {
		print STDERR "Unknown name: \"$name\"\n";
	}
}


print "}";

exit 0;

sub generateBenchmark2i {
	my ($name, $l, $op, $r, $imp) = (@_);

	&generateBenchmarkHeader($name, $imp);
	print "    }\n";
}

sub generateBenchmark2o {
	my ($name, $l, $op, $r, $imp) = (@_);

	&generateBenchmarkHeader($name, $imp);
	print "    }\n";
}

sub generateBenchmark1i {
	my ($name, $l, $op, $imp) = (@_);

	&generateBenchmarkHeader($name, $imp);
	print "    }\n";
}

sub generateBenchmark1o {
	my ($name, $l, $op, $imp) = (@_);

	&generateBenchmarkHeader($name, $imp);
	print "    }\n";
}

sub generateBenchmarkHeader {
	my ($name, $imp) = @_;
	print "\n";
	print "    \@Benchmark\n";
	print "    public void ${imp}_${name}() {\n";
}


sub loadFile {
	my ($name, $base) = @_;
	open(my $fh, '<', $name) or die "Can npot open \"$name\"\n";

	my $RV = {};
	my $total = 0;
	while (<$fh>) {
		s/^\s+//; s/\s+$//;
		next unless /^public static void ([a-z0-9_]+)\(.+?\) \{$/;
		my $name = $1;
		# Check if $name is or _w or _iw or _f or _fw
		$total++;
		if ($name =~ /_[a-z]?w$/) {
			print STDERR "Vectorized version implements strange method: \"$name\"\n" unless $base;
			next;
		}
		$RV->{$name} = 1;
	}
	print STDERR "\"$name\": loaded ", scalar(keys %{$RV}) + 1, " out of $total methods\n";
	close($fh);
	return $RV;
}
