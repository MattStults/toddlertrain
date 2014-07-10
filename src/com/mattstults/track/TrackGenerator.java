package com.mattstults.track;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mattstults.track.Svgable.PathPart;

public class TrackGenerator {
	private final int nPositions;
	public TrackGenerator(int nPositions){
		this.nPositions = nPositions;
	}
	
	public Set<BluePrint> buildBluePrints() {
		List<Integer> combination = new ArrayList<Integer>(nPositions);
		for (int i = 0; i < nPositions; i++) {
			combination.add(i);
		}
		return buildBluePrints(0, combination);
	}
	
	public List<Track> generateSvgTracks(int nPerRow, int segmentHeight, float ratio, float yOffsetRatio) {
		Set<BluePrint> bluePrints = buildBluePrints();
		List<Track> ret = new ArrayList<Track>();
		int i = 0;
		int width = (int) (nPositions * ratio * segmentHeight);
		int height= (int) (nPositions * segmentHeight); 
		for (BluePrint bluePrint : bluePrints) {
			int column = i % nPerRow;
			int row = i / nPerRow;
			ret.add(buildTrack(bluePrint, (int)((column + .5f) * width), segmentHeight/2 + row * height, ratio, segmentHeight, (int)(segmentHeight * yOffsetRatio)));
			++i;
		}
		return ret;
	}
	
	public String buildSvg() {
		int nPerRow = 10;
		int segDiameter = 10;
		float widthToHeight = 1f;
		List<Track> paths = generateSvgTracks(nPerRow, segDiameter, widthToHeight, .25f);
		int width = (int) ((Math.min(nPerRow, paths.size())) * nPositions * segDiameter * widthToHeight);
		int height = ((paths.size() / nPerRow) + 1) * nPositions * segDiameter;
		StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
		builder.append("<svg "
				+ "version=\"1.1\" width=\"" + width + "\" height=\"" + height + "\" id=\"paths\">\n");
		for (Track path : paths) {
			builder.append(path.toSvg(2));
		}
		builder.append("</svg>");
		return builder.toString();
	}
	
	private Track buildTrack(BluePrint bluePrint, int xStart, int yStart, float widthRatio, int segmentHeight, int verticalOffset) {
		Track.Builder builder = new Track.Builder(bluePrint.toString(), 3);
		int a = bluePrint.positions.get(0);
		int multiplier = -1;
		for (int i = 1; i < bluePrint.positions.size(); i++) {
			int b = bluePrint.positions.get(i);
			builder.append(buildPathSegment(bluePrint, xStart, yStart, a, b, multiplier, widthRatio, segmentHeight, (i > 1 ? verticalOffset : 0), (i < bluePrint.positions.size() -1 ? verticalOffset : 0)));
			a = b;
			multiplier *= -1;
		}
		for (int i = bluePrint.positions.size() - 2; i >= 0; i--) {
			int b = bluePrint.positions.get(i);
			builder.append(buildPathSegment(bluePrint, xStart, yStart, a, b, multiplier, widthRatio, segmentHeight, i < bluePrint.positions.size() - 2 ? -verticalOffset : 0, (i > 0 ? -verticalOffset : 0)));
			a = b;
			multiplier *= -1;
		}
		return builder.build();
	}
	
	private static TrackLayer buildPathSegment(BluePrint bluePrint, int xStart, int yStart, int a, int b, int multiplier, float widthRatio, int segmentHeight, int yOffset1, int yOffset2) {
		int width = (int) ((Math.abs(yOffset2 - yOffset1) + widthRatio * Math.abs(b - a) * segmentHeight) / 2f);
		int controlPointX = xStart + multiplier * width;
		int y1 = yStart + yOffset1 + a * segmentHeight;
		int y2 = yStart + yOffset2 + b * segmentHeight;
		List<PathPart> parts = new ArrayList<PathPart>();
		parts.add(new Move(Context.ABSOLUTE, Point.of(xStart, y1)));
		parts.add(new Cubic.Builder(
				Context.ABSOLUTE,
				Point.of(controlPointX, y1),
				Point.of(controlPointX, y2),
				Point.of(xStart, y2)).build());
		Rect.Builder bounds = new Rect.Builder(new Metadata("bounds", null, null, 0xff0000, 0.5f, 0.5f));
		bounds.expandRect(Point.of(solveCubic(0.5f, xStart, controlPointX, controlPointX, xStart), y1));
		bounds.expandRect(Point.of(xStart, y2));
		TrackLayer.Builder layerBuilder = new TrackLayer.Builder(bounds.build());
		SvgContainer.Path path = new SvgContainer.Path(
				parts,
				new Metadata("trackPath", null, null, 0, 1f, 1f));
		layerBuilder.append(path);
		return layerBuilder.build();
	}
	
	private static float solveCubic(float t, float p0, float p1, float p2, float p3) {
		float iT = 1f - t;
		return (float) (iT * iT * iT * p0
				+ 3 * iT * iT * t * p1
				+ 3 * iT * t * t * p2
				+ t * t * t * p3);
	}
	
	private static StringBuilder bounds(StringBuilder builder, float left, float top, float right, float bottom) {
		return builder.append("<rect width=\"").append(right - left)
				.append("\" height=\"").append(bottom - top)
				.append("\" x=\"").append(left)
				.append("\" y=\"").append(top)
				.append("\" class=\"bounds\" style=\"fill:none;stroke:#ff0000;stroke-width:0.1;stroke-miterlimit:4;stroke-dasharray:none\"/>\n");
	}
	
	private static StringBuilder appendPoint(StringBuilder builder, int x, int y) {
		return builder.append(x).append(",").append(y).append(" ");
	}
	
	private Set<BluePrint> buildBluePrints(int i, List<Integer> input) {
		Set<BluePrint> bluePrints = null;
		if (i >= input.size() - 1) {
			bluePrints = new HashSet<BluePrint>();
			if (input.get(0) == 1 && input.get(1) == 0) {//&& input.get(2) == 3 && input.get(3) == 2) {
				i += 0;
			}
			bluePrints.add(new BluePrint(input));
		} else {
			for (int k = i; k < input.size(); k++) {
				Collections.swap(input, k, i);
				if (bluePrints == null) {
					bluePrints = buildBluePrints(i + 1, input);
				} else {
					bluePrints.addAll(buildBluePrints(i + 1, input));
				}
				Collections.swap(input, i, k);
			}
		}
		return bluePrints;
	}
	
	enum Mirror {
		FORWARDS {
			@Override
			protected int get(int i, List<Integer> input) {
				return input.get(i);
			}
		},
		BACKWARDS {
			@Override
			protected int get(int i, List<Integer> input) {
				return input.get(input.size() - i - 1);
			}
		},
		FORWARDS_SWAPPED {
			@Override
			protected int get(int i, List<Integer> input) {
				return input.size() - FORWARDS.get(i, input) - 1;
			}
		},
		BACKWARDS_SWAPPED {
			@Override
			protected int get(int i, List<Integer> input) {
				return input.size() - BACKWARDS.get(i, input) - 1;
			}
		};
		
		public static Mirror getCanonical(List<Integer> original){
			int min = Integer.MAX_VALUE;
			Mirror ret = null;
			for (Mirror mirror : Mirror.values()) {
				int current = mirror.get(0, original);
				if (min > current) {
					min = current;
					ret = mirror;
				}
			}
			return ret;
		}
		
		public static List<Integer> buildCanonical(List<Integer> original) {
			ArrayList<Integer> ret = new ArrayList<Integer>();
			Mirror mirror = getCanonical(original);
			for (int i = 0; i < original.size(); i++) {
				ret.add(mirror.get(i, original));
			}
			return ret;
		}
		
		protected abstract int get(int i, List<Integer> input);
	}
	
	private class BluePrint {
		private List<Integer> positions;
		
		public BluePrint(List<Integer> positions) {
			this.positions =  Mirror.buildCanonical(positions);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof BluePrint)) {
				return false;
			}
			BluePrint bluePrint = (BluePrint) o;
			boolean match = true;
			for (int i = 0; i < nPositions && match; i++) {
				match = match && bluePrint.positions.get(i) == positions.get(i);
			}
			return match;
		}
		
		@Override
		public int hashCode() {
			int ret = 0;
			for (int i = 0; i < nPositions; i++) {
				ret += positions.get(i) * Math.pow(nPositions, i);
			}
			return ret;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder((nPositions + 1) * 3);
			for (int i = 0; i < positions.size(); i++) {
				builder.append(positions.get(i));
				if (i < positions.size() - 1) {
					builder.append("-");
				}
			}
			return builder.toString();
		}
	}
}
