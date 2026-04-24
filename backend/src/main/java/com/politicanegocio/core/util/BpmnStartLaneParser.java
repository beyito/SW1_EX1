package com.politicanegocio.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

@Component
public class BpmnStartLaneParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractStartLaneId(String diagramPayload) {
        if (diagramPayload == null || diagramPayload.isBlank()) {
            return "";
        }

        String directXmlLaneId = tryExtractFromXml(diagramPayload);
        if (!directXmlLaneId.isBlank()) {
            return directXmlLaneId;
        }

        String nestedXmlLaneId = tryExtractNestedXml(diagramPayload);
        if (!nestedXmlLaneId.isBlank()) {
            return nestedXmlLaneId;
        }

        return tryExtractFromJointJson(diagramPayload);
    }

    private String tryExtractNestedXml(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String[] candidateFields = {"bpmnXml", "xml", "diagramXml", "processXml"};
            for (String field : candidateFields) {
                String maybeXml = root.path(field).asText("");
                String laneId = tryExtractFromXml(maybeXml);
                if (!laneId.isBlank()) {
                    return laneId;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String tryExtractFromXml(String xml) {
        if (xml == null || xml.isBlank() || !xml.trim().startsWith("<")) {
            return "";
        }

        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            Set<String> startEventIds = new HashSet<>();
            NodeList startEvents = document.getElementsByTagNameNS("*", "startEvent");
            for (int i = 0; i < startEvents.getLength(); i++) {
                Node node = startEvents.item(i);
                if (node instanceof Element element) {
                    String id = element.getAttribute("id");
                    if (!id.isBlank()) {
                        startEventIds.add(id);
                    }
                }
            }

            if (startEventIds.isEmpty()) {
                return "";
            }

            NodeList lanes = document.getElementsByTagNameNS("*", "lane");
            for (int i = 0; i < lanes.getLength(); i++) {
                Node laneNode = lanes.item(i);
                if (!(laneNode instanceof Element laneElement)) {
                    continue;
                }

                String laneId = laneElement.getAttribute("id");
                NodeList refs = laneElement.getElementsByTagNameNS("*", "flowNodeRef");
                for (int j = 0; j < refs.getLength(); j++) {
                    String refId = refs.item(j).getTextContent();
                    if (startEventIds.contains(refId)) {
                        return laneId == null ? "" : laneId.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    private String tryExtractFromJointJson(String diagramJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(diagramJson);
            JsonNode cells = rootNode.path("cells");
            if (!cells.isArray()) {
                return "";
            }

            for (JsonNode cell : cells) {
                String nodeType = cell.path("nodeType").asText("");
                if (!"START".equals(nodeType)) {
                    continue;
                }
                return cell.path("laneId").asText("");
            }
        } catch (Exception ignored) {
        }

        return "";
    }
}
