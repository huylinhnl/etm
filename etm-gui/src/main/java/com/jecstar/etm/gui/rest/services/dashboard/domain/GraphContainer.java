package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.DataConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.GraphConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.Graph;
import com.jecstar.etm.server.core.converter.JsonField;

/**
 * A <code>GraphContainer</code> holds all information to display a visual representation of one ore more groups of data.
 */
public class GraphContainer {

    public static final String NAME = "name";
    public static final String DATA = "data";
    public static final String GRAPH = "graph";

    @JsonField(NAME)
    private String name;

    @JsonField(value = DATA, converterClass = DataConverter.class)
    private Data data;

    @JsonField(value = GRAPH, converterClass = GraphConverter.class)
    private Graph<Graph> graph;

    public String getName() {
        return this.name;
    }

    public Data getData() {
        return this.data;
    }

    public Graph<Graph> getGraph() {
        return this.graph;
    }

    public GraphContainer setName(String name) {
        this.name = name;
        return this;
    }

    public GraphContainer setData(Data data) {
        this.data = data;
        return this;
    }

    @SuppressWarnings("unchecked")
    public GraphContainer setGraph(Graph<? extends Graph> graph) {
        this.graph = (Graph<Graph>) graph;
        return this;
    }

    public void mergeFromColumn(Column column) {
        if (column.getData() != null) {
            getData().mergeFromColumn(column.getData());
        }
        if (column.getGraph() != null) {
            getGraph().mergeFromColumn(column.getGraph());
        }
    }
}