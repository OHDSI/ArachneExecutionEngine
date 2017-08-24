package com.odysseusinc.arachne.executionengine.api.v1;

import com.odysseusinc.arachne.executionengine.util.exception.ErrorInfo;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class ExceptionInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionInfoHandler.class);

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public @ResponseBody ErrorInfo validationError(HttpServletRequest req, MethodArgumentNotValidException ex) {
        log.error(ex.getMessage());
        return new ErrorInfo(req.getRequestURL(), ex);
    }

}
