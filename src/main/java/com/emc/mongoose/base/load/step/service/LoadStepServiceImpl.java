package com.emc.mongoose.base.load.step.service;

import static com.emc.mongoose.base.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.base.Constants.KEY_STEP_ID;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;
import static org.apache.logging.log4j.CloseableThreadContext.put;

import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.load.step.LoadStepFactory;
import com.emc.mongoose.base.load.step.LoadStep;
import com.emc.mongoose.base.logging.Loggers;
import com.emc.mongoose.base.metrics.MetricsManager;
import com.emc.mongoose.base.metrics.snapshot.AllMetricsSnapshot;
import com.emc.mongoose.base.svc.ServiceBase;
import com.github.akurilov.confuse.Config;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LoadStepServiceImpl extends ServiceBase implements LoadStepService {

	private final LoadStep localLoadStep;

	public LoadStepServiceImpl(
					final int port,
					final List<Extension> extensions,
					final String stepType,
					final Config baseConfig,
					final List<Config> ctxConfigs,
					final MetricsManager metricsManager) {
		super(port);
		baseConfig.val(
						"load-step-idAutoGenerated",
						false); // don't override the step-id value on the remote node again
		localLoadStep = LoadStepFactory.createLocalLoadStep(
						baseConfig, extensions, ctxConfigs, metricsManager, stepType);
		final String stepId = baseConfig.stringVal("load-step-id");
		try (final Instance logCtx = put(KEY_STEP_ID, stepId).put(KEY_CLASS_NAME, getClass().getSimpleName())) {
			Loggers.MSG.info("New step service for type \"{}\"", stepType);
			super.doStart();
		}
	}

	@Override
	protected final void doStart() {
		try (final Instance logCtx = put(KEY_CLASS_NAME, getClass().getSimpleName()).put(KEY_STEP_ID, localLoadStep.loadStepId())) {
			localLoadStep.start();
			Loggers.MSG.info("Step service for \"{}\" is started", localLoadStep.loadStepId());
		} catch (final RemoteException ignored) {}
	}

	@Override
	protected void doStop() {
		try (final Instance logCtx = put(KEY_CLASS_NAME, getClass().getSimpleName()).put(KEY_STEP_ID, localLoadStep.loadStepId())) {
			localLoadStep.stop();
			Loggers.MSG.info("Step service for \"{}\" is stopped", localLoadStep.loadStepId());
		} catch (final RemoteException ignored) {}
	}

	@Override
	protected final void doClose() throws IOException {
		try (final Instance logCtx = put(KEY_CLASS_NAME, getClass().getSimpleName()).put(KEY_STEP_ID, localLoadStep.loadStepId())) {
			super.doStop();
			localLoadStep.close();
			Loggers.MSG.info("Step service for \"{}\" is closed", localLoadStep.loadStepId());
		}
	}

	@Override
	public String name() {
		return SVC_NAME_PREFIX + hashCode();
	}

	@Override
	public final String loadStepId() throws RemoteException {
		return localLoadStep.loadStepId();
	}

	@Override
	public long runId()
					throws RemoteException {
		return localLoadStep.runId();
	}

	@Override
	public final String getTypeName() throws RemoteException {
		return localLoadStep.getTypeName();
	}

	@Override
	public final List<? extends AllMetricsSnapshot> metricsSnapshots() throws RemoteException {
		return localLoadStep.metricsSnapshots();
	}

	@Override
	public final boolean await(final long timeout, final TimeUnit timeUnit)
					throws IllegalStateException, InterruptedException {
		try (final Instance logCtx = put(KEY_CLASS_NAME, getClass().getSimpleName()).put(KEY_STEP_ID, localLoadStep.loadStepId())) {
			return localLoadStep.await(timeout, timeUnit);
		} catch (final RemoteException ignored) {}
		return false;
	}
}
