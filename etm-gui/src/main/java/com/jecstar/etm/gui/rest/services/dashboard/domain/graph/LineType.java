package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

public enum LineType {

    STRAIGHT, SMOOTH, STEP_LEFT, STEP_CENTER, STEP_RIGHT;

    public static LineType safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LineType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getHighchartsChartType(AxesGraph axesGraph) {
        if (axesGraph instanceof LineGraph) {
            return SMOOTH.equals(this) ? "spline" : "line";
        } else if (axesGraph instanceof AreaGraph) {
            return SMOOTH.equals(this) ? "areaspline" : "area";
        }
        throw new IllegalArgumentException("Unsupported graph: " + axesGraph.getClass().getName());
    }

    public void addHighchartsPlotOptions(JsonBuilder builder) {
        if (STEP_LEFT.equals(this)) {
            builder.field("step", "left");
        } else if (STEP_CENTER.equals(this)) {
            builder.field("step", "center");
        } else if (STEP_RIGHT.equals(this)) {
            builder.field("step", "right");
        }
    }
}
