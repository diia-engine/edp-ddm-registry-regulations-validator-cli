package com.epam.digital.data.platform.registry.regulation.validation.cli.validator.assets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class SVGValidator {

  private static final Pattern SAFE_XLINK_HREF_PATTERN = Pattern.compile("^#[\\w\\-]+$");

  /**
   * Validates an SVG file.
   *
   * @param svgFile The SVG file to validate
   * @return ValidationResult with the validation status
   */
  public ValidationResult validateSvg(File svgFile) {
    try {
      String svgContent = new String(Files.readAllBytes(svgFile.toPath()));
      return validateSvgContent(svgContent);
    } catch (IOException e) {
      return new ValidationResult(false, "Could not read SVG file: " + e.getMessage());
    }
  }

  /**
   * Validates an SVG content.
   *
   * @param svgContent The SVG content to validate
   * @return ValidationResult with the validation status
   */
  public ValidationResult validateSvgContent(String svgContent) {
    try {

      Document originalDoc = Jsoup.parse(svgContent, "", Parser.xmlParser());

      if (!originalDoc.select("script").isEmpty()) {
        return new ValidationResult(false, "SVG contains script tags");
      }

      for (Element element : originalDoc.getAllElements()) {
        for (var attribute : element.attributes()) {
          String attrName = attribute.getKey().toLowerCase();
          String attrValue = attribute.getValue();

          if (attrName.startsWith("on")) {
            return new ValidationResult(false, "SVG contains event handlers");
          }

          if ("xlink:href".equals(attrName) || "href".equals(attrName)) {
            if (!SAFE_XLINK_HREF_PATTERN.matcher(attrValue).matches() ) {
              return new ValidationResult(false,
                  "SVG contains unsafe URL reference: " + attrValue);
            }
          }

          if ("style".equals(attrName)) {
            if (attrValue.contains("javascript:") ||
                attrValue.contains("expression(") ||
                attrValue.contains("url(") && attrValue.contains(":")) {
              return new ValidationResult(false, "SVG contains dangerous style content");
            }
          }
        }
      }

      return new ValidationResult(true, null);

    } catch (Exception e) {
      return new ValidationResult(false, "Error validating SVG: " + e.getMessage());
    }
  }
}