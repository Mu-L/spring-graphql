/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.graphql.client;

import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.GraphQlRequest;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;


/**
 * Transport to execute GraphQL requests over HTTP via {@link RestClient}.
 *
 * <p>Supports only single-response requests over HTTP POST. For subscriptions,
 * see {@link WebSocketGraphQlTransport} and {@link RSocketGraphQlTransport}.
 *
 * @author Rossen Stoyanchev
 * @since 1.3
 */
final class RestClientGraphQlTransport implements GraphQlTransport {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};


	private final RestClient restClient;

	private final MediaType contentType;

	private final Scheduler scheduler;


	RestClientGraphQlTransport(RestClient restClient, @Nullable Scheduler scheduler) {
		Assert.notNull(restClient, "RestClient is required");
		this.restClient = restClient;
		this.contentType = initContentType(restClient);
		this.scheduler = (scheduler!= null ? scheduler : Schedulers.boundedElastic());
	}

	private static MediaType initContentType(RestClient webClient) {
		HttpHeaders headers = new HttpHeaders();
		webClient.mutate().defaultHeaders(headers::putAll);
		MediaType contentType = headers.getContentType();
		return (contentType != null ? contentType : MediaType.APPLICATION_JSON);
	}


	@Override
	public Mono<GraphQlResponse> execute(GraphQlRequest request) {
		return Mono
				.fromCallable(() -> this.restClient.post()
						.contentType(this.contentType)
						.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_GRAPHQL_RESPONSE)
						.body(request.toMap())
						.retrieve()
						.body(MAP_TYPE))
				.map(responseMap -> (GraphQlResponse) new ResponseMapGraphQlResponse(responseMap))
				.subscribeOn(this.scheduler);
	}

	@Override
	public Flux<GraphQlResponse> executeSubscription(GraphQlRequest request) {
		throw new UnsupportedOperationException("Subscriptions not supported");
	}

}
