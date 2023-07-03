/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.olog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * Handles request exceeding configured sizes (spring.servlet.multipart.max-file-size and
 * spring.servlet.multipart.max-request-size). In such cases client will get an HTTP 413 (payload too large) response
 * with a (hopefully) useful message.
 */
@ControllerAdvice
@SuppressWarnings("unused")
public class FileUploadSizeExceededHandler {

    /**
     * Specifies the allowed origins for CORS requests. Defaults to http://localhost:3000,
     * which is useful during development of the web front-end in NodeJS.
     */
    @Value("${cors.allowed.origins:http://localhost:3000}")
    private String corsAllowedOrigins;


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeExceededException(RuntimeException ex, WebRequest request) {
        // These HTTP headers are needed by browsers in order to handle the 413 response properly.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", corsAllowedOrigins);
        headers.add("Access-Control-Allow-Credentials", "true");
        return new ResponseEntity<>("Log entry exceeds size limits",
                headers,
                HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
