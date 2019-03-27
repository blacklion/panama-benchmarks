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

my $HORIZONTAL = {
	'dot' => 1,
	'sum' => 1
};

my $APPROX = {
	'10log10' => 1,
	'20log10' => 1
};

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

my $CODE_INDENT = "        ";

# Generate benchmark
print<<__HEADER;
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\\
!! THIS FILE IS GENERATED WITH genTests.pl SCRIPT. DO NOT EDIT! !!
\\!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * \@author Lev Serebryakov
 * \@noinspection CStyleArrayDeclaration, WeakerAccess
 */
public class VectorTests {
    private static final float EPSILON = 0.000001f;
    private static final float EPSILON_APPROX = 0.0001f;

    private static final int DATA_SIZE = 65536;
    private static final int MAX_OFFSET = 1;

    private final static FloatVector.FloatSpecies PFS = FloatVector.preferredSpecies();

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
    
    \@BeforeAll
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

__HEADER


for my $name (sort keys %{$VEC}) {
	next unless exists $BASE->{$name};

	# Parse name
	my $inplace = $name =~ /_i$/;
	if      ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{2,})_(rs|rv|cs|cv)(_i)?$/) {
		my $l  = $1;
		my $op = $2;
		my $r  = $3;

		if ($inplace) {
			&generateTest2i($name, $VEC->{$name}, $l, $op, $r);
		} else {
			&generateTest2o($name, $VEC->{$name}, $l, $op, $r);
		}
	} elsif ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{2,})(_i)?$/) {
		my $l  = $1;
		my $op = $2;

		if ($inplace) {
			&generateTest1i($name, $VEC->{$name}, $l, $op);
		} else {
			&generateTest1o($name, $VEC->{$name}, $l, $op);
		}
	} elsif ($name =~ /^rv_rs_lin_rv_rs(_i)?$/) {
		&generateTestRVRSLinRVRS($name, $VEC->{$name}, $inplace);
	} else {
		print STDERR "Unknown name: \"$name\"\n";
	}
}

print "}";

exit 0;

sub generateTest2i {
	my ($name, $rtype, $l, $op, $r) = (@_);
	
	# Return type for this could be only void
	if ($rtype ne 'void') {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}
	
	# Make args
	my @args  = ();
	my $out;

	if      ($l eq 'rv' || $l eq 'cv') {
		$out = $l;
	} else {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}

	if      ($r eq 'rs' || $r eq 'cs') {
		push @args, $r.'x';
	} elsif ($r eq 'rv' || $r eq 'cv') {
		push @args, $r.'x', 'offset';
	} else {
		print STDERR "Function \"$name\" has wrong second argument\n";
		return;
	}
	push @args, 'size';

	&generateTestHeader($name);
	# Generatr data preparation
		# Prepare destructive data
	print $CODE_INDENT, "float ${out}z1[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
	print $CODE_INDENT, "float ${out}z2[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
	print "\n";
	print $CODE_INDENT, "VO.$name(",    join(', ', ($out.'z1', 'offset', @args)), ");\n";
	print $CODE_INDENT, "VOVec.$name(", join(', ', ($out.'z2', 'offset', @args)), ");\n";
	print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
	&generateTestFooter();
}

sub generateTest2o {
	my ($name, $rtype, $l, $op, $r) = (@_);

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

	if      ($l eq 'rs' || $l eq 'cs') {
		push @args, $l.'x';
	} elsif ($l eq 'rv' || $l eq 'cv') {
		push @args, $l.'x', 'offset';
	} else {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}
	if      ($r eq 'rs' || $r eq 'cs') {
		push @args, $r.'y';
	} elsif ($r eq 'rv' || $r eq 'cv') {
		push @args, $r.'y', 'offset';
	} else {
		print STDERR "Function \"$name\" has wrong second argument\n";
		return;
	}
	push @args, 'size';

	# Return type for this could be only float or void
	if      ($rtype eq 'float' || $rtype eq 'void') {
		&generateTestHeader($name);
	} else {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}

	if      ($out eq 'rs') {
		print $CODE_INDENT, "$rtype ${out}z1 = VO.$name(", join(', ', @args), ");\n";
		print $CODE_INDENT, "$rtype ${out}z2 = VOVec.$name(", join(', ', @args), ");\n";
		if ($rtype eq 'float') {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
		} else {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2);\n";
		}
	} elsif ($out eq 'cs') {
		print $CODE_INDENT, "float ${out}z1[] = new float[2];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[2];\n";
		print $CODE_INDENT, "VO.$name(",    join(', ', ("${out}z1", @args)), ");\n";
		print $CODE_INDENT, "VOVec.$name(", join(', ', ("${out}z2", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";

		# And second one!
		&generateTestFooter();
		&generateTestHeader($name.'_zoffset');
		print $CODE_INDENT, "float ${out}z1[] = new float[6];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[6];\n";
		print $CODE_INDENT, "VO.$name(",    join(', ', ("${out}z1", "1", @args)), ");\n";
		print $CODE_INDENT, "VOVec.$name(", join(', ', ("${out}z2", "1", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
	} elsif ($out eq 'rv' || $out eq 'cv') {
		print $CODE_INDENT, "float ${out}z1[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "VO.$name(",    join(', ', ("${out}z1", "0", @args)), ");\n";
		print $CODE_INDENT, "VOVec.$name(", join(', ', ("${out}z2", "0", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
	} else {
		die "Internal error\n";
	}

	&generateTestFooter();
}

sub generateTest1i {
	my ($name, $rtype, $l, $op) = (@_);

	# Return type for this could be only void
	if ($rtype ne 'void') {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}
	if ($l ne 'rv' && $l ne 'cv') {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}
		
	&generateTestHeader($name);
	print $CODE_INDENT, "float ${l}z1[] = Arrays.copyOf(${l}z, ${l}z.length);\n";
	print $CODE_INDENT, "float ${l}z2[] = Arrays.copyOf(${l}z, ${l}z.length);\n";
	print $CODE_INDENT, "VO.$name(${l}z1, offset, size);\n";
	print $CODE_INDENT, "VOVec.$name(${l}z2, offset, size);\n";
	print $CODE_INDENT, "assertArrayEquals(${l}z1, ${l}z2, ", &getEpsilon($op), ");\n";
	&generateTestFooter();
}

sub generateTest1o {
	my ($name, $rtype, $l, $op) = (@_);
	
	
	# Make args
	my @args = ();
	my $out;

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
		$out = 'rs';
	} elsif ($l eq 'cv' && ($op eq 'max' || $op eq 'min' || $op eq 'sum')) {
		$out = 'cs';
	} elsif ($l eq 'cv' && ($op eq 'im' || $op eq 're')) {
		$out = 'rv';
	} elsif ($l eq 'rv' && ($op eq 'cvt' || $op eq 'expi')) {
		$out = 'cv';
	} else {
		$out = ($c ? 'c' : 'r') . ($v ? 'v' : 's');
	}

	if      ($l eq 'rs' || $l eq 'cs') {
		push @args, $l.'x';
	} elsif ($l eq 'rv' || $l eq 'cv') {
		push @args, $l.'x', 'offset';
	} else {
		print STDERR "Function \"$name\" has wrong first argument\n";
		return;
	}
	push @args, 'size';

	# Return type for this could be only float or void
	if      ($rtype eq 'float' || $rtype eq 'int' || $rtype eq 'void') {
		&generateTestHeader($name);
	} else {
		print STDERR "Bad return type \"$rtype\" for \"$name\"\n";
		return;
	}

	if      ($out eq 'rs') {
		print $CODE_INDENT, "$rtype ${out}z1 = VO.$name(", join(', ', @args), ");\n";
		print $CODE_INDENT, "$rtype ${out}z2 = VOVec.$name(", join(', ', @args), ");\n";
		if ($rtype eq 'float') {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
		} else {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2);\n";
		}
	} elsif ($out eq 'cs') {
		print $CODE_INDENT, "float ${out}z1[] = new float[2];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[2];\n";
		print $CODE_INDENT, "VO.$name(",    join(', ', ("${out}z1", @args)), ");\n";
		print $CODE_INDENT, "VOVec.$name(", join(', ', ("${out}z2", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
	} elsif ($out eq 'rv' || $out eq 'cv') {
		print $CODE_INDENT, "float ${out}z1[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "VO.$name(",    join(', ', ("${out}z1", "0", @args)), ");\n";
		print $CODE_INDENT, "VOVec.$name(", join(', ', ("${out}z2", "0", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op), ");\n";
	} else {
		die "Internal error\n";
	}

	&generateTestFooter();

}

sub generateTestRVRSLinRVRS {
	my ($name, $rtype, $inplace) = @_;

	&generateTestHeader($name);
	my $out = 'rv';
	if ($inplace) {
		print $CODE_INDENT, "float ${out}z1[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
		print $CODE_INDENT, "float ${out}z2[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
		print $CODE_INDENT, "VO.$name(${out}z1, offset, rsz, rvx, offset, rsx, size);\n";
		print $CODE_INDENT, "VOVec.$name(${out}z2, offset, rsz, rvx, offset, rsx, size);\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z2, ${out}z2, ", &getEpsilon('lin'), ");\n";
	} else {
		print $CODE_INDENT, "float ${out}z1[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "VO.$name(${out}z1, 0, rvx, offset, rsx, rvy, offset, rsy, size);\n";
		print $CODE_INDENT, "VOVec.$name(${out}z2, 0, rvx, offset, rsx, rvy, offset, rsy, size);\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon('lin'), ");\n";
	}
	&generateTestFooter();
}

sub generateTestHeader {
	my ($name) = @_;
	print "\n";
	print "    \@ParameterizedTest(name = \"${name}({0}, {1})\")\n";
	print "    \@MethodSource(\"params\")\n";
	print "    public void Test_${name}(int size, int offset) {\n";
}

sub generateTestFooter {
	print "    }\n";
}

sub getEpsilon {
	my $op = shift;
	if      (exists $HORIZONTAL->{$op}) {
		return 'EPSILON * size';
	} elsif (exists $APPROX->{$op}) {
		return 'EPSILON_APPROX';
	} else {
		return 'EPSILON';
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
		if ($name =~ /_[fi]*w$/ || $name =~ /_i?f$/) {
			print STDERR "Vectorized version implements strange method: \"$name\"\n" unless $base;
			next;
		}
		$RV->{$name} = $rt;
	}
	print STDERR "\"$name\": loaded ", scalar(keys %{$RV}) + 1, " out of $total methods\n";
	close($fh);
	return $RV;
}
