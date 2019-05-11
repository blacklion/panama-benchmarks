#! /usr/bin/perl -w
use warnings;
use strict;

my @HEADER_FIXED = ('"Benchmark"', '"Mode"', '"Threads"', '"Samples"', '"Score"', '"Score Error (99.9%)"', '"Unit"');

my %PATCH = ();

# Read header of patch
$_ = <>; s/^\s+//; s/\s+$//;
my @HEADER = split /,/;

if (!&checkHeader(@HEADER)) {
	print STDERR "First line must be benchmark header\n";
	exit -1;
}

my @PARAMS = @HEADER[$#HEADER_FIXED + 1 .. $#HEADER];

# Read patch
while (<>) {
	my $verbatim = $_;
	s/^\s+//; s/\s+$//;
	my @rec = split /,/;
	if (@rec != @HEADER) {
		print STDERR "Patch line '$_' has wrong number of elements\n";
		exit -1;
	}
	my @p = @rec[$#HEADER_FIXED + 1 .. $#rec];
	if (&checkHeader(@rec)) {
		if (!&checkParams(@p)) {
			print STDERR "Line '$_' looks like header, but params are wrong\n";
			exit -1;
		}
		print $verbatim;
		last;
	}
	# Ok, make key
	my $key = join('|', ($rec[0], @p));
	if (exists $PATCH{$key}) {
		print STDERR "Patch line '$_' have same key '$key' as '", $PATCH{$key}->{'c'}, "'\n";
		exit -1;
	}
	$PATCH{$key} = { 'c' => $_, 'v' => $verbatim };
}

my %SEEN = ();

# Ok, we need to have main file here
while (<>) {
	my $verbatim = $_;
	s/^\s+//; s/\s+$//;
	my @rec = split /,/;
	if (@rec != @HEADER) {
		print STDERR "Main line '$_' has wrong number of elements\n";
		exit -1;
	}
	my @p = @rec[$#HEADER_FIXED + 1 .. $#rec];
	if (&checkHeader(@rec)) {
		print STDERR "Main line '$_' looks like header, but we had two headers already\n";
		exit -1;
	}
	my $key = join('|', ($rec[0], @p));
	if (exists $SEEN{$key}) {
		print STDERR "Main line '$_' have same key '$key' as '", $SEEN{$key}, "'\n";
		exit -1;
	}
	$SEEN{$key} = $_;
	if (exists $PATCH{$key}) {
		print $PATCH{$key}->{'v'};
		delete $PATCH{$key};
	} else {
		print $verbatim;
	}
}

if (keys %PATCH) {
	print STDERR "Patch lines are unused:\n", join("\n", map { "  ".$_->{'c'} } values %PATCH);
	exit -1;
}

exit 0;

sub checkHeader {
	return 0 if @_ < @HEADER_FIXED;
	for (my $i = 0; $i < @HEADER_FIXED; $i++) {
		return 0 unless $_[$i] eq $HEADER_FIXED[$i];
	}
	return 1;
}

sub checkParams {
	return 0 if @_ != @PARAMS;
	for (my $i = 0; $i < @PARAMS; $i++) {
		return 0 unless $_[$i] eq $PARAMS[$i];
	}
	return 1;
}