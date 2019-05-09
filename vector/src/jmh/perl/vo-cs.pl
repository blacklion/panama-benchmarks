#! /usr/bin/perl -w
use strict;
use warnings;

my %B = ();
my %CS = ();

<>; # SKIP

while (<>) {
	s/^\s+//; s/\s+$//;
	my ($name, $_mode, $_thr, $_samp, $score, $_err, $_unit, $cs, $_so) = split /,/;
	$name =~ s/^"(.+)"$/$1/;
	if ($name !~ /^vector\.VectorBenchmarks[a-zA-Z]+\.(VOVec|VO)_([a-z0-9_]+)$/) {
		print STDERR "Unknown name: \"$name\"\n";
		next;
	}
	my ($type, $op) = ($1, $2);
	$B{$op}->{$cs}->{$type} = $score;
	$CS{$cs} = 1;
}


my @CS_ALL = sort { $a <=> $b } keys %CS;

# Make headers
print "Operation,", join(',', map { ($_, '', '') } @CS_ALL), "\n";
print ",", join(',', map { ('VO', 'VOVec', 'Diff') } @CS_ALL), "\n";

for my $op (sort keys %B) {
	my @line = ($op);
	for my $cs (@CS_ALL) {
		if (exists $B{$op}->{$cs}) {
			if (exists $B{$op}->{$cs}->{'VO'}) {
				push @line, $B{$op}->{$cs}->{'VO'};
			} else {
				print STDERR "Operation \"$op\" doesn't have VO result for callSize $cs\n";
				push @line, '';
			}
			if (exists $B{$op}->{$cs}->{'VOVec'}) {
				push @line, $B{$op}->{$cs}->{'VOVec'};
			} else {
				print STDERR "Operation \"$op\" doesn't have VOVec result for callSize $cs\n";
				push @line, '';
			}
			push @line, '';
		} else {
			print STDERR "Operation \"$op\" doesn't have callSize $cs\n";
			push @line, ('', '', '');
		}
	}
	print join(',', @line), "\n";
}
