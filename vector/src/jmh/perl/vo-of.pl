#! /usr/bin/perl -w
use strict;
use warnings;

my %B = ();
my %SO = ();

<>; # SKIP

while (<>) {
	s/^\s+//; s/\s+$//;
	my ($name, $_mode, $_thr, $_samp, $score, $_err, $_unit, $_cs, $so) = split /,/;
	$name =~ s/^"(.+)"$/$1/;
	if ($name !~ /^vector\.VectorBenchmarks[a-zA-Z]+\.(VOVec|VO)_([a-z0-9_]+)$/) {
		print STDERR "Unknown name: \"$name\"\n";
		next;
	}
	my ($type, $op) = ($1, $2);
	if (exists $B{$op}->{$cs}->{$type}) {
		print STDERR "Dup: ${type}_$op $cs\n";
	}
	$B{$op}->{$type}->{$so} = $score;
	$SO{$so} = 1;
}


my @SO_ALL = sort { $a <=> $b } keys %SO;

# Make headers
print "Operation,", join(',', 'VO', ('') x $#SO_ALL, 'VOVec', ('') x $#SO_ALL), "\n";
print ",", join(',', @SO_ALL, @SO_ALL), "\n";

for my $op (sort keys %B) {
	my @line = ($op);
	push @line, &getResults($op, 'VO');
	push @line, &getResults($op, 'VOVec');
	print join(',', @line), "\n";
}

sub getResults {
	my ($op, $type) = @_;
	my @line = ();
	if (!exists $B{$op}->{$type}) {
		print STDERR "Operation \"$op\" doesn't have $type results at all\n";
		return ('') x ($#SO_ALL + 1);
	}
	for my $so (@SO_ALL) {
		if (exists $B{$op}->{$type}->{$so}) {
			push @line, $B{$op}->{$type}->{$so};
		} else {
			print STDERR "Operation \"$op\" doesn't have $type result of offset $so\n";
			push @line, '';
		}
	}
	return @line;
}
