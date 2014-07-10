package com.mattstults.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mattstults.track.Svgable.PathPart;

public class Track extends SvgContainer.Group{
	
	public static class TrackMetadata extends Metadata {
		private final String name;
		private final int nCycles;
		public TrackMetadata(String name, int cycles) {
			super("track", null, null, null, null, null);
			this.name = name;
			nCycles = cycles;
		}
		
		@Override
		public String toString() {
			return new StringBuilder("id=\"").append(name).append("\" ")
				.append(super.toString()).append(" ").append("cycles=\"")
				.append(nCycles).append("\"").toString();
		}
	}
	
	private Track(List<Svgable> contents, TrackMetadata metadata) {
		super(contents, metadata);
	}
	
	public static class Builder {
		private final int nCycles;
		private final String name;
		private final List<Svgable> contents;
		public Builder(String name, int nCycles) {
			this.name = name;
			this.nCycles = nCycles;
			this.contents = new ArrayList<Svgable>();
		}
		public Builder append(TrackLayer content) {
			contents.add(content);
			return this;
		}
		
		public Track build() {
			return new Track(contents, new TrackMetadata(name, nCycles));
		}
	}
}
