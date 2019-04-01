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

my $HORIZONTAL = {
	'dot' => 1,
	'sum' => 1
};

my $APPROX = {
	'10log10' => 1,
	'20log10' => 1
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

	# Parse this op
	my $op;
	
	eval { $op = &OpAnalyzer::parseOp($name, $VEC->{$name}); };
	if ($@) {
		print STDERR $@;
		next;
	}

	# Call generators
	if      ($op->{'type'} eq 'u' &&  $op->{'ip'}) {
		&generateTest1i($op);
	} elsif ($op->{'type'} eq 'u' && !$op->{'ip'}) {
		&generateTest1o($op);
	} elsif ($op->{'type'} eq 'b' &&  $op->{'ip'}) {
		&generateTest2i($op);
	} elsif ($op->{'type'} eq 'b' && !$op->{'ip'}) {
		&generateTest2o($op);
	} elsif ($op->{'type'} eq 'q' &&  $op->{'ip'}) {
		&generateTest4i($op);
	} elsif ($op->{'type'} eq 'q' && !$op->{'ip'}) {
		&generateTest4o($op);
	} else {
		print STDERR 'Unknown ', ($op->{'ip'} ? 'in-place' : 'out-of-place'), " operation '$name' type '", $op->{'type'}, "'\n";
	}
}

print "}";

exit 0;

sub generateTest1i {
	my $op = shift;

	my $out;
	eval { $out = &OpAnalyzer::getOutType($op); };
	if ($@) {
		print STDERR $@;
		return;
	}

	&generateTestHeader($op->{'name'});
	print $CODE_INDENT, 'float ', $op->{'l'}.'z1[] = Arrays.copyOf(', $op->{'l'}.'z, ', $op->{'l'}."z.length);\n";
	print $CODE_INDENT, 'float ', $op->{'l'}.'z2[] = Arrays.copyOf(', $op->{'l'}.'z, ', $op->{'l'}."z.length);\n";
	print $CODE_INDENT, 'VO.',    $op->{'name'}, '(', $op->{'l'}."z1, offset, size);\n";
	print $CODE_INDENT, 'VOVec.', $op->{'name'}, '(', $op->{'l'}."z2, offset, size);\n";
	print $CODE_INDENT, "assertArrayEquals(", $op->{'l'}.'z1, ', $op->{'l'}.'z2, ', &getEpsilon($op->{'op'}), ");\n";
	&generateTestFooter();
}

sub generateTest1o {
	my $op = shift;
	
	my $out;
	eval { $out = &OpAnalyzer::getOutType($op); };
	if ($@) {
		print STDERR $@;
		return;
	}

	# Make args
	my @args = ();
	if      ($op->{'l'} eq 'rs' || $op->{'l'} eq 'cs') {
		push @args, $op->{'l'}.'x';
	} elsif ($op->{'l'} eq 'rv' || $op->{'l'} eq 'cv') {
		push @args, $op->{'l'}.'x', 'offset';
	} else {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong first argument\n";
	}
	push @args, 'size';

	&generateTestHeader($op->{'name'});

	if      ($out eq 'rs' || $out eq 'int') {
		print $CODE_INDENT, $op->{'rt'}, " ${out}z1 = VO.",    $op->{'name'}, '(', join(', ', @args), ");\n";
		print $CODE_INDENT, $op->{'rt'}, " ${out}z2 = VOVec.", $op->{'name'}, '(', join(', ', @args), ");\n";
		if ($op->{'rt'} eq 'float') {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";
		} else {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2);\n";
		}
	} elsif ($out eq 'cs') {
		print $CODE_INDENT, "float ${out}z1[] = new float[2];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[2];\n";
		print $CODE_INDENT, 'VO.'    ,$op->{'name'}, '(', join(', ', ("${out}z1", @args)), ");\n";
		print $CODE_INDENT, 'VOVec.', $op->{'name'}, '(', join(', ', ("${out}z2", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";
	} elsif ($out eq 'rv' || $out eq 'cv') {
		print $CODE_INDENT, "float ${out}z1[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[${out}z.length];\n";
		print $CODE_INDENT, 'VO.',    $op->{'name'}, '(', join(', ', ("${out}z1", "0", @args)), ");\n";
		print $CODE_INDENT, 'VOVec.', $op->{'name'}, '(', join(', ', ("${out}z2", "0", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";
	} else {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong output type \"$out\"\n";
	}

	&generateTestFooter();

}

sub generateTest2i {
	my $op = shift;

	my $out;
	eval { $out = &OpAnalyzer::getOutType($op); };
	if ($@) {
		print STDERR $@;
		return;
	}

	my @args = ();
	if      ($op->{'r'} eq 'rs' || $op->{'r'} eq 'cs') {
		push @args, $op->{'r'}.'x';
	} elsif ($op->{'r'} eq 'rv' || $op->{'r'} eq 'cv') {
		push @args, $op->{'r'}.'x', 'offset';
	} else {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong second argument\n";
	}
	push @args, 'size';

	&generateTestHeader($op->{'name'});

	print $CODE_INDENT, "float ${out}z1[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
	print $CODE_INDENT, "float ${out}z2[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
	print "\n";

	print $CODE_INDENT, 'VO.',    $op->{'name'}, '(', join(', ', ($out.'z1', 'offset', @args)), ");\n";
	print $CODE_INDENT, 'VOVec.', $op->{'name'}, '(', join(', ', ($out.'z2', 'offset', @args)), ");\n";
	print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";

	&generateTestFooter();
}

sub generateTest2o {
	my $op = shift;

	my $out;
	eval { $out = &OpAnalyzer::getOutType($op); };
	if ($@) {
		print STDERR $@;
		return;
	}

	# Make args
	my @args = ();
	if      ($op->{'l'} eq 'rs' || $op->{'l'} eq 'cs') {
		push @args, $op->{'l'}.'x';
	} elsif ($op->{'l'} eq 'rv' || $op->{'l'} eq 'cv') {
		push @args, $op->{'l'}.'x', 'offset';
	} else {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong first argument\n";
	}

	if      ($op->{'r'} eq 'rs' || $op->{'r'} eq 'cs') {
		push @args, $op->{'r'}.'y';
	} elsif ($op->{'r'} eq 'rv' || $op->{'r'} eq 'cv') {
		push @args, $op->{'r'}.'y', 'offset';
	} else {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong second argument\n";
	}

	push @args, 'size';

	# Return type for this could be only float or void
	&generateTestHeader($op->{'name'});

	if      ($out eq 'rs' || $out eq 'int') {
		print $CODE_INDENT, $op->{'rt'}, " ${out}z1 = VO.",    $op->{'name'}, '(', join(', ', @args), ");\n";
		print $CODE_INDENT, $op->{'rt'}, " ${out}z2 = VOVec.", $op->{'name'}, '(', join(', ', @args), ");\n";
		if ($op->{'rt'} eq 'float') {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";
		} else {
			print $CODE_INDENT, "assertEquals(${out}z1, ${out}z2);\n";
		}
	} elsif ($out eq 'cs') {
		print $CODE_INDENT, "float ${out}z1[] = new float[2];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[2];\n";
		print $CODE_INDENT, 'VO.',    $op->{'name'}, '(', join(', ', ("${out}z1", @args)), ");\n";
		print $CODE_INDENT, 'VOVec.', $op->{'name'}, '(', join(', ', ("${out}z2", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";

		# And second one!
		&generateTestFooter();
		&generateTestHeader($op->{'name'}.'_zoffset');
		print $CODE_INDENT, "float ${out}z1[] = new float[6];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[6];\n";
		print $CODE_INDENT, 'VO.',    $op->{'name'}, '(', join(', ', ("${out}z1", "1", @args)), ");\n";
		print $CODE_INDENT, 'VOVec.', $op->{'name'}. '(', join(', ', ("${out}z2", "1", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";
	} elsif ($out eq 'rv' || $out eq 'cv') {
		print $CODE_INDENT, "float ${out}z1[] = new float[${out}z.length];\n";
		print $CODE_INDENT, "float ${out}z2[] = new float[${out}z.length];\n";
		print $CODE_INDENT, 'VO.',    $op->{'name'}, '(', join(', ', ("${out}z1", "0", @args)), ");\n";
		print $CODE_INDENT, 'VOVec.', $op->{'name'}, '(', join(', ', ("${out}z2", "0", @args)), ");\n";
		print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon($op->{'op'}), ");\n";
	} else {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong output type \"$out\"\n";
	}

	&generateTestFooter();
}

sub generateTest4i {
	my $op = shift;

	if ($op->{'op'} ne 'lin' || $op->{'l1'} ne 'rv' || $op->{'l2'} ne 'rs' || $op->{'r1'} ne 'rv' || $op->{'r2'} ne 'rs') {
		print STDERR "Can not generate test for \"", $op->{'name'}, "\" yet\n";
		return;
	}

	my $out;
	eval { $out = &OpAnalyzer::getOutType($op); };
	if ($@) {
		print STDERR $@;
		return;
	}
	if ($out ne 'rv') {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong output type \"$out\"\n";
	}

	&generateTestHeader($op->{'name'});
	print $CODE_INDENT, "float ${out}z1[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
	print $CODE_INDENT, "float ${out}z2[] = Arrays.copyOf(${out}z, ${out}z.length);\n";
	print $CODE_INDENT, 'VO.',    $op->{'name'}, "(${out}z1, offset, rsz, rvx, offset, rsx, size);\n";
	print $CODE_INDENT, 'VOVec.', $op->{'name'}, "(${out}z2, offset, rsz, rvx, offset, rsx, size);\n";
	print $CODE_INDENT, "assertArrayEquals(${out}z2, ${out}z2, ", &getEpsilon('lin'), ");\n";
	&generateTestFooter();
}

sub generateTest4o {
	my $op = shift;

	if ($op->{'op'} ne 'lin' || $op->{'l1'} ne 'rv' || $op->{'l2'} ne 'rs' || $op->{'r1'} ne 'rv' || $op->{'r2'} ne 'rs') {
		print STDERR "Can not generate test for \"", $op->{'name'}, "\" yet\n";
		return;
	}

	my $out;
	eval { $out = &OpAnalyzer::getOutType($op); };
	if ($@) {
		print STDERR $@;
		return;
	}
	if ($out ne 'rv') {
		die "Internal consistency error: Function \"".$op->{'name'}."\" has wrong output type \"$out\"\n";
	}

	&generateTestHeader($op->{'name'});
	print $CODE_INDENT, "float ${out}z1[] = new float[${out}z.length];\n";
	print $CODE_INDENT, "float ${out}z2[] = new float[${out}z.length];\n";
	print $CODE_INDENT, 'VO.',    $op->{'name'}, "(${out}z1, 0, rvx, offset, rsx, rvy, offset, rsy, size);\n";
	print $CODE_INDENT, 'VOVec.', $op->{'name'}, "(${out}z2, 0, rvx, offset, rsx, rvy, offset, rsy, size);\n";
	print $CODE_INDENT, "assertArrayEquals(${out}z1, ${out}z2, ", &getEpsilon('lin'), ");\n";
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
