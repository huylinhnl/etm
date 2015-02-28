package com.jecstar.etm.core;

public class EtmException extends RuntimeException {

	private static final long serialVersionUID = 3169443490910515556L;
	
	public static final int WRAPPED_EXCEPTION 				= 100_000;
	public static final int CONFIGURATION_LOAD_EXCEPTION 	= 100_001;
	public static final int UNMARSHALLER_CREATE_EXCEPTION 	= 100_002;

	private int errorCode;

	public EtmException(int errorCode) {
		this(errorCode, null);
	}

	public EtmException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}


	public int getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": Reason " + this.errorCode;
	}
}