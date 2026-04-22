package tn.esprit.lost_item_service.Exception;

public class DuplicateReportException extends RuntimeException {
    public DuplicateReportException(String message) {
        super(message);
    }
}
