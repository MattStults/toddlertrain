package com.mattstults.track;

public class Metadata {
	private final String classText;
	private final Integer fillColor;
	private final Float fillOpacity;
	private final Integer strokeColor;
	private final Float strokeOpacity;
	private final Float strokeWidth;
	
	protected Metadata(String classText, Integer fillColor, Float fillOpacity, Integer strokeColor, Float strokeOpacity, Float strokeWidth) {
		this.classText = classText;
		this.fillColor = fillColor;
		this.fillOpacity = fillOpacity;
		this.strokeColor = strokeColor;
		this.strokeOpacity = strokeOpacity;
		this.strokeWidth = strokeWidth;
	}
	
	public class Builder {
		private String classText;
		private Integer fillColor;
		private Float fillOpacity;
		private Integer strokeColor;
		private Float strokeOpacity;
		private Float strokeWidth;
		
		public Builder() {
			classText = null; 
			fillColor = null;
			fillOpacity = null;
			strokeColor = null;
			strokeOpacity = null;
			strokeWidth = null;
		}
		
		public Builder setClass(String text) {
			classText = text;
			return this;
		}
		
		public Builder setFill(int color, Float opacity) {
			fillColor = color;
			fillOpacity = opacity;
			return this;
		}
		public Builder setStroke(int color, Float opacity, Float width) {
			strokeColor = color;
			strokeOpacity = opacity;
			strokeWidth = width;
			return this;
		}
		
		public Metadata build() {
			return new Metadata(classText, fillColor, fillOpacity, strokeColor, strokeOpacity, strokeWidth);
		}
		
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if (classText != null) {
			ret.append("class=\"").append(classText).append("\" ");
		}
		ret.append("style=\"");
		if (fillColor != null) {
			ret.append("fill:").append(String.format("#%05X", fillColor)).append(";");	
		} else if (strokeColor != null){
			ret.append("fill:none;");
		}
		if (fillOpacity != null) {
			ret.append("fill-opacity:").append(fillOpacity).append(";");
		}
		if (strokeColor != null) {
			ret.append("stroke:").append(String.format("#%05X", strokeColor)).append(";");	
		}
		if (strokeOpacity != null) {
			ret.append("stroke-opacity:").append(strokeOpacity).append(";");
		}
		if (strokeWidth != null) {
			ret.append("stroke-width:").append(strokeWidth).append(";");	
		}
		ret.append("\"");
		return ret.toString();
	}
}
