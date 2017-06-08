package com.bmc.truesight.remedy.util;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.thirdparty.org.apache.commons.codec.binary.Base64;
import com.bmc.truesight.saas.remedy.integration.beans.Configuration;
import com.bmc.truesight.saas.remedy.integration.beans.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * It is HttpClient which sends the Events to TSI system based on the
 * configuration
 *
 * @author vitiwari
 */
public class TsiHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(TsiHttpClient.class);

    private Configuration configuration;

    public TsiHttpClient(Configuration configuration) {
        this.configuration = configuration;
    }

    public void pushBulkEventsToTSI(final List<Event> bulkEvents) {
        LOG.info("Starting ingestion of {} events  to TSI ", bulkEvents.size());
        HttpClient httpClient = null;
        //for(Event payload:bulkEvents){
        boolean isSuccessful = false;
        int retryCount = 0;
        while (!isSuccessful && retryCount <= this.configuration.getRetryConfig()) {
            httpClient = HttpClientBuilder.create().build();
            HttpPost httpPost = new HttpPost(this.configuration.getTsiEventEndpoint());
            httpPost.addHeader("Authorization", "Basic " + encodeBase64("" + ":" + this.configuration.getTsiApiToken()));
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("accept", "application/json");
            httpPost.addHeader("User-Agent", "RemedyScript");
            ObjectMapper mapper = new ObjectMapper();
            String jsonInString = null;
            try {
                jsonInString = mapper.writeValueAsString(bulkEvents);
                Charset charsetD = Charset.forName("UTF-8");
                StringEntity postingString = new StringEntity(jsonInString, charsetD);
                httpPost.setEntity(postingString);
            } catch (Exception e) {
                LOG.error("Can not Send events, There is an issue in creating http request data [{}]", e.getMessage());
                break;
            }
            HttpResponse response;
            try {
                response = httpClient.execute(httpPost);
            } catch (Exception e) {
                LOG.error("Sending Event resulted into an exception [{}]", e.getMessage());
                if (retryCount < this.configuration.getRetryConfig()) {
                    retryCount++;
                    try {
                        LOG.info("[Retry  {} ], Waiting for {} sec before trying again ......", retryCount, (this.configuration.getWaitMsBeforeRetry() / 1000));
                        Thread.sleep(this.configuration.getWaitMsBeforeRetry());
                    } catch (InterruptedException e1) {
                        LOG.info("Thread interrupted ......");
                    }
                    continue;
                } else {
                    break;
                }
            }
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != Constants.TSI_EVENT_SUCCESS) {
                if (retryCount < this.configuration.getRetryConfig()) {
                    retryCount++;
                    LOG.error("Sending Event did not result in success, response status Code : {}", new Object[]{response.getStatusLine().getStatusCode()});
                    try {
                        LOG.info("[Retry  {} ], Waiting for {} sec before trying again ......", retryCount, (this.configuration.getWaitMsBeforeRetry() / 1000));
                        Thread.sleep(this.configuration.getWaitMsBeforeRetry());
                    } catch (InterruptedException e1) {
                        LOG.error("Thread interrupted ......");
                    }
                    continue;
                } else {
                    break;
                }
            } else {
                isSuccessful = true;
                LOG.info("Event sending successful {}", response.getStatusLine());
                break;
            }
        }
        // }
    }

    public static String encodeBase64(final String encodeToken) {
        byte[] encoded = Base64.encodeBase64(encodeToken.getBytes());
        return new String(encoded);
    }
}
