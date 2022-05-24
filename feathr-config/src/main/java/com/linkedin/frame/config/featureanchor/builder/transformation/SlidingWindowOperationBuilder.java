package com.linkedin.frame.config.featureanchor.builder.transformation;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.frame.core.config.TimeWindowAggregationType;
import com.linkedin.frame.core.config.producer.ExprType;
import com.linkedin.frame.core.config.producer.TypedExpr;
import com.linkedin.frame.core.config.producer.anchors.AnchorConfig;
import com.linkedin.frame.core.config.producer.anchors.AnchorConfigWithKey;
import com.linkedin.frame.core.config.producer.anchors.AnchorConfigWithKeyExtractor;
import com.linkedin.frame.core.config.producer.anchors.LateralViewParams;
import com.linkedin.frame.core.config.producer.anchors.TimeWindowFeatureConfig;
import com.linkedin.feathr.featureDataModel.SparkSqlExpression;
import com.linkedin.feathr.featureDataModel.LateralView;
import com.linkedin.feathr.featureDataModel.LateralView.TableGeneratingFunction;
import com.linkedin.feathr.featureDataModel.LateralViewArray;
import com.linkedin.feathr.featureDataModel.Unit;
import com.linkedin.feathr.featureDataModel.Window;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Builder for SlidingWindowOperation (also known as Sliding Window Aggregation). It models how feature value is
 * aggregated from a set of data (called fact data) in a certain interval of time. This builder can be used to build
 * {@link com.linkedin.proml.mlFeatureAnchor.transformation.SlidingWindowAggregation},
 * {@link com.linkedin.proml.mlFeatureAnchor.transformation.SlidingWindowEmbeddingAggregation} and
 * {@link com.linkedin.proml.mlFeatureAnchor.transformation.SlidingWindowLatestAvailable}.
 */
abstract class SlidingWindowOperationBuilder<SLIDING_WINDOW_OPERATION extends RecordTemplate> {
  private Optional<String> _filter = Optional.empty();
  private Optional<String> _groupBy = Optional.empty();
  private Optional<Integer> _limit = Optional.empty();
  private Window _window;
  private String _targetColumn;
  private LateralViewArray _lateralViews;
  private TimeWindowAggregationType _timeWindowAggregationType;

  abstract SLIDING_WINDOW_OPERATION buildSlidingWindowOperationObject(String filter, String groupBy, Integer limit,
      Window window, String targetColumn, LateralViewArray lateralViews, TimeWindowAggregationType aggregationType);

  /**
   * Build SlidingWindowOperation. It sets window, targetColumn, groupBy, limit and aggregationType given
   * {@link TimeWindowFeatureConfig}, and sets lateralViews given {@link AnchorConfig}. Filter comes from either
   * TimeWindowFeatureConfig or AnchorConfig. Setting it in both places will cause exception. Currently, Frame only
   * supports single laterView, but it is modeled as an array for future extensibility.
   */
  public SLIDING_WINDOW_OPERATION build(TimeWindowFeatureConfig timeWindowFeatureConfig, AnchorConfig anchorConfig) {
    _timeWindowAggregationType = timeWindowFeatureConfig.getAggregation();
    _filter = timeWindowFeatureConfig.getTypedFilter().map(
      typedFilter -> {
        if (typedFilter.getExprType() != ExprType.SQL) {
          throw new IllegalArgumentException(String.format("Trying to set filter expr %s with an invalid expression "
                  + "type %s. The only supported type is SQL. Provided feature config is %s", typedFilter.getExpr(),
              typedFilter.getExprType(), timeWindowFeatureConfig));
        }
        return typedFilter.getExpr();
      }
    );
    _groupBy = timeWindowFeatureConfig.getGroupBy();
    _limit = timeWindowFeatureConfig.getLimit();
    _window = buildWindow(timeWindowFeatureConfig.getWindow());
    TypedExpr columnExpr = timeWindowFeatureConfig.getTypedColumnExpr();
    if (columnExpr.getExprType() != ExprType.SQL) {
      throw new IllegalArgumentException(String.format("Trying to set target column expr %s with an invalid expression "
              + "type %s. The only supported type is SQL. Provided feature config is %s", columnExpr.getExpr(),
          columnExpr.getExprType(), timeWindowFeatureConfig));
    }
    _targetColumn = columnExpr.getExpr();
    Optional<LateralViewParams> lateralViewParamsOptional;
    if (anchorConfig instanceof AnchorConfigWithKey) {
      AnchorConfigWithKey anchorConfigWithKey = (AnchorConfigWithKey) anchorConfig;
      lateralViewParamsOptional = anchorConfigWithKey.getLateralViewParams();
    } else if (anchorConfig instanceof AnchorConfigWithKeyExtractor) {
      AnchorConfigWithKeyExtractor anchorConfigWithKeyExtractor = (AnchorConfigWithKeyExtractor) anchorConfig;
      lateralViewParamsOptional = anchorConfigWithKeyExtractor.getLateralViewParams();
    } else {
      lateralViewParamsOptional = Optional.empty();
    }

    if (lateralViewParamsOptional.isPresent()) {
      _lateralViews = buildLateralViews(lateralViewParamsOptional.get());
      //If filter field of lateralView is present and top level filter in feature config is not set yet, we will use the
      //lateralView filter as the SWA filter.
      //lateralView filter and top level filters should not be present at the same time.
      if (lateralViewParamsOptional.get().getFilter().isPresent()) {
        if (_filter.isPresent()) {
          throw new IllegalArgumentException(String.format("Filter present in both feature config %s and "
              + "lateral view %s", timeWindowFeatureConfig, lateralViewParamsOptional.get()));
        } else {
          _filter = lateralViewParamsOptional.get().getFilter();
        }
      }
    } else {
      _lateralViews = new LateralViewArray();
    }

    return buildSlidingWindowOperationObject(_filter.orElse(null), _groupBy.orElse(null),
        _limit.orElse(null), _window, _targetColumn, _lateralViews,
        _timeWindowAggregationType);
  }

  @VisibleForTesting
  protected Window buildWindow(Duration windowDuration) {
    long size = windowDuration.getSeconds();
    Unit unit = Unit.SECOND;
    if (size > 0 && size % 60 == 0) {
      size = size / 60;
      unit = Unit.MINUTE;
      if (size % 60 == 0) {
        size = size / 60;
        unit = Unit.HOUR;
        if (size % 24 == 0) {
          size = size / 24;
          unit = Unit.DAY;
        }
      }
    }
    if (size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(String.format("window size %d too big", size));
    }
    Window window = new Window();
    window.setSize((int) size);
    window.setUnit(unit);
    return window;
  }

  @VisibleForTesting
  protected LateralViewArray buildLateralViews(@Nullable LateralViewParams lateralViewParams) {
    if (lateralViewParams == null) {
      return new LateralViewArray();
    }
    LateralView lateralView = new LateralView();
    lateralView.setVirtualTableAlias(lateralViewParams.getItemAlias());
    TableGeneratingFunction tableGeneratingFunction = new TableGeneratingFunction();
    SparkSqlExpression sparkSqlExpression = new SparkSqlExpression();
    sparkSqlExpression.setSql(lateralViewParams.getDef());
    tableGeneratingFunction.setSparkSqlExpression(sparkSqlExpression);
    lateralView.setTableGeneratingFunction(tableGeneratingFunction);
    List<LateralView> lateralViews = Collections.singletonList(lateralView);
    return new LateralViewArray(lateralViews);
  }
}
