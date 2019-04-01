#! /usr/bin/perl
package OpAnalyzer;

use strict;
use warnings;

sub cannotVectorize {
	my $name = shift;
	return $name =~ /_i?[fw]+$/;
}

sub parseOp {
	my ($name, $rtype) = @_;

	my $res;
	if      ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{2,})_(rs|rv|cs|cv)(_i)?$/) {
		$res = {
			'type' => 'b',
			'l'    => $1,
			'op'   => $2,
			'r'    => $3,
			'ip'   => defined($4) && $4 eq '_i'
		};
	} elsif ($name =~ /^(rs|rv|cs|cv)_([a-z0-9]{2,})(_i)?$/) {
		$res = {
			'type' => 'u',
			'l'    => $1,
			'op'   => $2,
			'ip'   => defined($3) && $3 eq '_i'
		};
	} elsif ($name =~ /^(rs|rv|cs|cv)_(rs|rv|cs|cv)_([a-z0-9]{2,})_(rs|rv|cs|cv)_(rs|rv|cs|cv)(_i)?$/) {
		$res = {
			'type' => 'q',
			'l1'   => $1,
			'l2'   => $2,
			'op'   => $3,
			'r1'   => $4,
			'r2'   => $5,
			'ip'   => defined($6) && $6 eq '_i'
		};
	} else {
		die "Unknown oeration name \'$name\'\n";
	}
	$res->{'name'} = $name;
	$res->{'rt'} = $rtype;
	return $res;
}

sub getOutType {
	my $op = shift;
	my $ot;
	if      ($op->{'type'} eq 'u' &&  $op->{'ip'}) {
		die "Invalid in-place operation '".$op->{'name'}."': need first vector arg, get '".$op->{'l'}."'\n" unless &_isVector($op->{'l'});
		$ot = $op->{'l'};
	} elsif ($op->{'type'} eq 'u' && !$op->{'ip'}) {
		$ot = &_getOutType1o($op);
	} elsif ($op->{'type'} eq 'b' &&  $op->{'ip'}) {
		die "Invalid in-place operation '".$op->{'name'}."': need first vector arg, get '".$op->{'l'}."'\n" unless &_isVector($op->{'l'});
		die "Invalid in-place operation '".$op->{'name'}."': need first complex vector arg, get '".$op->{'l'}."'\n" unless &_isComplex($op->{'l'}) == (&_isComplex($op->{'l'}) || &_isComplex($op->{'r'}));
		$ot =  $op->{'l'};
	} elsif ($op->{'type'} eq 'b' && !$op->{'ip'}) {
		$ot = &_getOutType2o($op);
	} elsif ($op->{'type'} eq 'q' &&  $op->{'ip'}) {
		$ot = &_getOutType4i($op);
	} elsif ($op->{'type'} eq 'q' && !$op->{'ip'}) {
		$ot = &_getOutType4o($op);
	} else {
		die "Internal error: Invalid operation descriptor for '".$op->{'name'}."'\n";
	}

	die "Invalid method return type of operation '".$op->{'name'}."': '".$op->{'rt'}."', need 'void'\n" if (&_isVector($ot) || &_isComplex($ot)) && $op->{'rt'} ne 'void';
	die "Invalid method return type of operation '".$op->{'name'}."': '".$op->{'rt'}."', need 'float'\n" if $ot eq 'rs' && $op->{'rt'} ne 'float';
	die "Invalid method return type of operation '".$op->{'name'}."': '".$op->{'rt'}."', need 'int'\n" if $ot eq 'int' && $op->{'rt'} ne 'int';

	return $ot;
}

sub _getOutType1o {
	my $op = shift;

	die "Invalid operation '".$op->{'name'}."': need first vector arg, get '".$op->{'l'}."'\n" unless &_isVector($op->{'l'});

	# Special cases:
	if      ($op->{'op'} eq 'max' || $op->{'op'} eq 'min' || $op->{'op'} eq 'sum') {
		return (&_isComplex($op->{'l'}) ? 'c' : 'r').'s';
	}  elsif ($op->{'op'} eq 'maxarg' || $op->{'op'} eq 'minarg') {
		return 'int';
	} elsif ($op->{'l'} eq 'cv' && ($op->{'op'} eq 'im' || $op->{'op'} eq 're')) {
		return 'rv';
	} elsif ($op->{'l'} eq 'rv' && ($op->{'op'} eq 'cvt' || $op->{'op'} eq 'expi')) {
		return 'cv';
	}
	return $op->{'l'};
}

sub _getOutType2o {
	my $op = shift;

	# Special case: 'dot' returns non-vector result
	if ($op->{'op'} eq 'dot') {
		die "Invalid type of left argument for operation '".$op->{'name'}."': '".$op->{'l'}."', need some vector\n" unless &_isVector($op->{'l'});
		die "Invalid type of right argument for operation '".$op->{'name'}."': '".$op->{'r'}."', need some vector\n" unless &_isVector($op->{'r'});

		return (&_isComplex($op->{'l'}) || &_isComplex($op->{'r'})) ? 'cs' : 'rs';
	}

	# Simple: result is vector of maximum type
	return ((&_isComplex($op->{'l'}) || &_isComplex($op->{'r'})) ? 'c' : 'r').'v';
}

sub _getOutType4i {
	my $op = shift;

	die "Invalid in-place operation '".$op->{'name'}."': only 'lin' is known as 4-arg\n" unless $op->{'op'} eq 'lin';
	die "Invalid in-place operation '".$op->{'name'}."': need first vector arg, get '".$op->{'l1'}."'\n" unless &_isVector($op->{'l1'});
	die "Invalid in-place operation '".$op->{'name'}."': need second scalar arg, get '".$op->{'l2'}."'\n" unless !&_isVector($op->{'l2'});
	die "Invalid in-place operation '".$op->{'name'}."': need third vector arg, get '".$op->{'r1'}."'\n" unless &_isVector($op->{'r1'});
	die "Invalid in-place operation '".$op->{'name'}."': need fourth scalar arg, get '".$op->{'r2'}."'\n" unless !&_isVector($op->{'r2'});

	my $cl1  = &_isComplex($op->{'l1'});
	my $cAny = $cl1 || &_isComplex($op->{'l2'}) || &_isComplex($op->{'r1'}) || &_isComplex($op->{'r2'});

	die "Invalid in-place operation '".$op->{'name'}."': need first ".($cAny ? 'complex' : 'real')." vector arg, get '".$op->{'l1'}."'\n" unless $cl1 == $cAny;

	# Ok, same as l1
	return $op->{'l1'};
}

sub _getOutType4o {
	my $op = shift;

	die "Invalid operation '".$op->{'name'}."': only 'lin' is known as 4-arg\n" unless $op->{'op'} eq 'lin';
	die "Invalid operation '".$op->{'name'}."': need first vector arg, get '".$op->{'l1'}."'\n" unless &_isVector($op->{'l1'});
	die "Invalid operation '".$op->{'name'}."': need second scalar arg, get '".$op->{'l2'}."'\n" unless !&_isVector($op->{'l2'});
	die "Invalid operation '".$op->{'name'}."': need third vector arg, get '".$op->{'r1'}."'\n" unless &_isVector($op->{'r1'});
	die "Invalid operation '".$op->{'name'}."': need fourth scalar arg, get '".$op->{'r2'}."'\n" unless !&_isVector($op->{'r2'});

	my $cAny = &_isComplex($op->{'l1'}) || &_isComplex($op->{'l2'}) || &_isComplex($op->{'r1'}) || &_isComplex($op->{'r2'});

	# Ok, some vector
	return ($cAny ? 'c' : 'r').'v';
}

sub _isVector {
	my $t = shift;
	return 1 if $t eq 'rv' || $t eq 'cv';
	return 0 if $t eq 'rs' || $t eq 'cs' || $t eq 'int';
	die "Internal error: Invalid type '$t'\n";
}

sub _isComplex {
	my $t = shift;
	return 1 if $t eq 'cs' || $t eq 'cv';
	return 0 if $t eq 'rs' || $t eq 'rv' || $t eq 'int';
	die "Internal error: Invalid type '$t'\n";
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

sub loadFile {
	my ($name, $base) = @_;
	open(my $fh, '<', $name) or die "Can npot open \"$name\"\n";

	my $rv = {};
	my $total = 0;
	my $loaded = 0;
	while (<$fh>) {
		s/^\s+//; s/\s+$//;
		next unless /^public static (\S+) ([a-z0-9_]+)\(.+?\) \{$/;
		my $rt = $1;
		my $name = $2;
		# Check if $name is or _w or _iw or _f or _fw
		$total++;
		if (&cannotVectorize($name)) {
			print STDERR "Vectorized version implements strange method: \"$name\"\n" unless $base;
			next;
		}
		$rv->{$name} = $rt;
		$loaded++;
	}
	print STDERR "\"$name\": loaded $loaded out of $total methods\n";
	close($fh);
	return $rv;
}

1;
