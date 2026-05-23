package com.att.tdp.issueflow.exception;

public class TicketDoneException extends RuntimeException {

    public TicketDoneException() {
        super("Cannot update a ticket that is already DONE");
    }
}
