package com.jecstar.etm.processor.processor;

import com.jecstar.etm.core.TelemetryEvent;
import com.jecstar.etm.core.converter.json.TelemetryEventConverterJsonImpl;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.lmax.disruptor.ExceptionHandler;

public class TelemetryEventExceptionHandler implements ExceptionHandler<TelemetryEvent> {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(TelemetryEventExceptionHandler.class);
	private static final LogWrapper failureLogger = LogFactory.getLogger("etm.processor.failure");
	
	

	@Override
	public void handleEventException(Throwable t, long sequence, TelemetryEvent event) {
		if (log.isErrorLevelEnabled()) {
			log.logErrorMessage("Unable to process event '" + event.id + "'.", t);
		}
		if (failureLogger.isErrorLevelEnabled()) {
			failureLogger.logErrorMessage(new TelemetryEventConverterJsonImpl().convert(event));
		}
	}

	@Override
	public void handleOnStartException(Throwable t) {
		if (log.isFatalLevelEnabled()) {
			log.logFatalMessage("Unable to start disruptor. No events will be processed", t);
		}
	}

	@Override
	public void handleOnShutdownException(Throwable t) {
		if (log.isWarningLevelEnabled()) {
			log.logWarningMessage("Unable to stop disruptor.", t);
		}
	}

}
