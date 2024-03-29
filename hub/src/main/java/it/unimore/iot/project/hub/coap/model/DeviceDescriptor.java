package it.unimore.iot.project.hub.coap.model;

import com.google.common.collect.Lists;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;

public class DeviceDescriptor {
    public static final long RESOURCE_DISCOVERY_RETRY_PERIOD_MS = 60000;
    public static final long RESOURCE_DISCOVERY_TIMEOUT_MS = 30000;

    private Set<ResourceDescriptor> resources;

    private String name;
    private String address;
    private int port;


    public DeviceDescriptor(String name, String address, int port) {
        this.resources = new HashSet<>();

        this.name = name;
        this.address = address;
        this.port = port;

        Executors.newSingleThreadExecutor().submit(() -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        System.out.println("[TASK] Discover resources - [DEVICE] " + name + " [STATUS] Start");
                        if (discoverResources()) {
                            timer.cancel();
                            System.out.println("[TASK] Discover resources - [DEVICE] " + name + " [STATUS] Cancel");
                        }

                        System.out.println("[TASK] Discover resources - [DEVICE] " + name + " [STATUS] End");
                    } catch (ConnectorException e) {
                        System.out.println("[ERROR][RESOURCE DISCOVERY] - [DEVICE] " + name + " [TYPE] ConnectorException");
                    } catch (IOException e) {
                        System.out.println("[ERROR][RESOURCE DISCOVERY] - [DEVICE] " + name + " [TYPE] IOException");
                    } catch (Exception e) {
                        System.out.println("[ERROR][RESOURCE DISCOVERY] - [DEVICE] " + name + " [TYPE] Exception (Generic)");
                    }
                }
            }, 0, RESOURCE_DISCOVERY_RETRY_PERIOD_MS);
        });
    }



    private synchronized boolean discoverResources() throws ConnectorException, IOException {
        this.resources.clear();

        System.out.println("[RESOURCE DISCOVERY] - [DEVICE] " + name + " [STATUS] Starting");
        CoapClient client = new CoapClient(String.format("coap://%s:%d/.well-known/core", address, port));
        client.setTimeout(RESOURCE_DISCOVERY_TIMEOUT_MS);

        Request request = new Request(CoAP.Code.GET);

        CoapResponse response = client.advanced(request);

        if (response != null && response.isSuccess()) {
            if (response.getOptions().getContentFormat() != MediaTypeRegistry.APPLICATION_LINK_FORMAT) {
                System.out.println("[RESOURCE DISCOVERY] - [DEVICE] " + name + " [STATUS] Error: Not link format");
                return false;
            }

            Set<WebLink> links = LinkFormat.parse(response.getResponseText());

            for (WebLink link : links) {
                System.out.println("[RESOURCE DISCOVERY] - [DEVICE] " + name + " [LINK] " + link.toString());

                Set<String> keySet = link.getAttributes().getAttributeKeySet();

                if (link.getURI() != null && !link.getURI().isBlank()) {
                    String uri = link.getURI().split("/")[1];
                    ResourceDescriptor resourceDescriptor;

                    if (keySet.contains("obs")) {
                        resourceDescriptor = new ObservableResourceDescriptor(this, uri);
                    }
                    else {
                        resourceDescriptor = new ResourceDescriptor(this, uri);
                    }
                    resources.add(resourceDescriptor);
                }
            }

            System.out.println("[RESOURCE DISCOVERY] - [DEVICE] " + name + " [STATUS] Ended [RESOURCES] " + resources);
            return true;
        }

        return false;
    }

    public String sendGetRequest(String resourceName) {
        for (ResourceDescriptor resource : resources) {
            if (resource.getName().equals(resourceName)) {
                return resource.handleGET();
            }
        }

        return "Error: no resource with this name";
    }

    public String sendGetRequest(String resourceName, String querystring) {
        for (ResourceDescriptor resource : resources) {
            if (resource.getName().equals(resourceName)) {
                return resource.handleGET(querystring);
            }
        }

        return "Error: no resource with this name";
    }

    public String sendPostRequest(String resourceName) {
        for (ResourceDescriptor resource : resources) {
            if (resource.getName().equals(resourceName)) {
                return resource.handlePOST();
            }
        }

        return "Error: no resource with this name";
    }

    public String sendPutRequest(String resourceName, String payload) {
        for (ResourceDescriptor resource : resources) {
            if (resource.getName().equals(resourceName)) {
                return resource.handlePUT(payload);
            }
        }

        return "Error: no resource with this name";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getResources() {
        ArrayList<String> resourceNames = new ArrayList<>();
        for (ResourceDescriptor resource : resources) {
            resourceNames.add(resource.getName());
        }
        return resourceNames;
    }
}
