/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.client.dsl.internal;

import com.ning.http.client.AsyncHttpClient;
import io.fabric8.kubernetes.api.model.DoneableEvent;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventList;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.net.URL;

public class EventOperationsImpl extends BaseOperation<Event, EventList, DoneableEvent,
  Resource<Event, DoneableEvent>> {

  public EventOperationsImpl(AsyncHttpClient httpClient, URL rootUrl) {
    super(httpClient, rootUrl, "events", null, null, Event.class, EventList.class, DoneableEvent.class);
  }

  public EventOperationsImpl(AsyncHttpClient httpClient, URL rootUrl, String namespace, String name) {
    super(httpClient, rootUrl, "events", namespace, name);
  }
}
