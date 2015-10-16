package util;

public class HttpFormatException extends Exception {
	
	private static final long serialVersionUID = 837910846054497673L;

	public HttpFormatException() {

	}

	public HttpFormatException(String message) {
		super(message);
	}

	public HttpFormatException(Throwable cause) {
		super(cause);
	}

	public HttpFormatException(String message, Throwable cause) {
		super(message, cause);
	}

}
