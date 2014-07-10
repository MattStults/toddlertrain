package com.mattstults.track;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TrackLayer extends SvgContainer.Group{
	
	public static class LayerMetadata extends Metadata {
		public LayerMetadata() {
			super("layer", null, null, null, null, null);
		}
	}
	
	private TrackLayer(List<Svgable> contents, LayerMetadata metadata) {
		super(contents, metadata);
	}
	
	public static class Builder {
		private final List<Svgable> contents;
		public Builder(Rect bounds) {
			this.contents = new LinkedList<Svgable>();
			this.contents.add(bounds);
		}
		public Builder append(Svgable content) {
			contents.add(0, content);
			return this;
		}
		
		public TrackLayer build() {
			return new TrackLayer(contents, new LayerMetadata());
		}
	}
}
