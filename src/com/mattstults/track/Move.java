package com.mattstults.track;

import com.mattstults.track.Svgable.PathPart;

public class Move implements PathPart {
	private final Context context;
	private final Point position;
	
	public Move(Context context, Point position) {
		this.context = context;
		this.position = position;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(context == Context.ABSOLUTE
				? "M"
				: "m");
		return ret.append(" ").append(position.toString()).toString();
	}
	
	
}
