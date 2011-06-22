/*
 * Copyright 2011 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.samples.twitter;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.jersey.SuspendResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.resource.Singleton;

@Path("/search/{tagid}")
@Produces("text/html;charset=ISO-8859-1")
@Singleton
public class TwitterFeed {

    private static final Logger logger = LoggerFactory.getLogger(TwitterFeed.class);

    private final CountDownLatch suspendLatch = new CountDownLatch(1);

    @GET
    public SuspendResponse<String> search(final @PathParam("tagid") Broadcaster feed,
                                          final @PathParam("tagid") String tagid) {

        if (feed.getAtmosphereResources().size() == 0) {

            final Future<?> future = feed.scheduleFixedBroadcast(new Callable<String>() {

                public String call() throws Exception {

                    // Wait for the connection to be suspended.
                    suspendLatch.await();

                    final TwitterMessage twitterMessage = new TwitterMessage(
                            1L,
                            new Date(),
                            "Message", "generator", "http://something.com/");

                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(twitterMessage); // where 'dst' can be File, OutputStream or Writer

                    feed.broadcast(json).get();

                    return null;

                }

            }, 200, TimeUnit.MILLISECONDS);

            final Future<?> future2 = feed.scheduleFixedBroadcast(new Callable<String>() {

                public String call() throws Exception {

                    // Wait for the connection to be suspended.
                    suspendLatch.await();

                    final TwitterMessage twitterMessage = new TwitterMessage(
                            1L,
                            new Date(),
                            "Message2", "generator2", "http://something.com/2");

                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(twitterMessage); // where 'dst' can be File, OutputStream or Writer

                    feed.broadcast(json).get();

                    return null;

                }

            }, 200, TimeUnit.MILLISECONDS);

        }

        return new SuspendResponse.SuspendResponseBuilder<String>().broadcaster(feed).outputComments(true)
                .addListener(new EventsLogger() {

                    @Override
                    public void onSuspend(
                            final AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
                        super.onSuspend(event);

                        // OK, we can start polling Twitter!
                        suspendLatch.countDown();
                    }
                }).build();
    }

}