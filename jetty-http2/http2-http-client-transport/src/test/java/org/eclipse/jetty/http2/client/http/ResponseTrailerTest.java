//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http2.client.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Promise;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseTrailerTest extends AbstractTest
{
    @Test
    public void testEmptyTrailersWithoutContent() throws Exception
    {
        testEmptyTrailers(null);
    }

    @Test
    public void testEmptyTrailersWithContent() throws Exception
    {
        testEmptyTrailers("data");
    }

    public void testEmptyTrailers(String data) throws Exception
    {
        start(new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                // Send empty response trailers.
                Response jettyResponse = jettyRequest.getResponse();
                jettyResponse.setTrailers(HttpFields::new);
                if (data != null)
                    response.getOutputStream().write(data.getBytes(StandardCharsets.US_ASCII));
            }
        });

        HTTP2Client http2Client = new HTTP2Client();
        http2Client.start();
        try
        {
            String host = "localhost";
            int port = connector.getLocalPort();
            InetSocketAddress address = new InetSocketAddress(host, port);
            FuturePromise<Session> sessionPromise = new FuturePromise<>();
            http2Client.connect(address, new Session.Listener.Adapter(), sessionPromise);
            Session session = sessionPromise.get(5, TimeUnit.SECONDS);

            HttpURI uri = new HttpURI("http://" + host + ":" + port + "/");
            MetaData.Request request = new MetaData.Request(HttpMethod.GET.asString(), uri, HttpVersion.HTTP_2, new HttpFields());
            HeadersFrame frame = new HeadersFrame(request, null, true);
            BlockingQueue<HeadersFrame> headers = new LinkedBlockingQueue<>();
            CountDownLatch latch = new CountDownLatch(1);
            session.newStream(frame, new Promise.Adapter<>(), new Stream.Listener.Adapter()
            {
                @Override
                public void onHeaders(Stream stream, HeadersFrame frame)
                {
                    headers.offer(frame);
                    if (frame.isEndStream())
                        latch.countDown();
                }

                @Override
                public void onData(Stream stream, DataFrame frame, Callback callback)
                {
                    super.onData(stream, frame, callback);
                    if (frame.isEndStream())
                        latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(1, headers.size());
            frame = headers.poll();
            assertNotNull(frame);
            assertTrue(frame.getMetaData().isResponse());
        }
        finally
        {
            http2Client.stop();
        }
    }
}
