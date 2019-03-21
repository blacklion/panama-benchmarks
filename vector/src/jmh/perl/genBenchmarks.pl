#! /usr/bin/perl -w
#***************************************************************************
# Copyright (c) 2014, Lev Serebryakov <lev@serebryakov.spb.ru>
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

die "Syntax: $0 <BaseImpl.java> <VectorImpl.java>\n" unless @ARGV == 2;

my $BASE = &loadFile($ARGV[0], 1);
my $VEC  = &loadFile($ARGV[1], 0);

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

	# Parse name
	my $inplace = $name =~ /_i$/;
	if      ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{3,})_(rs|rv|cs|cv)(_i)?$/) {
		my $l  = $1;
		my $op = $2;
		my $r  = $3;
		
		if ($op eq 'sub') {
			print STDERR "Skip \"$name\" as it is not interesting\n";
			next;
		}

		if ($inplace) {
			&generateBenchmark2i($name, $VEC->{$name}, $l, $op, $r, 'VO');
			&generateBenchmark2i($name, $VEC->{$name}, $l, $op, $r, 'VOVec');
		} else {
			&generateBenchmark2o($name, $VEC->{$name}, $l, $op, $r, 'VO');
			&generateBenchmark2o($name, $VEC->{$name}, $l, $op, $r, 'VOVec');
		}
	} elsif ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{2,})(_i)?$/) {
		my $l  = $1;
		my $op = $2;

		if ($op eq '20log10') {
			print STDERR "Skip \"$name\" as it is not interesting\n";
			next;
		}

		if ($inplace) {
			&generateBenchmark1i($name, $VEC->{$name}, $l, $op, 'VO');
			&generateBenchmark1i($name, $VEC->{$name}, $l, $op, 'VOVec');
		} else {
			&generateBenchmark1o($name, $VEC->{$name}, $l, $op, 'VO');
			&generateBenchmark1o($name, $VEC->{$name}, $l, $op, 'VOVec');
		}
	} elsif ($name =~ /^rv_rs_lin_rv_rs(_i)?$/) {
		&generateBenchmarkRVRSLinRVRS($name, $VEC->{$name}, $inplace, 'VO');
		&generateBenchmarkRVRSLinRVRS($name, $VEC->{$name}, $inplace, 'VOVec');
	} else {
		print STDERR "Unknown name: \"$name\"\n";
	}
}

print "}";

exit 0;

sub generateBenchmark2i {
	my ($name, $rtype, $l, $op, $r, $imp) = (@_);
	
	# Return type for this could be only void
	if ($rtype ne 'void') {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}
	
	# Make args
	my @args = ();
	if      ($l eq 'rv' || $l eq 'cv') {
		push @args, $l.'z', 'i';
	} else {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}

	if      ($r eq 'rs' || $r eq 'cs') {
		push @args, $r.'x';
	} elsif ($r eq 'rv' || $r eq 'cv') {
		push @args, $r.'x', 'i';
	} else {
		print STDERR "Function \"$name\" has wrong second argument\n";
		return;
	}
	push @args, 'callSize';

	&generateBenchmarkHeader($name, $imp);
	print $CODE_INDENT, "$imp.$name(", join(', ', @args), ");\n";
	&generateBenchmarkFooter();
}

sub generateBenchmark2o {
	my ($name, $rtype, $l, $op, $r, $imp) = (@_);

	# Make args
	my @args = ();

	my $out;
	if (($l eq 'cv' || $r eq 'cv') && $op eq 'dot') {
		# Special case: not a vector
		$out = 'cs';
	} else {
		my $c = $l =~ /^c/ || $r =~ /^c/;
		my $v = $l =~ /v$/ || $r =~ /v$/;

		if ($c && $rtype ne 'void') {
			print STDERR "Function \"$name\" has wrong combination of ($l, $r) -> $rtype\n";
			return;
		}
		if (!$v && !$c && $rtype eq 'void') {
			print STDERR "Function \"$name\" has wrong combination of ($l, $r) -> $rtype\n";
			return;
		}

		if ($rtype ne 'void') {
			$out = 'rs';
		} else {
			$out = ($c ? 'c' : 'r') . ($v ? 'v' : 's');
		}
	}
	if      ($out eq 'rs') {
		# Do nothing, need BH
	} elsif ($out eq 'cs') {
		push @args, 'csz';
	} else {
		push @args, $out.'z', 'i';
	}

	if      ($l eq 'rs' || $l eq 'cs') {
		push @args, $l.'x';
	} elsif ($l eq 'rv' || $l eq 'cv') {
		push @args, $l.'x', 'i';
	} else {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}
	if      ($r eq 'rs' || $r eq 'cs') {
		push @args, $r.'y';
	} elsif ($r eq 'rv' || $r eq 'cv') {
		push @args, $r.'y', 'i';
	} else {
		print STDERR "Function \"$name\" has wrong second argument\n";
		return;
	}
	push @args, 'callSize';

	# Return type for this could be only void
	if      ($rtype eq 'float' || $rtype eq 'int') {
		if ($out ne 'rs') {
			print STDERR "Function \"$name\": Internal error ($l, $r) -> ($out, $rtype)\n";
			return;
		}
		&generateBenchmarkHeaderTyped($name, $imp, $rtype);
	} elsif ($rtype eq 'void') {
		&generateBenchmarkHeader($name, $imp);
	} else {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}

	if ($rtype eq 'void') {
		print $CODE_INDENT, "$imp.$name(", join(', ', @args), ");\n";
	} else {
		print $CODE_INDENT, "bh.consume($imp.$name(", join(', ', @args), "));\n";
	}
	&generateBenchmarkFooter();
}

sub generateBenchmark1i {
	my ($name, $rtype, $l, $op, $imp) = (@_);

	# Return type for this could be only void
	if ($rtype ne 'void') {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}
	if ($l ne 'rv' && $l ne 'cv') {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}
		
	&generateBenchmarkHeader($name, $imp);
	print $CODE_INDENT, "$imp.$name(${l}z, i, callSize);\n";
	&generateBenchmarkFooter();
}

sub generateBenchmark1o {
	my ($name, $rtype, $l, $op, $imp) = (@_);
	
	my @args;
	
	my $c = $l =~ /^c/;
	my $v = $l =~ /v$/;
	if ($c && ($rtype ne 'void' && $rtype ne 'int')) {
		print STDERR "Function \"$name\" has wrong combination of ($l) -> $rtype\n";
		return;
	}
	if (!$v && !$c && $rtype eq 'void') {
		print STDERR "Function \"$name\" has wrong combination of ($l) -> $rtype\n";
		return;
	}
	if      ($rtype ne 'void') {
		# Don't add to @args anything
	} elsif ($l eq 'cv' && ($op eq 'max' || $op eq 'min' || $op eq 'sum')) {
		push @args, 'csz';
	} elsif ($v) {
		push @args, ($c?'cv':'rv').'z', 'i';
	} else {
		# Not vector, not real
		push @args, 'csz';
	}

	if      ($l eq 'rs' || $l eq 'cs') {
		push @args, $l.'x';
	} elsif ($l eq 'rv' || $l eq 'cv') {
		push @args, $l.'x', 'i';
	} else {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}
	push @args, 'callSize';

	# Return type for this could be only void, int or float
	if      ($rtype eq 'int' || $rtype eq 'float') {
		&generateBenchmarkHeaderTyped($name, $imp, $rtype);
	} elsif ($rtype eq 'void') {
		&generateBenchmarkHeader($name, $imp);
	} else {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}

	if ($rtype eq 'void') {
		print $CODE_INDENT, "$imp.$name(", join(', ', @args), ");\n";
	} else {
		print $CODE_INDENT, "bh.consume($imp.$name(", join(', ', @args), "));\n";
	}

	&generateBenchmarkFooter();
}

sub generateBenchmarkRVRSLinRVRS {
	my ($name, $rtype, $inplace, $imp) = @_;

	&generateBenchmarkHeader($name, $imp);
	if ($inplace) {
		print $CODE_INDENT, "$imp.$name(rvz, i, rsz, rvx, i, rsx, callSize);\n";
	} else {
		print $CODE_INDENT, "$imp.$name(rvz, i, rvx, i, rsx, rvy, i, rsy, callSize);\n";
	}
	&generateBenchmarkFooter();
}

sub generateBenchmarkHeader {
	my ($name, $imp) = @_;
	print "\n";
	print "    \@Benchmark\n";
	print "    public void ${imp}_${name}() {\n";
	print "        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {\n";
}

sub generateBenchmarkHeaderTyped {
	my ($name, $imp, $rtype) = @_;
	print "\n";
	print "    \@Benchmark\n";
	print "    public void ${imp}_${name}(Blackhole bh) {\n";
	print "        for (int i = startOffset; i < DATA_SIZE + startOffset; i+= callSize) {\n";
}

sub generateBenchmarkFooter {
	print "        }\n";
	print "    }\n";
}

sub calcReturnType {
	my ($name, $l, $r, $rt, $op) = @_;
	if      ($l eq 'rs') {
		return () if $rt ne 'void';
		if      ($r eq 'rs') {
			die "Function \"$name\" has wrong combination of rt \"$rt\" and first argument\n" unless $rt ne 'void';
		} elsif ($r eq 'rv') {
			return ('rvz', 'i');
		} elsif ($r eq 'cs') {
			return ('csz');
		} elsif ($r eq 'cv') {
			return ('cvz', 'i');
		} else {
			die "Function \"$name\" has wrong combination of first and second argument\n";
		}
	} elsif ($l eq 'rv') {
		return () if $rt ne 'void';
		# Special case
		if ($r eq 'cv' && $op eq 'dot') {
			return ('csz');
		}
		
		if ($r eq 'cv' || $r eq 'cs')  {
			return ('cvz', 'i');
		} else {
			return ('rvz', 'i');
		}
	} elsif ($l eq 'cs') {
		if      ($rt eq 'float') {
			return ();
		} elsif ($rt eq 'void') {
			return ('csz');
		} else {
			die "Function \"$name\" has wrong combination of rt \"$rt\" and first argument\n" unless $rt eq 'void';
		}
	} elsif ($l eq 'cv') {
		return ('cvz', 'i');
	} else {
		die "Function \"$name\" has wrong first argument\n";
	}
}

sub loadFile {
	my ($name, $base) = @_;
	open(my $fh, '<', $name) or die "Can npot open \"$name\"\n";

	my $RV = {};
	my $total = 0;
	while (<$fh>) {
		s/^\s+//; s/\s+$//;
		next unless /^public static (\S+) ([a-z0-9_]+)\(.+?\) \{$/;
		my $rt = $1;
		my $name = $2;
		# Check if $name is or _w or _iw or _f or _fw
		$total++;
		if ($name =~ /_[fi]*w$/) {
			print STDERR "Vectorized version implements strange method: \"$name\"\n" unless $base;
			next;
		}
		$RV->{$name} = $rt;
	}
	print STDERR "\"$name\": loaded ", scalar(keys %{$RV}) + 1, " out of $total methods\n";
	close($fh);
	return $RV;
}
