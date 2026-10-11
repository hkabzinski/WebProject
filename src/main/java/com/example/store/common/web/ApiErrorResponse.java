package com.example.store.common.web;

public record ApiErrorResponse(int status, String error, String message) {
}
