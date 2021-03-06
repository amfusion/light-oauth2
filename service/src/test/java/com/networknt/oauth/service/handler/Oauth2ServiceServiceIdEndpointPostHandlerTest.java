
package com.networknt.oauth.service.handler;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.status.Status;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import java.io.IOException;


public class Oauth2ServiceServiceIdEndpointPostHandlerTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(Oauth2ServiceServiceIdEndpointPostHandlerTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;

    @Test
    public void testOauth2ServiceServiceIdEndpointPostHandlerTest() throws ClientException, ApiException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestBody = "[{\"endpoint\":\"/v1/data@post\",\"operation\":\"createData\",\"scope\":\"data.w\"},{\"endpoint\":\"/v1/data@put\",\"operation\":\"updateData\",\"scope\":\"data.w\"},{\"endpoint\":\"/v1/data@get\",\"operation\":\"retrieveData\",\"scope\":\"data.r\"},{\"endpoint\":\"/v1/data@delete\",\"operation\":\"deleteData\",\"scope\":\"data.w\"}]";
        try {
            ClientRequest request = new ClientRequest().setPath("/oauth2/service/AACT0001/endpoint").setMethod(Methods.POST);
            
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        Assert.assertNotNull(body);
    }

    @Test
    public void testServiceNotFound() throws ClientException, IOException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestBody = "[{\"endpoint\":\"/v1/data@post\",\"operation\":\"createData\",\"scope\":\"data.w\"},{\"endpoint\":\"/v1/data@put\",\"operation\":\"updateData\",\"scope\":\"data.w\"},{\"endpoint\":\"/v1/data@get\",\"operation\":\"retrieveData\",\"scope\":\"data.r\"},{\"endpoint\":\"/v1/data@delete\",\"operation\":\"deleteData\",\"scope\":\"data.w\"}]";
        try {
            ClientRequest request = new ClientRequest().setPath("/oauth2/service/fake/endpoint").setMethod(Methods.POST);

            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(404, statusCode);
        if(statusCode == 404) {
            Status status = Config.getInstance().getMapper().readValue(body, Status.class);
            Assert.assertNotNull(status);
            Assert.assertEquals("ERR12015", status.getCode());
            Assert.assertEquals("SERVICE_NOT_FOUND", status.getMessage());
        }
    }

}
