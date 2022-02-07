/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.elasticsearch.crawler.fs.client.v7;

import fr.pilato.elasticsearch.crawler.fs.client.ElasticsearchClient;
import fr.pilato.elasticsearch.crawler.fs.framework.bulk.Engine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public class ElasticsearchEngine implements Engine<ElasticsearchOperation, ElasticsearchBulkRequest, ElasticsearchBulkResponse> {
    private static final Logger logger = LogManager.getLogger(ElasticsearchEngine.class);
    private final ElasticsearchClient elasticsearchClient;

    public ElasticsearchEngine(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public ElasticsearchBulkResponse bulk(ElasticsearchBulkRequest request) {
        StringBuilder ndjson = new StringBuilder();

        request.getOperations().forEach(r -> {
            // Header
            ndjson.append("{\"")
                    .append(r.getOperation().toString().toLowerCase(Locale.ROOT))
                    .append("\":{\"_index\":\"")
                    .append(r.getIndex())
                    .append("\",\"_id\":\"")
                    .append(r.getId())
                    .append("\"");

            if (r instanceof ElasticsearchIndexOperation && ((ElasticsearchIndexOperation) r).getPipeline() != null) {
                ndjson
                        .append(",\"_pipeline\":\"")
                        .append(((ElasticsearchIndexOperation) r).getPipeline())
                        .append("\"");
            }
            ndjson.append("}}\n");
            if (r instanceof ElasticsearchIndexOperation) {
                ElasticsearchIndexOperation indexOp = (ElasticsearchIndexOperation) r;
                ndjson.append(indexOp.getJson())
                        .append("\n");
            }
        });

        logger.debug("Sending a bulk request of [{}] documents to the Elasticsearch service", request.numberOfActions());
        String response = elasticsearchClient.bulk(ndjson.toString());
        return new ElasticsearchBulkResponse(response);
    }
}
