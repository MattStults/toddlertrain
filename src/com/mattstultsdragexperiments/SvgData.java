package com.mattstultsdragexperiments;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.larvalabs.svgandroid.SVGParser;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.Display;

public class SvgData {
	public static List<SvgData> readSvgObjects(InputStream trackFile)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.parse(trackFile);
		NodeList groups = doc.getElementsByTagName("g");
		List<SvgData> paths = new ArrayList<SvgData>();
		for (int i = 0; i < groups.getLength(); i++) {
			paths.add(fromNode(groups.item(i)));
		}
		return Collections.unmodifiableList(paths);
	}
	
	public static Map<String, SvgData> readSvgObjectLibrary(InputStream file) throws SAXException, IOException, ParserConfigurationException {
		List<SvgData> objects = readSvgObjects(file);
		Map<String, SvgData> library = new HashMap<String, SvgData>();
		for (SvgData data : objects) {
			library.put(data.getName(), data);
		}
		return Collections.unmodifiableMap(library);
	}
	
	private static final Pattern POINT = Pattern.compile("\\s([-]?\\d*[.]?\\d*)\\s*,([-]?\\d*[.]?\\d*)");
	private static SvgData fromNode(Node group) {
		NodeList parts = group.getChildNodes();
		List<Part> pathDescriptions = new ArrayList<Part>(parts.getLength());
		RectF bounds = null;
		float originX = 0f, originY = 0f;
		for (int i = 0; i < parts.getLength(); i++) {
			Node part = parts.item(i);
			if (!part.hasAttributes()) {
				continue;
			}
			String id = part.getAttributes().getNamedItem("id").getTextContent();
			if (id.startsWith("origin")) {
				Matcher matcher = POINT.matcher(part.getAttributes().getNamedItem("d").getTextContent());
				if (matcher.find()) {
					originX = Float.parseFloat(matcher.group(1));
					originY = Float.parseFloat(matcher.group(2));
				}
			} else if (id.startsWith("bounds")) {
				bounds = new RectF();
				Node left = part.getAttributes().getNamedItem("x");
				Node top = part.getAttributes().getNamedItem("y");
				Node width = part.getAttributes().getNamedItem("width");
				Node height = part.getAttributes().getNamedItem("height");
				bounds.left = Float.parseFloat(left.getTextContent());
				bounds.right = bounds.left + Float.parseFloat(width.getTextContent());
				bounds.top = Float.parseFloat(top.getTextContent());
				bounds.bottom = bounds.top + Float.parseFloat(height.getTextContent());
			} else {
				Node name = part.getAttributes().getNamedItem("id");
				Node path = part.getAttributes().getNamedItem("d");
				Node style = part.getAttributes().getNamedItem("style");
				pathDescriptions.add(new Part(name.getTextContent(), path.getTextContent(), style.getTextContent()));
			}
		}
		Node name = group.getAttributes().getNamedItem("id");
		return new SvgData(name.getTextContent(), originX, originY, bounds, pathDescriptions);
	}

	private final String name;
	private final List<Part> parts;
	private final RectF bounds;
	private final float originX;
	private final float originY;

	private SvgData(String name, float originX, float originY, RectF bounds, List<Part> parts) {
		this.name = name;
		this.originX = originX;
		this.originY = originY;
		this.bounds = bounds;
		List<Part> builder = new ArrayList<Part>();
		builder.addAll(parts);
		this.parts = Collections.unmodifiableList(builder);
	}
	
	public String getName() {
		return name;
	}
	
	public class SvgDrawable {
		private final Bitmap bitmap;
		private final float scale;
		public SvgDrawable(float scale) {
			this.scale = scale;
			bitmap = Bitmap.createBitmap((int)(bounds.width() * scale), (int)(bounds.height() * scale), Config.ARGB_8888);
			Canvas c = new Canvas(bitmap);
			c.translate(-bounds.left * scale, -bounds.top * scale);
			c.scale(scale, scale);
			for (Part part : parts) {
				part.draw(c);
			}
		}
		public void draw(Canvas c) {
			draw(c, null);
		}
		
		public void draw(Canvas c, Paint paintOverride) {
			c.drawBitmap(bitmap, (bounds.left - originX) * scale, (bounds.top - originY) * scale, paintOverride);
		}
	}
	
	public SvgDrawable getIconAtHeight(float height) {
		return new SvgDrawable(height / bounds.height());
	}

	public TrackRider getCarAtHeight(float height) {
		final float scale = height / bounds.height();
		final SvgDrawable drawable = new SvgDrawable(scale);
		return new TrackRider() {
			private float xScale = 0f;
			private float distanceTravelled;
			@Override
			public float getDistanceFront() {
				return distanceTravelled + (bounds.right - originX) * scale;
			}

			@Override
			public float getDistanceBack() {
				return distanceTravelled + (bounds.left - originX) * scale;
			}

			@Override
			public float getDistanceTravelled() {
				return distanceTravelled;
			}

			@Override
			public void draw(Canvas canvas, float[] tangent) {
				float scaleGoal = tangent[0] < 0 ? -1 : 1;
				xScale = xScale + (scaleGoal - xScale)/10f;
				canvas.scale(1f, xScale);
				drawable.draw(canvas);
			}

			@Override
			public void setFrontPosition(float distanceTravelled) {
				this.distanceTravelled = distanceTravelled - (bounds.right - originX) * scale;
			}
			
		};
	}
	
	private static class Part {
		private final Path path;
		private final String name;
		private final List<Paint> paints;
		public Part(String name, String path, String style) {
			this.name = name;
			this.path = SVGParser.parsePath(path);
			this.paints = fromStyle(style);
		}
		
		public void draw(Canvas c) {
			for (Paint paint : paints) {
			  c.drawPath(path, paint);
			}
		}
	}
	private static final Pattern FILL = Pattern.compile("fill:[#]?([^;]*)[;]?$?");
	private static final Pattern STROKE = Pattern.compile("stroke:[#]?([^;]*)[;]?$?");
	private static final Pattern FILL_OPACITY = Pattern.compile("fill-opacity:([^;]*)[;]?$?");
	private static final Pattern STROKE_OPACITY = Pattern.compile("stroke-opacity:([^;]*)[;]?$?");
	private static final Pattern STROKE_WIDTH = Pattern.compile("stroke-width:([^;]*)[;]?$?");
	private static List<Paint> fromStyle(String style){
		List<Paint> ret = new ArrayList<Paint>(2);
		Paint fill = readColor(style, Style.FILL);
		Paint stroke = readColor(style, Style.STROKE);
		if (fill != null) {
			ret.add(fill);
		}
		if (stroke != null) {
			ret.add(stroke);
		}
		return ret;
	}
	
	private static float getOpacity(Matcher opacityMatcher) {
		if (opacityMatcher.find()) {
			return Float.parseFloat(opacityMatcher.group(1));
		}
		return 1f;
	}
	
	private static String getOpacityHex(Matcher opacityMatcher) {
		float opacity = getOpacity(opacityMatcher);
		String ret = Integer.toString((int)(opacity * 255), 16);
		if (ret.length() == 1) {
			return "0" + ret;
		}
		return ret;
	}
	
	private static Paint readColor(String styleString, Style style) {
		Matcher colorMatcher;
		Matcher opacityMatcher;
		switch(style) {
		case FILL:
			colorMatcher = FILL.matcher(styleString);
			opacityMatcher = FILL_OPACITY.matcher(styleString);
			break;
		case STROKE:
			colorMatcher = STROKE.matcher(styleString);
			opacityMatcher = STROKE_OPACITY.matcher(styleString);
			break;
			default:
				throw new IllegalStateException("Unknown style: " + style);
		}
		if (colorMatcher.find()) {
			String input = colorMatcher.group(1);
			Integer color = (input != null && input.length() != 0 && !"none".equals(input))
					? (int) Long.parseLong(getOpacityHex(opacityMatcher)+input, 16)
					: null;
			Matcher strokeWidth = STROKE_WIDTH.matcher(styleString);
			Float lineWidth = null;
			if(strokeWidth.find()) {
				lineWidth = Float.parseFloat(strokeWidth.group(1));
			}
			
			if (color != null) {
				Paint paint = new Paint();
				paint.setColor(color);
				paint.setStyle(style);
				if (lineWidth != null) {
					paint.setStrokeWidth(lineWidth);
				}
				return paint;
			}
		}
		return null;
	}
}
