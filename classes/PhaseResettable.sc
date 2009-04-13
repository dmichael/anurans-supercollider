PhaseResettable{
	var <>clock;
	
	*new { arg options = Dictionary.new;
		^super.new.init(options) 
	}

	init { arg options = Dictionary.new;
		// creation of the internal clock
		clock = TempoClock(1);
	}
	
	trigger { |beat, sec|
		[beat, sec].postln;
	}
	
	play {
		var start_time;
		// start time set to next whole beat, just for debugging
		start_time = clock.beats.ceil;
		
		clock.schedAbs(start_time, {|beat, sec|
			this.trigger(beat, sec);
			1;
		});
	}
	
	stop {
		clock.clear
	}
}