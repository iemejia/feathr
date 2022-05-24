package com.linkedin.frame.core.configbuilder.typesafe.producer.common;

import com.linkedin.feathr.featureDataModel.KeyPlaceholder;
import com.linkedin.feathr.featureDataModel.KeyPlaceholderArray;
import com.typesafe.config.ConfigException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.linkedin.frame.core.config.producer.common.KeyListExtractor;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class KeyListExtractorTest {
  private KeyListExtractor _keyListConverter = KeyListExtractor.getInstance();

  @Test(description = "test get single key from HOCON expression, and verify that the quote does not influence the parsing")
  public void testSingleKeyInHocon() {
    String keyExpression1 = "key1";
    String keyExpression2 = "\"key1\"";
    List<String> keysFromExpression1 = _keyListConverter.extractFromHocon(keyExpression1);
    assertEquals(keysFromExpression1, Collections.singletonList(keyExpression1));
    assertEquals(keysFromExpression1, _keyListConverter.extractFromHocon(keyExpression2));
  }

  @Test(description = "test get single key from HOCON expression with complex quote notation")
  public void testSingleKeyInHocon2() {
    String keyExpression = "\"toCompoundKey({\\\"jobPosting\\\" : toUrn(\\\"jobPosting\\\", key[0]), \\\"member\\\" : toUrn(\\\"member\\\", key[1])})\"";
    String expectedResult = "toCompoundKey({\"jobPosting\" : toUrn(\"jobPosting\", key[0]), \"member\" : toUrn(\"member\", key[1])})";
    List<String> keys = _keyListConverter.extractFromHocon(keyExpression);
    assertEquals(keys, Collections.singletonList(expectedResult));
  }

  @Test(description = "test get single key from invalid HOCON expression", expectedExceptions = ConfigException.class)
  public void testSingleKeyInHocon3() {
    String keyExpression = "toCompoundKey({\"jobPosting\" : toUrn(\"jobPosting\", key[0]), \"member\" : toUrn(\"member\", key[1])})";
    List<String> keys = _keyListConverter.extractFromHocon(keyExpression);
    assertEquals(keys, Collections.singletonList(keyExpression));
  }

  @Test(description = "test get multiple key from HOCON expression")
  public void testMultipleKeyInHocon() {
    String keyExpression = "[\"key1\", \"key2\"]";
    List<String> keys = _keyListConverter.extractFromHocon(keyExpression);
    assertEquals(keys, Arrays.asList("key1", "key2"));
  }

  @Test(description = "test get multiple key from HOCON expression")
  public void testMultipleKeyInHocon2() {
    String keyExpression = "[key1, key2]";
    List<String> keys = _keyListConverter.extractFromHocon(keyExpression);
    assertEquals(keys, Arrays.asList("key1", "key2"));
  }

  @Test(description = "test get empty key from KeyPlaceholderArray")
  public void testEmptyKeyInKeyPlaceholderArray() {
    KeyPlaceholderArray keyPlaceholders = new KeyPlaceholderArray();
    List<String> keys = _keyListConverter.extractFromKeyPlaceholderArray(keyPlaceholders);
    assertEquals(keys.size(), 0);
  }

  @Test(description = "test get single key from KeyPlaceholderArray")
  public void testSingleKeyInKeyPlaceholderArray() {
    KeyPlaceholderArray keyPlaceholders = new KeyPlaceholderArray();
    keyPlaceholders.add(new KeyPlaceholder().setKeyPlaceholderRef("key1"));
    List<String> keys = _keyListConverter.extractFromKeyPlaceholderArray(keyPlaceholders);
    assertEquals(keys, Collections.singletonList("key1"));
  }

  @Test(description = "test get multiple key from KeyPlaceholderArray")
  public void testMultipleKeyInKeyPlaceholderArray() {
    KeyPlaceholderArray keyPlaceholders = new KeyPlaceholderArray();
    keyPlaceholders.add(new KeyPlaceholder().setKeyPlaceholderRef("key1"));
    keyPlaceholders.add(new KeyPlaceholder().setKeyPlaceholderRef("key2"));
    List<String> keys = _keyListConverter.extractFromKeyPlaceholderArray(keyPlaceholders);
    assertEquals(keys, Arrays.asList("key1", "key2"));
  }
}
