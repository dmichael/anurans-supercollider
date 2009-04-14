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
	var <>period_buffer, <>refractory;
	var <>voice, <>voice_buffer;
	var <>server;
	
	//----------
	// init
	//----------	
	
	*new { arg options = IdentityDictionary.new;
		^super.new.init(options) 
	}

	init { arg options = IdentityDictionary.new;
		t = 0.1;
		s = options.at('prc');
		period = options.at('period');
		clock  = TempoClock(period);
		server = Server.local; // eventually fed in.
		
		// create the voice and send it to the server
		SynthDef("voice-buffer", { arg out = 0, bufnum;
			Out.ar(out, 
				PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum))
			)
		}).send(server);
	}
	
	//----------
	// vocalize
	//----------	
		
	vocalize {
		var x, y, b;
		y = Synth.basicNew("voice-buffer");
		b = Buffer.readNoUpdate(server,"sounds/peeper.wav", 
			completionMessage: { arg buffer;
				// synth add its s_new msg to follow 
				// after the buffer read completes
				y.newMsg(server,[\bufnum, buffer],\addToTail)
			});
		
		//voice_buffer = Buffer.read(server, 'sounds/peeper.wav');
		//server.sendMsg("/s_new", "voice-buffer", x = server.nextNodeID, 1, 1, "freq", 800);
	}
	
	//----------
	// trigger
	//----------
	
	trigger { |beat, seconds|
		this.vocalize;
		[clock.beatDur, beat, seconds].postln;
		if( period_buffer.isNil, {
			this.set_tempo_in_seconds(period);
		},{
			"period buffer".postln;
			this.set_tempo_in_seconds(period_buffer);
			period_buffer = nil;
		});
		
	}
	
	//----------
	// stimulate
	//----------
	
	stimulate {
		var new_period, period_elapsed;
		
		// Calculate the time elapsed since the onset of the last call.
		period_elapsed = this.period_percent_elapsed * this.current_period;
		d = period_elapsed;
		
		if( d > (this.current_period - t), {
			new_period = (s * (d - this.current_period)) + period;
			// Save it for next beat
			period_buffer = new_period;
		},{
			new_period = (s * d) + period;
			// Set the tempo immediately
			this.set_tempo_in_seconds(new_period);
		});
	}
	
	//----------
	// play
	//----------	
	
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
	
	//----------
	// stop
	//----------	
		
	stop {
		clock.clear
	}
	
	//----------
	// utility
	//----------	
	
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
