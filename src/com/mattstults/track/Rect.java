package com.mattstults.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mattstults.track.Svgable.PathPart;

public class Rect implements Svgable{
	private final Point topLeft;
	private final Point bottomRight;
	private final Metadata metadata;
	
	private Rect(Point topLeft, Point bottomRight, Metadata metadata) {
		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
		this.metadata = metadata;
	}
	
	public static class Builder {
		private float top, bottom, left, right;
		private final Metadata metadata;
		public Builder(Metadata metadata) {
			this.metadata = metadata;
			left = top = Float.POSITIVE_INFINITY;
			bottom = right = Float.NEGATIVE_INFINITY;
		}
		public Builder expandRect(Point point) {
			top = Math.min(top, point.getY());
			bottom = Math.max(bottom, point.getY());
			left = Math.min(left, point.getX());
			right = Math.max(right, point.getX());
			return this;
		}
		
		public Rect build() {
			return new Rect(new Point(left, top), new Point(right, bottom), metadata);
		}
	}
	@Override
	public String toSvg(int indentation) {
		StringBuilder ret = new StringBuilder();
		Svgable.Utilities.indent(ret, indentation);
		ret.append("<rect width=\"").append(bottomRight.getX() - topLeft.getX())
			.append("\" height=\"").append(bottomRight.getY() - topLeft.getY())
			.append("\" x=\"").append(topLeft.getX())
			.append("\" y=\"").append(topLeft.getY())
			.append("\"\n");
		Svgable.Utilities.indent(ret, indentation + 4);
		ret.append(metadata.toString()).append("/>\n");
		return ret.toString();
	}
	
}
