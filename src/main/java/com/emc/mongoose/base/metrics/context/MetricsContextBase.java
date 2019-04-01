package com.emc.mongoose.base.metrics.context;

import com.emc.mongoose.base.Constants;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.metrics.MetricsConstants;
import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import com.github.akurilov.commons.system.SizeInBytes;

import java.util.HashMap;
import java.util.Map;

import static com.emc.mongoose.base.metrics.MetricsConstants.*;

public abstract class MetricsContextBase<S extends AllMetricsSnapshot>
				implements MetricsContext<S> {

	protected final Map metaData;
	protected final long ts;
	protected final int concurrencyThreshold;
	protected final boolean stdOutColorFlag;
	protected final long outputPeriodMillis;
	private volatile long tsStart = -1;
	private volatile long lastOutputTs = 0;
	private volatile boolean thresholdStateExitedFlag = false;
	protected volatile MetricsContextBase thresholdMetricsCtx = null;
	protected volatile S lastSnapshot = null;


	protected MetricsContextBase(
					final Map metaData,
					final int concurrencyThreshold,
					final boolean stdOutColorFlag,
					final long outputPeriodMillis) {
		ts = System.nanoTime();
		this.metaData = metaData;
		this.concurrencyThreshold = concurrencyThreshold > 0 ? concurrencyThreshold : Integer.MAX_VALUE;
		this.stdOutColorFlag = stdOutColorFlag;
		this.outputPeriodMillis = outputPeriodMillis;
	}

	@Override
	public void start() {
		tsStart = System.currentTimeMillis();
		metaData.put("start_time", tsStart);
	}

	@Override
	public final boolean isStarted() {
		return tsStart > -1;
	}

	@Override
	public final long startTimeStamp() {
		return tsStart;
	}

	@Override
	public final Map metaData(){
		return metaData;
	}

	@Override
	public final String loadStepId() {
		return (String) metaData.get(META_DATA_STEP_ID);
	}

	@Override
	public final String runId() {
		return (String) metaData.get(META_DATA_RUN_ID);
	}

	@Override
	public final OpType opType() {
		return (OpType) metaData.get(META_DATA_OP_TYPE);
	}

	@Override
	public final int concurrencyLimit() {
		return (int) metaData.get(META_DATA_LIMIT_CONC);
	}

	@Override
	public final int concurrencyThreshold() {
		return concurrencyThreshold;
	}

	@Override
	public final SizeInBytes itemDataSize() {
		return (SizeInBytes) metaData.get(META_DATA_ITEM_DATA_SIZE);
	}

	@Override
	public final boolean stdOutColorEnabled() {
		return stdOutColorFlag;
	}

	@Override
	public final long outputPeriodMillis() {
		return outputPeriodMillis;
	}

	@Override
	public final long lastOutputTs() {
		return lastOutputTs;
	}

	@Override
	public final void lastOutputTs(final long ts) {
		lastOutputTs = ts;
	}

	@Override
	public final S lastSnapshot() {
		if (lastSnapshot == null) {
			refreshLastSnapshot();
		}
		return lastSnapshot;
	}

	@Override
	public void refreshLastSnapshot() {
		if (thresholdMetricsCtx != null) {
			thresholdMetricsCtx.refreshLastSnapshot();
		}
	}

	@Override
	public final MetricsContext thresholdMetrics() throws IllegalStateException {
		if (thresholdMetricsCtx == null) {
			throw new IllegalStateException("Nested metrics context is not exist");
		}
		return thresholdMetricsCtx;
	}

	@Override
	public final void enterThresholdState() throws IllegalStateException {
		if (thresholdMetricsCtx != null) {
			throw new IllegalStateException("Nested metrics context already exists");
		}
		thresholdMetricsCtx = newThresholdMetricsContext();
		thresholdMetricsCtx.start();
	}

	protected abstract MetricsContextBase<S> newThresholdMetricsContext();

	@Override
	public final boolean thresholdStateEntered() {
		return thresholdMetricsCtx != null && thresholdMetricsCtx.isStarted();
	}

	@Override
	public final void exitThresholdState() throws IllegalStateException {
		if (thresholdMetricsCtx == null) {
			throw new IllegalStateException("Threshold state was not entered");
		}
		thresholdMetricsCtx.close();
		thresholdStateExitedFlag = true;
	}

	@Override
	public final boolean thresholdStateExited() {
		return thresholdStateExitedFlag;
	}

	@Override
	public final int hashCode() {
		return (int) ts;
	}

	@Override
	public final int compareTo(final MetricsContext<S> other) {
		return Long.compare(hashCode(), other.hashCode());
	}

	@Override
	public void close() {
		tsStart = -1;
		lastSnapshot = null;
		if (thresholdMetricsCtx != null) {
			thresholdMetricsCtx.close();
			thresholdMetricsCtx = null;
		}
	}

	public long elapsedTimeMillis() {
		return (System.currentTimeMillis() - tsStart);
	}

	public String comment() {
		return (String) this.metaData.get(META_DATA_COMMENT);
	}
}
