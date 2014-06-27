package com.mattstultsdragexperiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	public List<String> generateSvgTracks(int nPerRow, int segmentHeight, float ratio, float yOffsetRatio) {
		Set<BluePrint> bluePrints = buildBluePrints();
		List<String> ret = new ArrayList<String>();
		int i = 0;
		int width = (int) (nPositions * ratio * segmentHeight);
		int height= (int) (nPositions * segmentHeight); 
		for (BluePrint bluePrint : bluePrints) {
			int column = i % nPerRow;
			int row = i / nPerRow;
			ret.add(buildPath(bluePrint, (int)((column + .5f) * width), segmentHeight/2 + row * height, ratio, segmentHeight, (int)(segmentHeight * yOffsetRatio)));
			++i;
		}
		return ret;
	}
	
	public String buildSvg() {
		int nPerRow = 10;
		int segDiameter = 10;
		float widthToHeight = 1f;
		List<String> paths = generateSvgTracks(nPerRow, segDiameter, widthToHeight, .25f);
		int width = (int) ((Math.min(nPerRow, paths.size())) * nPositions * segDiameter * widthToHeight);
		int height = ((paths.size() / nPerRow) + 1) * nPositions * segDiameter;
		StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
		builder.append("<svg "
				+ "version=\"1.1\" width=\"" + width + "\" height=\"" + height + "\" d=\"paths\">");
		for (String path : paths) {
			builder.append(path);
		}
		builder.append("</svg>");
		return builder.toString();
	}
	
	private String buildPath(BluePrint bluePrint, int xStart, int yStart, float widthRatio, int segmentHeight, int verticalOffset) {
		StringBuilder builder = new StringBuilder();
		builder.append("<path d=\"M ");
		int a = bluePrint.positions.get(0);
		appendPoint(builder, xStart +0, yStart + a * segmentHeight).append("C ");
		int multiplier = -1;
		for (int i = 1; i < bluePrint.positions.size(); i++) {
			int b = bluePrint.positions.get(i);
			buildPathSegment(builder, bluePrint, xStart, yStart, a, b, multiplier, widthRatio, segmentHeight, (i > 1 ? verticalOffset : 0), (i < bluePrint.positions.size() -1 ? verticalOffset : 0));
			a = b;
			multiplier *= -1;
		}
		for (int i = bluePrint.positions.size() - 2; i >= 0; i--) {
			int b = bluePrint.positions.get(i);
			buildPathSegment(builder, bluePrint, xStart, yStart, a, b, multiplier, widthRatio, segmentHeight, i < bluePrint.positions.size() - 2 ? -verticalOffset : 0, (i > 0 ? -verticalOffset : 0));
			a = b;
			multiplier *= -1;
		}
		return builder.append("z\" ")
				.append("center=\"0\" ")
				.append("cycles=\"3\" ")
				.append("id=\"t").append(bluePrint.toString()).append("\" ")
				.append("style=\"fill:none;stroke:#000000;stroke-width:1\" ")
				.append("/>\n").toString();
	}
	
	private static StringBuilder buildPathSegment(StringBuilder builder, BluePrint bluePrint, int xStart, int yStart, int a, int b, int multiplier, float widthRatio, int segmentHeight, int yOffset1, int yOffset2) {
		int width = (int) ((Math.abs(yOffset2 - yOffset1) + widthRatio * Math.abs(b - a) * segmentHeight) / 2f);
		appendPoint(builder, xStart + multiplier * width, yStart + yOffset1 + a * segmentHeight);
		appendPoint(builder, xStart + multiplier * width, yStart + yOffset2 + b * segmentHeight);
		appendPoint(builder, xStart, yStart + yOffset2 + b * segmentHeight);
		return builder;
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
