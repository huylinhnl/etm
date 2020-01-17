package com.jecstar.etm.launcher.background;

import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.UpdateRequestBuilder;
import com.jecstar.etm.server.core.persisting.RequestEnhancer;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.util.HashMap;

/**
 * Class that broadcast the presence of this Enterprise Telemetry Monitor instance to the Elasticsearch database.
 */
public class InstanceBroadcaster implements Runnable {

    private final DataRepository dataRepository;
    private final RequestEnhancer requestEnhancer;
    private final String nodeName;
    private final String instanceHash;

    public InstanceBroadcaster(final EtmConfiguration etmConfiguration, final DataRepository dataRepository, final String instanceHash) {
        this.dataRepository = dataRepository;
        this.requestEnhancer = new RequestEnhancer(etmConfiguration);
        this.nodeName = etmConfiguration.getNodeName();
        this.instanceHash = instanceHash;
    }

    @Override
    public void run() {
        var scriptParams = new HashMap<String, Object>();
        scriptParams.put("node_name", this.nodeName);
        scriptParams.put("instance", this.instanceHash);
        this.dataRepository.updateAsync(requestEnhancer.enhance(
                new UpdateRequestBuilder(ElasticsearchLayout.CONFIGURATION_INDEX_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_NODE_ID_PREFIX + this.nodeName)
                        .setScript(new Script(ScriptType.STORED, null, "etm_update-node", scriptParams))
                        .setUpsert("{}", XContentType.JSON)
                        .setScriptedUpsert(true)
                        .setDetectNoop(true)
                ),
                DataRepository.noopActionListener());
    }
}
