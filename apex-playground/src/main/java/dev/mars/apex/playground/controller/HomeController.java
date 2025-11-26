package dev.mars.apex.playground.controller;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle root path redirection.
 * 
 * Redirects requests from the root path (/) to the main playground interface (/playground).
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-26
 * @version 1.0
 */
@Controller
public class HomeController {

    /**
     * Redirect root URL to the playground.
     * 
     * @return Redirect string to /playground
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/playground";
    }
}
