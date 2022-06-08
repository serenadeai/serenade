package core.formattedtext;

import com.google.common.base.MoreObjects;
import core.util.TextStyle;
import java.util.List;
import java.util.Optional;

public class FormattedTextOptions {

  public static class Builder {

    public boolean expression = false;
    public Optional<TextStyle> style = Optional.empty();
    public Optional<ConversionMap> conversionMap = Optional.empty();

    public Builder setConversionMap(Optional<ConversionMap> conversionMap) {
      this.conversionMap = conversionMap;
      return this;
    }

    public Builder setExpression(boolean expression) {
      this.expression = expression;
      return this;
    }

    public Builder setStyle(TextStyle style) {
      this.style = Optional.of(style);
      return this;
    }

    public FormattedTextOptions build() {
      return new FormattedTextOptions(expression, style.orElse(TextStyle.LOWERCASE), conversionMap);
    }

    @Override
    public String toString() {
      return MoreObjects
        .toStringHelper(this)
        .add("conversionMap", conversionMap)
        .add("expression", expression)
        .add("style", style)
        .toString();
    }
  }

  public boolean expression;
  public TextStyle style;
  public Optional<ConversionMap> conversionMap;

  public FormattedTextOptions(
    boolean expression,
    TextStyle style,
    Optional<ConversionMap> conversionMap
  ) {
    this.conversionMap = conversionMap;
    this.expression = expression;
    this.style = style;
  }

  public static FormattedTextOptions fromString(List<String> options) {
    Builder builder = newBuilder();
    for (String o : options) {
      if (o.equals("expression")) {
        builder.setExpression(true);
      } else if (o.equals("pascal")) {
        builder.setStyle(TextStyle.PASCAL_CASE);
      } else if (o.equals("camel")) {
        builder.setStyle(TextStyle.CAMEL_CASE);
      } else if (o.equals("underscores")) {
        builder.setStyle(TextStyle.UNDERSCORES);
      } else if (o.equals("caps")) {
        builder.setStyle(TextStyle.ALL_CAPS);
      } else if (o.equals("capital")) {
        builder.setStyle(TextStyle.CAPITALIZED);
      } else if (o.equals("dashes")) {
        builder.setStyle(TextStyle.DASHES);
      } else if (o.equals("lowercase")) {
        builder.setStyle(TextStyle.LOWERCASE);
      }
    }

    return builder.build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(FormattedTextOptions o) {
    return new Builder()
      .setExpression(o.expression)
      .setStyle(o.style)
      .setConversionMap(o.conversionMap);
  }

  public static FormattedTextOptions newCamelCaseExpression() {
    return newBuilder().setExpression(true).setStyle(TextStyle.CAMEL_CASE).build();
  }

  public static FormattedTextOptions newCamelCaseIdentifier() {
    return newBuilder().setStyle(TextStyle.CAMEL_CASE).build();
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("conversionMap", conversionMap)
      .add("expression", expression)
      .add("style", style)
      .toString();
  }
}
