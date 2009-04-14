/*
	T' = sd + Te
	
	T' = s[(d + l/v) - (r - t)] + (T + e) + (y - x)
		
	where:
	T  = free running call period (seconds)
	T' = modified call period after receipt of stimulus (seconds) 
	d  = time elapsed since onset of last call (seconds)
	l  = distance of the stimulus (meters)
	v  = the speed of sound (344 m per s)
	r  = length of the decending slope
	t  = effector delay
	s  = phase response curve
	e  = stochastic element
	y  = length of the stimulus call
	x  = length of the call
	
*/

PhaseResettable {	
	var <>clock;
	var <>period, <>d, <>s, <>t;
	var <>period_buffer;
	var <>refractory;
	
	*new { arg options = IdentityDictionary.new;
		^super.new.init(options) 
	}

	init { arg options = IdentityDictionary.new;
		s 		= options.at('prc');
		period 	= options.at('period');
		t = 0.1;
		// creation of the internal clock
		clock 	= TempoClock(period);
	}
	
	trigger { |beat, seconds|
		[clock.beatDur, beat, seconds].postln;
		if( period_buffer.isNil, {
			this.set_tempo_in_seconds(period);
		},{
			"period buffer".postln;
			this.set_tempo_in_seconds(period_buffer);
			period_buffer = nil;
		});
		
	}
	
	stimulate {
		var new_period, period_elapsed;
		
		// Calculate the time elapsed since the onset of the last call.
		period_elapsed = this.period_percent_elapsed * this.current_period;
		d = period_elapsed;
		
		if( d > (this.current_period - t), {
			new_period = s * (d - this.current_period) + period;
			// Save it for next beat
			period_buffer = new_period;
		},{
			new_period = (s * d) + period;
			// Set the tempo immediately
			this.set_tempo_in_seconds(new_period);
		});
	}
	
	play {
		var start_time;
		// start time set to next whole beat, just for debugging
		start_time = clock.beats.ceil;
		
		clock.schedAbs(start_time, {|beat, sec|
			this.trigger(beat, sec);
			clock.beatsPerBar_(1);
			1;
		});
	}
	
	stop {
		clock.clear
	}
	
	// Convert seconds/beat to beats/second and
	// set the new tempo (beats/second)
		
	set_tempo_in_seconds { arg seconds;
		var new_tempo;
		new_tempo = seconds.reciprocal;
		clock.tempo_(new_tempo);
	}
	
	current_period {
		^clock.beatDur
	}
	
	period_percent_elapsed {
		^clock.beatInBar
	}
	
} 
