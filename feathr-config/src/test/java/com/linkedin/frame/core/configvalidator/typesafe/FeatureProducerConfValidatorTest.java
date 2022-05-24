package com.linkedin.frame.core.configvalidator.typesafe;

import com.linkedin.frame.core.config.ConfigType;
import com.linkedin.frame.core.configbuilder.typesafe.TypesafeConfigBuilder;
import com.linkedin.frame.core.configdataprovider.ConfigDataProvider;
import com.linkedin.frame.core.configdataprovider.ResourceConfigDataProvider;
import com.linkedin.frame.core.configdataprovider.StringConfigDataProvider;
import com.linkedin.frame.core.configvalidator.ConfigValidatorFixture;
import com.linkedin.frame.core.configvalidator.ValidationResult;
import com.linkedin.frame.core.configvalidator.ValidationStatus;
import com.linkedin.frame.core.configvalidator.ValidationType;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Test class for {@link FeatureProducerConfValidator}
 */
public class FeatureProducerConfValidatorTest {
  private FeatureProducerConfValidator _featureProducerConfValidator = new FeatureProducerConfValidator();
  private TypesafeConfigBuilder _configBuilder = new TypesafeConfigBuilder();

  @Test(expectedExceptions = RuntimeException.class,
      description = "test unsupported Config type for Frame feature producer")
  public void testUnsupportedConfigType() {
    Map<ConfigType, ConfigDataProvider> configs = new HashMap<>();
    configs.put(ConfigType.FeatureDef, new ResourceConfigDataProvider("invalidSemanticsConfig/feature-not-reachable-def.conf"));
    configs.put(ConfigType.Join, new StringConfigDataProvider(ConfigValidatorFixture.joinConfig1));

    // perform semantic validation
    Map<ConfigType, ValidationResult> semanticResult = _featureProducerConfValidator.validate(configs, ValidationType.SEMANTIC);
  }

  @Test(description = "For Frame feature producer, feature reachable validation won't be applied")
  public void testRequestUnreachableFeatures() {
    Map<ConfigType, ConfigDataProvider> configs = new HashMap<>();
    configs.put(ConfigType.FeatureDef, new ResourceConfigDataProvider("invalidSemanticsConfig/feature-not-reachable-def.conf"));

    // perform semantic validation
    Map<ConfigType, ValidationResult> semanticResult = _featureProducerConfValidator.validate(configs, ValidationType.SEMANTIC);
    ValidationResult featureDefSemanticResult = semanticResult.get(ConfigType.FeatureDef);
    Assert.assertEquals(featureDefSemanticResult.getValidationStatus(), ValidationStatus.VALID);
  }
}
