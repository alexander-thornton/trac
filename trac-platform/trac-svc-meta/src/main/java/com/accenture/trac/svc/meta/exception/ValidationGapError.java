/*
 * Copyright 2020 Accenture Global Solutions Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.accenture.trac.svc.meta.exception;


/**
 * A validation gap error is a type of internal error, it indicates a condition
 * inside a TRAC component that should have been caught higher up the stack
 * in a validation layer.
 */
public class ValidationGapError extends TracInternalError {

    public ValidationGapError(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationGapError(String message) {
        super(message);
    }
}
