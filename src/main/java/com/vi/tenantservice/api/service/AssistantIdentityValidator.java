package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.exception.TenantBadRequestException;
import com.vi.tenantservice.api.model.Theming;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Assistant-only image validation; does not widen the legacy branding upload contract. */
public final class AssistantIdentityValidator {
  private static final int MAX_DECODED_ICON_BYTES = 512 * 1024;
  private static final int MAX_ENCODED_ICON_CHARACTERS = 700000;
  private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
  private static final Set<String> PRESETS =
      Set.of("default", "robot-7341990", "robot-1184077", "robot-3548536", "robot-5475944");
  private static final Set<String> ELEMENTS =
      Set.of(
          "svg",
          "g",
          "path",
          "rect",
          "circle",
          "ellipse",
          "line",
          "polyline",
          "polygon",
          "title",
          "desc");
  private static final Set<String> ATTRIBUTES =
      Set.of(
          "xmlns",
          "viewBox",
          "width",
          "height",
          "x",
          "y",
          "x1",
          "x2",
          "y1",
          "y2",
          "cx",
          "cy",
          "r",
          "rx",
          "ry",
          "d",
          "points",
          "fill",
          "fill-rule",
          "clip-rule",
          "stroke",
          "stroke-width",
          "stroke-linecap",
          "stroke-linejoin",
          "opacity",
          "fill-opacity",
          "stroke-opacity",
          "transform",
          "version",
          "id");

  private AssistantIdentityValidator() {}

  public static void validate(Theming theming) {
    if (theming == null) return;
    String name = theming.getAssistantName();
    if (name != null
        && (name.length() > 80
            || name.contains("<")
            || name.contains(">")
            || name.codePoints().anyMatch(Character::isISOControl))) invalid();
    String icon = theming.getAssistantIcon();
    if (icon == null || icon.isBlank() || PRESETS.contains(icon)) return;
    if (icon.length() > MAX_ENCODED_ICON_CHARACTERS) invalid();
    boolean svg = icon.startsWith("data:image/svg+xml;base64,");
    boolean png = icon.startsWith("data:image/png;base64,");
    if (!svg && !png) invalid();
    try {
      byte[] bytes = Base64.getDecoder().decode(icon.substring(icon.indexOf(',') + 1));
      if (bytes.length == 0 || bytes.length > MAX_DECODED_ICON_BYTES) invalid();
      if (png) {
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < 24) invalid();
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) invalid();
      } else {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        if (!document.getDocumentElement().getTagName().equals("svg")) invalid();
        validateElement(document.getDocumentElement(), 0);
      }
    } catch (TenantBadRequestException exception) {
      throw exception;
    } catch (Exception exception) {
      invalid();
    }
  }

  private static void validateElement(Element element, int depth) {
    if (depth > 64
        || !SVG_NAMESPACE.equals(element.getNamespaceURI())
        || !ELEMENTS.contains(element.getTagName())) invalid();
    var attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attr = attributes.item(i);
      String value = attr.getNodeValue().toLowerCase(java.util.Locale.ROOT);
      if (!ATTRIBUTES.contains(attr.getNodeName())
          || value.contains("url(")
          || value.contains("javascript:")) invalid();
    }
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element nested) validateElement(nested, depth + 1);
    }
  }

  private static void invalid() {
    throw new TenantBadRequestException(
        "Invalid assistant name or icon; use a preset or a passive SVG/PNG up to 512 KiB.");
  }
}
