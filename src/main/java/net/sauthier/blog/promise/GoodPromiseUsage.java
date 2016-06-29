/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package net.sauthier.blog.promise;

import java.net.URI;
import java.net.URISyntaxException;

import org.forgerock.http.apache.async.AsyncHttpClientProvider;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.ContentLengthHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Options;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

public class GoodPromiseUsage {
    public static void main(String[] args) throws Exception {

        // Create an HTTP Client with a single thread
        Options options = Options.defaultOptions()
                                 .set(AsyncHttpClientProvider.OPTION_WORKER_THREADS, 1);
        try (HttpClientHandler client = new HttpClientHandler(options)) {
            // Get the response on the "main" thread
            Response response = asyncPromise(client).getOrThrow();
            long length = response.getHeaders().get(ContentLengthHeader.class).getLength();
            System.out.printf("response size: %d bytes%n", length);
        }
    }

    private static Promise<Response, NeverThrowsException> asyncPromise(final HttpClientHandler client)
            throws URISyntaxException {

        // Perform a first request
        Request first = new Request().setMethod("GET").setUri("http://forgerock.org");
        Promise<Response, NeverThrowsException> promise;
        promise = client.handle(new RootContext(), first)
                        .thenAsync(new AsyncFunction<Response, Response, NeverThrowsException>() {
                            @Override
                            public Promise<Response, NeverThrowsException> apply(final Response value) {
                                // Perform a second request on the thread used to receive the response
                                Request second = new Request().setMethod("GET")
                                                              .setUri(URI.create("http://www.apache.org"));
                                return client.handle(new RootContext(), second);
                            }
                        });
        return promise;
    }
}
